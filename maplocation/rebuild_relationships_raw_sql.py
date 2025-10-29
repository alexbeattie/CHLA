#!/usr/bin/env python3
"""Use raw SQL to rebuild relationships"""
import os

os.environ["DB_HOST"] = "chla-postgres-db.cpkvcu4f59w6.us-west-2.rds.amazonaws.com"
os.environ["DB_NAME"] = "postgres"
os.environ["DB_USER"] = "chla_admin"
os.environ["DB_PASSWORD"] = "CHLASecure2024"
os.environ["DB_SSL_REQUIRE"] = "true"
os.environ.setdefault("DJANGO_SETTINGS_MODULE", "maplocation.settings")

import django

django.setup()

from django.db import connection
import json

print("=" * 80)
print("REBUILD PROVIDER-REGIONAL CENTER RELATIONSHIPS")
print("=" * 80)
print()

with connection.cursor() as cursor:
    # Get LA regional centers with ZIP codes
    cursor.execute(
        """
        SELECT id, regional_center, zip_codes
        FROM regional_centers
        WHERE is_la_regional_center = true 
        AND zip_codes IS NOT NULL
        AND jsonb_array_length(zip_codes) > 0
    """
    )
    regional_centers = cursor.fetchall()

    print(f"Found {len(regional_centers)} LA regional centers")

    # Build ZIP to RC mapping
    zip_to_rc_id = {}
    for rc_id, rc_name, zip_codes in regional_centers:
        for zip_code in zip_codes:
            if zip_code not in zip_to_rc_id:
                zip_to_rc_id[zip_code] = []
            zip_to_rc_id[zip_code].append((rc_id, rc_name))

    print(f"Mapped {len(zip_to_rc_id)} unique ZIP codes")
    print()

    # Get all providers
    cursor.execute("SELECT id, name, address FROM providers_v2")
    providers = cursor.fetchall()

    print(f"Found {len(providers)} providers")
    print()

    # Link providers to RCs based on ZIP
    created = 0
    no_match = []

    for provider_id, provider_name, address in providers:
        zip_code = None
        if address:
            try:
                addr = json.loads(address) if isinstance(address, str) else address
                zip_code = addr.get("zip")
            except:
                pass

        if not zip_code:
            no_match.append(f"{provider_name} - no ZIP")
            continue

        rcs = zip_to_rc_id.get(zip_code, [])

        if not rcs:
            no_match.append(f"{provider_name} - ZIP {zip_code} not covered")
            continue

        # Create relationships for each matching RC
        for rc_id, rc_name in rcs:
            cursor.execute(
                """
                INSERT INTO provider_regional_centers 
                (provider_id, regional_center_id, is_primary, notes, created_at)
                VALUES (%s, %s, %s, %s, NOW())
                ON CONFLICT DO NOTHING
            """,
                [provider_id, rc_id, True, "Linked by ZIP code"],
            )
            if cursor.rowcount > 0:
                created += 1

print("=" * 80)
print("RESULTS")
print("=" * 80)
print(f"✅ Created {created} relationships")
print(f"❌ {len(no_match)} providers not matched")

if no_match[:10]:
    print("\nSample unmatched (first 10):")
    for item in no_match[:10]:
        print(f"  - {item}")

# Final count
with connection.cursor() as cursor:
    cursor.execute("SELECT COUNT(*) FROM provider_regional_centers")
    final_count = cursor.fetchone()[0]
    print()
    print(f"✅ Final relationship count: {final_count}")
