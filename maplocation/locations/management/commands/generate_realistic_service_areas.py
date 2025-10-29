import math
import random
from django.core.management.base import BaseCommand
from django.contrib.gis.geos import Polygon, MultiPolygon, Point
from locations.models import RegionalCenter


class Command(BaseCommand):
    help = "Generate realistic service area polygons for regional centers based on counties served"

    def add_arguments(self, parser):
        parser.add_argument(
            "--overwrite",
            action="store_true",
            help="Overwrite existing service areas",
        )
        parser.add_argument(
            "--dry-run",
            action="store_true",
            help="Show what would be generated without saving",
        )

    def handle(self, *args, **options):
        centers = RegionalCenter.objects.all()

        if not options["overwrite"]:
            centers = centers.filter(service_area__isnull=True)

        total_centers = centers.count()
        self.stdout.write(f"Processing {total_centers} regional centers...")

        generated_count = 0
        for center in centers:
            try:
                service_area = self.generate_realistic_service_area(center)

                if options["dry_run"]:
                    self.stdout.write(
                        f"Would generate service area for: {center.regional_center} "
                        f"({center.county_served}) - {service_area.area:.6f} sq degrees"
                    )
                else:
                    center.service_area = service_area
                    center.save()
                    generated_count += 1
                    self.stdout.write(
                        f"✓ Generated service area for: {center.regional_center} "
                        f"({center.county_served})"
                    )

            except Exception as e:
                self.stdout.write(
                    self.style.ERROR(
                        f"✗ Error generating service area for {center.regional_center}: {e}"
                    )
                )

        if not options["dry_run"]:
            self.stdout.write(
                self.style.SUCCESS(
                    f"Successfully generated {generated_count} realistic service areas!"
                )
            )

    def generate_realistic_service_area(self, center):
        """Generate a realistic service area polygon based on the center's characteristics"""

        if not center.latitude or not center.longitude:
            raise ValueError("Center must have coordinates")

        # Base parameters
        center_lat = float(center.latitude)
        center_lng = float(center.longitude)
        base_radius = float(center.service_radius_miles or 15)

        # Create county-specific and office-type-specific shapes
        county = center.county_served or "Unknown"
        office_type = center.office_type or "Main"

        # Determine shape characteristics based on county and office type
        shape_params = self.get_shape_parameters(county, office_type, base_radius)

        # Generate polygon based on parameters
        polygon = self.create_geometric_polygon(center_lat, center_lng, shape_params)

        return polygon

    def get_shape_parameters(self, county, office_type, base_radius):
        """Get shape parameters based on county characteristics and office type"""

        # California geographic regions and their characteristics
        southern_counties = {
            "Los Angeles",
            "Orange",
            "Riverside",
            "San Bernardino",
            "Ventura",
            "Santa Barbara",
            "Kern",
            "Imperial",
            "San Diego",
        }

        northern_counties = {
            "San Francisco",
            "Alameda",
            "Contra Costa",
            "Marin",
            "San Mateo",
            "Santa Clara",
            "Solano",
            "Napa",
            "Sonoma",
        }

        central_valley_counties = {
            "Fresno",
            "Kern",
            "Kings",
            "Madera",
            "Merced",
            "San Joaquin",
            "Stanislaus",
            "Tulare",
            "Sacramento",
            "Yolo",
        }

        mountain_counties = {
            "Alpine",
            "Amador",
            "Calaveras",
            "El Dorado",
            "Inyo",
            "Mariposa",
            "Mono",
            "Nevada",
            "Placer",
            "Plumas",
            "Sierra",
            "Tuolumne",
        }

        coastal_counties = {
            "Santa Barbara",
            "San Luis Obispo",
            "Monterey",
            "Santa Cruz",
            "San Mateo",
            "Marin",
            "Mendocino",
            "Humboldt",
            "Del Norte",
        }

        # Base shape parameters
        params = {
            "radius": base_radius * 1609.34,  # Convert miles to meters
            "vertices": 12,  # Number of polygon vertices
            "irregularity": 0.3,  # How irregular the shape is (0-1)
            "spikiness": 0.2,  # How spiky the edges are (0-1)
            "rotation": 0,  # Rotation angle in degrees
            "elongation": 1.0,  # Width/height ratio
            "elongation_angle": 0,  # Direction of elongation
        }

        # Adjust based on county type
        if county in southern_counties:
            # Southern California - more urban, rectangular patterns
            params["vertices"] = 8
            params["irregularity"] = 0.4
            params["spikiness"] = 0.1
            if county == "Los Angeles":
                params["elongation"] = 1.3
                params["elongation_angle"] = 45  # Northwest-Southeast
            elif county == "Riverside":
                params["elongation"] = 1.6
                params["elongation_angle"] = 90  # North-South
            elif county == "San Bernardino":
                params["elongation"] = 2.0
                params["elongation_angle"] = 15  # Northeast-Southwest

        elif county in northern_counties:
            # Northern California - follow bay and hills
            params["vertices"] = 10
            params["irregularity"] = 0.5
            params["spikiness"] = 0.3
            if county == "San Francisco":
                params["radius"] *= 0.5  # Smaller, city boundaries
                params["vertices"] = 6
            elif county in ["Alameda", "Contra Costa"]:
                params["elongation"] = 1.4
                params["elongation_angle"] = 45

        elif county in central_valley_counties:
            # Central Valley - agricultural, more regular shapes
            params["vertices"] = 6
            params["irregularity"] = 0.2
            params["spikiness"] = 0.1
            params["elongation"] = 1.8
            params["elongation_angle"] = 0  # North-South valley

        elif county in mountain_counties:
            # Mountain counties - irregular due to terrain
            params["vertices"] = 16
            params["irregularity"] = 0.7
            params["spikiness"] = 0.4
            params["elongation"] = 1.2

        elif county in coastal_counties:
            # Coastal counties - follow coastline
            params["vertices"] = 14
            params["irregularity"] = 0.6
            params["spikiness"] = 0.5
            params["elongation"] = 1.5
            params["elongation_angle"] = 15  # Follow coast angle

        # Adjust based on office type
        if office_type == "Satellite":
            params["radius"] *= 0.7
            params["vertices"] = max(6, params["vertices"] - 2)
        elif office_type == "Outreach":
            params["radius"] *= 0.4
            params["vertices"] = max(5, params["vertices"] - 4)
            params["elongation"] *= 1.5  # More elongated outreach areas
        elif office_type == "Administrative":
            params["radius"] *= 0.3
            params["vertices"] = 6
            params["irregularity"] = 0.1

        return params

    def create_geometric_polygon(self, center_lat, center_lng, params):
        """Create a realistic polygon with the given parameters"""

        vertices = params["vertices"]
        radius = params["radius"]
        irregularity = params["irregularity"]
        spikiness = params["spikiness"]
        rotation = math.radians(params["rotation"])
        elongation = params["elongation"]
        elongation_angle = math.radians(params["elongation_angle"])

        # Convert center to projected coordinates (approximate)
        # Using simple conversion: 1 degree ≈ 111,319.9 meters
        meters_per_degree_lat = 111319.9
        meters_per_degree_lng = meters_per_degree_lat * math.cos(
            math.radians(center_lat)
        )

        # Generate irregular polygon vertices
        coordinates = []

        # Create base angular steps
        angle_steps = []
        lower = (2 * math.pi / vertices) - irregularity
        upper = (2 * math.pi / vertices) + irregularity
        cumulative_angle = 0

        for i in range(vertices):
            angle = random.uniform(lower, upper)
            cumulative_angle += angle
            angle_steps.append(cumulative_angle)

        # Normalize to full circle
        for i in range(len(angle_steps)):
            angle_steps[i] = angle_steps[i] * (2 * math.pi) / cumulative_angle

        # Generate points
        for angle in angle_steps:
            # Add spikiness variation
            current_radius = radius * (1 + random.uniform(-spikiness, spikiness))

            # Apply elongation
            x = current_radius * math.cos(angle)
            y = current_radius * math.sin(angle) * elongation

            # Rotate for elongation direction
            rotated_x = x * math.cos(elongation_angle) - y * math.sin(elongation_angle)
            rotated_y = x * math.sin(elongation_angle) + y * math.cos(elongation_angle)

            # Apply final rotation
            final_x = rotated_x * math.cos(rotation) - rotated_y * math.sin(rotation)
            final_y = rotated_x * math.sin(rotation) + rotated_y * math.cos(rotation)

            # Convert back to lat/lng
            lat = center_lat + (final_y / meters_per_degree_lat)
            lng = center_lng + (final_x / meters_per_degree_lng)

            coordinates.append((lng, lat))

        # Close the polygon
        coordinates.append(coordinates[0])

        # Create polygon
        polygon = Polygon(coordinates)

        # Ensure the polygon is valid
        if not polygon.valid:
            polygon = polygon.buffer(0)  # Fix any self-intersections

        # Convert to MultiPolygon for the database field
        multipolygon = MultiPolygon([polygon])

        return multipolygon

    def degrees_to_meters(self, degrees, latitude):
        """Convert degrees to meters at given latitude"""
        meters_per_degree = 111319.9 * math.cos(math.radians(latitude))
        return degrees * meters_per_degree
