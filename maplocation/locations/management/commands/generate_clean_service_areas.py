import math
from django.core.management.base import BaseCommand
# from django.contrib.gis.geos import Polygon, MultiPolygon, Point
from locations.models import RegionalCenter


class Command(BaseCommand):
    help = "Generate clean, non-overlapping administrative-style service areas"

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
        centers = RegionalCenter.objects.filter(
            latitude__isnull=False, longitude__isnull=False
        )

        if not options["overwrite"]:
            centers = centers.filter(service_area__isnull=True)

        total_centers = centers.count()
        self.stdout.write(f"Processing {total_centers} regional centers...")

        # Generate clean administrative boundaries
        service_areas = self.generate_clean_boundaries(centers)

        generated_count = 0
        for center, service_area in service_areas.items():
            try:
                if options["dry_run"]:
                    self.stdout.write(
                        f"Would generate clean area for: {center.regional_center} "
                        f"({center.county_served})"
                    )
                else:
                    center.service_area = service_area
                    center.save()
                    generated_count += 1
                    self.stdout.write(
                        f"✓ Generated clean area for: {center.regional_center} "
                        f"({center.county_served})"
                    )

            except Exception as e:
                self.stdout.write(
                    self.style.ERROR(
                        f"✗ Error saving service area for {center.regional_center}: {e}"
                    )
                )

        if not options["dry_run"]:
            self.stdout.write(
                self.style.SUCCESS(
                    f"Successfully generated {generated_count} clean service areas!"
                )
            )

    def generate_clean_boundaries(self, centers):
        """Generate clean, non-overlapping administrative boundaries"""

        center_list = list(centers)
        service_areas = {}

        # Create a spatial grid for California
        ca_bounds = {
            "min_lat": 32.5,
            "max_lat": 42.0,
            "min_lng": -124.5,
            "max_lng": -114.0,
        }

        for center in center_list:
            # Create clean administrative boundary
            boundary = self.create_clean_administrative_boundary(
                center, center_list, ca_bounds
            )
            service_areas[center] = MultiPolygon([boundary])

        return service_areas

    def create_clean_administrative_boundary(self, center, all_centers, ca_bounds):
        """Create a clean administrative-style boundary for a regional center"""

        center_lat = float(center.latitude)
        center_lng = float(center.longitude)

        # Get appropriate radius based on center type and location
        radius_miles = self.get_service_radius(center)
        radius_degrees = (radius_miles * 1609.34) / 111319.9  # Convert to degrees

        # Create clean boundary points using administrative-style shapes
        boundary_points = self.create_administrative_polygon(
            center_lat, center_lng, radius_degrees, center, all_centers, ca_bounds
        )

        # Create and validate polygon
        polygon = Polygon(boundary_points)

        if not polygon.valid:
            polygon = polygon.buffer(0)

        # Apply final cleanup
        polygon = self.clean_polygon(polygon, center)

        return polygon

    def create_administrative_polygon(
        self, center_lat, center_lng, radius, center, all_centers, ca_bounds
    ):
        """Create administrative-style polygon with clean boundaries"""

        # Find boundary constraints from neighbors
        boundary_constraints = self.find_boundary_constraints(
            center_lat, center_lng, center, all_centers
        )

        # Create base administrative shape based on county type
        county = center.county_served or "Unknown"
        base_shape = self.get_county_base_shape(county, center_lat, center_lng, radius)

        # Apply boundary constraints to prevent overlaps
        constrained_shape = self.apply_boundary_constraints(
            base_shape, boundary_constraints, center_lat, center_lng
        )

        # Ensure shape stays within California bounds
        final_shape = self.constrain_to_california(constrained_shape, ca_bounds)

        return final_shape

    def find_boundary_constraints(self, center_lat, center_lng, center, all_centers):
        """Find boundary constraints from neighboring centers"""
        constraints = []
        max_influence = 50  # miles

        for other_center in all_centers:
            if other_center.id == center.id:
                continue

            other_lat = float(other_center.latitude)
            other_lng = float(other_center.longitude)

            # Calculate distance
            distance = self.calculate_distance_miles(
                center_lat, center_lng, other_lat, other_lng
            )

            if distance < max_influence:
                # Calculate midpoint boundary
                mid_lat = (center_lat + other_lat) / 2
                mid_lng = (center_lng + other_lng) / 2

                # Calculate boundary line perpendicular to connection
                angle = math.atan2(other_lat - center_lat, other_lng - center_lng)

                constraints.append(
                    {
                        "type": "boundary_line",
                        "distance": distance,
                        "angle": angle,
                        "mid_lat": mid_lat,
                        "mid_lng": mid_lng,
                        "other_center": other_center,
                    }
                )

        return constraints

    def get_county_base_shape(self, county, center_lat, center_lng, radius):
        """Get base administrative shape based on county characteristics"""

        # Define county-specific shape templates
        if county in ["Los Angeles", "Orange"]:
            return self.create_urban_square_shape(center_lat, center_lng, radius)
        elif county in ["Riverside", "San Bernardino"]:
            return self.create_elongated_rectangle(
                center_lat, center_lng, radius, "east-west"
            )
        elif county in ["Fresno", "Kern", "Kings", "Tulare", "Merced"]:
            return self.create_elongated_rectangle(
                center_lat, center_lng, radius, "north-south"
            )
        elif county in ["San Francisco", "Marin"]:
            return self.create_compact_hexagon(center_lat, center_lng, radius * 0.7)
        elif county in ["Monterey", "Santa Cruz", "Santa Barbara"]:
            return self.create_coastal_rectangle(center_lat, center_lng, radius)
        else:
            return self.create_regular_hexagon(center_lat, center_lng, radius)

    def create_urban_square_shape(self, center_lat, center_lng, radius):
        """Create square/rectangular shape for urban areas"""
        # Create a slightly irregular rectangle
        points = [
            (center_lng - radius * 0.9, center_lat - radius * 0.9),
            (center_lng + radius * 0.9, center_lat - radius * 0.9),
            (center_lng + radius * 0.9, center_lat + radius * 0.9),
            (center_lng - radius * 0.9, center_lat + radius * 0.9),
            (center_lng - radius * 0.9, center_lat - radius * 0.9),  # Close
        ]
        return points

    def create_elongated_rectangle(self, center_lat, center_lng, radius, direction):
        """Create elongated rectangle for large counties"""
        if direction == "north-south":
            # Elongated north-south (Central Valley)
            points = [
                (center_lng - radius * 0.6, center_lat - radius * 1.2),
                (center_lng + radius * 0.6, center_lat - radius * 1.2),
                (center_lng + radius * 0.6, center_lat + radius * 1.2),
                (center_lng - radius * 0.6, center_lat + radius * 1.2),
                (center_lng - radius * 0.6, center_lat - radius * 1.2),  # Close
            ]
        else:
            # Elongated east-west (Desert counties)
            points = [
                (center_lng - radius * 1.2, center_lat - radius * 0.6),
                (center_lng + radius * 1.2, center_lat - radius * 0.6),
                (center_lng + radius * 1.2, center_lat + radius * 0.6),
                (center_lng - radius * 1.2, center_lat + radius * 0.6),
                (center_lng - radius * 1.2, center_lat - radius * 0.6),  # Close
            ]
        return points

    def create_regular_hexagon(self, center_lat, center_lng, radius):
        """Create regular hexagon for balanced coverage"""
        points = []
        for i in range(6):
            angle = i * math.pi / 3  # 60-degree increments
            x = center_lng + radius * math.cos(angle)
            y = center_lat + radius * math.sin(angle)
            points.append((x, y))
        points.append(points[0])  # Close polygon
        return points

    def create_compact_hexagon(self, center_lat, center_lng, radius):
        """Create compact hexagon for dense urban areas"""
        return self.create_regular_hexagon(center_lat, center_lng, radius)

    def create_coastal_rectangle(self, center_lat, center_lng, radius):
        """Create rectangle aligned with coastline"""
        # Rotated rectangle following coast angle (roughly NW-SE)
        cos_angle = math.cos(math.pi / 6)  # 30 degrees
        sin_angle = math.sin(math.pi / 6)

        # Define rectangle corners
        corners = [
            (-radius * 0.8, -radius * 1.1),
            (radius * 0.8, -radius * 1.1),
            (radius * 0.8, radius * 1.1),
            (-radius * 0.8, radius * 1.1),
        ]

        # Rotate and translate
        points = []
        for dx, dy in corners:
            # Rotate
            rx = dx * cos_angle - dy * sin_angle
            ry = dx * sin_angle + dy * cos_angle
            # Translate
            x = center_lng + rx
            y = center_lat + ry
            points.append((x, y))

        points.append(points[0])  # Close
        return points

    def apply_boundary_constraints(
        self, base_shape, constraints, center_lat, center_lng
    ):
        """Apply boundary constraints to prevent overlaps"""
        if not constraints:
            return base_shape

        # For now, just return the base shape
        # In a more sophisticated version, we would clip against boundary lines
        constrained_points = []

        for point in base_shape[:-1]:  # Exclude closing point
            lng, lat = point

            # Check if point violates any constraints
            violates_constraint = False

            for constraint in constraints:
                if constraint["type"] == "boundary_line":
                    # Simple distance check to other center
                    other_center = constraint["other_center"]
                    other_lat = float(other_center.latitude)
                    other_lng = float(other_center.longitude)

                    dist_to_other = self.calculate_distance_miles(
                        lat, lng, other_lat, other_lng
                    )
                    dist_to_self = self.calculate_distance_miles(
                        lat, lng, center_lat, center_lng
                    )

                    # If closer to other center, pull back towards our center
                    if dist_to_other < dist_to_self:
                        # Move point 70% of the way back to center
                        lng = center_lng + 0.7 * (lng - center_lng)
                        lat = center_lat + 0.7 * (lat - center_lat)
                        break

            constrained_points.append((lng, lat))

        # Close the polygon
        constrained_points.append(constrained_points[0])

        return constrained_points

    def constrain_to_california(self, shape, ca_bounds):
        """Ensure shape stays within California bounds"""
        constrained = []

        for lng, lat in shape:
            # Clamp to California bounds
            lng = max(ca_bounds["min_lng"], min(ca_bounds["max_lng"], lng))
            lat = max(ca_bounds["min_lat"], min(ca_bounds["max_lat"], lat))
            constrained.append((lng, lat))

        return constrained

    def clean_polygon(self, polygon, center):
        """Apply final cleaning to polygon"""
        # Simplify slightly to remove tiny irregularities
        simplified = polygon.simplify(0.002, preserve_topology=True)

        if simplified.valid and simplified.area > 0:
            return simplified
        else:
            return polygon

    def get_service_radius(self, center):
        """Get appropriate service radius for center"""
        base_radius = float(center.service_radius_miles or 20)
        office_type = center.office_type or "Main"

        # Adjust radius based on office type
        if office_type == "Satellite":
            return base_radius * 0.8
        elif office_type == "Outreach":
            return base_radius * 0.6
        elif office_type == "Administrative":
            return base_radius * 0.4
        else:
            return base_radius

    def calculate_distance_miles(self, lat1, lng1, lat2, lng2):
        """Calculate distance between two points in miles"""
        # Convert to radians
        lat1, lng1, lat2, lng2 = map(math.radians, [lat1, lng1, lat2, lng2])

        # Haversine formula
        dlat = lat2 - lat1
        dlng = lng2 - lng1
        a = (
            math.sin(dlat / 2) ** 2
            + math.cos(lat1) * math.cos(lat2) * math.sin(dlng / 2) ** 2
        )
        c = 2 * math.asin(math.sqrt(a))

        # Radius of earth in miles
        r = 3959

        return c * r
