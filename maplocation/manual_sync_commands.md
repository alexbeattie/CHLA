# Manual PostgreSQL to RDS Sync Commands

## 🚀 **Quick Data Sync (Recommended)**

### **Step 1: Create Local Database Dump**
```bash
# From your local machine, create a complete dump
cd maplocation

# Get your local DB details from Django settings
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

### **Step 2: Restore to RDS**
```bash
# Restore the dump to RDS (replace with your RDS details)
psql -h chla-postgres-db.xxx.rds.amazonaws.com \
  -U chla_admin \
  -d postgres \
  -p 5432 \
  -f chla_local_dump.sql
```

### **Step 3: Deploy to Trigger Migrations**
```bash
# Push to trigger automatic deployment and migrations
git add .
git commit -m "Sync local data to RDS"
git push origin main
```

## 🔧 **Alternative: Step-by-Step Sync**

### **Option A: Using Django Management Commands**
```bash
# 1. Export data from local
python3 manage.py dumpdata locations > locations_data.json
python3 manage.py dumpdata users > users_data.json

# 2. Deploy to EB (this will create tables)
git push origin main

# 3. Import data to RDS (via EB SSH)
eb ssh chla-api-env
python manage.py loaddata locations_data.json
python manage.py loaddata users_data.json
```

### **Option B: Direct Database Copy**
```bash
# 1. Create schema-only dump
pg_dump -h localhost -U alexbeattie -d shafali \
  --schema-only --no-owner --no-privileges \
  -f schema_only.sql

# 2. Create data-only dump
pg_dump -h localhost -U alexbeattie -d shafali \
  --data-only --no-owner --no-privileges \
  -f data_only.sql

# 3. Restore schema to RDS
psql -h chla-postgres-db.xxx.rds.amazonaws.com -U chla_admin -d postgres -f schema_only.sql

# 4. Restore data to RDS
psql -h chla-postgres-db.xxx.rds.amazonaws.com -U chla_admin -d postgres -f data_only.sql
```

## 📋 **Required Information**

### **Local Database Details**
- Host: `localhost` (usually)
- User: `alexbeattie`
- Database: `shafali`
- Port: `5432` (usually)

### **RDS Database Details**
- Host: `chla-postgres-db.xxx.rds.amazonaws.com`
- User: `chla_admin`
- Database: `postgres`
- Port: `5432`
- Password: Your RDS password

## ⚠️ **Important Notes**

1. **Backup First**: Always backup your RDS data before syncing
2. **Network Access**: Ensure your local machine can reach RDS
3. **Security Groups**: RDS must allow connections from your IP
4. **SSL**: RDS requires SSL connections
5. **Permissions**: Your RDS user needs CREATE/DROP/INSERT permissions

## 🚨 **Troubleshooting**

### **Connection Refused**
```bash
# Check if RDS is accessible
telnet chla-postgres-db.xxx.rds.amazonaws.com 5432

# Check security groups in AWS console
# Ensure your IP is allowed
```

### **Permission Denied**
```bash
# Check RDS user permissions
psql -h chla-postgres-db.xxx.rds.amazonaws.com -U chla_admin -d postgres
\du  # List users and roles
```

### **SSL Required**
```bash
# Add SSL requirement to psql commands
psql "postgresql://chla_admin:password@chla-postgres-db.xxx.rds.amazonaws.com:5432/postgres?sslmode=require"
```

## 🎯 **Recommended Approach**

1. **Use the automated script**: `python3 sync_data_to_rds.py`
2. **If that fails**: Use the manual pg_dump/pg_restore approach
3. **Always deploy after sync**: `git push origin main`
4. **Verify the sync**: Check your API endpoints work

## 📞 **Need Help?**

- Check the deployment guide: `DEPLOYMENT_GUIDE.md`
- Run the sync helper: `python3 sync_rds.py`
- Check EB logs: `eb logs chla-api-env --all`
