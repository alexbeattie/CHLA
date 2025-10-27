"""
Management command to set is_la_regional_center=True for the 7 LA County Regional Centers
"""
from django.core.management.base import BaseCommand
from locations.models import RegionalCenter


class Command(BaseCommand):
    help = "Set is_la_regional_center=True for the 7 LA County Regional Centers"

    # The official 7 LA County Regional Centers
    LA_COUNTY_REGIONAL_CENTERS = [
        "North Los Angeles County Regional Center",
        "Eastern Los Angeles Regional Center",
        "Frank D. Lanterman Regional Center",
        "Harbor Regional Center",
        "San Gabriel/Pomona Regional Center",
        "South Central Los Angeles Regional Center",
        "Westside Regional Center",
    ]

    def handle(self, *args, **options):
        self.stdout.write("🔧 Fixing is_la_regional_center flags for LA County RCs...")
        self.stdout.write("")

        updated_count = 0
        not_found = []

        for rc_name in self.LA_COUNTY_REGIONAL_CENTERS:
            # Find all RCs with this name (there may be duplicates)
            rcs = RegionalCenter.objects.filter(regional_center=rc_name)

            if not rcs.exists():
                not_found.append(rc_name)
                self.stdout.write(
                    self.style.ERROR(f"  ❌ NOT FOUND: {rc_name}")
                )
                continue

            # Update all instances with this name
            count = rcs.count()
            updated = rcs.update(is_la_regional_center=True)

            if updated > 0:
                updated_count += updated
                if count > 1:
                    self.stdout.write(
                        self.style.WARNING(
                            f"  ⚠️  UPDATED {count} instances: {rc_name} (DUPLICATES EXIST!)"
                        )
                    )
                else:
                    self.stdout.write(
                        self.style.SUCCESS(f"  ✅ UPDATED: {rc_name}")
                    )

        self.stdout.write("")
        self.stdout.write(
            self.style.SUCCESS(
                f"✨ Updated {updated_count} Regional Center record(s)"
            )
        )

        if not_found:
            self.stdout.write("")
            self.stdout.write(
                self.style.ERROR(
                    f"⚠️  WARNING: {len(not_found)} LA County RC(s) not found in database:"
                )
            )
            for name in not_found:
                self.stdout.write(f"  - {name}")

        # Show summary
        self.stdout.write("")
        self.stdout.write("📊 Summary:")
        total_rcs = RegionalCenter.objects.count()
        la_county_rcs = RegionalCenter.objects.filter(
            is_la_regional_center=True
        ).count()
        other_rcs = total_rcs - la_county_rcs

        self.stdout.write(f"  Total Regional Centers in database: {total_rcs}")
        self.stdout.write(f"  LA County RCs (is_la_regional_center=True): {la_county_rcs}")
        self.stdout.write(f"  Other California RCs: {other_rcs}")

        if la_county_rcs > 7:
            self.stdout.write("")
            self.stdout.write(
                self.style.WARNING(
                    f"⚠️  WARNING: Expected 7 LA County RCs, found {la_county_rcs}. Duplicates likely exist."
                )
            )

        self.stdout.write("")
        self.stdout.write(
            self.style.SUCCESS(
                "✅ Done! You should now run 'deduplicate_regional_centers' to remove duplicates."
            )
        )
