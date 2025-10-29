#!/usr/bin/env python3
import os
import django

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'maplocation.settings')
django.setup()

from locations.models import ProviderV2

print("✅ All imports successful!")
print(f"ProviderV2 count: {ProviderV2.objects.count()}")
print("✅ Database connection works!")
print("\nYou can now run:")
print("  python3 check_geocoding_status.py")
print("  python3 manage.py geocode_providers --all")

