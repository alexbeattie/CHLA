"""
Import Los Angeles Provider List CSV into ProviderV2 model with proper normalization.
Handles geocoding, insurance parsing, and therapy type extraction.

Usage:
    python manage.py import_la_providers /path/to/csv_file.csv
"""

import csv
import re
from django.core.management.base import BaseCommand
from django.db import transaction
from locations.models import ProviderV2, InsuranceCarrier, ProviderInsuranceCarrier
from locations.utils.geocode import geocode_address
from decimal import Decimal


class Command(BaseCommand):
    help = 'Import providers from Los Angeles Provider List CSV'

    def add_arguments(self, parser):
        parser.add_argument('csv_file', type=str, help='Path to CSV file')
        parser.add_argument(
            '--dry-run',
            action='store_true',
            help='Preview import without saving to database'
        )

    def handle(self, *args, **options):
        csv_file = options['csv_file']
        dry_run = options.get('dry_run', False)

        if dry_run:
            self.stdout.write(self.style.WARNING('🔍 DRY RUN MODE - No data will be saved'))

        self.stdout.write(f'📂 Reading CSV file: {csv_file}')

        # Statistics
        stats = {
            'total': 0,
            'created': 0,
            'updated': 0,
            'skipped': 0,
            'geocoded': 0,
            'geocode_failed': 0,
            'insurance_created': 0
        }

        # Read CSV file
        with open(csv_file, 'r', encoding='utf-8') as f:
            # Read raw lines to handle multi-line addresses
            reader = csv.reader(f)
            header = next(reader)  # Skip header: Provider Name,Address,Services,Insurance,Phone,

            for row in reader:
                if not row or len(row) < 5:
                    continue

                # CSV columns: Provider Name, Address, Services, Insurance, Phone
                name = row[0].strip() if row[0] else None
                address = row[1].strip() if row[1] else None
                services = row[2].strip() if row[2] else None
                insurance_text = row[3].strip() if row[3] else None
                phone = row[4].strip() if row[4] else None

                if not name:
                    continue

                stats['total'] += 1

                self.stdout.write(f'\n📋 Processing: {name}')

                # Parse services to extract therapy types
                therapy_types = self.parse_therapy_types(services)

                # Parse insurance carriers
                insurance_carriers = self.parse_insurance(insurance_text)

                # Clean and format address
                clean_address = self.clean_address(address) if address else None

                # Geocode address with enhanced strategies
                latitude = None
                longitude = None
                if clean_address:
                    from locations.utils.geocode import clean_address_aggressive, geocode_with_nominatim

                    # Try all address variations
                    variations = clean_address_aggressive(clean_address)
                    coords = None
                    successful_variant = None

                    for i, variant in enumerate(variations):
                        delay = 1.0 if i == 0 else 1.5
                        coords = geocode_with_nominatim(variant, delay=delay)
                        if coords:
                            successful_variant = variant
                            break

                    if coords:
                        latitude, longitude = coords
                        stats['geocoded'] += 1
                        if successful_variant != clean_address:
                            self.stdout.write(f'  📍 Geocoded: {latitude}, {longitude}')
                            self.stdout.write(f'      Used variant: {successful_variant}')
                        else:
                            self.stdout.write(f'  📍 Geocoded: {latitude}, {longitude}')
                    else:
                        stats['geocode_failed'] += 1
                        stats['skipped'] += 1
                        self.stdout.write(self.style.WARNING(f'  ⚠️  Geocoding failed after {len(variations)} attempts, skipping provider'))
                        self.stdout.write(f'      Original address: {clean_address}')
                        continue

                # Prepare provider data
                provider_data = {
                    'name': name,
                    'type': 'Service Provider',
                    'phone': self.clean_phone(phone) if phone else None,
                    'address': clean_address,
                    'latitude': Decimal(str(latitude)) if latitude else None,
                    'longitude': Decimal(str(longitude)) if longitude else None,
                    'therapy_types': therapy_types,
                    'insurance_accepted': insurance_text or '',  # Legacy field
                }

                if not dry_run:
                    # Create or update provider
                    with transaction.atomic():
                        # Use name + address for matching to handle providers with same name
                        if clean_address:
                            provider, created = ProviderV2.objects.update_or_create(
                                name=name,
                                address=clean_address,
                                defaults=provider_data
                            )
                        else:
                            provider, created = ProviderV2.objects.update_or_create(
                                name=name,
                                defaults=provider_data
                            )

                        if created:
                            stats['created'] += 1
                            self.stdout.write(self.style.SUCCESS(f'  ✅ Created provider'))
                        else:
                            stats['updated'] += 1
                            self.stdout.write(self.style.SUCCESS(f'  🔄 Updated provider'))

                        # Create insurance carrier relationships
                        if insurance_carriers:
                            self.create_insurance_relationships(provider, insurance_carriers, stats)
                else:
                    self.stdout.write(f'  🔍 Would create/update: {name}')
                    self.stdout.write(f'     Therapies: {therapy_types}')
                    self.stdout.write(f'     Insurance: {insurance_carriers}')

        # Print summary
        self.stdout.write('\n' + '='*60)
        self.stdout.write(self.style.SUCCESS('📊 IMPORT SUMMARY'))
        self.stdout.write('='*60)
        self.stdout.write(f'Total providers processed: {stats["total"]}')
        self.stdout.write(f'Created: {stats["created"]}')
        self.stdout.write(f'Updated: {stats["updated"]}')
        self.stdout.write(f'Skipped: {stats["skipped"]}')
        self.stdout.write(f'Geocoded successfully: {stats["geocoded"]}')
        self.stdout.write(f'Geocoding failed: {stats["geocode_failed"]}')
        self.stdout.write(f'Insurance carriers created: {stats["insurance_created"]}')
        self.stdout.write('='*60)

        if dry_run:
            self.stdout.write(self.style.WARNING('\n🔍 DRY RUN COMPLETE - No data was saved'))
            self.stdout.write('Run without --dry-run to import data')
        else:
            self.stdout.write(self.style.SUCCESS('\n✅ IMPORT COMPLETE'))

    def clean_address(self, address):
        """Clean and format address from CSV"""
        if not address:
            return None

        # Remove extra whitespace and newlines
        address = ' '.join(address.split())

        # Fix common address formatting issues
        # Replace comma before suite/unit/apt with space
        address = re.sub(r',\s*(Suite|Unit|Apt|#|Ste|STE)', r' \1', address, flags=re.IGNORECASE)

        # Ensure there's a comma after street address before city
        # Pattern: number + street name + city
        # e.g., "123 Main St Los Angeles" -> "123 Main St, Los Angeles"
        # Look for CA followed by zip code
        address = re.sub(r'\s+CA\s+', ', CA ', address)

        return address

    def clean_phone(self, phone):
        """Clean phone number"""
        if not phone:
            return None

        # Remove non-digit characters except + and - and ()
        phone = phone.strip()
        # Basic formatting - keep as-is for now
        return phone

    def parse_therapy_types(self, services):
        """Extract therapy types from services string"""
        if not services:
            return []

        therapy_types = []
        services_upper = services.upper()

        # Common therapy type mappings
        therapy_map = {
            'ABA': 'ABA Therapy',
            'OT': 'Occupational Therapy',
            'PT': 'Physical Therapy',
            'ST': 'Speech Therapy',
            'SPEECH': 'Speech Therapy',
            'FEEDING': 'Feeding Therapy',
        }

        for key, value in therapy_map.items():
            if key in services_upper:
                therapy_types.append(value)

        return therapy_types if therapy_types else None

    def parse_insurance(self, insurance_text):
        """Parse insurance carriers from text"""
        if not insurance_text:
            return []

        insurance_carriers = []

        # Common insurance carrier names to look for
        carriers = [
            'Medi-Cal', 'Medicaid', 'Medicare',
            'Aetna', 'Anthem Blue Cross', 'Blue Shield', 'Blue Cross',
            'Cigna', 'Kaiser Permanente', 'Kaiser',
            'United Healthcare', 'UnitedHealthcare', 'Optum',
            'Magellan', 'Magellan Health',
            'Tricare', 'TriWest', 'TRICARE',
            'Humana', 'Healthnet', 'Health Net',
            'LA Care', 'L.A. Care',
            'PPO', 'HMO', 'EPO',
            'Regional Center',
        ]

        insurance_upper = insurance_text.upper()

        for carrier in carriers:
            if carrier.upper() in insurance_upper:
                insurance_carriers.append(carrier)

        # Check for "Private Insurance" or "Private Pay"
        if 'PRIVATE' in insurance_upper:
            if 'insurance' not in [c.lower() for c in insurance_carriers]:
                insurance_carriers.append('Private Insurance')

        return insurance_carriers

    def create_insurance_relationships(self, provider, insurance_names, stats):
        """Create ProviderInsuranceCarrier relationships"""
        for insurance_name in insurance_names:
            # Get or create insurance carrier
            carrier, created = InsuranceCarrier.objects.get_or_create(
                name=insurance_name,
                defaults={'description': f'Imported from CSV'}
            )

            if created:
                stats['insurance_created'] += 1

            # Create relationship if it doesn't exist
            ProviderInsuranceCarrier.objects.get_or_create(
                provider=provider,
                insurance_carrier=carrier
            )
