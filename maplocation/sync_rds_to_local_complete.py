#!/usr/bin/env python3
"""
Complete sync script: AWS RDS → Local Database
Syncs both providers and regional centers
Source of truth: AWS RDS

Usage:
    python3 sync_rds_to_local_complete.py [--auto]

Options:
    --auto    Run without prompts (useful for automation)
"""
import os
import sys
import json
from datetime import datetime
from decimal import Decimal


def export_from_rds():
    """Step 1: Export data from AWS RDS"""
    print("=" * 70)
    print("STEP 1: EXPORT FROM AWS RDS (Source of Truth)")
    print("=" * 70)
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

    from locations.models import ProviderV2, RegionalCenter

    print("📍 Connected to: AWS RDS (Production)")
    print()

    # Export providers
    providers = ProviderV2.objects.all()
    provider_count = providers.count()

    geocoded = (
        providers.exclude(latitude=Decimal("0.00000000"))
        .exclude(longitude=Decimal("0.00000000"))
        .exclude(latitude__isnull=True)
        .count()
    )

    print(f"📊 Providers on RDS: {provider_count}")
    print(f"   ✅ With coordinates: {geocoded} ({geocoded/provider_count*100:.1f}%)")
    print()

    # Export regional centers
    centers = RegionalCenter.objects.all()
    center_count = centers.count()
    print(f"📊 Regional Centers on RDS: {center_count}")
    print()

    timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
    provider_file = f"rds_providers_{timestamp}.json"
    center_file = f"rds_centers_{timestamp}.json"

    # Export providers
    print(f"💾 Exporting providers to: {provider_file}")
    provider_data = []
    for i, provider in enumerate(providers, 1):
        if i % 20 == 0:
            print(f"   Progress: {i}/{provider_count} providers...", end="\r")

        provider_data.append({
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
                "accepts_regional_center": getattr(provider, "accepts_regional_center", False),
                "accepts_insurance": getattr(provider, "accepts_insurance", False),
                "in_person_services": getattr(provider, "in_person_services", True),
                "virtual_services": getattr(provider, "virtual_services", False),
                "home_based_services": getattr(provider, "home_based_services", False),
                "center_based_services": getattr(provider, "center_based_services", True),
            },
        })

    with open(provider_file, "w") as f:
        json.dump(provider_data, f, indent=2)
    print(f"\n✅ Exported {provider_count} providers")

    # Export regional centers
    print(f"💾 Exporting regional centers to: {center_file}")
    center_data = []
    for i, center in enumerate(centers, 1):
        if i % 5 == 0:
            print(f"   Progress: {i}/{center_count} centers...", end="\r")

        center_data.append({
            "model": "locations.regionalcenter",
            "pk": center.id,
            "fields": {
                "regional_center": center.regional_center,
                "office_type": center.office_type,
                "address": center.address,
                "suite": center.suite,
                "city": center.city,
                "state": center.state,
                "zip_code": center.zip_code,
                "telephone": center.telephone,
                "website": center.website,
                "county_served": center.county_served,
                "los_angeles_health_district": center.los_angeles_health_district,
                "latitude": center.latitude,
                "longitude": center.longitude,
                "service_radius_miles": center.service_radius_miles,
                "zip_codes": center.zip_codes,
                "service_areas": center.service_areas,
                "is_la_regional_center": center.is_la_regional_center,
            },
        })

    with open(center_file, "w") as f:
        json.dump(center_data, f, indent=2)
    print(f"\n✅ Exported {center_count} regional centers")
    print()

    return provider_file, center_file, provider_count, center_count


def import_to_local(provider_file, center_file, auto_mode=False):
    """Step 2: Import to local database and clean duplicates"""
    print()
    print("=" * 70)
    print("STEP 2: IMPORT TO LOCAL DATABASE")
    print("=" * 70)
    print()

    # Clear RDS environment vars
    for var in ["DB_HOST", "DB_NAME", "DB_USER", "DB_PASSWORD", "DB_SSL_REQUIRE"]:
        os.environ.pop(var, None)

    # Force reload Django settings for local DB
    import django
    from django.conf import settings

    if settings.configured:
        from importlib import reload
        import maplocation.settings as settings_module
        reload(settings_module)

    django.setup()

    from locations.models import ProviderV2, RegionalCenter

    db = settings.DATABASES["default"]
    print(f"📍 Target: {db['HOST']} ({db['NAME']})")
    print()

    # Import providers
    if not os.path.exists(provider_file):
        print(f"❌ Provider file not found: {provider_file}")
        return

    with open(provider_file, "r") as f:
        provider_data = json.load(f)

    print(f"📥 Importing {len(provider_data)} providers...")
    current_local = ProviderV2.objects.count()
    print(f"   Current local providers: {current_local}")
    print()

    # Get RDS provider IDs
    rds_provider_ids = set(item["pk"] for item in provider_data)

    # Import/update providers
    updated = 0
    created = 0
    errors = 0

    for i, item in enumerate(provider_data, 1):
        if i % 20 == 0:
            print(f"   Progress: {i}/{len(provider_data)} providers...", end="\r")

        try:
            pk = item["pk"]
            fields = item["fields"]

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
                    "insurance_accepted": fields.get("insurance_accepted", ""),
                    "age_groups": fields.get("age_groups"),
                    "diagnoses_treated": fields.get("diagnoses_treated"),
                    "therapy_types": fields.get("therapy_types"),
                    "languages_spoken": fields.get("languages_spoken"),
                    "funding_sources": fields.get("funding_sources"),
                    "accepts_private_pay": fields.get("accepts_private_pay", False),
                    "accepts_regional_center": fields.get("accepts_regional_center", False),
                    "accepts_insurance": fields.get("accepts_insurance", False),
                    "in_person_services": fields.get("in_person_services", True),
                    "virtual_services": fields.get("virtual_services", False),
                    "home_based_services": fields.get("home_based_services", False),
                    "center_based_services": fields.get("center_based_services", True),
                }
            )

            if is_new:
                created += 1
            else:
                updated += 1

        except Exception as e:
            errors += 1
            print(f"\n⚠️  Error importing {fields.get('name', 'unknown')}: {e}")

    print()

    # Delete local duplicates not in RDS
    local_provider_ids = set(str(p.id) for p in ProviderV2.objects.all())
    to_delete = local_provider_ids - rds_provider_ids

    if to_delete:
        print(f"🧹 Cleaning: {len(to_delete)} duplicate providers not in RDS...")
        deleted_count = ProviderV2.objects.filter(id__in=to_delete).delete()[0]
        print(f"   ✅ Deleted {deleted_count} duplicates")

    print()
    print("✅ PROVIDERS SYNCED!")
    print(f"   Created: {created}")
    print(f"   Updated: {updated}")
    print(f"   Deleted: {len(to_delete) if to_delete else 0}")
    print(f"   Errors: {errors}")
    print(f"   Final count: {ProviderV2.objects.count()}")
    print()

    # Import regional centers
    if not os.path.exists(center_file):
        print(f"❌ Regional center file not found: {center_file}")
        return

    with open(center_file, "r") as f:
        center_data = json.load(f)

    print(f"📥 Importing {len(center_data)} regional centers...")

    center_updated = 0
    center_created = 0
    center_errors = 0

    for i, item in enumerate(center_data, 1):
        try:
            pk = item["pk"]
            fields = item["fields"]

            center, is_new = RegionalCenter.objects.update_or_create(
                id=pk,
                defaults={
                    "regional_center": fields["regional_center"],
                    "office_type": fields.get("office_type"),
                    "address": fields.get("address"),
                    "suite": fields.get("suite"),
                    "city": fields.get("city"),
                    "state": fields.get("state"),
                    "zip_code": fields.get("zip_code"),
                    "telephone": fields.get("telephone"),
                    "website": fields.get("website"),
                    "county_served": fields.get("county_served"),
                    "los_angeles_health_district": fields.get("los_angeles_health_district"),
                    "latitude": fields.get("latitude"),
                    "longitude": fields.get("longitude"),
                    "service_radius_miles": fields.get("service_radius_miles", 15.0),
                    "zip_codes": fields.get("zip_codes"),
                    "service_areas": fields.get("service_areas"),
                    "is_la_regional_center": fields.get("is_la_regional_center", False),
                }
            )

            if is_new:
                center_created += 1
            else:
                center_updated += 1

        except Exception as e:
            center_errors += 1
            print(f"⚠️  Error importing regional center: {e}")

    print()
    print("✅ REGIONAL CENTERS SYNCED!")
    print(f"   Created: {center_created}")
    print(f"   Updated: {center_updated}")
    print(f"   Errors: {center_errors}")
    print(f"   Final count: {RegionalCenter.objects.count()}")
    print()


def verify_sync():
    """Step 3: Verify the sync"""
    print()
    print("=" * 70)
    print("STEP 3: VERIFICATION")
    print("=" * 70)
    print()

    import os
    os.environ.pop("DB_HOST", None)
    os.environ["DJANGO_SETTINGS_MODULE"] = "maplocation.settings"

    import django
    django.setup()

    from locations.models import ProviderV2, RegionalCenter
    from decimal import Decimal

    # Providers
    total = ProviderV2.objects.count()
    geocoded = ProviderV2.objects.exclude(
        latitude=Decimal('0.00000000')
    ).exclude(
        longitude=Decimal('0.00000000')
    ).count()

    print(f"✅ Providers: {total}")
    print(f"   With coordinates: {geocoded} ({geocoded/total*100:.1f}%)")

    # Regional Centers
    centers = RegionalCenter.objects.count()
    la_centers = RegionalCenter.objects.filter(is_la_regional_center=True).count()
    print(f"✅ Regional Centers: {centers}")
    print(f"   LA County Centers: {la_centers}")
    print()

    print("=" * 70)
    print("🎉 SYNC COMPLETE! Local DB matches AWS RDS")
    print("=" * 70)
    print()


if __name__ == "__main__":
    auto_mode = "--auto" in sys.argv

    print()
    print("🔄 COMPLETE SYNC: AWS RDS → LOCAL DATABASE")
    print("=" * 70)
    print()

    if not auto_mode:
        print("⚠️  WARNING: This will overwrite your local database with RDS data!")
        print()
        response = input("Continue? (yes/no): ")
        if response.lower() not in ["yes", "y"]:
            print("❌ Sync cancelled")
            sys.exit(0)
        print()

    # Step 1: Export from RDS
    provider_file, center_file, provider_count, center_count = export_from_rds()

    # Step 2: Import to local
    import_to_local(provider_file, center_file, auto_mode)

    # Step 3: Verify
    verify_sync()

    # Cleanup
    if not auto_mode:
        print()
        response = input("Delete export files? (y/n): ")
        if response.lower() == "y":
            os.remove(provider_file)
            os.remove(center_file)
            print(f"✅ Deleted: {provider_file}")
            print(f"✅ Deleted: {center_file}")
        else:
            print(f"📦 Export files saved:")
            print(f"   {provider_file}")
            print(f"   {center_file}")
    else:
        # Auto cleanup in auto mode
        os.remove(provider_file)
        os.remove(center_file)
        print(f"🧹 Cleaned up export files")

    print()
