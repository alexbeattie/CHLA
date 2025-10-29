#!/usr/bin/env python3
"""
Regional Centers Cleanup Script
Removes duplicate regional centers while preserving distinct office types.
"""

import os
import sys
import django
from django.db import transaction


def setup_django():
    """Setup Django environment"""
    os.environ.setdefault("DJANGO_SETTINGS_MODULE", "maplocation.settings")
    django.setup()


def analyze_duplicates():
    """Analyze the current duplicates"""
    from locations.models import RegionalCenter

    print("🔍 Analyzing Regional Center Duplicates")
    print("=" * 50)

    centers = RegionalCenter.objects.all()
    print(f"Total regional centers: {centers.count()}")

    # Group by regional_center name
    name_groups = {}
    for center in centers:
        name = center.regional_center.strip() if center.regional_center else "No Name"
        if name not in name_groups:
            name_groups[name] = []
        name_groups[name].append(center)

    duplicates = {
        name: centers for name, centers in name_groups.items() if len(centers) > 1
    }
    print(f"Regional centers with duplicates: {len(duplicates)}")

    total_duplicates = sum(len(centers) - 1 for centers in duplicates.values())
    print(f"Total duplicate entries to remove: {total_duplicates}")

    return duplicates


def cleanup_strategy():
    """Define cleanup strategy for each regional center"""
    from locations.models import RegionalCenter

    duplicates = analyze_duplicates()

    print("\n📋 Cleanup Strategy")
    print("=" * 50)

    cleanup_plan = {}

    for name, centers in duplicates.items():
        print(f"\n{name} ({len(centers)} entries):")

        # Group by office type
        office_types = {}
        for center in centers:
            office_type = center.office_type or "Unknown"
            if office_type not in office_types:
                office_types[office_type] = []
            office_types[office_type].append(center)

        # Determine which entries to keep
        keep_entries = []
        remove_entries = []

        for office_type, type_centers in office_types.items():
            print(f"  {office_type}: {len(type_centers)} entries")

            if len(type_centers) == 1:
                # Only one entry for this office type - keep it
                keep_entries.extend(type_centers)
                print(
                    f"    ✅ Keep: ID {type_centers[0].id} ({type_centers[0].address})"
                )
            else:
                # Multiple entries for same office type - choose the best one
                # Priority: Main > Regional Center > Field > Mailing > others
                priority_order = ["Main", "Regional Center", "Field", "Mailing"]

                # Sort by priority
                sorted_centers = sorted(
                    type_centers,
                    key=lambda x: (
                        (
                            priority_order.index(x.office_type)
                            if x.office_type in priority_order
                            else 999
                        ),
                        x.id,  # Use ID as tiebreaker
                    ),
                )

                # Keep the first (highest priority), remove the rest
                keep_entries.append(sorted_centers[0])
                remove_entries.extend(sorted_centers[1:])

                print(
                    f"    ✅ Keep: ID {sorted_centers[0].id} ({sorted_centers[0].address})"
                )
                for center in sorted_centers[1:]:
                    print(f"    ❌ Remove: ID {center.id} ({center.address})")

        cleanup_plan[name] = {"keep": keep_entries, "remove": remove_entries}

    return cleanup_plan


def execute_cleanup(dry_run=True):
    """Execute the cleanup plan"""
    from locations.models import RegionalCenter

    cleanup_plan = cleanup_strategy()

    if dry_run:
        print("\n🧪 DRY RUN - No changes will be made")
    else:
        print("\n🚀 EXECUTING CLEANUP - Changes will be permanent!")

    print("=" * 50)

    total_to_remove = 0

    with transaction.atomic():
        for name, plan in cleanup_plan.items():
            keep_count = len(plan["keep"])
            remove_count = len(plan["remove"])
            total_to_remove += remove_count

            print(f"\n{name}:")
            print(f"  Keep: {keep_count} entries")
            print(f"  Remove: {remove_count} entries")

            if not dry_run and remove_count > 0:
                # Actually remove the duplicates
                for center in plan["remove"]:
                    print(f"    Deleting ID {center.id}: {center.address}")
                    center.delete()

    print(f"\n📊 Summary:")
    print(f"  Total entries to remove: {total_to_remove}")

    if dry_run:
        print(f"  This was a DRY RUN - no changes made")
        print(f"  Run with dry_run=False to execute the cleanup")
    else:
        print(f"  Cleanup completed!")

        # Verify results
        remaining = RegionalCenter.objects.count()
        print(f"  Remaining regional centers: {remaining}")


def main():
    """Main function"""
    print("🧹 Regional Centers Cleanup Tool")
    print("=" * 50)
    print("This script will remove duplicate regional centers while preserving")
    print("distinct office types (Main, Field, Mailing, etc.)")
    print()

    try:
        setup_django()

        # First, show the analysis
        analyze_duplicates()

        # Show cleanup strategy
        cleanup_strategy()

        # Ask user if they want to proceed
        print("\n" + "=" * 50)
        response = (
            input("Do you want to proceed with the cleanup? (y/N): ").strip().lower()
        )

        if response in ["y", "yes"]:
            # Execute cleanup
            execute_cleanup(dry_run=False)
        else:
            print("Cleanup cancelled.")

    except KeyboardInterrupt:
        print("\n\n⏹️ Cleanup cancelled by user")
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ Unexpected error: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()
