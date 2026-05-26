#!/usr/bin/env python3
"""
Check the status of Provider vs ProviderV2 tables
"""
import os
import django

os.environ.setdefault("DJANGO_SETTINGS_MODULE", "maplocation.settings")
django.setup()

from locations.models import Provider, ProviderV2

print("=" * 60)
print("PROVIDER TABLE COMPARISON")
print("=" * 60)
print()

old_count = Provider.objects.count()
new_count = ProviderV2.objects.count()

print(f"📊 Old Provider table: {old_count} records")
print(f"📊 New ProviderV2 table: {new_count} records")
print()

if old_count > 0:
    print("Sample from OLD Provider table:")
    for provider in Provider.objects.all()[:3]:
        print(f"  - {provider.name} (ID: {provider.id})")
    print()

if new_count > 0:
    print("Sample from NEW ProviderV2 table:")
    for provider in ProviderV2.objects.all()[:3]:
        print(f"  - {provider.name} (ID: {provider.id})")
    print()

print("=" * 60)
print("RECOMMENDATION:")
if old_count == 0 and new_count > 0:
    print("✅ Old Provider table is empty, safe to remove the model!")
elif old_count > 0 and new_count > old_count:
    print("✅ All data migrated to ProviderV2, old table can be removed")
elif old_count > 0 and new_count == 0:
    print("⚠️  WARNING: ProviderV2 is empty! Need to migrate data first!")
else:
    print(f"⚠️  Old table has {old_count} records, new has {new_count}")
    print("   Review data before removing old model")
print("=" * 60)
