#!/usr/bin/env python3
"""
Clean up duplicate provider entries in RDS
Keeps the most recent entry (by ID) and removes older duplicates
"""
import os
import sys

# Configure for RDS
os.environ["DB_HOST"] = "chla-postgres-db.cpkvcu4f59w6.us-west-2.rds.amazonaws.com"
os.environ["DB_NAME"] = "postgres"
os.environ["DB_USER"] = "chla_admin"
os.environ["DB_PASSWORD"] = "CHLASecure2024"
os.environ["DB_SSL_REQUIRE"] = "true"
os.environ.setdefault("DJANGO_SETTINGS_MODULE", "maplocation.settings")

import django

django.setup()

from locations.models import ProviderV2
from django.db.models import Count

# Check for dry-run mode
DRY_RUN = "--dry-run" in sys.argv

print("=" * 80)
print("RDS DUPLICATE CLEANUP")
if DRY_RUN:
    print("MODE: DRY RUN (no changes will be made)")
else:
    print("MODE: LIVE (duplicates will be deleted)")
print("=" * 80)
print()

# Connect to RDS
try:
    total_before = ProviderV2.objects.count()
    print(f"✅ Connected to RDS")
    print(f"Total providers: {total_before}")
    print()
except Exception as e:
    print(f"❌ RDS connection failed: {e}")
    sys.exit(1)

# Find duplicates
duplicates = (
    ProviderV2.objects.values("name")
    .annotate(count=Count("id"))
    .filter(count__gt=1)
    .order_by("-count")
)

if not duplicates.exists():
    print("✅ No duplicates found!")
    sys.exit(0)

print(f"Found {duplicates.count()} providers with duplicate entries:")
print()

total_to_delete = 0
deletion_plan = []

for dup in duplicates:
    name = dup["name"]
    count = dup["count"]

    # Get all entries for this provider
    entries = ProviderV2.objects.filter(name=name).order_by("id")

    print(f"📋 {name} ({count} entries)")

    # Keep the NEWEST entry (highest ID), delete the rest
    # This assumes newer entries have more complete/accurate data
    entries_list = list(entries)
    to_keep = entries_list[-1]  # Last entry (highest ID)
    to_delete = entries_list[:-1]  # All except last

    print(f"   ✓ KEEP:   ID {to_keep.id} (newest)")
    for entry in to_delete:
        print(f"   ✗ DELETE: ID {entry.id}")
        deletion_plan.append(entry)
        total_to_delete += 1

    print()

print("=" * 80)
print("SUMMARY")
print("=" * 80)
print()
print(f"Total providers before:  {total_before}")
print(f"Duplicate entries found: {total_to_delete}")
print(f"Total after cleanup:     {total_before - total_to_delete}")
print()
print(f"Unique providers will remain: {duplicates.count()} (one entry each)")
print()

if DRY_RUN:
    print("=" * 80)
    print("DRY RUN COMPLETE - No changes made")
    print("=" * 80)
    print()
    print("To execute the cleanup, run:")
    print("  python cleanup_rds_duplicates.py")
    print()
    sys.exit(0)

# Confirm before deleting
print("=" * 80)
print("⚠️  WARNING: You are about to delete data from RDS")
print("=" * 80)
print()
print(f"This will delete {total_to_delete} duplicate provider entries.")
print()

response = input("Are you sure you want to continue? Type 'yes' to confirm: ")

if response.lower() != "yes":
    print("❌ Cleanup cancelled")
    sys.exit(0)

print()
print("Deleting duplicates...")
print()

deleted_count = 0
errors = []

for entry in deletion_plan:
    try:
        entry_id = entry.id
        entry_name = entry.name
        entry.delete()
        deleted_count += 1
        print(f"  ✓ Deleted: {entry_name} (ID {entry_id})")
    except Exception as e:
        errors.append((entry.name, entry.id, str(e)))
        print(f"  ✗ Error deleting {entry.name} (ID {entry.id}): {e}")

print()
print("=" * 80)
print("CLEANUP COMPLETE")
print("=" * 80)
print()

total_after = ProviderV2.objects.count()

print(f"Deleted successfully: {deleted_count}")
if errors:
    print(f"Errors:               {len(errors)}")
print()
print(f"Providers before: {total_before}")
print(f"Providers after:  {total_after}")
print(f"Removed:          {total_before - total_after}")
print()

# Verify no duplicates remain
remaining_duplicates = (
    ProviderV2.objects.values("name").annotate(count=Count("id")).filter(count__gt=1)
)

if remaining_duplicates.exists():
    print(f"⚠️  {remaining_duplicates.count()} duplicates still remain!")
    for dup in remaining_duplicates:
        print(f"   - {dup['name']}: {dup['count']} entries")
else:
    print("✅ No duplicates remaining - database is clean!")

print()

if errors:
    print("Errors encountered:")
    for name, entry_id, error in errors:
        print(f"  - {name} (ID {entry_id}): {error}")
    print()
