"""
Import providers from CSV file to database
"""
from django.core.management.base import BaseCommand
from django.contrib.gis.geos import Point
from locations.models import ProviderV2
from decimal import Decimal
import csv
import json
from datetime import datetime


class Command(BaseCommand):
    help = 'Import providers from CSV file'

    def add_arguments(self, parser):
        parser.add_argument('csv_file', type=str, help='Path to CSV file')
        parser.add_argument('--dry-run', action='store_true', help='Dry run without saving')

    def handle(self, *args, **options):
        csv_file = options['csv_file']
        dry_run = options.get('dry_run', False)

        self.stdout.write(f"Importing providers from: {csv_file}")
        if dry_run:
            self.stdout.write(self.style.WARNING("DRY RUN MODE - No changes will be saved"))
        
        with open(csv_file, 'r', encoding='utf-8') as f:
            reader = csv.DictReader(f)
            providers = list(reader)
        
        self.stdout.write(f"Found {len(providers)} providers in CSV")
        
        created = 0
        updated = 0
        skipped = 0
        errors = []
        
        for i, row in enumerate(providers, 1):
            try:
                provider_id = row['id']
                
                # Check if provider exists
                existing = ProviderV2.objects.filter(id=provider_id).first()

                # Parse coordinates
                lat = Decimal(str(row['latitude'])) if row['latitude'] else Decimal('0.0')
                lng = Decimal(str(row['longitude'])) if row['longitude'] else Decimal('0.0')
                
                # Create PostGIS Point
                location = None
                if lat and lng and (lat != Decimal('0.0') or lng != Decimal('0.0')):
                    location = Point(float(lng), float(lat), srid=4326)
                
                # Parse JSON fields
                therapy_types = []
                if row.get('therapy_types'):
                    try:
                        therapy_types = json.loads(row['therapy_types'])
                    except:
                        therapy_types = []
                
                age_groups = []
                if row.get('age_groups'):
                    try:
                        age_groups = json.loads(row['age_groups'])
                    except:
                        age_groups = []
                
                diagnoses_treated = []
                if row.get('diagnoses_treated'):
                try:
                        diagnoses_treated = json.loads(row['diagnoses_treated'])
                    except:
                        diagnoses_treated = []
                
                # Insurance - handle both dict and string formats
                insurance_accepted = row.get('insurance_accepted', '')
                if insurance_accepted:
                    try:
                        if insurance_accepted.startswith('{'):
                            insurance_accepted = json.loads(insurance_accepted)
                        # Keep as string if it's already plain text
                    except:
                        pass  # Keep original string
                
                # Prepare data
                data = {
                    'name': row['name'],
                    'type': row.get('type', ''),
                    'phone': row.get('phone', ''),
                    'email': row.get('email', ''),
                    'website': row.get('website', ''),
                    'description': row.get('description', ''),
                    'latitude': lat,
                    'longitude': lng,
                    'location': location,
                    'address': row.get('address', ''),
                    'insurance_accepted': insurance_accepted,
                    'therapy_types': therapy_types,
                    'age_groups': age_groups,
                    'diagnoses_treated': diagnoses_treated,
                }
                
                if not dry_run:
                    if existing:
                        # Update existing
                        for key, value in data.items():
                            setattr(existing, key, value)
                        existing.save()
                        updated += 1
                        self.stdout.write(f"  Updated: {row['name'][:50]}")
                    else:
                        # Create new
                        provider = ProviderV2.objects.create(
                            id=provider_id,
                            **data
                        )
                        created += 1
                        self.stdout.write(self.style.SUCCESS(f"  ✅ Created: {row['name'][:50]}"))
                else:
                    if existing:
                        self.stdout.write(f"  [DRY] Would update: {row['name'][:50]}")
                        updated += 1
                        else:
                        self.stdout.write(f"  [DRY] Would create: {row['name'][:50]}")
                        created += 1

                except Exception as e:
                error_msg = f"Row {i} ({row.get('name', 'Unknown')[:30]}): {str(e)}"
                errors.append(error_msg)
                self.stdout.write(self.style.ERROR(f"  ❌ {error_msg}"))
                skipped += 1

        # Summary
        self.stdout.write("\n" + "="*60)
        self.stdout.write(self.style.SUCCESS(f"✅ Created: {created}"))
        self.stdout.write(self.style.WARNING(f"📝 Updated: {updated}"))
        self.stdout.write(self.style.ERROR(f"❌ Errors: {skipped}"))
        
        if errors:
            self.stdout.write("\n=== Errors ===")
            for error in errors:
                self.stdout.write(self.style.ERROR(error))
        
        if not dry_run and (created > 0 or updated > 0):
            self.stdout.write(self.style.SUCCESS(f"\n✅ Successfully imported {created + updated} providers"))
            self.stdout.write(f"\nNext step: Sync to RDS")
            self.stdout.write(f"  python3 manage.py sync_to_rds")
