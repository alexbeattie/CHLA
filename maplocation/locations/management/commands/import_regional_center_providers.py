"""
Management command to import provider data from Excel files for specific regional centers.
Specifically designed for Pasadena and San Gabriel/Pomona regional center provider lists.

Usage:
    python manage.py import_regional_center_providers --file data/Pasadena_Provider_List.xlsx --regional-center "Pasadena"
    python manage.py import_regional_center_providers --file data/San_Gabriel_Pomona_Provider_List.xlsx --regional-center "San Gabriel/Pomona"
"""
# pylint: disable=no-member

import os
from django.core.management.base import BaseCommand
from locations.models import ProviderV2, RegionalCenter

from .utils.excel_parser import ExcelParser, ColumnMapper
from .utils.geocoding import GeocodingService
from .utils.provider_parser import ProviderDataParser


class Command(BaseCommand):
    help = "Import providers from Excel files and associate with regional centers"

    def add_arguments(self, parser):
        parser.add_argument(
            "--file",
            type=str,
            required=True,
            help="Path to Excel file (e.g., data/Pasadena_Provider_List.xlsx)",
        )
        parser.add_argument(
            "--regional-center",
            type=str,
            required=False,
            help="Regional center name (e.g., 'Pasadena' or 'San Gabriel/Pomona')",
        )
        parser.add_argument(
            "--area",
            type=str,
            required=False,
            help="Area/city name for providers without specific regional center (e.g., 'Pasadena')",
        )
        parser.add_argument(
            "--clear-existing",
            action="store_true",
            help="Clear existing providers for this regional center before importing",
        )
        parser.add_argument(
            "--geocode",
            action="store_true",
            help="Geocode addresses using Mapbox API (requires MAPBOX_ACCESS_TOKEN env var)",
        )
        parser.add_argument(
            "--sheet",
            type=str,
            default=None,
            help="Specific sheet name to import (default: first sheet)",
        )

    def handle(self, *args, **options):
        file_path = options["file"]
        regional_center_name = options.get("regional_center")
        area_name = options.get("area")

        # Validate file exists
        if not os.path.exists(file_path):
            self.stdout.write(self.style.ERROR(f"File not found: {file_path}"))
            return

        # Get regional center if specified
        regional_center = self._get_regional_center(regional_center_name)
        if regional_center_name and not regional_center:
            return  # Error already printed

        # Validate area/regional center
        if not self._validate_location(area_name, regional_center_name, regional_center):
            return

        # Handle clear existing flag
        if options["clear_existing"]:
            self.stdout.write(
                self.style.WARNING(
                    "Clear existing is not implemented yet for providers"
                )
            )

        # Import providers
        self._import_providers(
            file_path,
            regional_center,
            area_name or (regional_center.city if regional_center else ""),
            options,
        )

    def _get_regional_center(self, regional_center_name):
        """Find and return regional center by name."""
        if not regional_center_name:
            return None

        try:
            regional_center = RegionalCenter.objects.get(
                regional_center__icontains=regional_center_name
            )
            self.stdout.write(
                self.style.SUCCESS(
                    f"Found regional center: {regional_center.regional_center}"
                )
            )
            return regional_center

        except RegionalCenter.DoesNotExist:
            self.stdout.write(
                self.style.ERROR(
                    f"Regional center not found: {regional_center_name}"
                )
            )
            self.stdout.write("Available regional centers:")
            for rc in RegionalCenter.objects.all().distinct("regional_center"):
                self.stdout.write(f"  - {rc.regional_center}")
            return None

        except RegionalCenter.MultipleObjectsReturned:
            regional_center = RegionalCenter.objects.filter(
                regional_center__icontains=regional_center_name
            ).first()
            self.stdout.write(
                self.style.WARNING(
                    f"Multiple matches found, using: {regional_center.regional_center}"
                )
            )
            return regional_center

    def _validate_location(self, area_name, regional_center_name, regional_center):
        """Validate that either area or regional center is provided."""
        if area_name and not regional_center_name:
            self.stdout.write(
                self.style.SUCCESS(f"Importing providers for area: {area_name}")
            )
            return True
        elif not regional_center and not area_name:
            self.stdout.write(
                self.style.ERROR("Please provide either --regional-center or --area")
            )
            return False
        return True

    def _import_providers(self, file_path, regional_center, area_name, options):
        """Import providers from Excel file."""
        self.stdout.write(f"\nImporting providers from {file_path}...")
        if regional_center:
            self.stdout.write(f"Regional Center: {regional_center.regional_center}")
        self.stdout.write(f"Area: {area_name}\n")

        # Initialize services
        excel_parser = ExcelParser(file_path, options["sheet"])
        geocoding_service = GeocodingService(self.stdout) if options["geocode"] else None

        # Open Excel file
        try:
            excel_parser.open()
        except Exception as e:  # pylint: disable=broad-except
            self.stdout.write(self.style.ERROR(str(e)))
            return

        self.stdout.write(f"Using sheet: {excel_parser.get_sheet_name()}\n")

        # Get headers and column mapping
        headers = excel_parser.get_headers()
        self.stdout.write(f"Columns found: {', '.join(headers)}\n")

        column_map = ColumnMapper.map_columns(headers)
        self.stdout.write(f"Column mapping: {column_map}\n")

        # Process rows
        stats = self._process_rows(
            excel_parser,
            column_map,
            area_name,
            geocoding_service,
        )

        # Close parser
        excel_parser.close()

        # Print summary
        self._print_summary(stats)

    def _process_rows(self, excel_parser, column_map, area_name, geocoding_service):
        """Process all rows from Excel file."""
        stats = {"created": 0, "updated": 0, "errors": 0}

        for row_num, row_data in excel_parser.iter_rows():
            try:
                # Get provider name
                provider_name = ColumnMapper.get_value(
                    row_data,
                    column_map.get("name")
                )

                # Skip empty rows
                if not provider_name or provider_name.strip() == "":
                    continue

                # Parse provider data
                provider_data = ProviderDataParser.parse(
                    row_data,
                    column_map,
                    area_name,
                    ColumnMapper.get_value
                )

                # Geocode if requested
                if geocoding_service and provider_data.get("address"):
                    coords = geocoding_service.geocode_address(provider_data["address"])
                    if coords:
                        provider_data["latitude"] = coords["latitude"]
                        provider_data["longitude"] = coords["longitude"]

                # Create or update provider
                provider, created = ProviderV2.objects.update_or_create(
                    name=provider_data["name"],
                    address=provider_data.get("address", ""),
                    defaults=provider_data,
                )

                if created:
                    stats["created"] += 1
                    self.stdout.write(f"  ✓ Created: {provider.name}")
                else:
                    stats["updated"] += 1
                    self.stdout.write(f"  ↻ Updated: {provider.name}")

            except Exception as e:  # pylint: disable=broad-except
                stats["errors"] += 1
                self.stdout.write(
                    self.style.ERROR(f"  ✗ Error on row {row_num}: {e}")
                )

        return stats

    def _print_summary(self, stats):
        """Print import summary statistics."""
        self.stdout.write("\n" + "=" * 50)
        self.stdout.write(self.style.SUCCESS("Import complete!"))
        self.stdout.write(f"  Created: {stats['created']}")
        self.stdout.write(f"  Updated: {stats['updated']}")
        self.stdout.write(f"  Errors:  {stats['errors']}")
        self.stdout.write("=" * 50 + "\n")
