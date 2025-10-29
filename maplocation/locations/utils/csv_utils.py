import csv
import io
from django.http import HttpResponse
from django.db import transaction
from django.utils import timezone
from ..models import Provider, RegionalCenter, ProviderRegionalCenter
import logging

logger = logging.getLogger(__name__)


class CSVExporter:
    """Utility class for exporting data to CSV format"""

    @staticmethod
    def export_providers(queryset):
        """Export providers to CSV"""
        response = HttpResponse(content_type="text/csv")
        response["Content-Disposition"] = (
            f'attachment; filename="providers_export_{timezone.now().strftime("%Y%m%d_%H%M%S")}.csv"'
        )

        writer = csv.writer(response)

        # Write header
        writer.writerow(
            [
                "ID",
                "Name",
                "Phone",
                "Address",
                "Latitude",
                "Longitude",
                "Website",
                "Areas",
                "Coverage Areas",
                "Center Based Services",
                "Specializations",
                "Services",
                "Insurance Accepted",
                "Regional Centers",
                "Created At",
                "Updated At",
            ]
        )

        # Write data
        for provider in queryset:
            # Get associated regional centers
            regional_centers = ", ".join(
                [
                    rel.regional_center.regional_center
                    for rel in provider.regional_centers.all()
                ]
            )

            writer.writerow(
                [
                    provider.id,
                    provider.name,
                    provider.phone or "",
                    provider.address or "",
                    provider.latitude or "",
                    provider.longitude or "",
                    provider.website_domain or "",
                    provider.areas or "",
                    provider.coverage_areas or "",
                    provider.center_based_services or "",
                    provider.specializations or "",
                    provider.services or "",
                    provider.insurance_accepted or "",
                    regional_centers,
                    provider.created_at if hasattr(provider, "created_at") else "",
                    provider.updated_at if hasattr(provider, "updated_at") else "",
                ]
            )

        return response

    @staticmethod
    def export_regional_centers(queryset):
        """Export regional centers to CSV"""
        response = HttpResponse(content_type="text/csv")
        response["Content-Disposition"] = (
            f'attachment; filename="regional_centers_export_{timezone.now().strftime("%Y%m%d_%H%M%S")}.csv"'
        )

        writer = csv.writer(response)

        # Write header
        writer.writerow(
            [
                "ID",
                "Regional Center",
                "Office Type",
                "Address",
                "Suite",
                "City",
                "State",
                "ZIP Code",
                "Latitude",
                "Longitude",
                "Telephone",
                "Website",
                "County Served",
                "LA Health District",
                "Service Radius (miles)",
                "Provider Count",
            ]
        )

        # Write data
        for center in queryset:
            writer.writerow(
                [
                    center.id,
                    center.regional_center,
                    center.office_type or "",
                    center.address or "",
                    center.suite or "",
                    center.city or "",
                    center.state or "",
                    center.zip_code or "",
                    center.latitude or "",
                    center.longitude or "",
                    center.telephone or "",
                    center.website or "",
                    center.county_served or "",
                    center.los_angeles_health_district or "",
                    center.service_radius_miles or "",
                    center.providers.count(),
                ]
            )

        return response


class CSVImporter:
    """Utility class for importing data from CSV format"""

    def __init__(self):
        self.errors = []
        self.warnings = []
        self.success_count = 0
        self.error_count = 0

    def import_providers(self, csv_file, update_existing=False):
        """Import providers from CSV file"""
        self.errors = []
        self.warnings = []
        self.success_count = 0
        self.error_count = 0

        try:
            # Read CSV file
            if hasattr(csv_file, "read"):
                content = csv_file.read()
                if isinstance(content, bytes):
                    content = content.decode("utf-8")
            else:
                content = csv_file

            # Parse CSV
            reader = csv.DictReader(io.StringIO(content))

            with transaction.atomic():
                for row_num, row in enumerate(
                    reader, start=2
                ):  # Start at 2 (header is row 1)
                    try:
                        self._process_provider_row(row, row_num, update_existing)
                        self.success_count += 1
                    except Exception as e:
                        self.error_count += 1
                        self.errors.append(f"Row {row_num}: {str(e)}")
                        logger.error(f"Error processing provider row {row_num}: {e}")

        except Exception as e:
            self.errors.append(f"File processing error: {str(e)}")
            logger.error(f"CSV import error: {e}")

        return {
            "success_count": self.success_count,
            "error_count": self.error_count,
            "errors": self.errors,
            "warnings": self.warnings,
        }

    def _process_provider_row(self, row, row_num, update_existing):
        """Process a single provider row"""
        # Required fields
        name = row.get("Name", "").strip()
        if not name:
            raise ValueError("Name is required")

        # NOTE: This CSV import utility has been deprecated in favor of ProviderV2
        # If you need to import providers, please use the admin interface or
        # the import_regional_center_providers management command

        raise NotImplementedError(
            "CSV import for the old Provider model has been removed. "
            "Please use ProviderV2 import methods instead (admin interface or "
            "import_regional_center_providers management command)."
        )

    def _associate_regional_centers(self, provider, regional_centers_str, row_num):
        """Associate provider with regional centers"""
        regional_center_names = [
            name.strip() for name in regional_centers_str.split(",")
        ]

        for rc_name in regional_center_names:
            if not rc_name:
                continue

            try:
                regional_center = RegionalCenter.objects.filter(
                    regional_center__icontains=rc_name
                ).first()

                if regional_center:
                    # Create association if it doesn't exist
                    ProviderRegionalCenter.objects.get_or_create(
                        provider=provider,
                        regional_center=regional_center,
                        defaults={"is_primary": False},
                    )
                else:
                    self.warnings.append(
                        f"Row {row_num}: Regional center '{rc_name}' not found"
                    )
            except Exception as e:
                self.warnings.append(
                    f"Row {row_num}: Error associating with regional center '{rc_name}': {str(e)}"
                )

    def import_regional_centers(self, csv_file, update_existing=False):
        """Import regional centers from CSV file"""
        self.errors = []
        self.warnings = []
        self.success_count = 0
        self.error_count = 0

        try:
            # Read CSV file
            if hasattr(csv_file, "read"):
                content = csv_file.read()
                if isinstance(content, bytes):
                    content = content.decode("utf-8")
            else:
                content = csv_file

            # Parse CSV
            reader = csv.DictReader(io.StringIO(content))

            with transaction.atomic():
                for row_num, row in enumerate(
                    reader, start=2
                ):  # Start at 2 (header is row 1)
                    try:
                        self._process_regional_center_row(row, row_num, update_existing)
                        self.success_count += 1
                    except Exception as e:
                        self.error_count += 1
                        self.errors.append(f"Row {row_num}: {str(e)}")
                        logger.error(
                            f"Error processing regional center row {row_num}: {e}"
                        )

        except Exception as e:
            self.errors.append(f"File processing error: {str(e)}")
            logger.error(f"CSV import error: {e}")

        return {
            "success_count": self.success_count,
            "error_count": self.error_count,
            "errors": self.errors,
            "warnings": self.warnings,
        }

    def _process_regional_center_row(self, row, row_num, update_existing):
        """Process a single regional center row"""
        # Required fields
        name = row.get("Regional Center", "").strip()
        if not name:
            raise ValueError("Regional Center name is required")

        # Check if regional center exists
        existing_center = None
        if update_existing:
            existing_center = RegionalCenter.objects.filter(
                regional_center=name
            ).first()

        # Create or update regional center
        center_data = {
            "regional_center": name,
            "office_type": row.get("Office Type", "").strip(),
            "address": row.get("Address", "").strip(),
            "suite": row.get("Suite", "").strip(),
            "city": row.get("City", "").strip(),
            "state": row.get("State", "").strip(),
            "zip_code": row.get("ZIP Code", "").strip(),
            "telephone": row.get("Telephone", "").strip(),
            "website": row.get("Website", "").strip(),
            "county_served": row.get("County Served", "").strip(),
            "los_angeles_health_district": row.get("LA Health District", "").strip(),
        }

        # Handle coordinates
        try:
            lat = row.get("Latitude", "").strip()
            lng = row.get("Longitude", "").strip()
            if lat and lng:
                center_data["latitude"] = float(lat)
                center_data["longitude"] = float(lng)
        except (ValueError, TypeError):
            self.warnings.append(f"Row {row_num}: Invalid coordinates for {name}")

        # Handle service radius
        try:
            radius = row.get("Service Radius (miles)", "").strip()
            if radius:
                center_data["service_radius_miles"] = float(radius)
        except (ValueError, TypeError):
            self.warnings.append(f"Row {row_num}: Invalid service radius for {name}")

        # Create or update regional center
        if existing_center:
            for key, value in center_data.items():
                setattr(existing_center, key, value)
            center = existing_center
            center.save()
        else:
            center = RegionalCenter.objects.create(**center_data)


def generate_csv_template(model_type):
    """Generate CSV template for import"""
    if model_type == "providers":
        headers = [
            "Name",
            "Phone",
            "Address",
            "Latitude",
            "Longitude",
            "Website",
            "Areas",
            "Coverage Areas",
            "Center Based Services",
            "Specializations",
            "Services",
            "Insurance Accepted",
            "Regional Centers",
        ]
        sample_data = [
            "Sample Provider Name",
            "555-123-4567",
            "123 Main St, Los Angeles, CA 90210",
            "34.0522",
            "-118.2437",
            "https://example.com",
            "Los Angeles County",
            "LA, Hollywood, Beverly Hills",
            "Main office services",
            "Autism, ADHD, Speech Therapy",
            "ABA Therapy, Speech Therapy, Occupational Therapy",
            "Insurance, Medi-Cal, Regional Center",
            "Los Angeles Regional Center, San Gabriel Regional Center",
        ]
    elif model_type == "regional_centers":
        headers = [
            "Regional Center",
            "Office Type",
            "Address",
            "Suite",
            "City",
            "State",
            "ZIP Code",
            "Latitude",
            "Longitude",
            "Telephone",
            "Website",
            "County Served",
            "LA Health District",
            "Service Radius (miles)",
        ]
        sample_data = [
            "Sample Regional Center",
            "Main Office",
            "123 Center St",
            "Suite 100",
            "Los Angeles",
            "CA",
            "90210",
            "34.0522",
            "-118.2437",
            "555-123-4567",
            "https://example.com",
            "Los Angeles",
            "District 1",
            "25.0",
        ]
    else:
        raise ValueError(f"Unknown model type: {model_type}")

    # Create CSV content
    output = io.StringIO()
    writer = csv.writer(output)
    writer.writerow(headers)
    writer.writerow(sample_data)

    return output.getvalue()
