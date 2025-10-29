from django.core.management.base import BaseCommand
from locations.models import RegionalCenter


class Command(BaseCommand):
    help = "Populate Eastern Los Angeles Regional Center (covers Pasadena area) with proper ZIP codes"

    def handle(self, *args, **options):
        try:
            # Pasadena is served by Eastern Los Angeles Regional Center (ELARC)
            elarc = RegionalCenter.objects.filter(
                regional_center__icontains="eastern"
            ).first()
            if elarc:
                # Add the ZIP codes for Pasadena and surrounding areas served by ELARC
                elarc_zip_codes = [
                    # Pasadena
                    "91101",
                    "91102",
                    "91103",
                    "91104",
                    "91105",
                    "91106",
                    "91107",
                    "91108",
                    "91109",
                    "91110",
                    "91114",
                    "91115",
                    "91116",
                    "91117",
                    "91121",
                    "91123",
                    "91124",
                    "91125",
                    "91126",
                    "91129",
                    "91131",
                    "91175",
                    "91182",
                    "91184",
                    "91185",
                    "91188",
                    "91189",
                    "91199",
                    # Altadena
                    "91001",
                    "91003",
                    # La Canada Flintridge
                    "91011",
                    "91012",
                    # Sierra Madre
                    "91024",
                    "91025",
                    # South Pasadena
                    "91030",
                    "91031",
                    # San Marino
                    "91108",
                    "91118",
                    # Alhambra
                    "91801",
                    "91802",
                    "91803",
                    "91804",
                    "91841",
                    "91896",
                    "91899",
                    # San Gabriel
                    "91775",
                    "91776",
                    "91778",
                    # Temple City
                    "91780",
                    # Arcadia
                    "91006",
                    "91007",
                    "91066",
                    "91077",
                    # Monrovia
                    "91016",
                    "91017",
                    # Duarte
                    "91008",
                    "91009",
                    "91010",
                    # Monterey Park
                    "91754",
                    "91755",
                    "91756",
                    # Rosemead
                    "91770",
                    "91771",
                    "91772",
                    # El Monte
                    "91731",
                    "91732",
                    "91733",
                    "91734",
                    "91735",
                    # South El Monte
                    "91733",
                    # Baldwin Park
                    "91706",
                    # West Covina
                    "91790",
                    "91791",
                    "91792",
                    "91793",
                    # Covina
                    "91722",
                    "91723",
                    "91724",
                    # Irwindale
                    "91706",
                    # Azusa
                    "91702",
                    # Glendora
                    "91740",
                    "91741",
                ]

                # Remove duplicates and sort
                elarc_zip_codes = sorted(list(set(elarc_zip_codes)))

                elarc.zip_codes = elarc_zip_codes
                elarc.save()

                self.stdout.write(
                    self.style.SUCCESS(
                        f"✅ Updated Eastern LA Regional Center with {len(elarc_zip_codes)} ZIP codes"
                    )
                )
                self.stdout.write(
                    f'ZIP 91101 (Pasadena) included: {"91101" in elarc.zip_codes}'
                )
                self.stdout.write(
                    f'ZIP 91106 (Pasadena) included: {"91106" in elarc.zip_codes}'
                )
            else:
                self.stdout.write(
                    self.style.ERROR("Eastern Los Angeles Regional Center not found")
                )
                self.stdout.write("Available regional centers:")
                for rc in RegionalCenter.objects.all():
                    self.stdout.write(f"  - {rc.regional_center}")
        except Exception as e:
            self.stdout.write(self.style.ERROR(f"Error: {e}"))
