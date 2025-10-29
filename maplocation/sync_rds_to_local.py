#!/usr/bin/env python3
"""
Sync provider data FROM RDS (production) TO local database
RDS is the source of truth
"""
import os
import sys
import django

# Setup for RDS connection
os.environ["DB_HOST"] = "chla-postgres-db.cpkvcu4f59w6.us-west-2.rds.amazonaws.com"
os.environ["DB_NAME"] = "postgres"
os.environ["DB_USER"] = "chla_admin"
os.environ["DB_PASSWORD"] = "CHLASecure2024"
os.environ["DB_SSL_REQUIRE"] = "true"
os.environ["DJANGO_SETTINGS_MODULE"] = "maplocation.settings"

django.setup()

from locations.models import ProviderV2, RegionalCenter
from decimal import Decimal

print("=" * 60)
print("SYNC FROM RDS TO LOCAL")
print("=" * 60)
print()
print("⚠️  This will:")
print("  1. Connect to RDS (production)")
print("  2. Export all provider data")
print("  3. Save to a JSON file")
print("  4. You can then import to local DB")
print()

# Export from RDS
providers = ProviderV2.objects.all()
total = providers.count()

print(f"📊 Found {total} providers on RDS")
print()

# Count geocoded providers
geocoded = (
    providers.exclude(latitude=Decimal("0.00000000"))
    .exclude(longitude=Decimal("0.00000000"))
    .exclude(latitude__isnull=True)
    .count()
)

print(f"✅ With coordinates: {geocoded} ({geocoded/total*100:.1f}%)")
print(f"❌ Without coordinates: {total - geocoded}")
print()

response = input("Export RDS data to JSON file? (y/n): ")
if response.lower() != "y":
    print("❌ Export cancelled")
    sys.exit(0)

# Export to JSON
import json
from datetime import datetime

filename = f"rds_providers_export_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"

data = []
for provider in providers:
    # Convert to dict
    provider_data = {
        "id": str(provider.id),
        "name": provider.name,
        "type": provider.type,
        "phone": provider.phone,
        "email": provider.email,
        "website": provider.website,
        "description": provider.description,
        "address": provider.address,
        "latitude": float(provider.latitude) if provider.latitude else None,
        "longitude": float(provider.longitude) if provider.longitude else None,
        "verified": provider.verified,
        "insurance_accepted": provider.insurance_accepted,
        "age_groups": provider.age_groups,
        "diagnoses_treated": provider.diagnoses_treated,
        "therapy_types": provider.therapy_types,
        "languages_spoken": provider.languages_spoken,
        "accepts_medi_cal": provider.accepts_medi_cal,
        "accepts_private_insurance": provider.accepts_private_insurance,
        "accepts_regional_center": provider.accepts_regional_center,
    }
    data.append(provider_data)

with open(filename, "w") as f:
    json.dump(data, f, indent=2)

print(f"✅ Exported to: {filename}")
print()
print("📋 NEXT STEPS:")
print()
print("To import to LOCAL database:")
print()
print("1. Switch to LOCAL database:")
print("   unset DB_HOST DB_NAME DB_USER DB_PASSWORD DB_SSL_REQUIRE")
print()
print("2. Run import script:")
print(f"   python3 import_from_json.py {filename}")
print()
print("Or use the bash script:")
print("   ./sync_from_rds_to_local.sh")
print()
