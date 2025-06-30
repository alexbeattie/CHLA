import csv
import os
import io
from django.core.management.base import BaseCommand, CommandError
from locations.models import Provider
from decimal import Decimal, InvalidOperation


class Command(BaseCommand):
    help = "Import providers from updated-providers-csv.txt file"

    def add_arguments(self, parser):
        parser.add_argument(
            "--file",
            type=str,
            default="data/updated-providers-csv.txt",
            help="Path to the providers text file (default: data/updated-providers-csv.txt)",
        )
        parser.add_argument(
            "--clear",
            action="store_true",
            help="Clear existing providers before importing",
        )

    def handle(self, *args, **options):
        file_path = options["file"]

        # Check if file exists
        if not os.path.exists(file_path):
            raise CommandError(f'File "{file_path}" does not exist.')

        if options["clear"]:
            self.stdout.write("Clearing existing providers...")
            Provider.objects.all().delete()
            self.stdout.write(self.style.SUCCESS("Existing providers cleared."))

        # Read and import data
        imported_count = 0
        skipped_count = 0
        error_count = 0

        self.stdout.write(f"Reading providers from {file_path}...")

        try:
            with open(file_path, "r", encoding="utf-8") as file:
                # Use Python's CSV reader which handles quoted strings properly
                csv_reader = csv.DictReader(file)

                # Get the field names from the CSV header
                fieldnames = csv_reader.fieldnames
                self.stdout.write(f"Found CSV headers: {fieldnames}")

                for line_num, row in enumerate(
                    csv_reader, 2
                ):  # Start at 2 since header is line 1
                    try:
                        # Clean the data - remove empty fields and strip whitespace
                        cleaned_row = {}
                        for key, value in row.items():
                            if key and key.strip():  # Skip empty header columns
                                cleaned_key = key.strip()
                                cleaned_value = value.strip() if value else ""
                                cleaned_row[cleaned_key] = cleaned_value

                        # Extract required fields
                        provider_id = cleaned_row.get("id", "").strip()
                        name = cleaned_row.get("name", "").strip()

                        if not name:
                            self.stdout.write(
                                self.style.WARNING(
                                    f"Line {line_num}: Missing name. Skipping."
                                )
                            )
                            skipped_count += 1
                            continue

                        # Parse coordinates
                        latitude = self.parse_coordinate(
                            cleaned_row.get("latitude", "")
                        )
                        longitude = self.parse_coordinate(
                            cleaned_row.get("longitude", "")
                        )

                        # Check if provider already exists (by name)
                        existing_provider = Provider.objects.filter(name=name).first()

                        if existing_provider:
                            # Update existing provider
                            self.update_provider(
                                existing_provider, cleaned_row, latitude, longitude
                            )
                            self.stdout.write(f"Updated: {name}")
                        else:
                            # Create new provider
                            self.create_provider(cleaned_row, latitude, longitude)
                            self.stdout.write(f"Created: {name}")

                        imported_count += 1

                    except Exception as e:
                        error_count += 1
                        self.stdout.write(
                            self.style.ERROR(
                                f'Line {line_num}: Error processing "{name}" - {str(e)}'
                            )
                        )

        except Exception as e:
            raise CommandError(f"Error reading file: {str(e)}")

        self.stdout.write(
            self.style.SUCCESS(
                f"Import complete! Imported: {imported_count}, Skipped: {skipped_count}, Errors: {error_count}"
            )
        )

    def parse_coordinate(self, coord_str):
        """Parse coordinate string to Decimal"""
        if not coord_str or coord_str.strip() == "":
            return None

        try:
            return Decimal(str(coord_str).strip())
        except (InvalidOperation, ValueError):
            return None

    def create_provider(self, data, latitude, longitude):
        """Create a new provider"""
        provider_data = {
            "name": data.get("name", "").strip(),
            "phone": data.get("phone", "").strip() or None,
            "address": data.get("address", "").strip() or None,
            "website_domain": self.clean_website(data.get("website_domain", "")),
            "latitude": latitude,
            "longitude": longitude,
            "center_based_services": data.get("center_based_services", "").strip()
            or None,
            "areas": data.get("areas", "").strip() or None,
            "specializations": data.get("specializations", "").strip() or None,
            "insurance_accepted": data.get("insurance_accepted", "").strip() or None,
            "services": data.get("services", "").strip() or None,
        }

        # Remove None values to avoid issues
        provider_data = {k: v for k, v in provider_data.items() if v is not None}

        Provider.objects.create(**provider_data)

    def update_provider(self, provider, data, latitude, longitude):
        """Update an existing provider"""
        provider.name = data.get("name", "").strip() or provider.name
        provider.phone = data.get("phone", "").strip() or provider.phone
        provider.address = data.get("address", "").strip() or provider.address
        provider.website_domain = (
            self.clean_website(data.get("website_domain", ""))
            or provider.website_domain
        )
        provider.latitude = latitude if latitude is not None else provider.latitude
        provider.longitude = longitude if longitude is not None else provider.longitude
        provider.center_based_services = (
            data.get("center_based_services", "").strip()
            or provider.center_based_services
        )
        provider.areas = data.get("areas", "").strip() or provider.areas
        provider.specializations = (
            data.get("specializations", "").strip() or provider.specializations
        )
        provider.insurance_accepted = (
            data.get("insurance_accepted", "").strip() or provider.insurance_accepted
        )
        provider.services = data.get("services", "").strip() or provider.services

        provider.save()

    def clean_website(self, website):
        """Clean and validate website URL"""
        if not website or website.strip() == "":
            return None

        website = website.strip()
        if not website.startswith(("http://", "https://")):
            website = "https://" + website

        return website
