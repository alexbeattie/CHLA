"""
Management command to deduplicate ALL Regional Centers in California
Keeps ONLY 21 entries - one for each of the 21 California Regional Centers
"""
from django.core.management.base import BaseCommand
from locations.models import RegionalCenter


class Command(BaseCommand):
    help = "Remove duplicate Regional Centers - keep only 21 total (one per RC)"

    def handle(self, *args, **options):
        self.stdout.write("🔧 Deduplicating ALL Regional Centers...")
        self.stdout.write("   Target: EXACTLY 21 Regional Centers (one per name)")
        self.stdout.write("")

        total_deleted = 0

        # Get all unique RC names
        unique_names = RegionalCenter.objects.values_list('regional_center', flat=True).distinct()

        for rc_name in unique_names:
            # Get all instances with this name
            rcs = list(RegionalCenter.objects.filter(regional_center=rc_name))
            count = len(rcs)

            if count == 1:
                self.stdout.write(
                    self.style.SUCCESS(f"  ✅ OK (1 instance): {rc_name}")
                )
            elif count > 1:
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
        total_count = RegionalCenter.objects.count()

        self.stdout.write("")
        self.stdout.write("📊 Final Status:")
        self.stdout.write(f"  Total Regional Centers: {total_count}")

        if total_count == 21:
            self.stdout.write("")
            self.stdout.write(
                self.style.SUCCESS(
                    "✅ SUCCESS! Exactly 21 Regional Centers remain."
                )
            )
        else:
            self.stdout.write("")
            self.stdout.write(
                self.style.ERROR(
                    f"⚠️  WARNING: Expected 21 RCs, but found {total_count}"
                )
            )

        # List all remaining RCs
        self.stdout.write("")
        self.stdout.write("📋 All 21 California Regional Centers:")
        for rc in RegionalCenter.objects.order_by('regional_center'):
            zip_count = len(rc.zip_codes) if rc.zip_codes else 0
            la_flag = "🌟 LA" if rc.is_la_regional_center else ""
            self.stdout.write(f"  • {rc.regional_center} (ID: {rc.id}, {zip_count} ZIPs) {la_flag}")
