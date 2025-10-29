from django.core.management.base import BaseCommand
from locations.models import RegionalCenter


class Command(BaseCommand):
    help = "Populate San Gabriel/Pomona Regional Center with proper ZIP codes"

    def handle(self, *args, **options):
        try:
            san_gabriel = RegionalCenter.objects.filter(
                regional_center__icontains="san gabriel"
            ).first()
            if san_gabriel:
                # Add the ZIP codes for San Gabriel/Pomona Regional Center service area
                # This covers Pomona, San Gabriel, Arcadia, Pasadena (partial), and surrounding areas
                san_gabriel_zip_codes = [
                    # Pomona area
                    "91766",
                    "91767",
                    "91768",
                    "91769",
                    # San Gabriel area
                    "91775",
                    "91776",
                    "91778",
                    # Arcadia
                    "91006",
                    "91007",
                    # Monrovia
                    "91016",
                    "91017",
                    # La Verne
                    "91750",
                    # Claremont
                    "91711",
                    # Diamond Bar
                    "91765",
                    # Walnut
                    "91789",
                    "91790",
                    # West Covina
                    "91790",
                    "91791",
                    "91792",
                    "91793",
                    # Covina
                    "91722",
                    "91723",
                    "91724",
                    # Glendora
                    "91740",
                    "91741",
                    # Azusa
                    "91702",
                    # Duarte
                    "91010",
                    # Irwindale
                    "91706",
                    # Baldwin Park
                    "91706",
                    # El Monte
                    "91731",
                    "91732",
                    "91733",
                    "91734",
                    "91735",
                    # South El Monte
                    "91733",
                    # Temple City
                    "91780",
                    # Rosemead
                    "91770",
                    # San Marino
                    "91108",
                    "91118",
                    # Sierra Madre
                    "91024",
                    # Altadena (partial overlap with Pasadena)
                    "91001",
                    # La Canada Flintridge
                    "91011",
                    "91012",
                    # Chino (partial)
                    "91708",
                    "91710",
                    # Rowland Heights
                    "91748",
                    # Hacienda Heights
                    "91745",
                    # Industry
                    "91746",
                    "91747",
                    # Alhambra
                    "91801",
                    "91802",
                    "91803",
                    "91804",
                    "91896",
                    "91899",
                    # Monterey Park
                    "91754",
                    "91755",
                    # South Pasadena
                    "91030",
                    "91031",
                ]

                # Remove duplicates and sort
                san_gabriel_zip_codes = sorted(list(set(san_gabriel_zip_codes)))

                san_gabriel.zip_codes = san_gabriel_zip_codes
                san_gabriel.save()

                self.stdout.write(
                    self.style.SUCCESS(
                        f"✅ Updated San Gabriel/Pomona Regional Center with {len(san_gabriel_zip_codes)} ZIP codes"
                    )
                )
                self.stdout.write(
                    f'ZIP 91769 (Pomona) included: {"91769" in san_gabriel.zip_codes}'
                )
                self.stdout.write(
                    f'ZIP 91776 (San Gabriel) included: {"91776" in san_gabriel.zip_codes}'
                )
            else:
                self.stdout.write(
                    self.style.ERROR("San Gabriel/Pomona Regional Center not found")
                )
        except Exception as e:
            self.stdout.write(self.style.ERROR(f"Error: {e}"))
