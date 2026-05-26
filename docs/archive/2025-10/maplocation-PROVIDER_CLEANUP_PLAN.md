# Provider Model Cleanup Plan

## Current Status
- **Old Model**: `Provider` (table: `providers`)
- **New Model**: `ProviderV2` (table: `providers_v2`)
- **Migration**: Data was copied via migration `0010_copy_providers_to_providerv2.py`

## What's Still Using the Old Provider Model

### 1. API Endpoints
- `/api/providers-legacy/` - Can be removed or kept for backward compatibility

### 2. Management Commands (Unused)
- `locations/management/commands/load_provider_data.py`
- `locations/management/commands/link_providers_centers.py`
- `locations/management/commands/import_providers_from_txt.py`
- `locations/management/commands/import_chla_data.py`
- `locations/management/commands/emergency_populate.py`

### 3. Old Scripts (Can be archived)
- `populate_db.py`
- `create_client_users.py` (only references it, doesn't use it)

### 4. Admin Interface
- Registered in `admin_original.py` (not current admin.py)

### 5. Utilities
- `locations/utils/csv_utils.py` - Has old Provider import logic

## Cleanup Steps

### Step 1: Verify Data Migration
```bash
cd maplocation && source venv/bin/activate
python3 manage.py shell -c "
from locations.models import Provider, ProviderV2
print(f'Old Provider count: {Provider.objects.count()}')
print(f'New ProviderV2 count: {ProviderV2.objects.count()}')
"
```

### Step 2: Remove API Endpoint
- Remove `ProviderViewSet` from `locations/views.py`
- Remove registration from `locations/urls.py`

### Step 3: Remove Model Definition
- Remove `Provider` class from `locations/models.py`

### Step 4: Archive Old Scripts
Move to archive/:
- `locations/management/commands/load_provider_data.py`
- `locations/management/commands/link_providers_centers.py`
- `locations/management/commands/import_providers_from_txt.py`
- `locations/management/commands/import_chla_data.py`
- `locations/management/commands/emergency_populate.py`
- `populate_db.py`

### Step 5: Create Migration to Drop Old Table
```bash
python3 manage.py makemigrations --empty locations --name drop_old_provider_table
```

Then edit the migration to drop the `providers` table.

### Step 6: Update CSV Utils
Update `locations/utils/csv_utils.py` to only use ProviderV2

## Recommendation

**Option A: Safe Removal (Recommended)**
1. Keep the `/api/providers-legacy/` endpoint for 30 days
2. Add deprecation warning
3. Monitor usage logs
4. Remove after no usage detected

**Option B: Immediate Removal**
1. Remove all references immediately
2. Drop the database table
3. Clean up code

## Should We Proceed?

I can execute the cleanup now if you'd like. It will:
1. ✅ Remove old Provider model and references
2. ✅ Archive old scripts
3. ✅ Create migration to drop old table
4. ⚠️  Keep `/api/providers-legacy/` endpoint with deprecation notice (or remove it)

What would you prefer?

