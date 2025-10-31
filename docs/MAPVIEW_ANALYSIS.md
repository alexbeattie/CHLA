# MapView.vue Analysis Report - Phase 1.1

**Date:** 2025-10-31  
**File:** `src/views/MapView.vue`  
**Total Lines:** 7,444

---

## 📊 File Breakdown

| Section | Lines | Percentage |
|---------|-------|------------|
| Template | 498 | 6.7% |
| Script | 5,538 | 74.4% |
| Styles | 1,406 | 18.9% |

**The script section is 74% of the file** - this is where we'll focus.

---

## 🔢 Component Inventory

### Imports
- **20 component imports**
- Already using: MapCanvas, SearchBar, ProviderList, FilterPanel, etc.
- Many components already extracted ✅

### Data Properties: ~40 properties
Key groups:
- Store instances (providerStore, mapStore, filterStore)
- UI state (showOnboarding, showMobileSidebar, showDirections)
- Map state (displayType, selectedRegionalCenter, showServiceAreas)
- User data (userData, userRegionalCenter)
- Regional center data (laRegionalCentersData, zipToCenter)

### Computed Properties: 12
- `isAuthenticated` - Check auth status
- `providers` - Get providers from store
- `filteredProviders` - Apply filters
- `filteredLocations` - Apply location filters
- `laRegionalCentersList` - LA regional centers
- `nearestRegionalCenters` - Calculate nearest centers
- Others for display logic

### Methods: 72 total

---

## 🎯 Method Categories

### 1. Format/Utility Functions (SAFE TO EXTRACT - 7 methods)
These are pure functions with no side effects:

**Priority 1 - Easiest to extract:**
- `formatHours(hours)` - Line 4804
- `formatHoursObject(hoursObj)` - Line 4833
- `formatDescription(description)` - Line 5956
- `formatInsurance(insurance)` - Line 5977
- `formatLanguages(languages)` - Line 6007
- `getApiRoot()` - Line 2401
- `getLACountyBounds()` - Line 2754

**Estimated savings:** ~150 lines  
**Risk level:** ⭐ LOW (pure functions)  
**Testing:** Easy - just compare input/output

### 2. Calculation Functions (MEDIUM SAFE - 4 methods)
Functions that calculate things but might use `this`:

- `calculateProviderBounds()` - Line 5905
- `countLocationsInRadius()` - Computed, but could be utility
- `findRegionalCenterByCoordinates()` - Line 2466
- `findRegionalCenterByZip()` - Line 2768

**Estimated savings:** ~200 lines  
**Risk level:** ⭐⭐ MEDIUM (may use this.$data)  
**Testing:** Need to verify with real data

### 3. Map Functions (KEEP FOR NOW - ~15 methods)
These directly manipulate the map and need `this.$refs`:

- `initMap()`
- `addServiceAreasToMap()`
- `removeServiceAreasFromMap()`
- `addLARegionalCentersToMap()`
- `updateMarkers()`
- `fitMapToProviders()`
- `removeRouteFromMap()`
- etc.

**Decision:** Leave these in MapView for now - they're tightly coupled to the map instance

### 4. API/Data Fetching (COULD MOVE TO STORE - ~8 methods)
These fetch data:

- `fetchProviders()`
- `fetchServiceAreas()`
- `loadAllProviders()`
- `loadInitialProviders()`
- `getUserZipCode()`
- `detectUserLocation()`
- etc.

**Decision:** Could move to Pinia stores, but not Phase 1

### 5. UI Event Handlers (KEEP - ~20 methods)
These respond to user actions:

- `toggleMobileSidebar()`
- `toggleSearch()`
- `toggleUserMenu()`
- `handleSearchClear()`
- `handleFilterReset()`
- `handleDetailsClose()`
- etc.

**Decision:** Keep in MapView - they're the orchestration layer

### 6. Lifecycle/Setup (KEEP - ~10 methods)
Initialization and setup:

- `created()`
- `mounted()`
- `initializeAfterOnboarding()`
- `checkOnboardingStatus()`
- etc.

**Decision:** Keep in MapView - component lifecycle

---

## 🥇 Phase 2 Extraction Plan

### Target: Format/Utility Functions

**Step 1: Create `src/utils/formatting.js`**

Extract these in order (easiest first):

1. ✅ `getApiRoot()` - ~10 lines
   - No dependencies
   - Returns API URL based on environment
   - **Risk:** NONE
   - **Test:** Call it, check URL matches
   
2. ✅ `getLACountyBounds()` - ~15 lines
   - Returns hardcoded bounds object
   - No dependencies
   - **Risk:** NONE
   - **Test:** Call it, check bounds correct

3. ✅ `formatDescription(description)` - ~20 lines
   - String manipulation only
   - No dependencies
   - **Risk:** NONE
   - **Test:** Compare before/after output

4. ✅ `formatInsurance(insurance)` - ~30 lines
   - String/array manipulation
   - No dependencies
   - **Risk:** NONE
   - **Test:** Test with different input types

5. ✅ `formatLanguages(languages)` - ~30 lines
   - String/array manipulation
   - No dependencies
   - **Risk:** NONE
   - **Test:** Test with different input types

6. ✅ `formatHours(hours)` - ~30 lines
   - Complex but pure function
   - No dependencies
   - **Risk:** LOW
   - **Test:** Test with various hour formats

7. ✅ `formatHoursObject(hoursObj)` - ~30 lines
   - Uses formatHours internally
   - Extract after #6
   - **Risk:** LOW
   - **Test:** Test with real hours objects

**Total Phase 2 savings:** ~165 lines  
**Timeline:** 3-4 hours (30 min per function)

---

## 📋 Extraction Checklist (For Each Function)

### Before:
- [ ] Function works in MapView
- [ ] All tests passing
- [ ] Git status clean

### During:
1. [ ] Copy function to `utils/formatting.js`
2. [ ] Export function
3. [ ] Import in MapView: `import { functionName } from '@/utils/formatting'`
4. [ ] Replace all `this.functionName` with `functionName`
5. [ ] Remove function from MapView methods

### After:
- [ ] Run `./scripts/test-mapview.sh`
- [ ] Manual test in browser
- [ ] Check no linter errors
- [ ] Commit: `git commit -m "refactor: Extract functionName to utils"`
- [ ] Verify line count reduced

---

## 🚨 Red Flags to Watch For

### Don't Extract If Method:
❌ Uses `this.$refs` (DOM access)  
❌ Uses `this.$router` or `this.$route`  
❌ Uses `this.providerStore` (keep in MapView, or move to composable)  
❌ Uses `this.mapStore`  
❌ Has side effects (updates state)  
❌ Is async and fetches data  

### Safe to Extract If Method:
✅ Takes input, returns output  
✅ No `this` references  
✅ Pure string/number/array manipulation  
✅ Has clear documentation what it does  
✅ Used in multiple places (bonus!)  

---

## 📈 Expected Results

### After Phase 2 (Format Functions):
- **Before:** 7,444 lines
- **After:** ~7,280 lines  
- **Saved:** ~165 lines (2.2%)
- **Benefit:** 7 testable utility functions
- **Time:** 1 day

### After Phase 3 (Calculation Functions):
- **Before:** 7,280 lines
- **After:** ~7,080 lines
- **Saved:** ~200 lines (2.7%)
- **Benefit:** More testable utilities
- **Time:** 1-2 days

### After Phase 4 (Composables):
- **Before:** 7,080 lines
- **After:** ~6,500 lines
- **Saved:** ~580 lines (7.8%)
- **Benefit:** Reusable logic
- **Time:** 3-5 days

### Final Target: 
- **~4,500 lines** (40% reduction)
- **15+ extracted utilities**
- **3-5 composables**
- **Maintainable codebase**

---

## 🎯 Next Action

**Ready to start Phase 2, Step 1:**

Extract `getApiRoot()` function

**Command:**
```bash
# 1. Review the function
grep -A 15 "getApiRoot()" src/views/MapView.vue

# 2. Create utils file
mkdir -p src/utils
touch src/utils/formatting.js

# 3. Follow extraction checklist
```

**Estimated time:** 15-20 minutes  
**Risk:** None  
**Benefit:** First utility extracted!

---

## 📝 Notes

- All 72 methods documented
- 7 functions ready for immediate extraction
- Clear path forward
- Test script ready
- Can stop after any extraction

**Status:** ✅ Phase 1.1 Complete - Ready for Phase 2

