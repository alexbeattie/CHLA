#!/usr/bin/env python3
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

print("REBUILD PROVIDER-REGIONAL CENTER RELATIONSHIPS")
print("=" * 80)

with connection.cursor() as cursor:
    # Get LA regional centers
    cursor.execute(
        """
        SELECT id, regional_center, zip_codes::text
        FROM regional_centers
        WHERE is_la_regional_center = true
    """
    )
    regional_centers = cursor.fetchall()

    print(f"✓ Found {len(regional_centers)} LA regional centers")

    # Build ZIP mapping
    zip_to_rc = {}
    total_zips = 0
    for rc_id, rc_name, zip_codes_json in regional_centers:
        if not zip_codes_json:
            continue
        zip_codes = json.loads(zip_codes_json)  # Parse the JSON string
        for zip_code in zip_codes:
            if zip_code not in zip_to_rc:
                zip_to_rc[zip_code] = []
            zip_to_rc[zip_code].append((rc_id, rc_name))
            total_zips += 1

    print(f"✓ Mapped {len(zip_to_rc)} unique ZIP codes")
    sample_zips = list(zip_to_rc.keys())[:5]
    print(f"✓ Sample ZIPs: {', '.join(sample_zips)}")
    print()

    # Get providers
    cursor.execute("SELECT id, name, address FROM providers_v2")
    providers = cursor.fetchall()
    print(f"✓ Found {len(providers)} providers\n")

    # Create relationships
    created = 0
    matched_providers = 0

    print("Linking providers...")
    for provider_id, provider_name, address in providers:
        zip_code = None
        if address:
            try:
                addr = json.loads(address) if isinstance(address, str) else address
                zip_code = addr.get("zip")
            except:
                pass

        if not zip_code:
            continue

        rcs = zip_to_rc.get(zip_code, [])
        if not rcs:
            continue

        matched_providers += 1
        for rc_id, rc_name in rcs:
            cursor.execute(
                """
                INSERT INTO provider_regional_centers 
                (provider_id, regional_center_id, is_primary, notes, created_at)
                VALUES (%s, %s, true, 'Linked by ZIP', NOW())
            """,
                [provider_id, rc_id],
            )
            created += 1

        if matched_providers % 25 == 0:
            print(f"  {matched_providers} providers matched...")

print("\n" + "=" * 80)
print("✅ SUCCESS")
print("=" * 80)
print(f"Created {created} relationships for {matched_providers} providers")

with connection.cursor() as cursor:
    cursor.execute("SELECT COUNT(*) FROM provider_regional_centers")
    print(f"Total relationships in database: {cursor.fetchone()[0]}")
