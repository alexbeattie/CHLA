import csv
import os
from decimal import Decimal, InvalidOperation
from django.core.management.base import BaseCommand
from django.conf import settings
from locations.models import Provider, RegionalCenter


class Command(BaseCommand):
    help = "Import CHLA provider and regional center data from CSV files"

    def add_arguments(self, parser):
        parser.add_argument(
            "--providers-file",
            type=str,
            default="data/providers-new.csv",
            help="Path to providers CSV file",
        )
        parser.add_argument(
            "--regional-centers-file",
            type=str,
            default="data/Regional_Centers_with_Coordinates.csv",
            help="Path to regional centers CSV file",
        )
        parser.add_argument(
            "--clear-existing",
            action="store_true",
            help="Clear existing data before importing",
        )

    def handle(self, *args, **options):
        if options["clear_existing"]:
            self.stdout.write("Clearing existing data...")
            Provider.objects.all().delete()
            RegionalCenter.objects.all().delete()
            self.stdout.write(self.style.SUCCESS("Existing data cleared."))

        # Import providers
        providers_file = options["providers_file"]
        if os.path.exists(providers_file):
            self.import_providers(providers_file)
        else:
            self.stdout.write(
                self.style.ERROR(f"Providers file not found: {providers_file}")
            )

        # Import regional centers
        centers_file = options["regional_centers_file"]
        if os.path.exists(centers_file):
            self.import_regional_centers(centers_file)
        else:
            self.stdout.write(
                self.style.ERROR(f"Regional centers file not found: {centers_file}")
            )

    def safe_decimal(self, value):
        """Safely convert string to Decimal, return None if invalid"""
        if not value or value.strip() == "":
            return None
        try:
            return Decimal(value.strip())
        except (InvalidOperation, ValueError):
            return None

    def import_providers(self, file_path):
        self.stdout.write(f"Importing providers from {file_path}...")

        created_count = 0
        updated_count = 0

        with open(file_path, "r", encoding="utf-8") as csvfile:
            reader = csv.DictReader(csvfile)

            for row in reader:
                provider_id = row.get("id")
                if not provider_id:
                    continue

                # Clean website URL
                website = row.get("website_domain", "").strip()
                if website and not website.startswith("http"):
                    website = f"http://{website}"

                # Convert areas to array format for PostgreSQL
                areas_str = row.get("areas", "").strip()
                if areas_str:
                    # Split by comma and clean up, then format as PostgreSQL array
                    areas_list = [
                        area.strip() for area in areas_str.split(",") if area.strip()
                    ]
                    areas_array = (
                        "{" + ",".join(f'"{area}"' for area in areas_list) + "}"
                    )
                else:
                    areas_array = None

                provider_data = {
                    "name": row.get("name", "").strip(),
                    "phone": row.get("phone", "").strip(),
                    "address": row.get("address", "").strip(),
                    "website_domain": website,
                    "latitude": self.safe_decimal(row.get("latitude")),
                    "longitude": self.safe_decimal(row.get("longitude")),
                    "center_based_services": row.get(
                        "center_based_services", ""
                    ).strip(),
                    "specializations": row.get("specializations", "").strip(),
                    "insurance_accepted": row.get("insurance_accepted", "").strip(),
                    "services": row.get("services", "").strip(),
                    "coverage_areas": row.get(
                        "areas", ""
                    ).strip(),  # For backward compatibility
                }

                # Remove empty string values, replace with None
                for key, value in provider_data.items():
                    if value == "":
                        provider_data[key] = None

                # Handle the areas array field separately using raw SQL
                provider, created = Provider.objects.update_or_create(
                    id=provider_id, defaults=provider_data
                )

                # Update the areas field separately with raw SQL to handle array type
                if areas_array is not None:
                    from django.db import connection

                    cursor = connection.cursor()
                    cursor.execute(
                        "UPDATE providers SET areas = %s WHERE id = %s",
                        [areas_array, provider_id],
                    )

                if created:
                    created_count += 1
                else:
                    updated_count += 1

        self.stdout.write(
            self.style.SUCCESS(
                f"Providers import complete: {created_count} created, {updated_count} updated"
            )
        )

    def import_regional_centers(self, file_path):
        self.stdout.write(f"Importing regional centers from {file_path}...")

        created_count = 0
        updated_count = 0

        with open(file_path, "r", encoding="utf-8") as csvfile:
            reader = csv.DictReader(csvfile)

            for row in reader:
                center_id = row.get("id")
                if not center_id:
                    continue

                # Clean website URL
                website = row.get("website", "").strip()
                if website and not website.startswith("http"):
                    website = f"http://{website}"

                center_data = {
                    "regional_center": row.get("regional_center", "").strip(),
                    "office_type": row.get("office_type", "").strip(),
                    "address": row.get("address", "").strip(),
                    "suite": row.get("suite", "").strip(),
                    "city": row.get("city", "").strip(),
                    "state": row.get("state", "").strip(),
                    "zip_code": row.get("zip_code", "").strip(),
                    "telephone": row.get("telephone", "").strip(),
                    "website": website,
                    "county_served": row.get("county_served", "").strip(),
                    "los_angeles_health_district": row.get(
                        "los_angeles_health_district", ""
                    ).strip(),
                    "latitude": self.safe_decimal(row.get("latitude")),
                    "longitude": self.safe_decimal(row.get("longitude")),
                }

                # Remove empty string values, replace with None
                for key, value in center_data.items():
                    if value == "":
                        center_data[key] = None

                center, created = RegionalCenter.objects.update_or_create(
                    id=center_id, defaults=center_data
                )

                if created:
                    created_count += 1
                else:
                    updated_count += 1

        self.stdout.write(
            self.style.SUCCESS(
                f"Regional centers import complete: {created_count} created, {updated_count} updated"
            )
        )
