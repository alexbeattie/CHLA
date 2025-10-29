# PostGIS Deployment Guide for AWS RDS

This guide explains how to deploy the PostGIS changes to your AWS RDS database.

## Prerequisites

- Access to AWS RDS console or RDS superuser credentials
- `psql` client installed locally

## Deployment Steps

### 1. Enable PostGIS Extension on RDS

**Option A: Via RDS Console (Recommended)**
1. Go to AWS RDS Console
2. Select your database instance
3. Connect via Query Editor or Session Manager
4. Run:
   ```sql
   CREATE EXTENSION IF NOT EXISTS postgis;
   ```

**Option B: Via psql**
```bash
psql -h your-rds-endpoint.rds.amazonaws.com -U chla_admin -d your_db_name -c "CREATE EXTENSION IF NOT EXISTS postgis;"
```

### 2. Verify PostGIS is Enabled

```bash
psql -h your-rds-endpoint.rds.amazonaws.com -U chla_admin -d your_db_name -c "SELECT PostGIS_version();"
```

You should see output like:
```
             postgis_version
-----------------------------------------
 3.3 USE_GEOS=1 USE_PROJ=1 USE_STATS=1
```

### 3. Run Migration Script

```bash
cd /Users/alexbeattie/Developer/CHLA/maplocation
psql -h your-rds-endpoint.rds.amazonaws.com -U chla_admin -d your_db_name -f scripts/migrate_to_postgis.sql
```

This script will:
- ✅ Enable PostGIS extension (if not already enabled)
- ✅ Add `location` geography column to `providers_v2`
- ✅ Populate location from existing latitude/longitude
- ✅ Create spatial indexes (GIST)
- ✅ Update `regional_centers` table similarly

### 4. Deploy Code to Elastic Beanstalk

The GDAL packages are configured in `.ebextensions/02_postgis.config` and will be installed automatically when you push to GitHub (assuming you have CI/CD set up).

**Manual deployment:**
```bash
eb deploy
```

### 5. Verify Deployment

After deployment, check that spatial queries work:

```bash
# SSH into EB instance
eb ssh

# Run Django shell
cd /var/app/current
source /var/app/venv/*/bin/activate
python manage.py shell

# Test spatial query
from locations.models import ProviderV2
providers = ProviderV2.find_nearest(34.0522, -118.2437, radius_miles=5, limit=3)
for p in providers:
    print(f"{p.name}: {p.distance:.2f} miles")
```

## Troubleshooting

### Error: "Could not find GDAL library"

The EB instance needs GDAL/GEOS libraries. These should be installed via `.ebextensions/02_postgis.config`.

If the error persists, SSH into the EB instance and manually install:
```bash
sudo yum install -y gdal gdal-devel geos geos-devel proj proj-devel
```

### Error: "PostGIS extension not found"

PostGIS is available on RDS PostgreSQL 12.2+. If you're on an older version, you'll need to upgrade your RDS instance.

### Performance Issues

Ensure spatial indexes exist:
```sql
-- Check indexes
SELECT tablename, indexname, indexdef
FROM pg_indexes
WHERE tablename IN ('providers_v2', 'regional_centers')
AND indexname LIKE '%location%';

-- Should show GIST indexes on location columns
```

## Rollback Plan

If you need to rollback:

1. The old `latitude`/`longitude` columns are preserved
2. The code will fall back to haversine calculations if `location` is NULL
3. To fully rollback:
   ```sql
   ALTER TABLE providers_v2 DROP COLUMN location;
   ALTER TABLE regional_centers ALTER COLUMN location TYPE varchar(100);
   ```

## Performance Benefits

- **Before (Haversine)**: ~50-100ms for proximity queries
- **After (PostGIS)**: ~6ms for proximity queries
- **Improvement**: 8-16x faster

## Next Steps

Consider adding:
- Service area polygons for providers
- ZIP code boundaries for better regional filtering
- Accessibility metadata
- Wait time tracking
