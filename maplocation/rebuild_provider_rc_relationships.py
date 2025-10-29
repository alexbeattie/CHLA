#!/usr/bin/env python3
"""
Rebuild provider-regional center relationships based on ZIP code overlap.
If a provider's address ZIP code falls within a regional center's ZIP codes,
create the relationship.
"""
import os
import sys

os.environ['DB_HOST'] = 'chla-postgres-db.cpkvcu4f59w6.us-west-2.rds.amazonaws.com'
os.environ['DB_NAME'] = 'postgres'
os.environ['DB_USER'] = 'chla_admin'
os.environ['DB_PASSWORD'] = 'CHLASecure2024'
os.environ['DB_SSL_REQUIRE'] = 'true'
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'maplocation.settings')

import django
django.setup()

from locations.models import ProviderV2, RegionalCenter, ProviderRegionalCenter
import json

print("=" * 80)
print("REBUILD PROVIDER-REGIONAL CENTER RELATIONSHIPS")
print("=" * 80)
print()

providers = ProviderV2.objects.all()
regional_centers = RegionalCenter.objects.all()

print(f"Providers: {providers.count()}")
print(f"Regional Centers: {regional_centers.count()}")
print()

# Build ZIP code to regional center mapping
zip_to_rc = {}
for rc in regional_centers:
    zip_codes = rc.zip_codes or []
    for zip_code in zip_codes:
        if zip_code not in zip_to_rc:
            zip_to_rc[zip_code] = []
        zip_to_rc[zip_code].append(rc)

print(f"Mapped {len(zip_to_rc)} ZIP codes to regional centers")
print()

# Link providers to regional centers based on their address ZIP
created = 0
no_match = []

for provider in providers:
    # Try to get ZIP from address
    zip_code = None
    if provider.address:
        try:
            addr = json.loads(provider.address) if isinstance(provider.address, str) else provider.address
            zip_code = addr.get('zip')
        except:
            pass
    
    if not zip_code:
        no_match.append(f"{provider.name} - no ZIP code")
        continue
    
    # Find regional centers for this ZIP
    rcs = zip_to_rc.get(zip_code, [])
    
    if not rcs:
        no_match.append(f"{provider.name} - ZIP {zip_code} not in any RC")
        continue
    
    # Create relationships
    for rc in rcs:
        ProviderRegionalCenter.objects.get_or_create(
            provider=provider,
            regional_center=rc,
            defaults={'is_primary': True, 'notes': 'Auto-linked by ZIP code'}
        )
        created += 1

print("=" * 80)
print("RESULTS")
print("=" * 80)
print(f"✅ Created {created} relationships")
print(f"❌ No match for {len(no_match)} providers")
if no_match[:5]:
    print("\nSample unmatched:")
    for item in no_match[:5]:
        print(f"  - {item}")

print()
print(f"Final relationship count: {ProviderRegionalCenter.objects.count()}")
