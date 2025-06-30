from django.core.management.base import BaseCommand
from django.contrib.gis.geos import Point, MultiPolygon
from django.contrib.gis.measure import Distance
from locations.models import RegionalCenter
from django.db import transaction
import math


class Command(BaseCommand):
    help = "Generate service area polygons for regional centers"

    def add_arguments(self, parser):
        parser.add_argument(
            "--update-existing",
            action="store_true",
            help="Update existing service areas (default: only create for centers without service areas)",
        )
        parser.add_argument(
            "--radius-multiplier",
            type=float,
            default=1.0,
            help="Multiplier for service radius (default: 1.0)",
        )

    def handle(self, *args, **options):
        update_existing = options["update_existing"]
        radius_multiplier = options["radius_multiplier"]

        self.stdout.write("Generating service area polygons for regional centers...")

        # Get regional centers that need service areas
        if update_existing:
            centers = RegionalCenter.objects.filter(
                latitude__isnull=False, longitude__isnull=False
            )
            self.stdout.write(f"Processing all {centers.count()} regional centers...")
        else:
            centers = RegionalCenter.objects.filter(
                latitude__isnull=False,
                longitude__isnull=False,
                service_area__isnull=True,
            )
            self.stdout.write(
                f"Processing {centers.count()} regional centers without service areas..."
            )

        created_count = 0
        updated_count = 0
        error_count = 0

        # Define different service area patterns for different types of centers
        service_patterns = {
            "main": self.create_circular_area,
            "satellite": self.create_smaller_circular_area,
            "outreach": self.create_elongated_area,
            "default": self.create_circular_area,
        }

        with transaction.atomic():
            for center in centers:
                try:
                    # Determine service pattern based on office type
                    office_type = (center.office_type or "default").lower()
                    pattern_func = service_patterns.get(
                        office_type, service_patterns["default"]
                    )

                    # Calculate effective radius
                    base_radius = center.service_radius_miles * radius_multiplier

                    # Create service area polygon
                    service_area = pattern_func(center, base_radius)

                    if service_area:
                        had_existing = center.service_area is not None
                        center.service_area = service_area
                        center.save()

                        if had_existing:
                            updated_count += 1
                            self.stdout.write(
                                f"Updated service area for {center.regional_center}"
                            )
                        else:
                            created_count += 1
                            self.stdout.write(
                                f"Created service area for {center.regional_center}"
                            )
                    else:
                        error_count += 1
                        self.stdout.write(
                            self.style.WARNING(
                                f"Could not create service area for {center.regional_center}"
                            )
                        )

                except Exception as e:
                    error_count += 1
                    self.stdout.write(
                        self.style.ERROR(
                            f"Error processing {center.regional_center}: {str(e)}"
                        )
                    )

        # Summary
        self.stdout.write(
            self.style.SUCCESS(
                f"Service area generation complete: {created_count} created, "
                f"{updated_count} updated, {error_count} errors"
            )
        )

        # Show some example areas
        self.show_service_area_examples()

    def create_circular_area(self, center, radius_miles):
        """Create a circular service area around the regional center"""
        try:
            center_point = Point(float(center.longitude), float(center.latitude))

            # Convert miles to degrees (rough approximation)
            # 1 degree ≈ 69 miles at the equator, but this varies by latitude
            lat_rad = math.radians(float(center.latitude))
            miles_per_degree_lat = 69.0
            miles_per_degree_lng = 69.0 * math.cos(lat_rad)

            radius_degrees = radius_miles / miles_per_degree_lat

            # Create a circular buffer
            buffer = center_point.buffer(radius_degrees)

            # Convert to MultiPolygon
            if buffer.geom_type == "Polygon":
                return MultiPolygon(buffer)
            else:
                return buffer

        except Exception as e:
            self.stdout.write(f"Error creating circular area: {str(e)}")
            return None

    def create_smaller_circular_area(self, center, radius_miles):
        """Create a smaller circular area for satellite offices"""
        return self.create_circular_area(center, radius_miles * 0.7)

    def create_elongated_area(self, center, radius_miles):
        """Create an elongated service area for outreach offices"""
        try:
            center_point = Point(float(center.longitude), float(center.latitude))

            # Create an elliptical area by stretching the buffer
            lat_rad = math.radians(float(center.latitude))
            miles_per_degree_lat = 69.0
            miles_per_degree_lng = 69.0 * math.cos(lat_rad)

            # Make it wider in longitude than latitude
            radius_lat = (radius_miles * 0.8) / miles_per_degree_lat
            radius_lng = (radius_miles * 1.2) / miles_per_degree_lng

            # Create points for an ellipse (simplified)
            ellipse_points = []
            for i in range(32):  # 32-point ellipse
                angle = 2 * math.pi * i / 32
                x = center_point.x + radius_lng * math.cos(angle)
                y = center_point.y + radius_lat * math.sin(angle)
                ellipse_points.append((x, y))

            # Close the polygon
            ellipse_points.append(ellipse_points[0])

            from django.contrib.gis.geos import Polygon

            polygon = Polygon(ellipse_points)
            return MultiPolygon(polygon)

        except Exception as e:
            self.stdout.write(f"Error creating elongated area: {str(e)}")
            # Fallback to circular
            return self.create_circular_area(center, radius_miles)

    def show_service_area_examples(self):
        """Show some examples of created service areas"""
        self.stdout.write("\nService area examples:")

        centers_with_areas = RegionalCenter.objects.filter(
            service_area__isnull=False
        ).order_by("regional_center")[:5]

        for center in centers_with_areas:
            area_info = center.get_service_area_as_geojson()
            if area_info:
                self.stdout.write(
                    f"  {center.regional_center}: "
                    f'{area_info["geometry"]["type"]} with '
                    f"{center.service_radius_miles} mile radius"
                )
