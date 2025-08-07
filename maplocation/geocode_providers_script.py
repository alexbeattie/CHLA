#!/usr/bin/env python3
"""
Quick script to geocode providers missing coordinates
Run this from the maplocation directory:
    python3 geocode_providers_script.py
"""

import os
import sys
import django

# Setup Django
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'maplocation.settings')
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
django.setup()

from locations.models import Provider
from django.contrib.gis.geos import Point
import requests
import time

def geocode_address(address):
    """Geocode an address using a free geocoding service"""
    try:
        # Using Nominatim (OpenStreetMap) - free, no API key needed
        url = "https://nominatim.openstreetmap.org/search"
        params = {
            'q': address,
            'format': 'json',
            'limit': 1,
            'countrycodes': 'us'
        }
        headers = {
            'User-Agent': 'CHLA Provider Geocoder/1.0'
        }
        
        response = requests.get(url, params=params, headers=headers)
        response.raise_for_status()
        
        data = response.json()
        if data:
            lat = float(data[0]['lat'])
            lon = float(data[0]['lon'])
            return lat, lon
    except Exception as e:
        print(f"Error geocoding {address}: {e}")
    
    return None, None

def main():
    # Get providers without coordinates
    providers_without_coords = Provider.objects.filter(
        latitude__isnull=True
    ) | Provider.objects.filter(
        longitude__isnull=True
    )
    
    total = providers_without_coords.count()
    print(f"Found {total} providers without coordinates")
    
    if total == 0:
        print("All providers have coordinates!")
        return
    
    geocoded = 0
    failed = 0
    
    for i, provider in enumerate(providers_without_coords, 1):
        # Build full address
        address_parts = []
        if provider.address:
            address_parts.append(provider.address)
        if provider.city:
            address_parts.append(provider.city)
        if provider.state:
            address_parts.append(provider.state)
        else:
            address_parts.append('CA')  # Default to California
        if provider.zip_code:
            address_parts.append(provider.zip_code)
        
        if not address_parts:
            print(f"[{i}/{total}] Skipping {provider.name} - no address")
            failed += 1
            continue
        
        full_address = ', '.join(filter(None, address_parts))
        print(f"[{i}/{total}] Geocoding {provider.name}: {full_address}")
        
        lat, lon = geocode_address(full_address)
        
        if lat and lon:
            provider.latitude = lat
            provider.longitude = lon
            # The save method will automatically create the location Point
            provider.save()
            geocoded += 1
            print(f"  ✓ Success: {lat}, {lon}")
        else:
            failed += 1
            print(f"  ✗ Failed to geocode")
        
        # Rate limit: 1 request per second for Nominatim
        time.sleep(1)
    
    print(f"\nGeocoding complete!")
    print(f"  Successfully geocoded: {geocoded}")
    print(f"  Failed: {failed}")
    
    # Show some San Diego providers
    print("\nSan Diego providers:")
    sd_providers = Provider.objects.filter(city__icontains='san diego').exclude(
        latitude__isnull=True
    )[:5]
    
    for p in sd_providers:
        print(f"  - {p.name}: {p.latitude}, {p.longitude}")

if __name__ == '__main__':
    main()