#!/usr/bin/env python3
"""
Check the status of providers in the database
Run: python3 check_providers.py
"""

import os
import sys
import django

# Setup Django
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'maplocation.settings')
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
django.setup()

from locations.models import Provider

def main():
    # Get all providers
    all_providers = Provider.objects.all()
    total = all_providers.count()
    
    print(f"Total providers in database: {total}")
    
    # Check coordinates
    with_coords = Provider.objects.exclude(latitude__isnull=True).exclude(longitude__isnull=True).count()
    without_coords = total - with_coords
    
    print(f"Providers with coordinates: {with_coords}")
    print(f"Providers without coordinates: {without_coords}")
    
    # Check San Diego providers
    print("\n--- San Diego Providers ---")
    sd_providers = Provider.objects.filter(city__icontains='san diego')
    print(f"Total San Diego providers: {sd_providers.count()}")
    
    for p in sd_providers[:10]:  # Show first 10
        coords = f"{p.latitude}, {p.longitude}" if p.latitude and p.longitude else "NO COORDINATES"
        print(f"  - {p.name}: {coords}")
        if p.address:
            print(f"    Address: {p.address}, {p.city}, {p.state} {p.zip_code}")
    
    # Check providers with location field
    print("\n--- Location Field Status ---")
    with_location = Provider.objects.exclude(location__isnull=True).count()
    print(f"Providers with PostGIS location field: {with_location}")
    
    # Sample a provider with no coordinates
    print("\n--- Sample Provider Without Coordinates ---")
    no_coords = Provider.objects.filter(latitude__isnull=True).first()
    if no_coords:
        print(f"Name: {no_coords.name}")
        print(f"Address: {no_coords.address}")
        print(f"City: {no_coords.city}")
        print(f"State: {no_coords.state}")
        print(f"Zip: {no_coords.zip_code}")

if __name__ == '__main__':
    main()