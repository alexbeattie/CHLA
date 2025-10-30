# Database Sync Guide: AWS RDS ↔ Local

## Overview

**Source of Truth:** AWS RDS
**Local Database:** `shafali`
**Current Status:** ✅ Synced (210 providers, 200 with coordinates)

---

## Quick Sync Commands

### Sync FROM RDS TO Local (Recommended)

```bash
# Interactive mode (with prompts)
python3 sync_rds_to_local_complete.py

# Automatic mode (no prompts)
python3 sync_rds_to_local_complete.py --auto
```

This script:
1. ✅ Exports all data from AWS RDS
2. ✅ Imports to local database
3. ✅ Deletes local duplicates not in RDS
4. ✅ Syncs both providers AND regional centers
5. ✅ Verifies the sync

---

## Database Credentials

### AWS RDS (Production)
- **Host:** `chla-postgres-db.cpkvcu4f59w6.us-west-2.rds.amazonaws.com`
- **Database:** `postgres`
- **User:** `chla_admin`
- **Password:** `CHLASecure2024`
- **SSL:** Required

### Local (Development)
- **Host:** `localhost`
- **Database:** `shafali`
- **User:** `alexbeattie`
- **Password:** (empty)
- **SSL:** Not required

---

## Manual Sync Steps

If you need to sync manually:

### Step 1: Export from RDS

```bash
PGPASSWORD="CHLASecure2024" pg_dump \
    -h chla-postgres-db.cpkvcu4f59w6.us-west-2.rds.amazonaws.com \
    -U chla_admin \
    -d postgres \
    --data-only \
    --no-owner \
    --no-privileges \
    -f rds_dump.sql
```

### Step 2: Restore to Local

```bash
psql -h localhost -U alexbeattie -d shafali -f rds_dump.sql
```

---

## Verification Commands

### Check Provider Count

```bash
python3 -c "
import os
os.environ['DJANGO_SETTINGS_MODULE'] = 'maplocation.settings'
import django
django.setup()
from locations.models import ProviderV2
print(f'Total providers: {ProviderV2.objects.count()}')
"
```

### Check Geocoded Providers

```bash
python3 verify_geocoded.py
```

### Compare RDS vs Local

```python
# Connect to RDS
python3 check_rds_providers.py
```

---

## Current Database Schema

### ProviderV2 Table
- **210 providers** total
- **200 geocoded** (95.2%)
- **10 missing coordinates** (4.8%)

### Key Fields Actually Used in UI:
1. `id` - UUID primary key
2. `name` - Provider name
3. `type` - Provider type
4. `phone`, `email`, `website` - Contact info
5. `address` - Full address text
6. `latitude`, `longitude` - Coordinates (will move to PostGIS-only)
7. `description` - About text
8. `insurance_accepted` - Insurance types (TEXT, comma-separated)
9. `therapy_types` - Services offered (JSONB array)
10. `age_groups` - Age ranges served (JSONB array)
11. `diagnoses_treated` - Conditions treated (JSONB array)

### Fields NOT Used (Candidates for Removal):
- `verified`, `languages_spoken`, `hours`
- `funding_sources` (redundant with insurance)
- Boolean flags: `accepts_*`, `*_services` (not displayed)
- License fields, fax, emergency_phone
- `service_radius_miles`, `specific_areas_served`
- Redundant `latitude`/`longitude` (after PostGIS migration)

---

## Future Normalization Plan

### Phase 1: Insurance Normalization
- ✅ Create `InsuranceCarrier` reference table
- ✅ Populate `ProviderInsurance` relationships
- ✅ Migrate from TEXT to proper many-to-many

### Phase 2: PostGIS-Only Coordinates
- ✅ Use `location` (PostGIS Point) as single source
- ✅ Remove redundant `latitude`/`longitude` columns
- ✅ Add computed properties for backward compatibility

### Phase 3: Drop Unused Fields
- ✅ Remove 21 unused fields
- ✅ Update serializers
- ✅ Clean up database schema

**Estimated Effort:** 2-4 days
**Risk Level:** Low-Medium (can rollback at each phase)

---

## Common Issues

### Issue: Local has more providers than RDS
**Solution:** Run `sync_rds_to_local_complete.py` - it will delete local duplicates

### Issue: Missing coordinates
**Solution:** These are providers without geocoded addresses - run geocoding script

### Issue: SSL connection errors
**Solution:** Ensure `DB_SSL_REQUIRE=true` when connecting to RDS

---

## Automation

### GitHub Actions Workflow
You can automate daily syncs with:

```yaml
name: Sync Local with RDS
on:
  schedule:
    - cron: '0 0 * * *'  # Daily at midnight
  workflow_dispatch:

jobs:
  sync:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Sync databases
        run: python3 sync_rds_to_local_complete.py --auto
```

---

## Rollback Procedure

If sync causes issues:

1. Restore from backup:
   ```bash
   psql -h localhost -U alexbeattie -d shafali -f backup_YYYYMMDD.sql
   ```

2. Or re-run migrations:
   ```bash
   python3 manage.py migrate locations zero
   python3 manage.py migrate locations
   ```

---

## Contact

For questions about the sync process or database schema, refer to:
- `locations/models.py` - Database models
- `locations/serializers.py` - API serializers
- `locations/views.py` - API endpoints

**Last Synced:** 2025-10-30
**Providers:** 210
**Regional Centers:** 7 LA County
