from django.core.management.base import BaseCommand
from locations.models import RegionalCenter


class Command(BaseCommand):
    help = "Populate Harbor Regional Center with proper ZIP codes including 90742"

    def handle(self, *args, **options):
        try:
            harbor = RegionalCenter.objects.filter(
                regional_center__icontains="harbor"
            ).first()
            if harbor:
                # Add the ZIP codes that should be in Harbor Regional Center
                harbor_zip_codes = [
                    "90501",
                    "90502",
                    "90503",
                    "90504",
                    "90505",
                    "90506",
                    "90507",
                    "90508",
                    "90509",
                    "90510",
                    "90710",
                    "90712",
                    "90713",
                    "90715",
                    "90716",
                    "90717",
                    "90720",
                    "90721",
                    "90723",
                    "90731",
                    "90732",
                    "90733",
                    "90740",
                    "90742",
                    "90743",
                    "90744",
                    "90745",
                    "90746",
                    "90747",
                    "90748",
                    "90749",
                    "90755",
                    "90780",
                    "90781",
                    "90782",
                    "90783",
                    "90784",
                    "90785",
                    "90786",
                    "90787",
                    "90788",
                    "90789",
                    "90790",
                    "90791",
                    "90792",
                    "90793",
                    "90794",
                    "90795",
                    "90796",
                    "90797",
                    "90798",
                    "90799",
                    "90801",
                    "90802",
                    "90803",
                    "90804",
                    "90805",
                    "90806",
                    "90807",
                    "90808",
                    "90809",
                    "90810",
                    "90813",
                    "90814",
                    "90815",
                    "90822",
                    "90831",
                    "90832",
                    "90833",
                    "90834",
                    "90835",
                    "90840",
                    "90842",
                    "90844",
                    "90846",
                    "90847",
                    "90848",
                    "90853",
                    "90888",
                    "90899",
                ]

                harbor.zip_codes = harbor_zip_codes
                harbor.save()

                self.stdout.write(
                    self.style.SUCCESS(
                        f"✅ Updated Harbor Regional Center with {len(harbor_zip_codes)} ZIP codes"
                    )
                )
                self.stdout.write(f'ZIP 90742 included: {"90742" in harbor.zip_codes}')
            else:
                self.stdout.write(self.style.ERROR("Harbor Regional Center not found"))
        except Exception as e:
            self.stdout.write(self.style.ERROR(f"Error: {e}"))
