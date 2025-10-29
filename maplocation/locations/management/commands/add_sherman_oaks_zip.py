"""
Management command to add Sherman Oaks ZIP code 91403 to North LA County Regional Center
"""

from django.core.management.base import BaseCommand
from locations.models import RegionalCenter


class Command(BaseCommand):
    help = "Add Sherman Oaks ZIP code 91403 to North LA County Regional Center"

    def handle(self, *args, **options):
        # Find North LA County Regional Center
        try:
            nlacrc = RegionalCenter.objects.get(
                regional_center="North Los Angeles County Regional Center"
            )
        except RegionalCenter.DoesNotExist:
            self.stdout.write(
                self.style.ERROR(
                    "North Los Angeles County Regional Center not found in database"
                )
            )
            return

        # Add ZIP code 91403 (Sherman Oaks)
        zip_code = "91403"

        if nlacrc.add_zip_code(zip_code):
            self.stdout.write(
                self.style.SUCCESS(
                    f"Successfully added ZIP {zip_code} (Sherman Oaks) to {nlacrc.regional_center}"
                )
            )
        else:
            self.stdout.write(
                self.style.WARNING(
                    f"ZIP {zip_code} already exists in {nlacrc.regional_center}"
                )
            )
