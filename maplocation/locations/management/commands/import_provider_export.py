"""
Import providers from export CSV file with Age Groups and Diagnoses Treated fields.
This script handles the providers_export_new.csv format.
"""

import csv
from decimal import Decimal
from django.core.management.base import BaseCommand
from django.contrib.gis.geos import Point
from locations.models import ProviderV2, InsuranceCarrier, ProviderInsuranceCarrier


class Command(BaseCommand):
    help = "Import providers from export CSV file (providers_export_new.csv format)"

    def add_arguments(self, parser):
        parser.add_argument(
            "csv_file",
            type=str,
            help="Path to the CSV file to import",
        )
        parser.add_argument(
            "--dry-run",
            action="store_true",
            help="Preview what would be imported without saving to database",
        )
        parser.add_argument(
            "--update",
            action="store_true",
            help="Update existing providers by ID instead of skipping them",
        )

    def handle(self, *args, **options):
        csv_file_path = options["csv_file"]
        dry_run = options["dry_run"]
        update_existing = options["update"]

        self.stdout.write(self.style.SUCCESS(f"Reading CSV file: {csv_file_path}"))

        if dry_run:
            self.stdout.write(self.style.WARNING("DRY RUN MODE - No changes will be saved"))

        created_count = 0
        updated_count = 0
        skipped_count = 0
        error_count = 0

        with open(csv_file_path, "r", encoding="utf-8") as file:
            # Use DictReader to handle CSV with headers
            reader = csv.DictReader(file)

            for row_num, row in enumerate(reader, start=2):  # start=2 because row 1 is header
                # Skip empty rows
                if not row.get("ID") or not row.get("Name"):
                    skipped_count += 1
                    continue

                try:
                    provider_id = row["ID"].strip()
                    name = row["Name"].strip()

                    # Check if provider exists
                    existing_provider = None
                    if provider_id:
                        try:
                            existing_provider = ProviderV2.objects.get(id=provider_id)
                            if not update_existing:
                                self.stdout.write(f"  Skipping existing provider: {name}")
                                skipped_count += 1
                                continue
                        except ProviderV2.DoesNotExist:
                            pass

                    # Parse latitude and longitude
                    try:
                        latitude = Decimal(row.get("Latitude", "0").strip() or "0")
                        longitude = Decimal(row.get("Longitude", "0").strip() or "0")
                    except (ValueError, TypeError):
                        latitude = Decimal("0")
                        longitude = Decimal("0")

                    # Parse therapy types
                    therapy_types_str = row.get("Therapy Types", "").strip()
                    therapy_types = []
                    if therapy_types_str:
                        # Split by comma and normalize
                        raw_types = [t.strip() for t in therapy_types_str.split(",")]
                        for therapy_type in raw_types:
                            # Normalize therapy type names
                            normalized = self.normalize_therapy_type(therapy_type)
                            if normalized and normalized not in therapy_types:
                                therapy_types.append(normalized)

                    # Parse insurance carriers
                    insurance_str = row.get("Insurance Carriers", "").strip()
                    insurance_carriers = []
                    if insurance_str:
                        insurance_carriers = [i.strip() for i in insurance_str.split(",") if i.strip()]

                    # Parse age groups
                    age_groups_str = row.get("Age Groups", "").strip()
                    age_groups = []
                    if age_groups_str:
                        age_groups = [ag.strip() for ag in age_groups_str.split(",") if ag.strip()]

                    # Parse diagnoses treated
                    diagnoses_str = row.get("Diagnoses Treated", "").strip()
                    diagnoses_treated = []
                    if diagnoses_str:
                        diagnoses_treated = [d.strip() for d in diagnoses_str.split(",") if d.strip()]

                    # Build address from components
                    address = row.get("Address", "").strip()
                    if not address:
                        # Build from city, state, zip if address is empty
                        city = row.get("City", "").strip()
                        state = row.get("State", "").strip()
                        zip_code = row.get("Zip Code", "").strip()
                        if city or state or zip_code:
                            address = f"{city}, {state} {zip_code}".strip()

                    # Prepare provider data
                    provider_data = {
                        "name": name,
                        "type": row.get("Type", "").strip() or "Service Provider",
                        "phone": row.get("Phone", "").strip() or None,
                        "email": row.get("Email", "").strip() or None,
                        "website": row.get("Website", "").strip() or None,
                        "address": address,
                        "latitude": latitude,
                        "longitude": longitude,
                        "therapy_types": therapy_types if therapy_types else None,
                        "age_groups": age_groups if age_groups else None,
                        "diagnoses_treated": diagnoses_treated if diagnoses_treated else None,
                    }

                    # Create PostGIS point if coordinates exist
                    if latitude != Decimal("0") and longitude != Decimal("0"):
                        provider_data["location"] = Point(float(longitude), float(latitude), srid=4326)

                    if dry_run:
                        action = "UPDATE" if existing_provider else "CREATE"
                        self.stdout.write(
                            f"  [{action}] {name} - "
                            f"Therapies: {len(therapy_types)}, "
                            f"Insurance: {len(insurance_carriers)}, "
                            f"Age Groups: {len(age_groups)}, "
                            f"Diagnoses: {len(diagnoses_treated)}"
                        )
                        if age_groups:
                            self.stdout.write(f"    Age Groups: {', '.join(age_groups)}")
                        if diagnoses_treated:
                            self.stdout.write(f"    Diagnoses: {', '.join(diagnoses_treated)}")
                    else:
                        # Create or update provider
                        if existing_provider:
                            # Update existing provider
                            for key, value in provider_data.items():
                                setattr(existing_provider, key, value)
                            existing_provider.save()
                            provider = existing_provider
                            updated_count += 1
                            self.stdout.write(self.style.SUCCESS(f"  ✓ Updated: {name}"))
                        else:
                            # Create new provider with explicit ID
                            if provider_id:
                                provider_data["id"] = provider_id
                            provider = ProviderV2.objects.create(**provider_data)
                            created_count += 1
                            self.stdout.write(self.style.SUCCESS(f"  ✓ Created: {name}"))

                        # Handle insurance carriers (only if not dry run)
                        if insurance_carriers:
                            # Clear existing carrier relationships
                            provider.provider_insurance_carriers.all().delete()
                            # Add new carrier relationships
                            for carrier_name in insurance_carriers:
                                carrier, _ = InsuranceCarrier.objects.get_or_create(name=carrier_name)
                                ProviderInsuranceCarrier.objects.get_or_create(
                                    provider=provider,
                                    insurance_carrier=carrier
                                )

                except Exception as e:
                    error_count += 1
                    self.stdout.write(
                        self.style.ERROR(f"  ✗ Error on row {row_num}: {str(e)}")
                    )
                    if options.get("verbosity", 1) > 1:
                        import traceback
                        self.stdout.write(traceback.format_exc())

        # Summary
        self.stdout.write("\n" + "=" * 60)
        self.stdout.write(self.style.SUCCESS(f"Import {'preview' if dry_run else 'complete'}!"))
        if not dry_run:
            self.stdout.write(f"  Created: {created_count}")
            self.stdout.write(f"  Updated: {updated_count}")
        self.stdout.write(f"  Skipped: {skipped_count}")
        self.stdout.write(f"  Errors:  {error_count}")
        self.stdout.write("=" * 60)

    def normalize_therapy_type(self, therapy_type):
        """Normalize therapy type names to match model choices"""
        therapy_type = therapy_type.strip()

        # Map common variations to standard names
        mapping = {
            "aba therapy": "ABA therapy",
            "aba": "ABA therapy",
            "speech therapy": "Speech therapy",
            "speech": "Speech therapy",
            "occupational therapy": "Occupational therapy",
            "ot": "Occupational therapy",
            "physical therapy": "Physical therapy",
            "pt": "Physical therapy",
            "feeding therapy": "Feeding therapy",
            "feeding": "Feeding therapy",
        }

        normalized = mapping.get(therapy_type.lower(), therapy_type)
        return normalized
