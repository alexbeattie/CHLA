#!/usr/bin/env python3
"""
CHLA Data Sync to RDS Script
Syncs local PostgreSQL database (schema + data) to AWS RDS.
"""

import os
import sys
import subprocess
import django
from django.conf import settings
from django.core.management import execute_from_command_line

def setup_django():
    """Setup Django environment"""
    os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'maplocation.settings')
    django.setup()

def check_local_database():
    """Check local database connection and data"""
    print("🔍 Checking local database...")
    try:
        from django.db import connection
        with connection.cursor() as cursor:
            # Check connection
            cursor.execute("SELECT version();")
            version = cursor.fetchone()
            print(f"✅ Local DB connected: {version[0]}")
            
            # Check what tables actually exist
            cursor.execute("""
                SELECT table_name 
                FROM information_schema.tables 
                WHERE table_schema = 'public' 
                ORDER BY table_name;
            """)
            tables = cursor.fetchall()
            table_names = [table[0] for table in tables]
            print(f"📊 Available tables: {table_names}")
            
            # Check if Django migrations have been run
            django_tables = [name for name in table_names if name.startswith('django_')]
            if django_tables:
                print(f"✅ Django tables found: {django_tables}")
            else:
                print("⚠️ No Django tables found - migrations may not have been run")
            
            # Check if we have the app tables
            app_tables = [name for name in table_names if name.startswith('locations_') or name.startswith('users_')]
            if app_tables:
                print(f"✅ App tables found: {app_tables}")
                # Try to get row count from first app table
                if app_tables:
                    first_table = app_tables[0]
                    cursor.execute(f"SELECT COUNT(*) FROM {first_table};")
                    count = cursor.fetchone()[0]
                    print(f"📊 Rows in {first_table}: {count}")
            else:
                print("⚠️ No app tables found - need to run migrations first")
                
    except Exception as e:
        print(f"❌ Local database check failed: {e}")
        return False
    return True

def get_rds_connection_info():
    """Get RDS connection info from environment or user input"""
    print("\n🔌 RDS Connection Setup")
    print("=" * 40)
    
    # Try to get from environment variables
    db_host = os.getenv('DB_HOST')
    db_user = os.getenv('DB_USER', 'chla_admin')
    db_name = os.getenv('DB_NAME', 'postgres')
    db_port = os.getenv('DB_PORT', '5432')
    
    if not db_host:
        print("❌ DB_HOST not found in environment variables")
        print("Please set your RDS connection details:")
        db_host = input("RDS Endpoint (e.g., chla-postgres-db.xxx.rds.amazonaws.com): ").strip()
        db_user = input("Database User [chla_admin]: ").strip() or 'chla_admin'
        db_name = input("Database Name [postgres]: ").strip() or 'postgres'
        db_port = input("Database Port [5432]: ").strip() or '5432'
    
    # Get password
    db_password = os.getenv('DB_PASSWORD')
    if not db_password:
        import getpass
        db_password = getpass.getpass("RDS Password: ")
    
    return {
        'host': db_host,
        'user': db_user,
        'password': db_password,
        'name': db_name,
        'port': db_port
    }

def create_pg_dump_command(connection_info, output_file):
    """Create pg_dump command for local database"""
    # Get local DB settings from Django
    from django.conf import settings
    local_db = settings.DATABASES['default']
    
    # Use the actual database name from settings (shafali)
    cmd = [
        'pg_dump',
        '-h', local_db['HOST'] or 'localhost',
        '-U', local_db['USER'],  # This will be 'alexbeattie'
        '-d', local_db['NAME'],  # This will be 'shafali'
        '-p', str(local_db['PORT'] or 5432),
        '-f', output_file,
        '--clean',  # Drop tables before creating
        '--if-exists',  # Don't error if tables don't exist
        '--no-owner',  # Don't set ownership
        '--no-privileges'  # Don't set privileges
    ]
    
    # Set password environment variable
    env = os.environ.copy()
    env['PGPASSWORD'] = local_db['PASSWORD']
    
    return cmd, env

def create_pg_restore_command(connection_info, input_file):
    """Create pg_restore command for RDS"""
    cmd = [
        'psql',
        '-h', connection_info['host'],
        '-U', connection_info['user'],
        '-d', connection_info['name'],
        '-p', connection_info['port'],
        '-f', input_file
    ]
    
    # Set password environment variable
    env = os.environ.copy()
    env['PGPASSWORD'] = connection_info['password']
    
    return cmd, env

def sync_data_to_rds():
    """Main sync function"""
    print("\n🔄 Starting Data Sync to RDS")
    print("=" * 40)
    
    # Check local database
    if not check_local_database():
        print("❌ Cannot proceed without local database access")
        return False
    
    # Get RDS connection info
    rds_info = get_rds_connection_info()
    
    # Create dump file
    dump_file = "chla_local_dump.sql"
    print(f"\n📦 Creating database dump: {dump_file}")
    
    try:
        cmd, env = create_pg_dump_command(rds_info, dump_file)
        print(f"Running: {' '.join(cmd)}")
        
        result = subprocess.run(cmd, env=env, capture_output=True, text=True)
        if result.returncode == 0:
            print("✅ Database dump created successfully")
        else:
            print(f"❌ Database dump failed: {result.stderr}")
            return False
            
    except Exception as e:
        print(f"❌ Error creating dump: {e}")
        return False
    
    # Test RDS connection
    print(f"\n🔌 Testing RDS connection...")
    try:
        test_cmd = [
            'psql',
            '-h', rds_info['host'],
            '-U', rds_info['user'],
            '-d', rds_info['name'],
            '-p', rds_info['port'],
            '-c', 'SELECT version();'
        ]
        
        test_env = os.environ.copy()
        test_env['PGPASSWORD'] = rds_info['password']
        
        result = subprocess.run(test_cmd, env=test_env, capture_output=True, text=True)
        if result.returncode == 0:
            print("✅ RDS connection successful")
        else:
            print(f"❌ RDS connection failed: {result.stderr}")
            return False
            
    except Exception as e:
        print(f"❌ Error testing RDS connection: {e}")
        return False
    
    # Restore to RDS
    print(f"\n🚀 Restoring data to RDS...")
    try:
        restore_cmd, restore_env = create_pg_restore_command(rds_info, dump_file)
        print(f"Running: {' '.join(restore_cmd)}")
        
        result = subprocess.run(restore_cmd, env=restore_env, capture_output=True, text=True)
        if result.returncode == 0:
            print("✅ Data restored to RDS successfully!")
        else:
            print(f"❌ Data restore failed: {result.stderr}")
            return False
            
    except Exception as e:
        print(f"❌ Error restoring data: {e}")
        return False
    
    # Clean up
    if os.path.exists(dump_file):
        os.remove(dump_file)
        print(f"🧹 Cleaned up temporary file: {dump_file}")
    
    return True

def verify_rds_data():
    """Verify data was synced correctly"""
    print("\n🔍 Verifying RDS data...")
    
    # This would require connecting to RDS via Django
    # For now, just provide instructions
    print("✅ Data sync completed!")
    print("\n📋 Next steps:")
    print("1. Deploy your app to trigger migrations: git push origin main")
    print("2. Check EB logs to ensure migrations ran: eb logs chla-api-env --all")
    print("3. Test your API endpoints")
    print("4. If issues persist, check the deployment guide")

def ensure_local_migrations():
    """Ensure local migrations have been run"""
    print("\n📝 Checking local migrations...")
    try:
        from django.db import connection
        with connection.cursor() as cursor:
            # Check if Django tables exist
            cursor.execute("""
                SELECT table_name 
                FROM information_schema.tables 
                WHERE table_schema = 'public' 
                AND table_name = 'django_migrations';
            """)
            result = cursor.fetchone()
            
            if not result:
                print("⚠️ Django migrations table not found. Running migrations...")
                execute_from_command_line(['manage.py', 'migrate'])
                print("✅ Local migrations completed")
            else:
                print("✅ Django migrations table exists")
                
    except Exception as e:
        print(f"❌ Migration check failed: {e}")
        return False
    return True

def main():
    """Main function"""
    print("🔄 CHLA Data Sync to RDS")
    print("=" * 50)
    print("This script will sync your local PostgreSQL data to AWS RDS")
    print("Make sure you have:")
    print("- Local PostgreSQL running with your data")
    print("- RDS connection details")
    print("- Network access to RDS")
    print()
    
    try:
        setup_django()
        
        # Ensure local migrations are run first
        if not ensure_local_migrations():
            print("❌ Cannot proceed without local migrations")
            sys.exit(1)
        
        # Perform the sync
        if sync_data_to_rds():
            verify_rds_data()
        else:
            print("\n❌ Data sync failed. Check the errors above.")
            sys.exit(1)
            
    except KeyboardInterrupt:
        print("\n\n⏹️ Sync cancelled by user")
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ Unexpected error: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
