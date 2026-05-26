#!/usr/bin/env python3
"""
Fix migration history and write detailed output to a log file.
"""

import os
import sys
import django
from datetime import datetime

# Setup paths and Django
sys.path.insert(0, '/Users/alexbeattie/Developer/CHLA/maplocation')
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'maplocation.settings')

# Output file
output_file = '/Users/alexbeattie/Developer/CHLA/maplocation/migration_fix_output.txt'

def log(message):
    """Write to both stdout and file"""
    print(message)
    with open(output_file, 'a') as f:
        f.write(message + '\n')

# Clear previous output
with open(output_file, 'w') as f:
    f.write(f"Migration Fix Log - {datetime.now()}\n")
    f.write("="*60 + "\n\n")

try:
    log("Setting up Django...")
    django.setup()
    log("✓ Django setup complete")
    
    from django.db import connection
    
    log("\n" + "="*60)
    log("CURRENT MIGRATION STATE")
    log("="*60)
    
    with connection.cursor() as cursor:
        cursor.execute(
            "SELECT id, name, applied FROM django_migrations WHERE app = 'locations' ORDER BY id"
        )
        rows = cursor.fetchall()
        log(f"\nFound {len(rows)} migration records:\n")
        for row in rows:
            log(f"  {row[0]:3d}. {row[1]:<60s} {row[2]}")
    
    log("\n" + "="*60)
    log("UPDATING MIGRATION RECORDS")
    log("="*60 + "\n")
    
    migration_updates = [
        ('0016_5_add_location_name_to_regionalcenter', '0017_add_location_name_to_regionalcenter'),
        ('0017_load_regional_center_data', '0018_load_regional_center_data'),
        ('0018_deduplicate_la_regional_centers', '0019_deduplicate_la_regional_centers'),
        ('0019_deduplicate_all_regional_centers', '0020_deduplicate_all_regional_centers'),
        ('0020_restore_regional_center_zip_codes', '0021_restore_regional_center_zip_codes'),
        ('0021_drop_old_provider_table', '0022_drop_old_provider_table'),
        ('0022_alter_providerfundingsource_provider_and_more', '0023_alter_providerfundingsource_provider_and_more'),
        ('0023_add_location_pointfield', '0024_add_location_pointfield'),
    ]
    
    with connection.cursor() as cursor:
        for old_name, new_name in migration_updates:
            cursor.execute(
                "UPDATE django_migrations SET name = %s WHERE app = %s AND name = %s",
                [new_name, 'locations', old_name]
            )
            if cursor.rowcount > 0:
                log(f"✓ Updated: {old_name:<60s} → {new_name}")
            else:
                log(f"  Skipped: {old_name:<60s} (not in database)")
    
    log("\n" + "="*60)
    log("UPDATED MIGRATION STATE")
    log("="*60)
    
    with connection.cursor() as cursor:
        cursor.execute(
            "SELECT id, name, applied FROM django_migrations WHERE app = 'locations' ORDER BY id"
        )
        rows = cursor.fetchall()
        log(f"\nMigration records after update:\n")
        for row in rows:
            log(f"  {row[0]:3d}. {row[1]:<60s} {row[2]}")
    
    log("\n" + "="*60)
    log("✅ MIGRATION HISTORY FIXED SUCCESSFULLY!")
    log("="*60)
    log(f"\nYou can now run: python3 manage.py migrate")
    
except Exception as e:
    log(f"\n❌ ERROR: {str(e)}")
    import traceback
    log("\nFull traceback:")
    log(traceback.format_exc())
    sys.exit(1)

