# CHLA Provider Map - API & Data Quality Issues

**Date**: 2025-10-27
**Status**: Critical data quality issues identified

---

## 🚨 Critical Data Issues

### 1. Regional Center Duplicate Entries

**Problem**: Database has 43 Regional Center entries when there should only be 21 (California total)

**Details**:
- California has exactly **21 Regional Centers**
- **7** are in LA County
- **14** are in other California counties
- Database has **22 duplicate entries**

**Duplicates identified**:
- Regional Center of Orange County: 3 entries (should be 1)
- Eastern Los Angeles Regional Center: 3 entries (should be 1)
- San Andreas Regional Center: 3 entries (should be 1)
- Valley Mountain Regional Center: 3 entries (should be 1)
- Inland Regional Center: 3 entries (should be 1)
- Central Valley Regional Center: 2 entries (should be 1)
- Golden Gate Regional Center: 2 entries (should be 1)
- North Los Angeles County Regional Center: 2 entries (should be 1)
- Regional Center of the East Bay: 2 entries (should be 1)
- Far Northern Regional Center: 2 entries (should be 1)
- Tri-Counties Regional Center: 2 entries (should be 1)
- San Diego Regional Center: 2 entries (should be 1)
- North Bay Regional Center: 2 entries (should be 1)
- Redwood Coast Regional Center: 2 entries (should be 1)
- Kern Regional Center: 2 entries (should be 1)
- Alta California Regional Center: 2 entries (should be 1)
- Harbor Regional Center: 2 entries (should be 1)

**Impact**:
- API returns duplicate data
- Confuses polygon rendering
- Makes ZIP code lookups unreliable

**Recommendation**:
- Create data cleanup script to deduplicate Regional Centers
- Keep the entry with ZIP codes and service area data
- Delete duplicates

---

### 2. `is_la_regional_center` Flag Not Set

**Problem**: ALL Regional Centers have `is_la_regional_center=False`

**Expected**: The following 7 LA County Regional Centers should have `is_la_regional_center=True`:
1. ✅ North Los Angeles County Regional Center
2. ✅ Eastern Los Angeles Regional Center
3. ✅ Frank D. Lanterman Regional Center
4. ✅ Harbor Regional Center
5. ✅ San Gabriel/Pomona Regional Center
6. ✅ South Central Los Angeles Regional Center
7. ✅ Westside Regional Center

**Impact**:
- `service_area_boundaries` endpoint returns empty data
- ZIP code analysis fails
- Map polygon filtering doesn't work

**Recommendation**:
- Create management command to set `is_la_regional_center=True` for these 7 RCs
- Run after deduplication

---

### 3. Missing ZIP Code Coverage

**Problem**: Entire Sherman Oaks/Van Nuys area (914xx ZIP codes) missing from database

**Missing ZIPs identified**:
- 91403 (Sherman Oaks)
- 91401 (Van Nuys)
- 91405 (Van Nuys)
- 91406 (Van Nuys)
- 91411 (Van Nuys)
- 91423 (Sherman Oaks)
- 91436 (Encino)

**Current Coverage**:
- Total unique ZIPs in database: 678
- Expected ZIPs should belong to: **North Los Angeles County Regional Center**

**Impact**:
- Users in these areas see "Regional Center (Not Found)"
- Cannot search for providers by ZIP code
- Onboarding fails for these ZIP codes

**Likely Scope**:
- Since an entire neighborhood cluster is missing, there are probably many more gaps throughout LA County
- Need comprehensive ZIP code audit against official Regional Center service areas

**Recommendation**:
- Obtain official Regional Center service area data from DDS (Department of Developmental Services)
- Import complete ZIP code coverage for all 7 LA County RCs
- Verify against official boundaries

---

## 📊 Provider Endpoint Versions

### Current State (3 endpoints, 2 models)

| Endpoint | ViewSet | Model | Count | Status | Recommendation |
|----------|---------|-------|-------|--------|----------------|
| `/api/providers/` | `ProviderV2ViewSet` | `ProviderV2` | 299 | ✅ **PRIMARY** | Keep - This is the main endpoint |
| `/api/providers-v2/` | `ProviderV2ViewSet` | `ProviderV2` | 299 | ⚠️ **ALIAS** | Keep for backward compatibility |
| `/api/providers-legacy/` | `ProviderViewSet` | `Provider` | 1 | ⚠️ **DEPRECATED** | Can be removed after migration |

### Explanation

Both `/api/providers/` and `/api/providers-v2/` point to the **same** `ProviderV2ViewSet` and return identical data. They are configured as aliases in `locations/urls.py`:

```python
router.register(r"providers", views.ProviderV2ViewSet)
router.register(r"providers-v2", views.ProviderV2ViewSet, basename="providers-v2")
```

The legacy endpoint uses the old `Provider` model and only has 1 record remaining.

### Recommendation

**Keep current structure** - it's working correctly:
- `/api/providers/` - Main endpoint (used by frontend)
- `/api/providers-v2/` - Compatibility alias (in case external clients use it)
- `/api/providers-legacy/` - Can be deprecated once old Provider model is fully migrated

---

## 🎯 New API Documentation Endpoint

**URL**: `/api/docs/`

**Purpose**: Exposes ALL API endpoints including @action routes that aren't visible in the DRF root

**Features**:
- Lists all core ViewSet endpoints
- Documents Regional Center @action endpoints
- Documents Provider @action endpoints
- Includes parameter descriptions and examples
- Shows known data issues

**Example endpoints now documented**:
- `service_area_boundaries/` - Returns GeoJSON with ZIP codes
- `lookup_by_zip/` - Find RC by ZIP code
- `by_regional_center/` - Filter providers by RC
- `comprehensive_search/` - Full provider search
- `zip_code_analysis/` - NEW - Analyzes ZIP code coverage and gaps

---

## 📝 Action Items

### Immediate (Data Quality)
1. ✅ **Create `/api/docs/` endpoint** - DONE
2. ✅ **Create `zip_code_analysis` endpoint** - DONE
3. ⏳ **Deduplicate Regional Centers** - Remove 22 duplicate entries
4. ⏳ **Set `is_la_regional_center=True`** - For the 7 LA County RCs
5. ⏳ **Import complete ZIP code coverage** - For all LA County RCs

### Short Term (Frontend)
1. Update frontend to use `/api/docs/` for API discovery
2. Add data quality warnings in UI when ZIP code not found
3. Implement fallback to geocoding when RC lookup fails

### Long Term (Architecture)
1. Implement data validation checks in Django admin
2. Create automated tests for data integrity
3. Add monitoring for duplicate entries
4. Set up periodic data audits

---

## 🔗 Related Documentation

- `STACK_DOCUMENTATION.md` - Full stack architecture
- `locations/views.py:192` - RegionalCenterViewSet with comprehensive docstrings
- `locations/urls.py:23` - API routing configuration
