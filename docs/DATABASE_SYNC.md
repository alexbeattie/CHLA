# Database Synchronization Guide

Guide for synchronizing data between local PostgreSQL and AWS RDS.

## Quick Reference

**Automatic Sync (Recommended for Schema Changes):**
```bash
python manage.py makemigrations
python manage.py migrate
git add . && git commit -m "Add migrations" && git push origin main
```

**Manual Data Sync:**
```bash
# See "Full Data Sync" section below
```

---

## Table of Contents
- [Migration-Based Sync (Recommended)](#migration-based-sync-recommended)
- [Full Data Sync](#full-data-sync)
- [Common Issues](#common-issues)
- [Emergency Recovery](#emergency-recovery)

---

## Migration-Based Sync (Recommended)

Use Django migrations for all schema changes. This ensures consistency and version control.

### Problem: Local DB vs RDS Out of Sync

**Symptoms:**
```
column regional_centers.zip_codes does not exist
500 errors on API calls
Migration X not found in remote database
```

### Solution 1: Automatic Sync via Deployment

```bash
# 1. Create migrations locally
cd maplocation
python manage.py makemigrations

# 2. Test migrations locally
python manage.py migrate

# 3. Commit and push (triggers auto-deployment with migrations)
git add .
git commit -m "Add new migrations for regional centers"
git push origin main
```

The `.ebextensions/01_auto_migrate.config` automatically runs migrations on deployment.

### Solution 2: Manual Sync Check

```bash
# Check local migration status
python3 manage.py showmigrations locations

# Check if specific migration exists
ls locations/migrations/

# Create missing migrations
python3 manage.py makemigrations

# Apply migrations locally first
python3 manage.py migrate
```

### Solution 3: Force Remote Migration

If auto-migration fails, manually run migrations on RDS:

```bash
# SSH to EB instance
eb ssh chla-api-prod --region us-west-2

# Check migration status on RDS
python manage.py showmigrations locations

# Force apply all migrations
python manage.py migrate --run-syncdb

# Verify specific column exists
python manage.py shell
>>> from django.db import connection
>>> with connection.cursor() as cursor:
...     cursor.execute("SELECT column_name FROM information_schema.columns WHERE table_name = 'regional_centers' AND column_name = 'zip_codes';")
...     print(cursor.fetchone())
>>> exit()
```

---

## Full Data Sync

Use this for initial setup or complete data recovery. **WARNING: This will overwrite the target database.**

### Local to RDS Sync

**Step 1: Create Local Database Dump**

```bash
cd maplocation

# Get your local DB credentials
python3 manage.py shell
>>> from django.conf import settings
>>> db = settings.DATABASES['default']
>>> print(f"Host: {db['HOST']}, User: {db['USER']}, Name: {db['NAME']}, Port: {db['PORT']}")
>>> exit()

# Create the dump (replace with your actual values)
pg_dump -h localhost -U alexbeattie -d shafali -p 5432 \
  --clean --if-exists --no-owner --no-privileges \
  -f chla_local_dump.sql
```

**Step 2: Restore to RDS**

```bash
# Get RDS credentials from EB environment
eb printenv chla-api-prod

# Restore the dump to RDS
psql -h chla-postgres-db.cpkvcu4f59w6.us-west-2.rds.amazonaws.com \
  -U chla_admin \
  -d postgres \
  -p 5432 \
  -f chla_local_dump.sql

# Enter password when prompted (from DB_PASSWORD secret)
```

**Step 3: Verify Data**

```bash
# Connect to RDS
psql -h chla-postgres-db.cpkvcu4f59w6.us-west-2.rds.amazonaws.com \
  -U chla_admin \
  -d postgres

# Check tables
\dt

# Count records
SELECT COUNT(*) FROM locations_providerv2;
SELECT COUNT(*) FROM locations_regionalcenter;

# Exit
\q
```

### RDS to Local Sync

**Step 1: Backup RDS Data**

```bash
# Create dump from RDS
pg_dump -h chla-postgres-db.cpkvcu4f59w6.us-west-2.rds.amazonaws.com \
  -U chla_admin \
  -d postgres \
  -p 5432 \
  --clean --if-exists --no-owner --no-privileges \
  -f rds_backup_$(date +%Y%m%d).sql
```

**Step 2: Restore to Local**

```bash
# Drop and recreate local database (optional, for clean slate)
dropdb shafali
createdb shafali

# Restore from RDS backup
psql -h localhost -U alexbeattie -d shafali -f rds_backup_20241026.sql
```

**Step 3: Run Migrations Locally**

```bash
cd maplocation
python manage.py migrate
```

---

## Check Migration Status

### Local Database

```bash
cd maplocation
python manage.py showmigrations

# Check specific app
python manage.py showmigrations locations

# View migration history
python manage.py showmigrations --list
```

### Remote Database (RDS)

```bash
# Via EB SSH
eb ssh chla-api-prod --region us-west-2
python manage.py showmigrations locations
exit

# Via direct DB connection
psql -h chla-postgres-db.cpkvcu4f59w6.us-west-2.rds.amazonaws.com \
  -U chla_admin \
  -d postgres \
  -c "SELECT * FROM django_migrations WHERE app='locations' ORDER BY id DESC LIMIT 10;"
```

---

## Common Issues

### Issue: Migration 0008 not applied to RDS

**Symptoms:**
- `zip_codes` column missing
- 500 errors on API calls to regional centers endpoint
- Error: `column "zip_codes" does not exist`

**Solution:**
1. Verify migration exists locally: `ls locations/migrations/0008_*.py`
2. Check EB deployment completed successfully: `eb status`
3. Review EB logs for migration errors: `eb logs`
4. Verify `.ebextensions/01_auto_migrate.config` is present
5. Manually run migrations if auto-migration failed (see Solution 3 above)

### Issue: Environment Variables Not Set

**Symptoms:**
- Database connection failures
- `FATAL: password authentication failed`
- Connection timeout errors

**Solution:**
1. Check GitHub secrets are configured in repository settings
2. Verify EB environment variables: `eb printenv chla-api-prod`
3. Set missing variables:
   ```bash
   eb setenv DB_HOST=chla-postgres-db.cpkvcu4f59w6.us-west-2.rds.amazonaws.com \
     DB_NAME=postgres \
     DB_USER=chla_admin \
     DB_PASSWORD=your_password \
     DB_SSL_REQUIRE=true
   ```

### Issue: Security Group Blocking RDS Access

**Symptoms:**
- Connection timeout errors
- `could not connect to server: Connection timed out`

**Solution:**
1. Check RDS security groups in AWS Console
2. Ensure security group allows inbound traffic from EB environment
3. Verify VPC configuration
4. Check network ACLs
5. Temporarily add your IP for debugging (remove after)

### Issue: Migration Conflicts

**Symptoms:**
- `Conflicting migrations detected`
- Multiple migration branches

**Solution:**
```bash
# Create a merge migration
python manage.py makemigrations --merge

# Review and commit the merge migration
git add locations/migrations/
git commit -m "Merge conflicting migrations"
git push origin main
```

### Issue: Stuck Migrations

**Symptoms:**
- Migration appears applied but changes not in database
- `Operation not permitted on this database`

**Solution:**
```bash
# Fake the migration (use with caution)
python manage.py migrate locations 0014 --fake

# Then apply the real migration
python manage.py migrate locations 0015

# Or start fresh (DEVELOPMENT ONLY)
python manage.py migrate locations zero
python manage.py migrate locations
```

---

## Emergency Recovery

### Rollback to Previous Migration

```bash
# Locally
python manage.py migrate locations 0014  # Roll back to migration 0014

# On RDS (via EB SSH)
eb ssh chla-api-prod
python manage.py migrate locations 0014
```

### Restore from Backup

```bash
# List available backups (if using RDS automated backups)
aws rds describe-db-snapshots --db-instance-identifier chla-postgres-db

# Restore from snapshot (creates new instance)
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier chla-postgres-db-restored \
  --db-snapshot-identifier rds:chla-postgres-db-2024-10-26-06-00

# Or restore from manual SQL dump
psql -h chla-postgres-db.cpkvcu4f59w6.us-west-2.rds.amazonaws.com \
  -U chla_admin \
  -d postgres \
  -f backup_20241026.sql
```

### Reset Database (DEVELOPMENT ONLY)

**WARNING: This will DELETE all data!**

```bash
# Local development only
python manage.py flush

# Or drop and recreate
dropdb shafali && createdb shafali
python manage.py migrate
python manage.py createsuperuser
```

---

## Best Practices

1. **Always use migrations** for schema changes (never alter database directly)
2. **Test locally first** before deploying to production
3. **Backup before major changes**:
   ```bash
   pg_dump -h chla-postgres-db.cpkvcu4f59w6.us-west-2.rds.amazonaws.com \
     -U chla_admin -d postgres > backup_$(date +%Y%m%d).sql
   ```
4. **Monitor migration status** after deployments
5. **Use version control** for all migration files
6. **Document manual interventions** in commit messages
7. **Never fake migrations** in production without understanding implications

---

## Useful Commands

### Database Information

```bash
# Django shell (local or via EB SSH)
python manage.py shell

# Check database connection
>>> from django.db import connection
>>> connection.ensure_connection()
>>> print("Connected to:", connection.settings_dict['NAME'])

# List all tables
>>> from django.db import connection
>>> with connection.cursor() as cursor:
...     cursor.execute("SELECT tablename FROM pg_tables WHERE schemaname='public';")
...     for table in cursor.fetchall():
...         print(table[0])

# Count records in a table
>>> from locations.models import ProviderV2
>>> print(ProviderV2.objects.count())
```

### PostgreSQL Commands

```bash
# Connect to database
psql -h HOST -U USER -d DATABASE

# Common psql commands
\dt                    # List tables
\d table_name          # Describe table
\l                     # List databases
\du                    # List users
\q                     # Quit

# SQL queries
SELECT * FROM django_migrations WHERE app='locations';
SELECT COUNT(*) FROM locations_providerv2;
```

---

## Additional Resources

- [Django Migrations Documentation](https://docs.djangoproject.com/en/5.2/topics/migrations/)
- [PostgreSQL Backup and Restore](https://www.postgresql.org/docs/current/backup.html)
- [AWS RDS Backup](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_CommonTasks.BackupRestore.html)
- [Deployment Guide](./DEPLOYMENT.md)
- [Troubleshooting Guide](./TROUBLESHOOTING.md)
