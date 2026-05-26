#!/usr/bin/env python3
"""
Check which providers and regional centers need geocoding
"""
import os
import django

# Setup Django
os.environ.setdefault("DJANGO_SETTINGS_MODULE", "maplocation.settings")
django.setup()

from locations.models import ProviderV2, RegionalCenter

# Check Providers
providers_total = ProviderV2.objects.count()
# Find providers with NULL or 0.00000000 coordinates (which means not geocoded)
from decimal import Decimal

providers_without_coords = (
    ProviderV2.objects.filter(latitude__isnull=True, longitude__isnull=True).count()
    + ProviderV2.objects.filter(
        latitude=Decimal("0.00000000"), longitude=Decimal("0.00000000")
    ).count()
)
providers_with_coords = (
    ProviderV2.objects.exclude(latitude__isnull=True)
    .exclude(longitude__isnull=True)
    .exclude(latitude=Decimal("0.00000000"))
    .exclude(longitude=Decimal("0.00000000"))
    .count()
)

print("=" * 60)
print("GEOCODING STATUS REPORT")
print("=" * 60)
print()

print(f"📍 PROVIDERS (ProviderV2)")
print(f"  Total: {providers_total}")
print(f"  ✅ With coordinates: {providers_with_coords}")
print(f"  ❌ Missing coordinates: {providers_without_coords}")
print()

# Check Regional Centers
centers_total = RegionalCenter.objects.count()
centers_without_coords = (
    RegionalCenter.objects.filter(latitude__isnull=True, longitude__isnull=True).count()
    + RegionalCenter.objects.filter(
        latitude=Decimal("0.00000000"), longitude=Decimal("0.00000000")
    ).count()
)
centers_with_coords = (
    RegionalCenter.objects.exclude(latitude__isnull=True)
    .exclude(longitude__isnull=True)
    .exclude(latitude=Decimal("0.00000000"))
    .exclude(longitude=Decimal("0.00000000"))
    .count()
)

print(f"🏢 REGIONAL CENTERS")
print(f"  Total: {centers_total}")
print(f"  ✅ With coordinates: {centers_with_coords}")
print(f"  ❌ Missing coordinates: {centers_without_coords}")
print()

print("=" * 60)

# Show sample records that need geocoding
if providers_without_coords > 0:
    print()
    print("Sample Providers needing geocoding:")
    # Get providers with NULL or 0.00 coordinates
    providers_needing_geocoding = list(
        ProviderV2.objects.filter(latitude__isnull=True, longitude__isnull=True)[:3]
    ) + list(
        ProviderV2.objects.filter(
            latitude=Decimal("0.00000000"), longitude=Decimal("0.00000000")
        )[:3]
    )

    for provider in providers_needing_geocoding[:5]:
        print(
            f"  - {provider.name} (lat: {provider.latitude}, lon: {provider.longitude})"
        )
        if hasattr(provider, "address") and provider.address:
            print(f"    Address: {provider.address}")

if centers_without_coords > 0:
    print()
    print("Sample Regional Centers needing geocoding:")
    centers_needing_geocoding = list(
        RegionalCenter.objects.filter(latitude__isnull=True, longitude__isnull=True)[:3]
    ) + list(
        RegionalCenter.objects.filter(
            latitude=Decimal("0.00000000"), longitude=Decimal("0.00000000")
        )[:3]
    )

    for center in centers_needing_geocoding[:5]:
        print(
            f"  - {center.regional_center} (lat: {center.latitude}, lon: {center.longitude})"
        )
        if hasattr(center, "address") and center.address:
            print(f"    Address: {center.address}")

print()
print("To geocode these records, run:")
print("  python3 manage.py geocode_providers --all")
print()
