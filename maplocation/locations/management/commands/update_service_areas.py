from django.core.management.base import BaseCommand
from locations.models import RegionalCenter


class Command(BaseCommand):
    help = "Update service_areas (cities) for LA Regional Centers"

    def handle(self, *args, **options):
        # Service areas data for LA Regional Centers
        service_areas_data = {
            "Eastern Los Angeles Regional Center": [
                "Alhambra",
                "Bell",
                "Bell Gardens",
                "Commerce",
                "Cudahy",
                "East Los Angeles",
                "Huntington Park",
                "Maywood",
                "Montebello",
                "South Gate",
                "Vernon",
                "Downey",
                "Norwalk",
                "La Mirada",
                "Santa Fe Springs",
                "Whittier",
                "Pico Rivera",
                "Monterey Park",
                "San Gabriel",
            ],
            "Frank D. Lanterman Regional Center": [
                "Central Los Angeles",
                "Hollywood",
                "Glendale",
                "Burbank",
                "Pasadena",
                "Eagle Rock",
                "Highland Park",
                "Silver Lake",
                "Los Feliz",
                "La Canada Flintridge",
                "Altadena",
                "Sierra Madre",
                "San Marino",
                "South Pasadena",
                "Koreatown",
            ],
            "Harbor Regional Center": [
                "Torrance",
                "Long Beach",
                "San Pedro",
                "Wilmington",
                "Carson",
                "Harbor City",
                "Lomita",
                "Palos Verdes",
                "Rancho Palos Verdes",
                "Rolling Hills",
                "Rolling Hills Estates",
                "Redondo Beach",
                "Manhattan Beach",
                "Hermosa Beach",
                "El Segundo",
                "Hawthorne",
                "Gardena",
                "Lawndale",
            ],
            "North Los Angeles County Regional Center": [
                "San Fernando Valley",
                "Santa Clarita Valley",
                "Antelope Valley",
                "Van Nuys",
                "North Hollywood",
                "Sherman Oaks",
                "Encino",
                "Tarzana",
                "Woodland Hills",
                "Canoga Park",
                "Winnetka",
                "Reseda",
                "Northridge",
                "Granada Hills",
                "Mission Hills",
                "Sylmar",
                "Pacoima",
                "Arleta",
                "Sun Valley",
                "Valley Village",
                "Studio City",
                "Toluca Lake",
                "Santa Clarita",
                "Valencia",
                "Canyon Country",
                "Newhall",
                "Castaic",
                "Lancaster",
                "Palmdale",
                "Quartz Hill",
                "Lake Los Angeles",
                "Littlerock",
                "Pearblossom",
                "Acton",
            ],
            "San Gabriel/Pomona Regional Center": [
                "San Gabriel Valley (eastern portion)",
                "Pomona Valley",
                "Diamond Bar",
                "Claremont",
                "La Verne",
                "San Dimas",
                "Glendora",
                "Covina",
                "West Covina",
                "Baldwin Park",
                "El Monte",
                "South El Monte",
                "Temple City",
                "Rosemead",
                "San Gabriel",
                "Monterey Park",
                "Walnut",
                "Pomona",
                "La Puente",
                "Hacienda Heights",
                "Rowland Heights",
                "Azusa",
                "Duarte",
                "Monrovia",
                "Arcadia",
                "Irwindale",
                "City of Industry",
            ],
            "South Central Los Angeles Regional Center": [
                "South Los Angeles",
                "Watts",
                "Compton",
                "Lynwood",
                "Inglewood",
                "Lennox",
                "Hyde Park",
                "Crenshaw",
                "Baldwin Hills",
                "View Park",
                "Ladera Heights",
                "Windsor Hills",
                "Westmont",
                "Willowbrook",
                "Athens",
                "Florence",
                "Green Meadows",
                "Vermont Square",
                "Leimert Park",
                "Paramount",
                "Bellflower",
                "Downey",
                "South Gate",
            ],
            "Westside Regional Center": [
                "West Los Angeles",
                "Santa Monica",
                "Culver City",
                "Marina del Rey",
                "Venice",
                "Mar Vista",
                "Del Rey",
                "Playa del Rey",
                "Playa Vista",
                "Westchester",
                "Cheviot Hills",
                "Pico-Robertson",
                "Beverly Hills",
                "West Hollywood",
                "Brentwood",
                "Pacific Palisades",
                "Malibu",
                "Sawtelle",
                "Palms",
                "Century City",
                "Westwood",
                "UCLA Area",
            ],
        }

        self.stdout.write("Updating service_areas for LA Regional Centers...")

        updated_count = 0
        errors_count = 0

        for center_name, cities in service_areas_data.items():
            try:
                # Find the regional center by name
                centers = RegionalCenter.objects.filter(
                    regional_center__icontains=center_name
                )

                if not centers.exists():
                    self.stdout.write(
                        self.style.WARNING(f"⚠ No match found for: {center_name}")
                    )
                    continue

                # Update the first matching center
                regional_center = centers.first()

                # Update only the service_areas field
                RegionalCenter.objects.filter(id=regional_center.id).update(
                    service_areas=cities
                )

                updated_count += 1
                self.stdout.write(
                    f"✓ Updated {center_name} with {len(cities)} cities"
                )

            except Exception as e:
                errors_count += 1
                self.stdout.write(
                    self.style.ERROR(f"✗ Error updating {center_name}: {e}")
                )

        self.stdout.write(
            self.style.SUCCESS(
                f"\nUpdate complete!\n"
                f"✓ Updated: {updated_count}\n"
                f"✗ Errors: {errors_count}"
            )
        )

