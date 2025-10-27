## 📊 **Database Management**

### **RDS Sync Issues & Solutions**

#### **Problem: Local DB vs RDS Out of Sync**
If your local database has migrations that haven't been applied to RDS, you'll get errors like:
```
column regional_centers.zip_codes does not exist
```

#### **Solution 1: Automatic Sync via Deployment**
```bash
# The easiest way - just push to main
git add .
git commit -m "Add new migrations"
git push origin main  # Triggers auto-deployment with migrations
```

#### **Solution 2: Manual Sync Check**
```bash
# Check local migration status
python3 manage.py showmigrations locations

# Check if specific migration exists
ls maplocation/locations/migrations/

# Create missing migrations
python3 manage.py makemigrations

# Apply migrations locally first
python3 manage.py migrate
```

#### **Solution 3: Use the RDS Sync Helper**
```bash
# Run the sync helper script
cd maplocation
python3 sync_rds.py
```

#### **Solution 4: Force RDS Migration**
```bash
# SSH to EB instance
eb ssh chla-api-env

# Check migration status on RDS
python manage.py showmigrations locations

# Force apply all migrations
python manage.py migrate --run-syncdb

# Check if zip_codes column exists
python manage.py shell
>>> from django.db import connection
>>> with connection.cursor() as cursor:
...     cursor.execute("SELECT column_name FROM information_schema.columns WHERE table_name = 'regional_centers' AND column_name = 'zip_codes';")
...     print(cursor.fetchone())
```

### **Local to RDS Sync**
```bash
# Option 1: Generate migrations from local changes
python manage.py makemigrations
git add .
git commit -m "Add new migrations"
git push origin main  # Triggers auto-deployment

# Option 2: Manual sync (if needed)
python manage.py migrate --run-syncdb
```

### **RDS to Local Sync**
```bash
# Backup RDS data
pg_dump -h <RDS_ENDPOINT> -U chla_admin -d postgres > backup.sql

# Restore to local
psql -U postgres -d your_local_db < backup.sql
```

### **Check Migration Status**
```bash
# Local
python manage.py showmigrations

# Remote (via EB SSH)
eb ssh chla-api-env
python manage.py showmigrations
```

### **Common RDS Sync Issues**

#### **Issue: Migration 0008 not applied to RDS**
**Symptoms**: `zip_codes` column missing, 500 errors on API calls
**Solution**: 
1. Ensure EB deployment completed successfully
2. Check EB logs for migration errors
3. Verify `.ebextensions` config is working
4. Manually run migrations if needed

#### **Issue: Environment variables not set**
**Symptoms**: Database connection failures
**Solution**:
1. Check GitHub secrets are configured
2. Verify EB environment variables are set
3. Use AWS CLI to set them manually if needed

#### **Issue: Security group blocking RDS access**
**Symptoms**: Connection timeout errors
**Solution**:
1. Check RDS security groups allow EB access
2. Verify VPC configuration
3. Check network ACLs
