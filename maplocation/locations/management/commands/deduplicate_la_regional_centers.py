"""
Management command to deduplicate LA County Regional Centers
Keeps ONLY 7 entries - one for each of the 7 LA County Regional Centers
"""
from django.core.management.base import BaseCommand
from locations.models import RegionalCenter
from django.db.models import Count


class Command(BaseCommand):
    help = "Remove duplicate LA County Regional Centers - keep only 7 total"

    # The ONLY 7 LA County Regional Centers
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
        self.stdout.write("🔧 Deduplicating LA County Regional Centers...")
        self.stdout.write("   Target: EXACTLY 7 Regional Centers (one per name)")
        self.stdout.write("")

        total_deleted = 0

        for rc_name in self.LA_COUNTY_REGIONAL_CENTERS:
            # Get all instances with this name
            rcs = RegionalCenter.objects.filter(
                regional_center=rc_name
            )

            count = rcs.count()

            if count == 0:
                self.stdout.write(
                    self.style.ERROR(f"  ❌ NOT FOUND: {rc_name}")
                )
            elif count == 1:
                self.stdout.write(
                    self.style.SUCCESS(f"  ✅ OK (1 instance): {rc_name}")
                )
            else:
                # Keep the one with the MOST ZIP codes (best data)
                # If tied, keep the one with lowest ID (oldest)
                rcs_with_zips = [(rc, len(rc.zip_codes) if rc.zip_codes else 0) for rc in rcs]
                rcs_with_zips.sort(key=lambda x: (-x[1], x[0].id))  # Sort by ZIP count desc, then ID asc
                keep = rcs_with_zips[0][0]
                duplicates = [rc for rc, _ in rcs_with_zips[1:]]

                # Show which one we're keeping
                zip_count = len(keep.zip_codes) if keep.zip_codes else 0
                self.stdout.write(
                    self.style.WARNING(
                        f"  🔍 FOUND {count} instances of: {rc_name}"
                    )
                )
                self.stdout.write(
                    f"     KEEPING: ID {keep.id} ({zip_count} ZIP codes)"
                )

                # Delete duplicates
                for dup in duplicates:
                    dup_zip_count = len(dup.zip_codes) if dup.zip_codes else 0
                    self.stdout.write(
                        f"     DELETING: ID {dup.id} ({dup_zip_count} ZIP codes)"
                    )
                    dup.delete()
                    total_deleted += 1

        self.stdout.write("")
        self.stdout.write(
            self.style.SUCCESS(
                f"✨ Deleted {total_deleted} duplicate Regional Center(s)"
            )
        )

        # Verify final count
        la_county_count = RegionalCenter.objects.filter(
            is_la_regional_center=True
        ).count()

        self.stdout.write("")
        self.stdout.write("📊 Final Status:")
        self.stdout.write(f"  LA County Regional Centers: {la_county_count}")

        if la_county_count == 7:
            self.stdout.write("")
            self.stdout.write(
                self.style.SUCCESS(
                    "✅ SUCCESS! Exactly 7 LA County Regional Centers remain."
                )
            )
        else:
            self.stdout.write("")
            self.stdout.write(
                self.style.ERROR(
                    f"⚠️  WARNING: Expected 7 LA County RCs, but found {la_county_count}"
                )
            )

        # List the 7 remaining RCs
        self.stdout.write("")
        self.stdout.write("📋 The 7 LA County Regional Centers:")
        for rc in RegionalCenter.objects.filter(is_la_regional_center=True).order_by('regional_center'):
            zip_count = len(rc.zip_codes) if rc.zip_codes else 0
            self.stdout.write(f"  • {rc.regional_center} (ID: {rc.id}, {zip_count} ZIPs)")
