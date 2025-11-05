#!/usr/bin/env python3
"""
Geocode providers from CSV and add PostGIS points
"""
import csv
import sys
import os
from decimal import Decimal

# Add Django project to path
sys.path.insert(0, '/Users/alexbeattie/Developer/CHLA/maplocation')
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'maplocation.settings')

import django
django.setup()

from locations.utils.mapbox_geocode import geocode_with_fallback

def geocode_csv_providers(input_csv, output_csv):
    """Geocode providers from CSV and add PostGIS points"""
    
    with open(input_csv, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        fieldnames = reader.fieldnames
        providers = list(reader)
    
    print(f"Loaded {len(providers)} providers from CSV")
    
    geocoded = 0
    failed = []
    
    for i, row in enumerate(providers, 1):
        lat = row.get('latitude', '').strip()
        lng = row.get('longitude', '').strip()
        loc = row.get('location', '').strip()
        
        # Skip if already has coordinates and location
        if lat and lng and loc and lat not in ['0.0', '0.00', '0']:
            continue
        
        # Need to geocode
        address = row.get('address', '').strip()
        if not address:
            print(f"⚠️  Row {i}: No address for {row['name'][:50]}")
            failed.append((i, row['name'], 'No address'))
            continue
        
        # Clean address (remove JSON formatting if present)
        clean_address = address.replace('\n', ', ').replace('"', '').replace('{', '').replace('}', '')
        
        print(f"\n🔍 Row {i}: Geocoding {row['name'][:50]}")
        print(f"   Address: {clean_address[:80]}")
        
        coordinates = geocode_with_fallback(clean_address)
        
        if coordinates:
            lat_val, lng_val = coordinates
            row['latitude'] = str(lat_val)
            row['longitude'] = str(lng_val)
            row['location'] = f'POINT ({lng_val} {lat_val})'
            print(f"   ✅ Success: {lat_val}, {lng_val}")
            geocoded += 1
        else:
            print(f"   ❌ Failed to geocode")
            failed.append((i, row['name'], clean_address[:50]))
    
    # Write updated CSV
    with open(output_csv, 'w', encoding='utf-8', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(providers)
    
    print(f"\n{'='*60}")
    print(f"✅ Successfully geocoded: {geocoded}")
    print(f"❌ Failed: {len(failed)}")
    
    if failed:
        print(f"\n=== Failed Geocoding ===")
        for row_num, name, reason in failed:
            print(f"Row {row_num}: {name[:50]} - {reason}")
    
    print(f"\n📝 Updated CSV saved to: {output_csv}")
    
    return geocoded, failed


if __name__ == '__main__':
    input_csv = '/Users/alexbeattie/Desktop/combined_providers_all.csv'
    output_csv = '/Users/alexbeattie/Desktop/combined_providers_all_geocoded.csv'
    
    geocoded, failed = geocode_csv_providers(input_csv, output_csv)
    
    if geocoded > 0:
        print(f"\n✅ Ready to import {geocoded} geocoded providers to database")
        print(f"\nNext steps:")
        print(f"1. Review the geocoded CSV: {output_csv}")
        print(f"2. Import to local database (create import script)")
        print(f"3. Sync to RDS: cd maplocation && python3 manage.py sync_to_rds")

