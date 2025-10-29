#!/usr/bin/env python3
"""
Sync providers FROM local TO RDS
This preserves RDS ZIP codes while adding missing providers
"""
import os
import sys

print("=" * 80)
print("SYNC PROVIDERS: LOCAL → RDS")
print("=" * 80)
print()

# Step 1: Export from LOCAL
print("STEP 1: Export providers from LOCAL database")
print("-" * 80)

os.environ.setdefault("DJANGO_SETTINGS_MODULE", "maplocation.settings")
import django

django.setup()

from locations.models import ProviderV2
import json
from datetime import datetime

# Export all local providers
providers = ProviderV2.objects.all()
local_count = providers.count()

print(f"Found {local_count} providers in LOCAL database")
print()

# Convert to JSON
provider_data = []
for p in providers:
    data = {
        "name": p.name,
        "type": p.type,
        "phone": p.phone,
        "email": p.email,
        "website": p.website,
        "description": p.description,
        "address": p.address,
        "latitude": float(p.latitude) if p.latitude else None,
        "longitude": float(p.longitude) if p.longitude else None,
        "verified": p.verified,
        "insurance_accepted": p.insurance_accepted,
        "age_groups": p.age_groups,
        "diagnoses_treated": p.diagnoses_treated,
        "therapy_types": p.therapy_types,
        "languages_spoken": p.languages_spoken,
        "accepts_insurance": p.accepts_insurance,
        "accepts_private_pay": p.accepts_private_pay,
        "accepts_regional_center": p.accepts_regional_center,
    }
    provider_data.append(data)

# Save to temp file
temp_file = (
    f'/tmp/local_providers_export_{datetime.now().strftime("%Y%m%d_%H%M%S")}.json'
)
with open(temp_file, "w") as f:
    json.dump(provider_data, f, indent=2)

print(f"✅ Exported {len(provider_data)} providers to {temp_file}")
print()

# Step 2: Import to RDS
print("STEP 2: Import providers to RDS database")
print("-" * 80)
print()

response = input("Continue to RDS import? (y/n): ")
if response.lower() != "y":
    print("❌ Sync cancelled")
    sys.exit(0)

# Reconfigure for RDS
os.environ["DB_HOST"] = "chla-postgres-db.cpkvcu4f59w6.us-west-2.rds.amazonaws.com"
os.environ["DB_NAME"] = "postgres"
os.environ["DB_USER"] = "chla_admin"
os.environ["DB_PASSWORD"] = "CHLASecure2024"
os.environ["DB_SSL_REQUIRE"] = "true"

# Reload Django with RDS settings
from importlib import reload
from django.conf import settings
from django.db import connection

# Close existing connection
connection.close()

# Force reload settings
settings._wrapped = None

# Reinitialize Django
django.setup()

from locations.models import ProviderV2 as RDSProviderV2

print(f"Connecting to RDS...")
try:
    rds_count_before = RDSProviderV2.objects.count()
    print(f"✅ Connected to RDS")
    print(f"Current providers on RDS: {rds_count_before}")
    print()
except Exception as e:
    print(f"❌ RDS connection failed: {e}")
    sys.exit(1)

# Load the exported data
with open(temp_file, "r") as f:
    providers_to_import = json.load(f)

print(f"Importing {len(providers_to_import)} providers to RDS...")
print()

created = 0
updated = 0
skipped = 0

for data in providers_to_import:
    try:
        # Try to find existing provider by name
        existing = RDSProviderV2.objects.filter(name=data["name"]).first()

        if existing:
            # Update existing provider (preserve id)
            for key, value in data.items():
                setattr(existing, key, value)
            existing.save()
            updated += 1
            print(f"  ↻ Updated: {data['name']}")
        else:
            # Create new provider
            RDSProviderV2.objects.create(**data)
            created += 1
            print(f"  + Created: {data['name']}")

    except Exception as e:
        print(f"  ✗ Error with {data['name']}: {e}")
        skipped += 1

print()
print("=" * 80)
print("SYNC COMPLETE")
print("=" * 80)
print()

rds_count_after = RDSProviderV2.objects.count()

print(f"RDS providers before: {rds_count_before}")
print(f"RDS providers after:  {rds_count_after}")
print(f"Difference:           +{rds_count_after - rds_count_before}")
print()
print(f"Created: {created}")
print(f"Updated: {updated}")
print(f"Skipped: {skipped}")
print()

print("✅ Regional Center ZIP codes were NOT modified (preserved from RDS)")
print()
print("NEXT STEPS:")
print("  1. Verify providers on RDS")
print("  2. Run geocoding on new providers if needed")
print()
