# Provider Model Cleanup - COMPLETED ✅

## Summary
Successfully removed the old `Provider` model and replaced all references with `ProviderV2`. The old `providers` database table will be dropped when you run the migrations.

## Changes Made

### 1. ✅ Removed Provider Model
**File**: `locations/models.py`
- Removed the entire `Provider` class (165 lines)
- Added comment noting it was replaced by ProviderV2
- Updated relationship models to use ProviderV2:
  - `ProviderRegionalCenter`
  - `ProviderFundingSource`
  - `ProviderInsuranceCarrier`
  - `ProviderServiceModel`

### 2. ✅ Removed ProviderViewSet 
**File**: `locations/views.py`
- Removed entire `ProviderViewSet` class (354 lines)
- Removed imports: `Provider`, `ProviderSerializer`, `ProviderWriteSerializer`, `ProviderGeoSerializer`
- All functionality now available in `ProviderV2ViewSet`

### 3. ✅ Removed Legacy API Endpoint
**File**: `locations/urls.py`
- Removed `/api/providers-legacy/` endpoint registration
- Only `ProviderV2ViewSet` endpoints remain active:
  - `/api/providers/` → ProviderV2ViewSet
  - `/api/providers-v2/` → ProviderV2ViewSet (compatibility alias)

### 4. ✅ Archived Old Management Commands
**Location**: `locations/management/commands/archived_old_provider_commands/`

Archived 5 old management commands that used the Provider model:
- `load_provider_data.py`
- `link_providers_centers.py`
- `import_providers_from_txt.py`
- `import_chla_data.py`
- `emergency_populate.py`

### 5. ✅ Updated CSV Utils
**File**: `locations/utils/csv_utils.py`
- Deprecated the old CSV import functionality
- Now raises `NotImplementedError` with helpful message directing users to:
  - Admin interface for imports
  - `import_regional_center_providers` management command

### 6. ✅ Archived Old Scripts
**Location**: `archive/old-provider-scripts/`

Moved old scripts:
- `populate_db.py` - Used old Provider model for database population

### 7. ✅ Created Migration to Drop Table
**File**: `locations/migrations/0021_drop_old_provider_table.py`
- Migration will drop the `providers` table from database
- Uses `DROP TABLE IF EXISTS providers CASCADE`
- Includes safety check with IF EXISTS clause

## Data Migration Status

✅ **Data Already Migrated**
- Migration `0010_copy_providers_to_providerv2.py` copied all data from `providers` → `providers_v2`
- Your data is safe in the `providers_v2` table

## Next Steps

### To Complete the Cleanup:

1. **Run the migration** to drop the old table:
```bash
cd /Users/alexbeattie/Developer/CHLA/maplocation
source venv/bin/activate
python3 manage.py migrate locations
```

2. **Verify no errors**:
```bash
python3 manage.py check
```

3. **Test the API**:
- Old endpoint removed: `https://api.kinddhelp.com/api/providers-legacy/` (will return 404)
- Current endpoints work: `https://api.kinddhelp.com/api/providers/`

### Geocoding Your Data

Since you have providers with `0.00000000` coordinates, run:
```bash
cd /Users/alexbeattie/Developer/CHLA/maplocation
source venv/bin/activate

# Check what needs geocoding
python3 check_geocoding_status.py

# Geocode all providers with missing/zero coordinates
python3 manage.py geocode_providers --all
```

The updated `geocode_providers` command now:
- Uses `ProviderV2` instead of old `Provider`
- Finds records with `0.00000000` coordinates (not just NULL)
- Uses Mapbox API with fallback strategies
- Handles JSON address format

## Files Modified

**Modified**:
- `locations/models.py` - Removed Provider model, updated relationships
- `locations/views.py` - Removed ProviderViewSet, cleaned imports
- `locations/urls.py` - Removed legacy endpoint
- `locations/utils/csv_utils.py` - Deprecated old import logic
- `locations/management/commands/geocode_providers.py` - Updated to use ProviderV2

**Created**:
- `locations/migrations/0021_drop_old_provider_table.py` - Drop old table migration
- `check_geocoding_status.py` - Utility to check geocoding status
- `check_provider_tables.py` - Utility to compare old/new tables
- `PROVIDER_CLEANUP_PLAN.md` - Original cleanup plan
- `PROVIDER_CLEANUP_COMPLETED.md` - This document

**Archived**:
- 5 management commands → `locations/management/commands/archived_old_provider_commands/`
- `populate_db.py` → `archive/old-provider-scripts/`

## Benefits

1. ✅ **Cleaner Codebase** - Removed 600+ lines of deprecated code
2. ✅ **No Confusion** - Only one Provider model (ProviderV2)
3. ✅ **Better Data Structure** - ProviderV2 has proper fields for onboarding flow
4. ✅ **Reduced Maintenance** - No duplicate endpoints or models to maintain
5. ✅ **Database Cleanup** - Old table will be removed, saving space

## Rollback (If Needed)

If you need to rollback (not recommended):
1. The old code is archived in `locations/management/commands/archived_old_provider_commands/` and `archive/old-provider-scripts/`
2. Migration 0021 can be reversed (though it won't recreate data)
3. Git history preserves all changes

## Notes

- The old `providers` database table will be dropped when you run `migrate`
- All relationship models now point to `ProviderV2`
- Frontend should already be using the new endpoints (`/api/providers/`)
- No changes needed to frontend code

---

**Cleanup completed on**: $(date)
**Status**: ✅ Ready for migration

