"""
Management command to add missing Sherman Oaks/Van Nuys area ZIP codes to regional centers
"""

from django.core.management.base import BaseCommand
from locations.models import RegionalCenter


class Command(BaseCommand):
    help = "Add missing ZIP codes to their appropriate regional centers"

    def handle(self, *args, **options):
        # Sherman Oaks and Van Nuys area ZIPs that should be in North LA County Regional Center
        north_la_zips = [
            "91401",  # Van Nuys
            "91402",  # Panorama City
            "91403",  # Sherman Oaks
            "91405",  # Van Nuys
            "91406",  # Van Nuys
            "91411",  # Van Nuys
            "91423",  # Sherman Oaks
            "91436",  # Encino
        ]

        try:
            # Find North Los Angeles County Regional Center
            north_la = RegionalCenter.objects.filter(
                regional_center__icontains="North Los Angeles County"
            ).first()

            if not north_la:
                self.stdout.write(
                    self.style.ERROR(
                        "North Los Angeles County Regional Center not found!"
                    )
                )
                return

            self.stdout.write(f"Found: {north_la.regional_center} (ID: {north_la.id})")

            # Get current ZIP codes
            current_zips = set(north_la.zip_codes or [])
            original_count = len(current_zips)

            self.stdout.write(f"Current ZIP count: {original_count}")

            # Add missing ZIPs
            added_count = 0
            for zip_code in north_la_zips:
                if zip_code not in current_zips:
                    current_zips.add(zip_code)
                    added_count += 1
                    self.stdout.write(self.style.SUCCESS(f"  ✅ Added: {zip_code}"))
                else:
                    self.stdout.write(f"  ℹ️  Already has: {zip_code}")

            # Update the regional center
            if added_count > 0:
                north_la.zip_codes = sorted(list(current_zips))
                north_la.save()
                self.stdout.write(
                    self.style.SUCCESS(
                        f"\n✅ Successfully added {added_count} ZIP codes!"
                    )
                )
                self.stdout.write(f"Total ZIPs now: {len(north_la.zip_codes)}")
            else:
                self.stdout.write(
                    self.style.WARNING("\nNo new ZIP codes needed to be added.")
                )

        except Exception as e:
            self.stdout.write(self.style.ERROR(f"Error: {str(e)}"))
