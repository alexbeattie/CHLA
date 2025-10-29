#!/usr/bin/env python3
"""
Fix migration history after renumbering migrations.
This script updates the django_migrations table to reflect the new migration numbering.
"""

import os
import sys
import django

# Setup Django
sys.path.insert(0, '/Users/alexbeattie/Developer/CHLA/maplocation')
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'maplocation.settings')
django.setup()

from django.db import connection

def fix_migration_history():
    """Update migration history to reflect renumbered migrations"""
    
    migration_updates = [
        # (old_name, new_name)
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
        print("Current migration records:")
        cursor.execute(
            "SELECT id, name FROM django_migrations WHERE app = 'locations' ORDER BY id"
        )
        for row in cursor.fetchall():
            print(f"  {row[0]}: {row[1]}")
        
        print("\nUpdating migration records...")
        for old_name, new_name in migration_updates:
            cursor.execute(
                "UPDATE django_migrations SET name = %s WHERE app = %s AND name = %s",
                [new_name, 'locations', old_name]
            )
            if cursor.rowcount > 0:
                print(f"✓ Updated: {old_name} → {new_name}")
            else:
                print(f"  Skipped: {old_name} (not found in database)")
        
        print("\nUpdated migration records:")
        cursor.execute(
            "SELECT id, name FROM django_migrations WHERE app = 'locations' ORDER BY id"
        )
        for row in cursor.fetchall():
            print(f"  {row[0]}: {row[1]}")
    
    print("\n✅ Migration history fixed!")

if __name__ == '__main__':
    fix_migration_history()

