#!/usr/bin/env python3
"""
Add missing migration records to django_migrations table.
"""

import os
import sys
import django
from datetime import datetime

sys.path.insert(0, '/Users/alexbeattie/Developer/CHLA/maplocation')
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'maplocation.settings')

output_file = '/Users/alexbeattie/Developer/CHLA/maplocation/add_migrations_output.txt'

def log(message):
    print(message)
    with open(output_file, 'a') as f:
        f.write(message + '\n')

with open(output_file, 'w') as f:
    f.write(f"Add Missing Migrations - {datetime.now()}\n")
    f.write("="*60 + "\n\n")

try:
    log("Setting up Django...")
    django.setup()
    log("✓ Django setup complete")
    
    from django.db import connection
    from django.utils import timezone
    
    missing_migrations = [
        '0017_add_location_name_to_regionalcenter',
        '0024_add_location_pointfield',
    ]
    
    log("\n" + "="*60)
    log("ADDING MISSING MIGRATIONS")
    log("="*60 + "\n")
    
    with connection.cursor() as cursor:
        for migration_name in missing_migrations:
            # Check if it already exists
            cursor.execute(
                "SELECT COUNT(*) FROM django_migrations WHERE app = %s AND name = %s",
                ['locations', migration_name]
            )
            exists = cursor.fetchone()[0] > 0
            
            if exists:
                log(f"  Skipped: {migration_name} (already exists)")
            else:
                # Insert the migration record
                cursor.execute(
                    "INSERT INTO django_migrations (app, name, applied) VALUES (%s, %s, %s)",
                    ['locations', migration_name, timezone.now()]
                )
                log(f"✓ Added: {migration_name}")
    
    log("\n" + "="*60)
    log("FINAL MIGRATION STATE")
    log("="*60)
    
    with connection.cursor() as cursor:
        cursor.execute(
            "SELECT id, name, applied FROM django_migrations WHERE app = 'locations' ORDER BY name"
        )
        rows = cursor.fetchall()
        log(f"\nAll location migrations (sorted by name):\n")
        for row in rows:
            log(f"  {row[1]:<60s} {row[2]}")
    
    log("\n" + "="*60)
    log("✅ MISSING MIGRATIONS ADDED!")
    log("="*60)
    log(f"\nYou can now run: python3 manage.py migrate")
    
except Exception as e:
    log(f"\n❌ ERROR: {str(e)}")
    import traceback
    log("\nFull traceback:")
    log(traceback.format_exc())
    sys.exit(1)

