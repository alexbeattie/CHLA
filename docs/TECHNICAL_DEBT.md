# Technical Debt & Future Improvements

This document tracks known limitations, technical debt, and improvement opportunities in the CHLA Provider Map project.

---

## High Priority Issues

### 1. Strict Coordinate Validation (Frontend)

**Location:** `/Users/alexbeattie/Developer/CHLA/map-frontend/src/views/MapView.vue` (lines 932-1115)

**Problem:**
Providers with coordinates outside California bounds (32-42°N, -125--114°W) are silently skipped from display. This validation is too strict and may exclude valid providers in border areas or with slightly incorrect geocoding.

**Current Behavior:**
```javascript
// Providers outside these bounds are marked with _coordinatesInvalid = true
// and never appear on the map
if (lat < 32 || lat > 42 || lng < -125 || lng > -114) {
  provider._coordinatesInvalid = true;
}
```

**Impact:**
- Valid providers near state borders may not display
- No user feedback when providers are filtered out
- Silent failures make debugging difficult

**Proposed Solution:**
1. Warn users when providers are filtered due to invalid coordinates
2. Loosen bounds or make them configurable
3. Add fallback geocoding for providers with suspicious coordinates
4. Display "X providers filtered due to invalid coordinates" message

**Priority:** High - directly affects user experience

---

### 2. Sequential Filter Application (Backend)

**Location:** `/Users/alexbeattie/Developer/CHLA/maplocation/locations/views.py` (lines 1055-1253)

**Problem:**
Filters are applied sequentially, meaning earlier filters reduce the result set for later filters. This can lead to unexpected behavior and performance issues.

**Current Behavior:**
```python
# Filters run in this exact order:
1. Text search
2. Insurance/funding (text-based)
3. Specialization
4. Age groups
5. Location/radius

# If an earlier filter excludes a provider, later filters never see it
```

**Impact:**
- Filters are interdependent in unexpected ways
- Hard to reason about query results
- Potential performance issues with large datasets
- Can't parallelize filter operations

**Proposed Solution:**
1. Refactor to use Django Q objects for parallel filter application
2. Use database-level filtering where possible (indexes, JSONField queries)
3. Add query optimization with `select_related` and `prefetch_related`
4. Consider caching frequently-used queries

**Example Refactor:**
```python
# Instead of sequential filtering:
from django.db.models import Q

filters = Q()
if text_search:
    filters &= Q(name__icontains=text_search) | Q(address__icontains=text_search)
if insurance:
    filters &= Q(insurance_accepted__icontains=insurance)
if specialization:
    filters &= Q(type__iexact=specialization)

queryset = ProviderV2.objects.filter(filters)
```

**Priority:** High - affects performance and correctness

---

### 3. MapView.vue Monolith (Frontend)

**Location:** `/Users/alexbeattie/Developer/CHLA/map-frontend/src/views/MapView.vue` (6000+ lines)

**Problem:**
The main map component is a single 6000+ line file containing all map logic, search logic, filter logic, and UI rendering. This is unmaintainable and makes debugging extremely difficult.

**Impact:**
- Hard to understand code flow
- Difficult to add new features
- Testing is nearly impossible
- Performance issues due to reactive state management
- High risk of bugs when making changes

**Proposed Solution:**
Break into logical components:

```
src/
├── components/
│   ├── map/
│   │   ├── MapContainer.vue          # Main map display
│   │   ├── MapMarkers.vue            # Marker management
│   │   ├── RegionalCenterBoundary.vue # RC polygon overlay
│   │   └── MapControls.vue           # Zoom, pan controls
│   ├── search/
│   │   ├── SearchBar.vue             # ZIP/address search
│   │   ├── FilterPanel.vue           # All filters
│   │   └── SearchResults.vue         # Results list
│   └── providers/
│       ├── ProviderCard.vue          # Provider detail card
│       ├── ProviderList.vue          # List view
│       └── ProviderPopup.vue         # Map popup
├── composables/
│   ├── useMapState.ts                # Map state management
│   ├── useProviderSearch.ts          # API calls & search logic
│   ├── useFilters.ts                 # Filter state & logic
│   └── useGeocoding.ts               # Address/ZIP geocoding
└── stores/
    ├── mapStore.ts                   # Pinia store for map state
    ├── providerStore.ts              # Provider data
    └── filterStore.ts                # Active filters
```

**Benefits:**
- Easier to test individual components
- Better code organization and readability
- Improved performance (smaller reactive state)
- Easier onboarding for new developers
- Can reuse components in other views

**Priority:** Medium-High - critical for long-term maintainability

---

## Medium Priority Issues

### 4. Text-Based Insurance Filtering

**Location:** Multiple
- Backend: `/Users/alexbeattie/Developer/CHLA/maplocation/locations/views.py` (lines 1132-1135)
- Model: `/Users/alexbeattie/Developer/CHLA/maplocation/locations/models.py` (lines 542-552)

**Problem:**
Insurance acceptance is determined by text search in `insurance_accepted` field rather than using boolean fields or proper relationships.

**Current Behavior:**
```python
# Backend checks for text
if 'regional center' in (provider.insurance_accepted or '').lower():
    # Provider accepts regional center

# Model has unused boolean properties
@property
def accepts_regional_center(self):
    return "regional center" in (self.insurance_accepted or "").lower()
```

**Impact:**
- Fragile (typos, variations break filtering)
- Can't use database indexes efficiently
- Different providers might use different text ("RC", "Regional Center", "regional centre")
- Properties are computed every time, not cached

**Proposed Solution:**
1. **Option A:** Use existing boolean fields properly
   - Add boolean fields to database
   - Update on save() to parse insurance_accepted text
   - Filter using boolean fields in queries

2. **Option B:** Normalize to M2M relationship
   ```python
   class InsuranceType(models.Model):
       name = models.CharField(max_length=100, unique=True)

   class ProviderV2(models.Model):
       insurance_types = models.ManyToManyField(InsuranceType)
   ```

**Priority:** Medium - works but not optimal

---

### 5. Regional Center ZIP Code Coverage

**Location:**
- `/Users/alexbeattie/Developer/CHLA/maplocation/locations/management/commands/populate_san_gabriel_zips.py`
- `/Users/alexbeattie/Developer/CHLA/maplocation/locations/management/commands/populate_pasadena_zips.py`

**Problem:**
Only San Gabriel/Pomona and Eastern LA (Pasadena) regional centers have ZIP codes populated. Other regional centers in California are missing ZIP mappings.

**Impact:**
- Users outside these areas won't see regional center boundaries
- ZIP lookup returns null for most California ZIPs
- Inconsistent user experience across regions

**Proposed Solution:**
1. Create ZIP population commands for all CA regional centers:
   - Harbor Regional Center (already has command)
   - Westside Regional Center
   - North Los Angeles County RC
   - South Central Los Angeles RC
   - Frank D. Lanterman RC
   - Inland Regional Center
   - And 14+ others

2. Source ZIP data from official regional center service area documents

3. Add validation to prevent ZIP overlap between RCs

**Priority:** Medium - only affects users outside implemented areas

---

### 6. Geocoding Errors and Retries

**Location:** `/Users/alexbeattie/Developer/CHLA/maplocation/locations/admin.py` (lines 188-225)

**Problem:**
Geocoding uses Mapbox API with basic error handling but no retry logic or fallback providers.

**Current Behavior:**
```python
def geocode_with_fallback(address):
    # Try Mapbox
    # If fails, no fallback
    return None
```

**Impact:**
- Transient API failures result in providers without coordinates
- No retry for rate-limited requests
- Single point of failure

**Proposed Solution:**
1. Add retry logic with exponential backoff
2. Implement fallback to alternative geocoding services:
   - Nominatim (OpenStreetMap)
   - Google Geocoding API
   - US Census Geocoder (free for US addresses)
3. Cache geocoding results to avoid repeated API calls
4. Add bulk geocoding command for efficiency

**Priority:** Low-Medium - current implementation works most of the time

---

## Low Priority / Nice-to-Have

### 7. Missing Tests

**Problem:** No automated tests for import commands, API endpoints, or frontend components.

**Solution:** Add comprehensive test suite using pytest (backend) and Vitest (frontend)

---

### 8. Hard-Coded Configuration

**Problem:** Many constants are hard-coded (radius defaults, coordinate bounds, API URLs)

**Solution:** Move to environment variables and configuration files

---

### 9. Documentation

**Problem:** Code lacks inline documentation and type hints

**Solution:** Add docstrings, type hints (Python), and JSDoc comments (TypeScript)

---

## Completed Improvements

### ✅ Provider Import Automation
- **Completed:** 2025-10-26
- **Commits:** ce2c4b9, 92a4842, 853da40
- **Impact:** Providers now import automatically on deployment

### ✅ Regional Center Text Field Fix
- **Completed:** 2025-10-26
- **Commit:** 92a4842
- **Impact:** Regional center filtering now works correctly

### ✅ ZIP Code Mappings
- **Completed:** 2025-10-26
- **Commit:** 853da40
- **Impact:** San Gabriel and Pasadena areas now have working ZIP lookups

---

## How to Contribute

When addressing technical debt:

1. **Create an issue** referencing this document
2. **Discuss approach** before starting major refactors
3. **Write tests** for new functionality
4. **Update this document** when items are completed
5. **Document breaking changes** in commit messages

---

## Priority Legend

- **High:** Affects user experience or system reliability
- **Medium:** Affects developer experience or performance
- **Low:** Nice-to-have improvements
