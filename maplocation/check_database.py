#!/usr/bin/env python3
"""
Check which database Django is connected to
"""
import os
import django

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'maplocation.settings')
django.setup()

from django.conf import settings
from django.db import connection

print("=" * 60)
print("DATABASE CONNECTION INFO")
print("=" * 60)
print()

db_settings = settings.DATABASES['default']

print(f"Database Engine: {db_settings['ENGINE']}")
print(f"Database Name:   {db_settings['NAME']}")
print(f"Database Host:   {db_settings['HOST']}")
print(f"Database Port:   {db_settings['PORT']}")
print(f"Database User:   {db_settings['USER']}")
print()

# Determine if local or RDS
if db_settings['HOST'] in ['localhost', '127.0.0.1', '']:
    print("📍 Location: LOCAL PostgreSQL (on your Mac)")
    print()
    print("⚠️  Your geocoded data is on your LOCAL database.")
    print("   To use it in production, you need to sync to RDS.")
elif 'rds.amazonaws.com' in db_settings['HOST']:
    print("📍 Location: AWS RDS (Production)")
    print()
    print("✅ Your geocoded data is already in production!")
else:
    print(f"📍 Location: Remote database at {db_settings['HOST']}")

print()
print("=" * 60)
print()

# Test connection
try:
    with connection.cursor() as cursor:
        cursor.execute("SELECT version();")
        version = cursor.fetchone()[0]
        print(f"✅ Connected successfully!")
        print(f"PostgreSQL Version: {version}")
except Exception as e:
    print(f"❌ Connection error: {e}")

