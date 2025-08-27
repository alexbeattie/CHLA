#!/usr/bin/env python3
"""
RDS Sync Helper Script
Helps sync local database with AWS RDS and check migration status.
"""

import os
import sys
import django
from django.conf import settings
from django.core.management import execute_from_command_line

def setup_django():
    """Setup Django environment"""
    os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'maplocation.settings')
    django.setup()

def check_migrations():
    """Check migration status"""
    print("🔍 Checking migration status...")
    execute_from_command_line(['manage.py', 'showmigrations', 'locations'])
    execute_from_command_line(['manage.py', 'showmigrations', 'users'])

def create_missing_migrations():
    """Create any missing migrations"""
    print("\n📝 Creating missing migrations...")
    execute_from_command_line(['manage.py', 'makemigrations'])

def apply_migrations():
    """Apply all pending migrations"""
    print("\n🚀 Applying migrations...")
    execute_from_command_line(['manage.py', 'migrate'])

def check_database_connection():
    """Check if we can connect to the database"""
    print("\n🔌 Checking database connection...")
    try:
        from django.db import connection
        with connection.cursor() as cursor:
            cursor.execute("SELECT version();")
            version = cursor.fetchone()
            print(f"✅ Connected to database: {version[0]}")
            
            # Check if regional_centers table has zip_codes column
            cursor.execute("""
                SELECT column_name 
                FROM information_schema.columns 
                WHERE table_name = 'regional_centers' 
                AND column_name = 'zip_codes';
            """)
            result = cursor.fetchone()
            if result:
                print("✅ zip_codes column exists in regional_centers table")
            else:
                print("❌ zip_codes column missing from regional_centers table")
                
    except Exception as e:
        print(f"❌ Database connection failed: {e}")

def main():
    """Main function"""
    print("🔄 CHLA RDS Sync Helper")
    print("=" * 40)
    
    try:
        setup_django()
        
        # Check current state
        check_database_connection()
        check_migrations()
        
        # Create and apply migrations
        create_missing_migrations()
        apply_migrations()
        
        # Check final state
        print("\n📊 Final migration status:")
        check_migrations()
        check_database_connection()
        
        print("\n✅ Sync complete! If you're still having issues:")
        print("1. Check that your EB environment variables are set correctly")
        print("2. Verify the EB deployment completed successfully")
        print("3. Check EB logs for migration errors")
        print("4. Ensure the .ebextensions config is working")
        
    except Exception as e:
        print(f"❌ Error: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
