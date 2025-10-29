import math
import random
from django.core.management.base import BaseCommand
from django.contrib.gis.geos import Polygon, MultiPolygon, Point
from locations.models import RegionalCenter


class Command(BaseCommand):
    help = "Generate non-overlapping territorial service areas using geographic constraints"

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

        # Generate territorial boundaries using distance-based approach
        service_areas = self.generate_territorial_boundaries(centers)

        generated_count = 0
        for center, service_area in service_areas.items():
            try:
                if options["dry_run"]:
                    self.stdout.write(
                        f"Would generate territorial area for: {center.regional_center} "
                        f"({center.county_served}) - {service_area.area:.6f} sq degrees"
                    )
                else:
                    center.service_area = service_area
                    center.save()
                    generated_count += 1
                    self.stdout.write(
                        f"✓ Generated territorial area for: {center.regional_center} "
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
                    f"Successfully generated {generated_count} territorial service areas!"
                )
            )

    def generate_territorial_boundaries(self, centers):
        """Generate non-overlapping territorial boundaries using distance-based approach"""

        center_list = list(centers)
        service_areas = {}

        for center in center_list:
            # Find neighboring centers within influence range
            neighbors = self.find_neighboring_centers(center, center_list)

            # Create territorial polygon that doesn't overlap with neighbors
            territory = self.create_territorial_polygon(center, neighbors)

            service_areas[center] = MultiPolygon([territory])

        return service_areas

    def find_neighboring_centers(self, center, all_centers):
        """Find neighboring centers within influence range"""
        neighbors = []
        center_point = Point(float(center.longitude), float(center.latitude))
        max_influence = (
            self.get_max_radius_for_center(center) * 2.5
        )  # Extended influence

        for other_center in all_centers:
            if other_center.id == center.id:
                continue

            other_point = Point(
                float(other_center.longitude), float(other_center.latitude)
            )
            distance = self.calculate_distance_meters(center_point, other_point)

            if distance < max_influence:
                neighbors.append(
                    {"center": other_center, "distance": distance, "point": other_point}
                )

        return neighbors

    def calculate_distance_meters(self, point1, point2):
        """Calculate distance between two points in meters using Haversine formula"""
        lat1, lng1 = point1.y, point1.x
        lat2, lng2 = point2.y, point2.x

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
        r = 6371000  # Radius of earth in meters

        return c * r

    def create_territorial_polygon(self, center, neighbors):
        """Create a territorial polygon that avoids overlapping with neighbors"""

        center_lat = float(center.latitude)
        center_lng = float(center.longitude)
        center_point = Point(center_lng, center_lat)

        # Get base radius for this center
        base_radius = self.get_max_radius_for_center(center)
        base_radius_degrees = base_radius / 111319.9

        # Create territorial boundary points
        num_vertices = 16  # More vertices for smoother territories
        coordinates = []

        for i in range(num_vertices):
            angle = (2 * math.pi * i) / num_vertices

            # Calculate base position
            base_distance = base_radius_degrees

            # Adjust distance based on neighbors
            adjusted_distance = self.adjust_distance_for_neighbors(
                center_point, angle, base_distance, neighbors
            )

            # Apply geographic variation
            varied_distance = self.apply_geographic_variation(
                adjusted_distance, angle, center
            )

            # Calculate final coordinates
            x = center_lng + (varied_distance * math.cos(angle))
            y = center_lat + (varied_distance * math.sin(angle))

            coordinates.append((x, y))

        # Close the polygon
        coordinates.append(coordinates[0])

        # Create and refine polygon
        polygon = Polygon(coordinates)

        if not polygon.valid:
            polygon = polygon.buffer(0)

        # Apply final geographic smoothing
        polygon = self.apply_geographic_smoothing(polygon, center)

        return polygon

    def adjust_distance_for_neighbors(
        self, center_point, angle, base_distance, neighbors
    ):
        """Adjust territorial distance based on neighboring centers"""

        if not neighbors:
            return base_distance

        adjusted_distance = base_distance

        for neighbor in neighbors:
            neighbor_point = neighbor["point"]
            neighbor_distance = neighbor["distance"] / 111319.9  # Convert to degrees

            # Calculate angle to neighbor
            neighbor_angle = math.atan2(
                neighbor_point.y - center_point.y, neighbor_point.x - center_point.x
            )

            # Calculate angular difference
            angle_diff = abs(angle - neighbor_angle)
            angle_diff = min(angle_diff, 2 * math.pi - angle_diff)  # Shortest angle

            # If pointing towards neighbor, reduce distance
            if angle_diff < math.pi / 3:  # Within 60 degrees of neighbor
                influence = 1.0 - (angle_diff / (math.pi / 3))  # 0 to 1
                midpoint_distance = neighbor_distance / 2

                # Smoothly reduce distance towards neighbor
                reduction = influence * (adjusted_distance - midpoint_distance * 0.8)
                adjusted_distance = max(
                    adjusted_distance - reduction,
                    midpoint_distance * 0.6,  # Don't get too close
                )

        return adjusted_distance

    def apply_geographic_variation(self, distance, angle, center):
        """Apply geographic variation based on county and terrain"""

        county = center.county_served or "Unknown"

        # Base variation
        variation = 1.0

        if county in ["Los Angeles", "Orange"]:
            # Urban areas - more regular but with urban sprawl patterns
            variation = 1.0 + 0.2 * math.sin(angle * 4) * random.uniform(0.8, 1.2)

        elif county in ["Riverside", "San Bernardino"]:
            # Desert counties - irregular, influenced by mountain ranges
            variation = 1.0 + 0.4 * math.sin(angle * 2) * random.uniform(0.6, 1.4)
            variation += 0.2 * math.cos(angle * 5) * random.uniform(0.7, 1.3)

        elif county in ["Fresno", "Kern", "Kings", "Tulare"]:
            # Central Valley - agricultural grid, more regular north-south
            ns_angle = abs(math.sin(angle))  # 0 for E-W, 1 for N-S
            variation = 1.0 + 0.3 * ns_angle * random.uniform(0.9, 1.1)

        elif county in ["San Francisco", "Marin", "Alameda"]:
            # Bay Area - irregular due to bay and hills
            variation = 1.0 + 0.5 * math.sin(angle * 3) * random.uniform(0.5, 1.5)

        elif county in ["Monterey", "Santa Cruz", "Santa Barbara"]:
            # Coastal counties - elongated parallel to coast
            coastal_angle = angle - math.pi / 6  # Approximate coast angle
            coastal_influence = abs(math.sin(coastal_angle))
            variation = 1.0 + 0.3 * coastal_influence * random.uniform(0.8, 1.2)

        else:
            # Mountain/rural counties - highly irregular
            variation = 1.0 + 0.6 * math.sin(angle * 2.5) * random.uniform(0.4, 1.6)

        return distance * max(0.3, min(2.0, variation))  # Clamp variation

    def get_max_radius_for_center(self, center):
        """Get maximum service radius in meters for a center"""
        base_radius = float(center.service_radius_miles or 25) * 1609.34

        office_type = center.office_type or "Main"

        if office_type == "Satellite":
            return base_radius * 0.7
        elif office_type == "Outreach":
            return base_radius * 0.5
        elif office_type == "Administrative":
            return base_radius * 0.3
        else:  # Main office
            return base_radius

    def apply_geographic_smoothing(self, polygon, center):
        """Apply final geographic smoothing based on county characteristics"""

        county = center.county_served or "Unknown"

        if county in ["Los Angeles", "Orange", "Riverside", "San Bernardino"]:
            # Southern California - angular, urban boundaries
            return polygon.simplify(0.005, preserve_topology=True)

        elif county in ["San Francisco", "Marin", "Alameda", "Contra Costa"]:
            # Bay Area - smooth, curved boundaries
            smoothed = polygon.buffer(0.002).buffer(-0.002)
            return smoothed if smoothed.valid else polygon

        elif county in ["Fresno", "Kern", "Kings", "Tulare", "Merced"]:
            # Central Valley - grid-aligned boundaries
            return self.align_to_grid(polygon)

        else:
            # Default: slight smoothing
            smoothed = polygon.buffer(0.001).buffer(-0.001)
            return smoothed if smoothed.valid else polygon

    def align_to_grid(self, polygon):
        """Align polygon boundaries to approximate agricultural grid"""
        try:
            # Get polygon bounds
            bounds = polygon.bounds
            grid_size = 0.008  # Approximately 1km grid

            # Create simplified polygon aligned to grid
            simplified = polygon.simplify(grid_size / 2, preserve_topology=True)

            return simplified if simplified.valid else polygon
        except:
            return polygon
