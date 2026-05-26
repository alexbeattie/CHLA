"""Import inclusive playground locations from a CSV file."""

import csv
from decimal import Decimal, InvalidOperation
from pathlib import Path

from django.conf import settings
from django.contrib.gis.geos import Point
from django.core.management.base import BaseCommand, CommandError

from locations.models import Location, LocationCategory

from .utils.geocoding import GeocodingService


DEFAULT_CATEGORY_NAME = "Inclusive Playgrounds"
DEFAULT_SOURCE_FILE = Path(settings.BASE_DIR) / "data" / "inclusive_playgrounds_ca_2023.csv"


class Command(BaseCommand):
    help = "Import inclusive playgrounds as Location records"

    def add_arguments(self, parser):
        parser.add_argument(
            "--file",
            type=str,
            default=str(DEFAULT_SOURCE_FILE),
            help="Path to inclusive playground CSV file",
        )
        parser.add_argument(
            "--category-name",
            type=str,
            default=DEFAULT_CATEGORY_NAME,
            help="Location category name to create/use",
        )
        parser.add_argument(
            "--geocode",
            action="store_true",
            help="Geocode rows missing latitude/longitude using MAPBOX_ACCESS_TOKEN",
        )
        parser.add_argument(
            "--dry-run",
            action="store_true",
            help="Parse and validate rows without saving changes",
        )

    def handle(self, *args, **options):
        csv_path = Path(options["file"])
        if not csv_path.exists():
            raise CommandError(f"CSV file not found: {csv_path}")

        dry_run = options["dry_run"]
        geocode = options["geocode"]
        category_name = options["category_name"]

        geocoder = None
        if geocode:
            geocoder = GeocodingService(self.stdout)
            if not geocoder.is_available():
                raise CommandError("MAPBOX_ACCESS_TOKEN is required when using --geocode")

        rows = self._read_rows(csv_path)
        if dry_run:
            self.stdout.write(self.style.WARNING("DRY RUN MODE - No changes will be saved"))

        category = None
        if not dry_run:
            category, _ = LocationCategory.objects.get_or_create(
                name=category_name,
                defaults={
                    "description": "Inclusive playgrounds and accessible play spaces.",
                },
            )

        stats = {"created": 0, "updated": 0, "skipped": 0, "errors": 0}
        for row_number, row in enumerate(rows, start=2):
            try:
                data = self._build_location_data(row, geocoder)
                if not data:
                    stats["skipped"] += 1
                    self.stdout.write(
                        self.style.WARNING(
                            f"Skipped row {row_number}: missing coordinates for {row.get('name', '').strip()}"
                        )
                    )
                    continue

                if dry_run:
                    stats["created"] += 1
                    self.stdout.write(f"Would import: {data['name']}")
                    continue

                location, created = Location.objects.update_or_create(
                    category=category,
                    name=data["name"],
                    address=data["address"],
                    city=data["city"],
                    state=data["state"],
                    zip_code=data["zip_code"],
                    defaults=data,
                )
                stats["created" if created else "updated"] += 1
                action = "Created" if created else "Updated"
                self.stdout.write(f"{action}: {location.name}")
            except Exception as exc:  # pylint: disable=broad-except
                stats["errors"] += 1
                self.stdout.write(self.style.ERROR(f"Error on row {row_number}: {exc}"))

        self._print_summary(stats)

        if stats["errors"]:
            raise CommandError(f"Import completed with {stats['errors']} errors")

    def _read_rows(self, csv_path):
        with csv_path.open("r", encoding="utf-8", newline="") as csv_file:
            return [row for row in csv.DictReader(csv_file) if row.get("name", "").strip()]

    def _build_location_data(self, row, geocoder):
        name = row.get("name", "").strip()
        address = row.get("address", "").strip()
        city = row.get("city", "").strip()
        state = row.get("state", "").strip() or "CA"
        zip_code = row.get("zip_code", "").strip()

        latitude = self._parse_decimal(row.get("latitude"))
        longitude = self._parse_decimal(row.get("longitude"))

        if (latitude is None or longitude is None) and geocoder:
            coords = geocoder.geocode_address(self._full_address(row))
            if coords:
                latitude = coords["latitude"]
                longitude = coords["longitude"]

        if latitude is None or longitude is None:
            return None

        description = self._build_description(row)

        return {
            "name": name,
            "address": address,
            "city": city,
            "state": state,
            "zip_code": zip_code,
            "latitude": latitude,
            "longitude": longitude,
            "location": Point(float(longitude), float(latitude), srid=4326),
            "description": description,
            "is_active": True,
            "rating": Decimal("0.0"),
            "price_level": 1,
            "has_parking": False,
            "is_accessible": True,
        }

    def _build_description(self, row):
        parts = ["Inclusive playground"]
        venue = row.get("venue", "").strip()
        opened_date = row.get("opened_date", "").strip()
        source = row.get("source", "").strip()
        notes = row.get("notes", "").strip()

        if venue:
            parts.append(f"at {venue}")
        if opened_date:
            parts.append(f"opened {opened_date}")

        description = " ".join(parts) + "."
        if source:
            description += f" Source: {source}."
        if notes:
            description += f" Notes: {notes}"
        return description

    def _full_address(self, row):
        address_parts = [
            row.get("address", "").strip(),
            row.get("city", "").strip(),
            row.get("state", "").strip() or "CA",
            row.get("zip_code", "").strip(),
        ]
        return ", ".join(part for part in address_parts if part)

    def _parse_decimal(self, value):
        if value is None or str(value).strip() == "":
            return None
        try:
            return Decimal(str(value).strip())
        except (InvalidOperation, ValueError):
            return None

    def _print_summary(self, stats):
        self.stdout.write("\n" + "=" * 50)
        self.stdout.write(self.style.SUCCESS("Inclusive playground import complete!"))
        self.stdout.write(f"  Created: {stats['created']}")
        self.stdout.write(f"  Updated: {stats['updated']}")
        self.stdout.write(f"  Skipped: {stats['skipped']}")
        self.stdout.write(f"  Errors:  {stats['errors']}")
        self.stdout.write("=" * 50 + "\n")
