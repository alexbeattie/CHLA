#!/usr/bin/env python3
"""
Restore provider-regional center relationships to RDS.
The migration converted the tables but truncated the data.
"""
import os
import sys

print("=" * 80)
print("RESTORE PROVIDER-REGIONAL CENTER RELATIONSHIPS TO RDS")
print("=" * 80)
print()

# First, get data from LOCAL database
print("Step 1: Reading relationships from LOCAL database...")
os.environ.pop('DB_HOST', None)
os.environ.pop('DB_NAME', None)
os.environ.pop('DB_USER', None)
os.environ.pop('DB_PASSWORD', None)
os.environ.pop('DB_SSL_REQUIRE', None)
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'maplocation.settings')

import django
django.setup()

from locations.models import ProviderV2, RegionalCenter, ProviderRegionalCenter

# Get all local relationships (using ProviderV2 which exists locally)
local_relationships = []
for rel in ProviderRegionalCenter.objects.select_related('provider', 'regional_center').all():
    local_relationships.append({
        'provider_name': rel.provider.name,
        'regional_center_name': rel.regional_center.name,
        'is_primary': rel.is_primary,
        'notes': rel.notes or '',
    })

print(f"Found {len(local_relationships)} relationships in LOCAL database")
print()

if not local_relationships:
    print("❌ No relationships found in local database!")
    sys.exit(1)

# Now configure for RDS
print("Step 2: Connecting to RDS...")
os.environ['DB_HOST'] = 'chla-postgres-db.cpkvcu4f59w6.us-west-2.rds.amazonaws.com'
os.environ['DB_NAME'] = 'postgres'
os.environ['DB_USER'] = 'chla_admin'
os.environ['DB_PASSWORD'] = 'CHLASecure2024'
os.environ['DB_SSL_REQUIRE'] = 'true'

# Force Django to reconnect
from django.db import connections
connections['default'].close()
django.setup()

from locations.models import ProviderV2, RegionalCenter, ProviderRegionalCenter

rds_providers = ProviderV2.objects.count()
rds_rcs = RegionalCenter.objects.count()
print(f"RDS has {rds_providers} providers and {rds_rcs} regional centers")
print()

# Create relationships in RDS
print("Step 3: Creating relationships in RDS...")
created = 0
errors = []

for rel_data in local_relationships:
    try:
        # Find provider by name
        provider = ProviderV2.objects.get(name=rel_data['provider_name'])
        
        # Find regional center by name
        rc = RegionalCenter.objects.get(name=rel_data['regional_center_name'])
        
        # Create relationship
        ProviderRegionalCenter.objects.get_or_create(
            provider=provider,
            regional_center=rc,
            defaults={
                'is_primary': rel_data['is_primary'],
                'notes': rel_data['notes'],
            }
        )
        created += 1
        if created % 50 == 0:
            print(f"  Created {created} relationships...")
            
    except ProviderV2.DoesNotExist:
        errors.append(f"Provider not found in RDS: {rel_data['provider_name']}")
    except RegionalCenter.DoesNotExist:
        errors.append(f"Regional Center not found in RDS: {rel_data['regional_center_name']}")
    except Exception as e:
        errors.append(f"Error for {rel_data['provider_name']}: {str(e)}")

print()
print("=" * 80)
print("RESULTS")
print("=" * 80)
print(f"✅ Successfully created: {created} relationships")
if errors:
    print(f"❌ Errors: {len(errors)}")
    for error in errors[:10]:  # Show first 10 errors
        print(f"  - {error}")
    if len(errors) > 10:
        print(f"  ... and {len(errors) - 10} more errors")
else:
    print("✅ No errors!")

print()
final_count = ProviderRegionalCenter.objects.count()
print(f"Final RDS relationship count: {final_count}")
