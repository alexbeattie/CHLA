#!/usr/bin/env python3
"""
Sync ProviderV2 data FROM RDS TO local database
Two-step process:
1. Export from RDS
2. Import to local
"""
import os
import sys
import json
from datetime import datetime


def export_from_rds():
    """Step 1: Export from RDS"""
    print("=" * 60)
    print("STEP 1: EXPORT FROM RDS")
    print("=" * 60)
    print()

    # Set RDS connection
    os.environ["DB_HOST"] = "chla-postgres-db.cpkvcu4f59w6.us-west-2.rds.amazonaws.com"
    os.environ["DB_NAME"] = "postgres"
    os.environ["DB_USER"] = "chla_admin"
    os.environ["DB_PASSWORD"] = "CHLASecure2024"
    os.environ["DB_SSL_REQUIRE"] = "true"
    os.environ["DJANGO_SETTINGS_MODULE"] = "maplocation.settings"

    import django

    django.setup()

    from locations.models import ProviderV2
    from decimal import Decimal

    print("📍 Connected to: RDS (Production)")
    print()

    providers = ProviderV2.objects.all()
    total = providers.count()

    geocoded = (
        providers.exclude(latitude=Decimal("0.00000000"))
        .exclude(longitude=Decimal("0.00000000"))
        .exclude(latitude__isnull=True)
        .count()
    )

    print(f"📊 Found {total} providers on RDS")
    print(f"   ✅ With coordinates: {geocoded} ({geocoded/total*100:.1f}%)")
    print(f"   ❌ Without coordinates: {total - geocoded}")
    print()

    filename = f"rds_export_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"

    print(f"💾 Exporting to: {filename}")

    data = []
    for i, provider in enumerate(providers, 1):
        if i % 10 == 0:
            print(f"   Progress: {i}/{total} providers...", end="\r")

        provider_data = {
            "model": "locations.providerv2",
            "pk": str(provider.id),
            "fields": {
                "name": provider.name,
                "type": provider.type,
                "phone": provider.phone,
                "email": provider.email,
                "website": provider.website,
                "description": provider.description,
                "address": provider.address,
                "latitude": str(provider.latitude) if provider.latitude else "0.0",
                "longitude": str(provider.longitude) if provider.longitude else "0.0",
                "verified": provider.verified,
                "insurance_accepted": provider.insurance_accepted,
                "age_groups": provider.age_groups,
                "diagnoses_treated": provider.diagnoses_treated,
                "therapy_types": provider.therapy_types,
                "languages_spoken": provider.languages_spoken,
                "funding_sources": provider.funding_sources,
                "accepts_private_pay": getattr(provider, "accepts_private_pay", False),
                "accepts_regional_center": getattr(
                    provider, "accepts_regional_center", False
                ),
                "accepts_insurance": getattr(provider, "accepts_insurance", False),
                "in_person_services": getattr(provider, "in_person_services", True),
                "virtual_services": getattr(provider, "virtual_services", False),
                "home_based_services": getattr(provider, "home_based_services", False),
                "center_based_services": getattr(
                    provider, "center_based_services", True
                ),
            },
        }
        data.append(provider_data)

    print()  # Clear progress line

    with open(filename, "w") as f:
        json.dump(data, f, indent=2)

    print(f"✅ Exported {total} providers to: {filename}")
    print()

    return filename


def import_to_local(filename):
    """Step 2: Import to local"""
    print()
    print("=" * 60)
    print("STEP 2: IMPORT TO LOCAL")
    print("=" * 60)
    print()

    # Clear RDS connection vars and use local
    for var in ["DB_HOST", "DB_NAME", "DB_USER", "DB_PASSWORD", "DB_SSL_REQUIRE"]:
        os.environ.pop(var, None)

    # Reimport Django with local settings
    import django
    from django.conf import settings

    if settings.configured:
        # Force reload settings
        from importlib import reload
        import maplocation.settings as settings_module

        reload(settings_module)

    django.setup()

    from locations.models import ProviderV2
    from decimal import Decimal

    db = settings.DATABASES["default"]
    print(f"📍 Connected to: {db['HOST']} ({db['NAME']})")
    print()

    if not os.path.exists(filename):
        print(f"❌ File not found: {filename}")
        return

    with open(filename, "r") as f:
        data = json.load(f)

    print(f"📥 Importing {len(data)} providers...")
    print()

    updated = 0
    created = 0
    errors = 0

    for i, item in enumerate(data, 1):
        if i % 10 == 0:
            print(f"   Progress: {i}/{len(data)} providers...", end="\r")

        try:
            pk = item["pk"]
            fields = item["fields"]

            # Update or create
            provider, is_new = ProviderV2.objects.update_or_create(
                id=pk,
                defaults={
                    "name": fields["name"],
                    "type": fields.get("type"),
                    "phone": fields.get("phone"),
                    "email": fields.get("email"),
                    "website": fields.get("website"),
                    "description": fields.get("description"),
                    "address": fields.get("address"),
                    "latitude": Decimal(fields.get("latitude", "0.0")),
                    "longitude": Decimal(fields.get("longitude", "0.0")),
                    "verified": fields.get("verified", False),
                    "insurance_accepted": fields.get("insurance_accepted"),
                    "age_groups": fields.get("age_groups"),
                    "diagnoses_treated": fields.get("diagnoses_treated"),
                    "therapy_types": fields.get("therapy_types"),
                    "languages_spoken": fields.get("languages_spoken"),
                    "funding_sources": fields.get("funding_sources"),
                    "accepts_private_pay": fields.get("accepts_private_pay", False),
                    "accepts_regional_center": fields.get(
                        "accepts_regional_center", False
                    ),
                    "accepts_insurance": fields.get("accepts_insurance", False),
                    "in_person_services": fields.get("in_person_services", True),
                    "virtual_services": fields.get("virtual_services", False),
                    "home_based_services": fields.get("home_based_services", False),
                    "center_based_services": fields.get("center_based_services", True),
                },
            )

            if is_new:
                created += 1
            else:
                updated += 1

        except Exception as e:
            errors += 1
            print(f"\n   ⚠️  Error importing {fields.get('name', 'unknown')}: {e}")

    print()  # Clear progress line
    print()
    print("=" * 60)
    print("✅ IMPORT COMPLETE!")
    print("=" * 60)
    print(f"   Created: {created}")
    print(f"   Updated: {updated}")
    print(f"   Errors: {errors}")
    print()


if __name__ == "__main__":
    print()
    print("🔄 SYNC PROVIDERS: RDS → LOCAL")
    print()

    # Step 1: Export from RDS
    filename = export_from_rds()

    # Step 2: Ask to import
    print()
    response = input("Continue with import to LOCAL database? (y/n): ")
    if response.lower() != "y":
        print()
        print(f"❌ Import cancelled. Export saved to: {filename}")
        print()
        print("To import later, run:")
        print(
            f"  python3 -c \"from sync_providers_rds_to_local import import_to_local; import_to_local('{filename}')\""
        )
        print()
        sys.exit(0)

    # Import to local
    import_to_local(filename)

    # Cleanup
    print()
    response = input("Delete export file? (y/n): ")
    if response.lower() == "y":
        os.remove(filename)
        print(f"✅ Deleted: {filename}")
    else:
        print(f"📦 Export saved: {filename}")

    print()
    print("🎉 Sync complete! Your local database now matches RDS.")
    print()
