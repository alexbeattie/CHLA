#!/usr/bin/env python
"""
Emergency database population script.
Upload this and run it directly on the EB instance.
"""
import os
import sys
import django

# Setup Django
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'maplocation.settings')
django.setup()

from locations.models import Provider

# Provider data to insert
providers_data = [
    {
        "name": "A & H BEHAVIORAL THERAPY",
        "phone": "909-665-7070, 818-823-1515",
        "address": "16000 Ventura Blvd #1103, Encino, CA 91436, United States",
        "website_domain": "http://www.ahappyfam.com/",
        "latitude": "34.155931",
        "longitude": "-118.481844",
        "areas": "SAN FERNANDO VALLEY,SAN GABRIEL VALLEY,LONG BEACH,INGLEWOOD,COMPTON",
        "specializations": "Autism",
        "insurance_accepted": "Health Net, MHN, Magellan, Blue Shield, Anthem/Blue Cross, Regional center, Self-determination programs, Kaiser/Easterseal",
        "services": "In Home ABA Therapy, Autism Behavioral Intervention, Autism Training for Parents, One-to-One Autism Therapy, Early Intervention, Telehealth",
        "coverage_areas": "SAN FERNANDO VALLEY, SAN GABRIEL VALLEY, LONG BEACH, INGLEWOOD, COMPTON"
    },
    # Add a few more sample providers
    {
        "name": "SAMPLE AUTISM CENTER",
        "phone": "555-123-4567",
        "address": "123 Main St, Los Angeles, CA 90210",
        "website_domain": "http://www.sample-autism.com/",
        "latitude": "34.0522",
        "longitude": "-118.2437",
        "areas": "LOS ANGELES",
        "specializations": "Autism, ABA Therapy",
        "insurance_accepted": "Blue Cross, Kaiser",
        "services": "ABA Therapy, Speech Therapy",
        "coverage_areas": "LOS ANGELES COUNTY"
    }
]

def populate_database():
    print("🚨 EMERGENCY: Populating database with sample providers...")
    
    # Clear existing data
    Provider.objects.all().delete()
    print("Cleared existing providers")
    
    # Add sample data
    for provider_data in providers_data:
        provider = Provider.objects.create(**provider_data)
        print(f"Created provider: {provider.name}")
    
    total = Provider.objects.count()
    print(f"✅ SUCCESS: {total} providers now in database!")
    
    return total

if __name__ == '__main__':
    try:
        count = populate_database()
        print(f"Database populated with {count} providers")
    except Exception as e:
        print(f"Error: {e}")
        sys.exit(1)
