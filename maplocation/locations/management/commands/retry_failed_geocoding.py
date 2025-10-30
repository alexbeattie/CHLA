"""
Retry geocoding for providers that failed in the initial import.
Uses enhanced geocoding strategies to capture previously failed addresses.

Usage:
    python manage.py retry_failed_geocoding /path/to/csv_file.csv
"""

import csv
import re
from django.core.management.base import BaseCommand
from django.db import transaction
from locations.models import ProviderV2, InsuranceCarrier, ProviderInsuranceCarrier
from locations.utils.geocode import clean_address_aggressive, geocode_with_nominatim
from decimal import Decimal


class Command(BaseCommand):
    help = 'Retry geocoding for providers that failed in initial import'

    def add_arguments(self, parser):
        parser.add_argument('csv_file', type=str, help='Path to CSV file')
        parser.add_argument(
            '--dry-run',
            action='store_true',
            help='Preview import without saving to database'
        )
        parser.add_argument(
            '--verbose',
            action='store_true',
            help='Show all address variations being tried'
        )

    def handle(self, *args, **options):
        csv_file = options['csv_file']
        dry_run = options.get('dry_run', False)
        verbose = options.get('verbose', False)

        if dry_run:
            self.stdout.write(self.style.WARNING('🔍 DRY RUN MODE - No data will be saved'))

        self.stdout.write(f'📂 Reading CSV file: {csv_file}')
        self.stdout.write('🔄 Only processing providers that previously failed geocoding...\n')

        # Statistics
        stats = {
            'total_processed': 0,
            'already_exists': 0,
            'geocoded_success': 0,
            'still_failed': 0,
            'created': 0,
            'insurance_created': 0
        }

        # Read CSV file
        with open(csv_file, 'r', encoding='utf-8') as f:
            reader = csv.reader(f)
            header = next(reader)  # Skip header

            for row in reader:
                if not row or len(row) < 5:
                    continue

                # CSV columns: Provider Name, Address, Services, Insurance, Phone
                name = row[0].strip() if row[0] else None
                address = row[1].strip() if row[1] else None
                services = row[2].strip() if row[2] else None
                insurance_text = row[3].strip() if row[3] else None
                phone = row[4].strip() if row[4] else None

                if not name or not address:
                    continue

                # Clean address
                clean_address = self.clean_address(address)

                # Check if provider already exists with valid coordinates
                existing = ProviderV2.objects.filter(
                    name=name,
                    address=clean_address
                ).first()

                if existing and existing.latitude and existing.longitude and \
                   existing.latitude != Decimal('0.0') and existing.longitude != Decimal('0.0'):
                    stats['already_exists'] += 1
                    continue

                # This provider either doesn't exist or failed geocoding
                stats['total_processed'] += 1
                self.stdout.write(f'\n📋 Processing: {name}')

                # Try enhanced geocoding
                variations = clean_address_aggressive(clean_address)

                if verbose:
                    self.stdout.write(f'  🔍 Trying {len(variations)} address variations:')
                    for i, var in enumerate(variations, 1):
                        self.stdout.write(f'      {i}. {var}')

                coords = None
                successful_variant = None

                for i, variant in enumerate(variations):
                    delay = 1.0 if i == 0 else 1.5
                    coords = geocode_with_nominatim(variant, delay=delay)
                    if coords:
                        successful_variant = variant
                        if verbose:
                            self.stdout.write(f'  ✅ Match found on attempt {i+1}!')
                        break

                if coords:
                    latitude, longitude = coords
                    stats['geocoded_success'] += 1
                    self.stdout.write(self.style.SUCCESS(f'  📍 Geocoded: {latitude}, {longitude}'))
                    if successful_variant != clean_address:
                        self.stdout.write(f'      Strategy worked: {successful_variant}')

                    # Parse services and insurance
                    therapy_types = self.parse_therapy_types(services)
                    insurance_carriers = self.parse_insurance(insurance_text)

                    # Prepare provider data
                    provider_data = {
                        'name': name,
                        'type': 'Service Provider',
                        'phone': self.clean_phone(phone) if phone else None,
                        'address': clean_address,
                        'latitude': Decimal(str(latitude)),
                        'longitude': Decimal(str(longitude)),
                        'therapy_types': therapy_types,
                        'insurance_accepted': insurance_text or '',
                    }

                    if not dry_run:
                        # Create or update provider
                        with transaction.atomic():
                            provider, created = ProviderV2.objects.update_or_create(
                                name=name,
                                address=clean_address,
                                defaults=provider_data
                            )

                            if created:
                                stats['created'] += 1
                                self.stdout.write(self.style.SUCCESS(f'  ✅ Created provider'))
                            else:
                                self.stdout.write(self.style.SUCCESS(f'  🔄 Updated provider with coordinates'))

                            # Create insurance carrier relationships
                            if insurance_carriers:
                                self.create_insurance_relationships(provider, insurance_carriers, stats)
                    else:
                        self.stdout.write(f'  🔍 Would create/update provider')
                else:
                    stats['still_failed'] += 1
                    self.stdout.write(self.style.ERROR(f'  ❌ Still failed after {len(variations)} attempts'))
                    if verbose:
                        self.stdout.write(f'      Original: {clean_address}')

        # Print summary
        self.stdout.write('\n' + '='*60)
        self.stdout.write(self.style.SUCCESS('📊 RETRY SUMMARY'))
        self.stdout.write('='*60)
        self.stdout.write(f'Providers processed: {stats["total_processed"]}')
        self.stdout.write(f'Already had valid coordinates: {stats["already_exists"]}')
        self.stdout.write(f'Successfully geocoded: {stats["geocoded_success"]}')
        self.stdout.write(f'Still failed: {stats["still_failed"]}')
        self.stdout.write(f'Providers created: {stats["created"]}')
        self.stdout.write(f'Insurance carriers created: {stats["insurance_created"]}')
        self.stdout.write('='*60)

        if stats['geocoded_success'] > 0:
            success_rate = (stats['geocoded_success'] / stats['total_processed'] * 100) if stats['total_processed'] > 0 else 0
            self.stdout.write(self.style.SUCCESS(f'\n✨ Recovered {stats["geocoded_success"]} providers ({success_rate:.1f}% of failures)'))

        if dry_run:
            self.stdout.write(self.style.WARNING('\n🔍 DRY RUN COMPLETE - No data was saved'))
            self.stdout.write('Run without --dry-run to import data')
        else:
            self.stdout.write(self.style.SUCCESS('\n✅ RETRY COMPLETE'))

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
        address = re.sub(r'\s+CA\s+', ', CA ', address)

        return address

    def clean_phone(self, phone):
        """Clean phone number"""
        if not phone:
            return None
        return phone.strip()

    def parse_therapy_types(self, services):
        """Extract therapy types from services string"""
        if not services:
            return []

        therapy_types = []
        services_upper = services.upper()

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

        if 'PRIVATE' in insurance_upper:
            if 'insurance' not in [c.lower() for c in insurance_carriers]:
                insurance_carriers.append('Private Insurance')

        return insurance_carriers

    def create_insurance_relationships(self, provider, insurance_names, stats):
        """Create ProviderInsuranceCarrier relationships"""
        for insurance_name in insurance_names:
            carrier, created = InsuranceCarrier.objects.get_or_create(
                name=insurance_name,
                defaults={'description': f'Imported from CSV'}
            )

            if created:
                stats['insurance_created'] += 1

            ProviderInsuranceCarrier.objects.get_or_create(
                provider=provider,
                insurance_carrier=carrier
            )
