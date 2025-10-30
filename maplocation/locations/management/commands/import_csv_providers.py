"""
Management command to import provider data from CSV files.
Handles geocoding with Mapbox API and stores both lat/lng and PostGIS geography point.

Usage:
    python manage.py import_csv_providers --file "/path/to/providers.csv" --area "Santa Monica" --geocode
"""

import csv
import os
from decimal import Decimal
from django.core.management.base import BaseCommand
from django.contrib.gis.geos import Point
from locations.models import ProviderV2
import requests
import time


class Command(BaseCommand):
    help = "Import providers from CSV files with geocoding support"

    def add_arguments(self, parser):
        parser.add_argument(
            "--file",
            type=str,
            required=True,
            help="Path to CSV file",
        )
        parser.add_argument(
            "--area",
            type=str,
            required=True,
            help="Area/city name for providers (e.g., 'Santa Monica')",
        )
        parser.add_argument(
            "--geocode",
            action="store_true",
            help="Geocode addresses using Mapbox API (requires MAPBOX_ACCESS_TOKEN env var)",
        )
        parser.add_argument(
            "--dry-run",
            action="store_true",
            help="Preview import without saving to database",
        )

    def handle(self, *args, **options):
        file_path = options["file"]
        area_name = options["area"]
        geocode = options["geocode"]
        dry_run = options["dry_run"]

        # Check if file exists
        if not os.path.exists(file_path):
            self.stdout.write(self.style.ERROR(f"File not found: {file_path}"))
            return

        # Check for Mapbox token if geocoding is enabled
        if geocode and not os.environ.get("MAPBOX_ACCESS_TOKEN"):
            self.stdout.write(
                self.style.ERROR("MAPBOX_ACCESS_TOKEN not set in environment")
            )
            return

        self.stdout.write(f"\nImporting providers from {file_path}...")
        self.stdout.write(f"Area: {area_name}")
        if geocode:
            self.stdout.write("Geocoding: ENABLED")
        if dry_run:
            self.stdout.write(self.style.WARNING("DRY RUN MODE - No changes will be saved"))
        self.stdout.write("")

        # Import the providers
        self.import_providers(file_path, area_name, geocode, dry_run)

    def import_providers(self, file_path, area_name, geocode, dry_run):
        """Import providers from CSV file"""
        created_count = 0
        updated_count = 0
        error_count = 0

        with open(file_path, "r", encoding="utf-8") as csvfile:
            reader = csv.DictReader(csvfile)

            # Verify expected columns
            required_cols = ["Provider Name", "Address", "Services", "Insurance", "Phone"]
            missing_cols = [col for col in required_cols if col not in reader.fieldnames]
            if missing_cols:
                self.stdout.write(
                    self.style.ERROR(f"Missing required columns: {missing_cols}")
                )
                self.stdout.write(f"Found columns: {reader.fieldnames}")
                return

            # Process each row
            for row_num, row in enumerate(reader, start=2):
                try:
                    provider_name = row.get("Provider Name", "").strip()

                    # Skip empty rows
                    if not provider_name:
                        continue

                    # Parse the provider data
                    provider_data = self.parse_provider_data(row, area_name)

                    # Geocode if requested
                    if geocode and provider_data.get("address"):
                        coords = self.geocode_address(provider_data["address"])
                        if coords:
                            provider_data["latitude"] = coords["latitude"]
                            provider_data["longitude"] = coords["longitude"]
                            # Create PostGIS Point for geography field
                            provider_data["location"] = Point(
                                float(coords["longitude"]),
                                float(coords["latitude"]),
                                srid=4326,
                            )
                            time.sleep(0.3)  # Rate limiting for Mapbox API
                        else:
                            self.stdout.write(
                                self.style.WARNING(
                                    f"  ⚠ No coordinates for: {provider_name}"
                                )
                            )

                    if dry_run:
                        self.stdout.write(f"  [DRY RUN] Would create/update: {provider_name}")
                        if provider_data.get("latitude"):
                            self.stdout.write(
                                f"            Location: {provider_data['latitude']}, {provider_data['longitude']}"
                            )
                        created_count += 1
                    else:
                        # Create or update provider
                        provider, created = ProviderV2.objects.update_or_create(
                            name=provider_data["name"],
                            address=provider_data.get("address", ""),
                            defaults=provider_data,
                        )

                        if created:
                            created_count += 1
                            self.stdout.write(f"  ✓ Created: {provider.name}")
                        else:
                            updated_count += 1
                            self.stdout.write(f"  ↻ Updated: {provider.name}")

                except Exception as e:
                    error_count += 1
                    self.stdout.write(
                        self.style.ERROR(f"  ✗ Error on row {row_num}: {e}")
                    )

        # Summary
        self.stdout.write("\n" + "=" * 50)
        if dry_run:
            self.stdout.write(self.style.SUCCESS(f"DRY RUN Complete!"))
        else:
            self.stdout.write(self.style.SUCCESS(f"Import complete!"))
        self.stdout.write(f"  Created: {created_count}")
        self.stdout.write(f"  Updated: {updated_count}")
        self.stdout.write(f"  Errors:  {error_count}")
        self.stdout.write("=" * 50 + "\n")

    def parse_provider_data(self, row, area_name):
        """Parse provider data from CSV row"""
        # Clean up the data
        name = row.get("Provider Name", "").strip()
        address = row.get("Address", "").strip()
        services = row.get("Services", "").strip()
        insurance = row.get("Insurance", "").strip()
        phone = row.get("Phone", "").strip()

        data = {
            "name": name,
            "phone": phone if phone else None,
            "address": address if address else "Address not provided",
            "verified": False,
            "serves_la_county": True,
            "center_based_services": True,
        }

        # Parse services into therapy_types
        if services:
            therapy_types = self.parse_therapy_types(services)
            if therapy_types:
                data["therapy_types"] = therapy_types

        # Parse insurance
        insurance_list, accepts_flags = self.parse_insurance(insurance)
        if insurance_list:
            data["insurance_accepted"] = ", ".join(insurance_list)
        data.update(accepts_flags)

        # Set area-specific data
        if area_name:
            data["specific_areas_served"] = [area_name]

        # Set default values for required fields
        # Defaults to 0,0 if no geocoding - will be updated if geocoding succeeds
        if "latitude" not in data:
            data["latitude"] = Decimal("0")
        if "longitude" not in data:
            data["longitude"] = Decimal("0")

        return data

    def parse_therapy_types(self, services_text):
        """Parse services text into therapy types array"""
        therapy_types = []
        services_lower = services_text.lower()

        # Map service keywords to therapy types
        therapy_mapping = {
            "aba": "ABA therapy",
            "applied behavior": "ABA therapy",
            "speech": "Speech therapy",
            "occupational": "Occupational therapy",
            "ot": "Occupational therapy",
            "physical": "Physical therapy",
            "pt": "Physical therapy",
            "feeding": "Feeding therapy",
            "parent": "Parent child interaction therapy/parent training behavior management",
            "social skill": "Social skills training",
            "early intervention": "Early intervention",
            "adaptive": "Adaptive skills training",
            "fba": "Functional behavior assessment",
        }

        for keyword, therapy_type in therapy_mapping.items():
            if keyword in services_lower and therapy_type not in therapy_types:
                therapy_types.append(therapy_type)

        return therapy_types if therapy_types else None

    def parse_insurance(self, insurance_text):
        """Parse insurance text into list and acceptance flags"""
        insurance_list = []
        accepts_flags = {
            "accepts_insurance": False,
            "accepts_regional_center": False,
            "accepts_private_pay": False,
        }

        if not insurance_text:
            return insurance_list, accepts_flags

        insurance_lower = insurance_text.lower()

        # Check for Regional Center
        if "regional center" in insurance_lower:
            insurance_list.append("Regional Center")
            accepts_flags["accepts_regional_center"] = True

        # Check for Private Pay/Insurance
        if (
            "private" in insurance_lower
            or "self pay" in insurance_lower
            or "cash" in insurance_lower
        ):
            insurance_list.append("Private Pay")
            accepts_flags["accepts_private_pay"] = True

        # Check for general "insurance" mention
        if "insurance" in insurance_lower and not accepts_flags["accepts_insurance"]:
            accepts_flags["accepts_insurance"] = True

        # Check for common insurance types
        insurance_keywords = {
            "medi-cal": "Medi-Cal",
            "medicaid": "Medicaid",
            "medicare": "Medicare",
            "blue cross": "Blue Cross",
            "blue shield": "Blue Shield",
            "anthem": "Anthem",
            "aetna": "Aetna",
            "cigna": "Cigna",
            "kaiser": "Kaiser Permanente",
            "united": "United Healthcare",
            "health net": "Health Net",
            "molina": "Molina",
            "la care": "L.A. Care",
            "l.a. care": "L.A. Care",
        }

        for keyword, insurance_name in insurance_keywords.items():
            if keyword in insurance_lower and insurance_name not in insurance_list:
                insurance_list.append(insurance_name)
                accepts_flags["accepts_insurance"] = True

        # If no specific insurance was found but "insurance" was mentioned, add generic
        if accepts_flags["accepts_insurance"] and not any(
            ins in insurance_list
            for ins in [
                "Medi-Cal",
                "Medicaid",
                "Medicare",
                "Blue Cross",
                "Blue Shield",
                "Anthem",
                "Aetna",
                "Cigna",
                "Kaiser Permanente",
                "United Healthcare",
                "Health Net",
                "Molina",
                "L.A. Care",
            ]
        ):
            insurance_list.append("Private Insurance")

        return insurance_list, accepts_flags

    def geocode_address(self, address):
        """Geocode address using Mapbox Geocoding API"""
        mapbox_token = os.environ.get("MAPBOX_ACCESS_TOKEN")

        if not mapbox_token:
            return None

        # Clean up address
        address = address.replace("\n", ", ").strip()

        # Add California to improve geocoding accuracy
        if "CA" not in address and "California" not in address:
            address = f"{address}, California"

        # Mapbox Geocoding API
        url = f"https://api.mapbox.com/geocoding/v5/mapbox.places/{address}.json"
        params = {
            "access_token": mapbox_token,
            "country": "US",
            "limit": 1,
        }

        try:
            response = requests.get(url, params=params, timeout=10)
            response.raise_for_status()
            data = response.json()

            if data.get("features"):
                feature = data["features"][0]
                longitude, latitude = feature["geometry"]["coordinates"]
                return {
                    "latitude": Decimal(str(latitude)),
                    "longitude": Decimal(str(longitude)),
                }
        except Exception as e:
            self.stdout.write(self.style.WARNING(f"Geocoding failed for {address}: {e}"))

        return None
