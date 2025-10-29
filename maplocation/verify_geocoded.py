#!/usr/bin/env python3
"""
Verify that providers were successfully geocoded
"""
import os
import django

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'maplocation.settings')
django.setup()

from locations.models import ProviderV2
from decimal import Decimal

print("=" * 60)
print("RECENTLY GEOCODED PROVIDERS")
print("=" * 60)
print()

# Find providers with real coordinates (not 0.00000000)
geocoded = ProviderV2.objects.exclude(
    latitude=Decimal('0.00000000')
).exclude(
    longitude=Decimal('0.00000000')
).exclude(
    latitude__isnull=True
).exclude(
    longitude__isnull=True
).order_by('-updated_at')[:10]

print(f"✅ Found {geocoded.count()} geocoded providers")
print()
print("Sample of recently geocoded providers:")
print()

for i, provider in enumerate(geocoded, 1):
    print(f"{i}. {provider.name}")
    print(f"   📍 Coordinates: {provider.latitude}, {provider.longitude}")
    if provider.address:
        if isinstance(provider.address, dict):
            addr = f"{provider.address.get('street', '')}, {provider.address.get('city', '')}, {provider.address.get('state', '')} {provider.address.get('zip', '')}"
        else:
            addr = provider.address
        print(f"   📮 Address: {addr}")
    print(f"   🕐 Updated: {provider.updated_at}")
    print()

# Check total stats
total = ProviderV2.objects.count()
with_coords = ProviderV2.objects.exclude(
    latitude=Decimal('0.00000000')
).exclude(
    longitude=Decimal('0.00000000')
).exclude(
    latitude__isnull=True
).count()
without_coords = total - with_coords

print("=" * 60)
print("OVERALL STATISTICS")
print("=" * 60)
print(f"Total providers: {total}")
print(f"✅ With valid coordinates: {with_coords} ({with_coords/total*100:.1f}%)")
print(f"❌ Still need geocoding: {without_coords} ({without_coords/total*100:.1f}%)")
print("=" * 60)

