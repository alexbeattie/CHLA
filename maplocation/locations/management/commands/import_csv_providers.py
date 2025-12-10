"""
Import providers from CSV file to database
Handles various CSV formats including the CHLA provider export format
"""

from django.core.management.base import BaseCommand
from django.contrib.gis.geos import Point
from locations.models import ProviderV2
from decimal import Decimal, InvalidOperation as DecimalInvalidOperation
import csv
import json
import re
import ast


class Command(BaseCommand):
    help = "Import providers from CSV file"

    def add_arguments(self, parser):
        parser.add_argument("csv_file", type=str, help="Path to CSV file")
        parser.add_argument(
            "--dry-run", action="store_true", help="Dry run without saving"
        )
        parser.add_argument(
            "--update-only",
            action="store_true",
            help="Only update existing providers (skip new ones)",
        )
        parser.add_argument(
            "--skip-empty",
            action="store_true",
            default=True,
            help="Skip rows with empty id or name",
        )

    def _normalize_quotes(self, text):
        """Normalize fancy/curly quotes to standard ASCII quotes."""
        if not text:
            return text
        # Replace curly quotes with straight quotes
        replacements = {
            "\u201c": '"',  # Left double quotation mark "
            "\u201d": '"',  # Right double quotation mark "
            "\u2018": "'",  # Left single quotation mark '
            "\u2019": "'",  # Right single quotation mark '
            "\u201e": '"',  # Double low-9 quotation mark „
            "\u201f": '"',  # Double high-reversed-9 quotation mark ‟
            "\u2032": "'",  # Prime ′
            "\u2033": '"',  # Double prime ″
        }
        for old, new in replacements.items():
            text = text.replace(old, new)
        return text

    def _clean_list_item(self, item):
        """Clean an individual list item - remove quotes, extra spaces, etc."""
        if not item:
            return ""
        cleaned = str(item).strip()
        # Normalize any fancy quotes
        cleaned = self._normalize_quotes(cleaned)
        # Remove leading/trailing quotes and brackets
        cleaned = cleaned.strip("\"'[]")
        # Remove any remaining stray quotes at the end (common data issue)
        cleaned = cleaned.rstrip("\"'")
        cleaned = cleaned.lstrip("\"'")
        return cleaned.strip()

    def parse_list_field(self, value):
        """
        Parse list fields that may be in various formats:
        - JSON: ["item1", "item2"]
        - Python repr: ['item1', 'item2']
        - PostgreSQL array: {"item1","item2"}
        - Plain text with commas
        """
        if not value or value.strip() == "":
            return []

        value = value.strip()

        # Normalize curly/fancy quotes to straight quotes before parsing
        value = self._normalize_quotes(value)

        # Try JSON first (double quotes)
        try:
            result = json.loads(value)
            if isinstance(result, list):
                return [self._clean_list_item(item) for item in result if item]
        except (json.JSONDecodeError, TypeError, ValueError):
            pass

        # Try Python literal eval (handles single quotes)
        try:
            result = ast.literal_eval(value)
            if isinstance(result, list):
                return [self._clean_list_item(item) for item in result if item]
        except (ValueError, SyntaxError, TypeError):
            pass

        # Try PostgreSQL array format: {"item1","item2"}
        if value.startswith("{") and value.endswith("}"):
            # Remove braces and split by comma, handling quoted values
            inner = value[1:-1]
            if inner:
                # Handle quoted strings in postgres format
                items = re.findall(r'"([^"]*)"', inner)
                if items:
                    return [
                        self._clean_list_item(item) for item in items if item.strip()
                    ]
                # Fallback: simple split
                return [
                    self._clean_list_item(item)
                    for item in inner.split(",")
                    if item.strip()
                ]

        # Try comma-separated (last resort)
        if "," in value:
            return [
                self._clean_list_item(item) for item in value.split(",") if item.strip()
            ]

        # Single value
        cleaned = self._clean_list_item(value)
        return [cleaned] if cleaned else []

    def parse_address(self, value):
        """
        Parse address field that may be JSON or plain text
        """
        if not value or value.strip() == "":
            return ""

        value = value.strip()

        # Try JSON format: {"zip": "91405", "city": "Van Nuys", "state": "CA", "street": "..."}
        try:
            addr_dict = json.loads(value)
            if isinstance(addr_dict, dict):
                parts = []
                if addr_dict.get("street"):
                    parts.append(addr_dict["street"])
                if addr_dict.get("city"):
                    parts.append(addr_dict["city"])
                if addr_dict.get("state"):
                    parts.append(addr_dict["state"])
                if addr_dict.get("zip"):
                    parts.append(addr_dict["zip"])
                return ", ".join(parts)
        except (json.JSONDecodeError, TypeError, ValueError):
            pass

        return value

    def handle(self, *args, **options):
        csv_file = options["csv_file"]
        dry_run = options.get("dry_run", False)
        update_only = options.get("update_only", False)
        options.get("skip_empty", True)

        self.stdout.write(f"Importing providers from: {csv_file}")
        if dry_run:
            self.stdout.write(
                self.style.WARNING("DRY RUN MODE - No changes will be saved")
            )
        if update_only:
            self.stdout.write(
                self.style.WARNING("UPDATE ONLY MODE - New providers will be skipped")
            )

        with open(csv_file, "r", encoding="utf-8") as f:
            reader = csv.DictReader(f)
            providers = list(reader)

        # Filter out empty rows
        providers = [p for p in providers if p.get("id") and p.get("name")]

        self.stdout.write(f"Found {len(providers)} valid providers in CSV")

        created = 0
        updated = 0
        skipped = 0
        errors = []

        for i, row in enumerate(providers, 1):
            try:
                provider_id = row.get("id", "").strip()
                name = row.get("name", "").strip()

                # Skip empty rows
                if not provider_id or not name:
                    skipped += 1
                    continue

                # Check if provider exists
                existing = ProviderV2.objects.filter(id=provider_id).first()

                # Skip new providers if update-only mode
                if update_only and not existing:
                    skipped += 1
                    continue

                # Parse coordinates (may not exist in all CSV formats)
                lat = Decimal("0.0")
                lng = Decimal("0.0")
                if row.get("latitude"):
                    try:
                        lat = Decimal(str(row["latitude"]))
                    except (ValueError, TypeError, DecimalInvalidOperation):
                        pass
                if row.get("longitude"):
                    try:
                        lng = Decimal(str(row["longitude"]))
                    except (ValueError, TypeError, DecimalInvalidOperation):
                        pass

                # If updating existing and no coordinates in CSV, keep existing coordinates
                if existing and lat == Decimal("0.0") and lng == Decimal("0.0"):
                    lat = existing.latitude
                    lng = existing.longitude

                # Create PostGIS Point
                location = None
                if lat and lng and (lat != Decimal("0.0") or lng != Decimal("0.0")):
                    location = Point(float(lng), float(lat), srid=4326)
                elif existing and existing.location:
                    location = existing.location

                # Parse list fields
                therapy_types = self.parse_list_field(row.get("therapy_types", ""))
                age_groups = self.parse_list_field(row.get("age_groups", ""))
                diagnoses_treated = self.parse_list_field(
                    row.get("diagnoses_treated", "")
                )

                # Parse address
                address = self.parse_address(row.get("address", ""))

                # Insurance - keep as text
                insurance_accepted = row.get("insurance_accepted", "")
                if insurance_accepted:
                    # Clean up postgres array format if needed
                    if insurance_accepted.startswith(
                        "{"
                    ) and insurance_accepted.endswith("}"):
                        items = re.findall(r'"([^"]*)"', insurance_accepted[1:-1])
                        if items:
                            insurance_accepted = ", ".join(items)
                        else:
                            insurance_accepted = insurance_accepted[1:-1]

                # Prepare data
                data = {
                    "name": name,
                    "type": row.get("type", "") or "",
                    "phone": row.get("phone", "") or "",
                    "email": row.get("email", "") or "",
                    "website": row.get("website", "") or "",
                    "description": row.get("description", "") or "",
                    "latitude": lat,
                    "longitude": lng,
                    "location": location,
                    "address": address,
                    "insurance_accepted": insurance_accepted,
                    "therapy_types": therapy_types if therapy_types else None,
                    "age_groups": age_groups if age_groups else None,
                    "diagnoses_treated": (
                        diagnoses_treated if diagnoses_treated else None
                    ),
                }

                if not dry_run:
                    if existing:
                        # Update existing
                        for key, value in data.items():
                            setattr(existing, key, value)
                        existing.save()
                        updated += 1
                        self.stdout.write(f"  📝 Updated: {name[:50]}")
                    else:
                        # Create new
                        ProviderV2.objects.create(id=provider_id, **data)
                        created += 1
                        self.stdout.write(
                            self.style.SUCCESS(f"  ✅ Created: {name[:50]}")
                        )
                else:
                    if existing:
                        self.stdout.write(f"  [DRY] Would update: {name[:50]}")
                        updated += 1
                    else:
                        self.stdout.write(f"  [DRY] Would create: {name[:50]}")
                        created += 1

            except Exception as e:
                error_msg = f"Row {i} ({row.get('name', 'Unknown')[:30]}): {str(e)}"
                errors.append(error_msg)
                self.stdout.write(self.style.ERROR(f"  ❌ {error_msg}"))
                skipped += 1

        # Summary
        self.stdout.write("\n" + "=" * 60)
        self.stdout.write(self.style.SUCCESS(f"✅ Created: {created}"))
        self.stdout.write(self.style.WARNING(f"📝 Updated: {updated}"))
        self.stdout.write(f"⏭️  Skipped: {skipped}")
        self.stdout.write(self.style.ERROR(f"❌ Errors: {len(errors)}"))

        if errors:
            self.stdout.write("\n=== Errors ===")
            for error in errors[:20]:  # Show first 20 errors
                self.stdout.write(self.style.ERROR(error))
            if len(errors) > 20:
                self.stdout.write(
                    self.style.ERROR(f"... and {len(errors) - 20} more errors")
                )

        if not dry_run and (created > 0 or updated > 0):
            self.stdout.write(
                self.style.SUCCESS(
                    f"\n✅ Successfully imported {created + updated} providers"
                )
            )
            self.stdout.write("\nNext steps:")
            self.stdout.write(
                "  1. Geocode addresses (if needed): python3 manage.py geocode_providers"
            )
            self.stdout.write("  2. Sync to RDS: python3 manage.py sync_to_rds")
