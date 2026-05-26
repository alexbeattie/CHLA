#!/usr/bin/env python3
"""
Import providers TO RDS from JSON file
"""
import os
import sys
from pathlib import Path as _Path
sys.path.insert(0, str(_Path(__file__).resolve().parents[2]))  # maplocation/
from scripts._rds_env import load_prod_rds_env
import json

# Configure for RDS
load_prod_rds_env()
os.environ.setdefault("DJANGO_SETTINGS_MODULE", "maplocation.settings")

import django

django.setup()

from locations.models import ProviderV2

print("=" * 80)
print("IMPORT PROVIDERS TO RDS")
print("=" * 80)
print()

# Load data file
data_file = "/tmp/local_providers_export_latest.json"

if not os.path.exists(data_file):
    print(f"❌ Data file not found: {data_file}")
    print("Run the export step first!")
    sys.exit(1)

with open(data_file, "r") as f:
    providers_to_import = json.load(f)

print(f"Loaded {len(providers_to_import)} providers from {data_file}")
print()

# Check RDS connection
try:
    rds_count_before = ProviderV2.objects.count()
    print(f"✅ Connected to RDS")
    print(f"Current providers on RDS: {rds_count_before}")
    print()
except Exception as e:
    print(f"❌ RDS connection failed: {e}")
    sys.exit(1)

print(f"Importing {len(providers_to_import)} providers to RDS...")
print()

created = 0
updated = 0
skipped = 0

for i, data in enumerate(providers_to_import):
    try:
        # Try to find existing provider by name
        existing = ProviderV2.objects.filter(name=data["name"]).first()

        if existing:
            # Update existing provider
            for key, value in data.items():
                setattr(existing, key, value)
            existing.save()
            updated += 1
            if updated <= 5:
                print(f"  ↻ Updated: {data['name']}")
        else:
            # Create new provider
            ProviderV2.objects.create(**data)
            created += 1
            if created <= 5:
                print(f"  + Created: {data['name']}")

        # Progress indicator
        if (i + 1) % 50 == 0:
            print(f"  ... processed {i + 1}/{len(providers_to_import)}")

    except Exception as e:
        print(f"  ✗ Error with {data.get('name', 'unknown')}: {e}")
        skipped += 1

if updated > 5:
    print(f"  ... and {updated - 5} more updates")
if created > 5:
    print(f"  ... and {created - 5} more creates")

print()
print("=" * 80)
print("IMPORT COMPLETE")
print("=" * 80)
print()

rds_count_after = ProviderV2.objects.count()

print(f"RDS providers before: {rds_count_before}")
print(f"RDS providers after:  {rds_count_after}")
print(f"Difference:           +{rds_count_after - rds_count_before}")
print()
print(f"✅ Created: {created}")
print(f"✅ Updated: {updated}")
if skipped > 0:
    print(f"⚠️  Skipped: {skipped}")
print()

print("✅ Regional Center ZIP codes were NOT modified (preserved from RDS)")
print()
