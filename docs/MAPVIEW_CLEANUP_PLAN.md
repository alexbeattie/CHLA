# MapView.vue Cleanup Plan

**Current State**: 6,948 lines
**Goal**: Remove dead code, keep ONLY what's needed
**MUST PRESERVE**: Onboarding flow, ZIP → Regional Center → Providers core flow

---

## Current Architecture (Week 5 Refactoring)

### ✅ What's ACTIVE and BEING USED

**New Components (Week 4-5)**:
- MapCanvas.vue - Handles map rendering, markers
- SearchBar.vue - Handles ZIP/address search
- FilterPanel.vue - Handles filter UI
- ProviderList.vue - Displays provider list
- ProviderCard.vue - Individual provider cards
- ProviderDetails.vue - Provider detail overlay

**New Stores (Week 3)**:
- providerStore.ts - Provider data & search
- mapStore.ts - Map state & viewport
- filterStore.ts - Filter state

**Orchestration Methods in MapView** (KEEP):
- handleNewSearch()
- handleSearchClear()
- handleFilterChange()
- handleFilterReset()
- handleProviderSelect()
- handleMapReady()
- handleMarkerClick()
- handleViewportChange()
- handleDetailsClose()
- handleGetDirections()
- fitMapToProviders()
- getUserZipCode()

**Critical MapView Responsibilities** (KEEP):
- Onboarding flow management
- Authentication/user profile
- Regional center data fetching
- Layout/navigation (top nav, sidebar)
- Component coordination

### ❌ What's DEAD CODE (Can be removed)

**Old Map Implementation** (~1,695 lines):
```javascript
// Lines 2393-4088
initMap() {
  // Creates old Mapbox map
  // OLD: MapCanvas.vue now handles this
}
```

**Old Marker Rendering** (~300 lines):
```javascript
// Lines 4090-4400
updateMarkers() {
  // Renders markers on old map
  // OLD: MapCanvas.vue now handles this
}
```

**Old Search Methods** (~150 lines):
```javascript
performSearch() { }
debounceSearch() { }
updateSearchResults() { }
// OLD: SearchBar.vue now handles this
```

**Old Filter Methods** (~200 lines):
```javascript
updateFilteredLocations() { }
applyFilters() { }
// OLD: FilterPanel.vue + filterStore now handle this
```

**Old Provider Data** (~100 lines):
```javascript
data() {
  return {
    providers: [], // OLD: providerStore.providers now
    filteredLocations: [], // OLD: computed from store
    searchText: '', // OLD: SearchBar manages this
  }
}
```

**Old Map State** (~50 lines):
```javascript
data() {
  return {
    map: null, // Partially needed for compatibility
    markers: [], // OLD: MapCanvas manages
    // etc.
  }
}
```

---

## Cleanup Strategy

### Phase 1: Safe Analysis (30 minutes)

1. **Grep for method calls** to verify which old methods are still called
2. **Check onboarding flow** to ensure it doesn't rely on old methods
3. **Create "Safe to Remove" list** of confirmed dead code
4. **Document dependencies** of methods that look old but might be used

### Phase 2: Remove Obviously Dead Methods (2-3 hours)

**High confidence removals**:
- `initMap()` - MapCanvas handles this
- `updateMarkers()` - MapCanvas handles this
- `performSearch()` - SearchBar + providerStore handle this
- `debounceSearch()` - SearchBar handles this
- Old marker creation helpers
- Old filter application methods

**After each removal**:
- Test in browser
- Check onboarding flow
- Check search functionality
- Check filter functionality

### Phase 3: Remove Dead Data Properties (1 hour)

**Remove from data()**:
- `providers` array (use providerStore.providers)
- `filteredLocations` array (computed from store)
- `searchText` (SearchBar manages)
- Old map-specific state

**Keep in data()**:
- `useNewComponents = true`
- `mapInstance` / `map` (for compatibility)
- `showOnboarding`
- `userData`
- `regionalCenters`
- Authentication state
- Layout state (sidebar, mobile menu)

### Phase 4: Fix Regional Center Filter Confusion (1 hour)

**In FilterPanel.vue**:
- Remove "Accepts Regional Center" checkbox
- OR rename to "Limit to my Regional Center area"

**In filterStore.ts**:
- Remove `params.insurance = 'regional center'`
- Regional Center is determined by ZIP, not a filter

**Update API calls**:
- Ensure ZIP-based search uses `by_regional_center` endpoint
- Remove "regional center" as insurance filter param

### Phase 5: Verify Onboarding Flow (1 hour)

**Test all onboarding steps**:
1. Initial welcome screen
2. ZIP code entry
3. Insurance/funding questions (if any)
4. Diagnosis/therapy questions
5. Results display with filtered providers
6. Regional Center polygon display

**Ensure onboarding**:
- Still captures ZIP code
- Still finds Regional Center
- Still loads providers for that RC
- Still displays correctly

---

## Expected Results

### Before Cleanup
```
MapView.vue: 6,948 lines
├── Template: ~500 lines (✅ clean)
├── Script: ~5,600 lines
│   ├── New orchestration: ~200 lines (✅ keep)
│   ├── Onboarding/auth: ~800 lines (✅ keep)
│   ├── Regional centers: ~400 lines (✅ keep)
│   ├── Layout/navigation: ~300 lines (✅ keep)
│   ├── OLD dead code: ~2,500 lines (❌ remove)
│   └── Utilities/helpers: ~1,400 lines (⚠️ review)
└── Styles: ~850 lines (✅ keep)
```

### After Cleanup (Estimated)
```
MapView.vue: ~3,500-4,000 lines
├── Template: ~500 lines
├── Script: ~2,200-2,700 lines
│   ├── New orchestration: ~200 lines
│   ├── Onboarding/auth: ~800 lines
│   ├── Regional centers: ~400 lines
│   ├── Layout/navigation: ~300 lines
│   └── Utilities (needed): ~500-1,000 lines
└── Styles: ~850 lines
```

**Line reduction**: ~3,000 lines removed (~43%)

---

## What MapView Should Do (After Cleanup)

### Primary Responsibilities

1. **Layout & Navigation**
   - Top navbar
   - Sidebar with filters
   - Mobile menu
   - Responsive design

2. **Onboarding Flow**
   - Show/hide onboarding overlay
   - Capture user ZIP code
   - Handle onboarding completion
   - Apply onboarding results to initial search

3. **Regional Center Management**
   - Fetch regional center data
   - Determine user's RC from ZIP
   - Display RC polygon overlay
   - Handle RC-specific provider loading

4. **Component Coordination**
   - Initialize stores (providerStore, mapStore, filterStore)
   - Handle events from child components
   - Coordinate state between components
   - Pass data down as props

5. **Authentication & User Profile**
   - User login/logout
   - User profile management
   - Save user preferences

### What MapView Should NOT Do (Delegated to Components)

❌ Map rendering → MapCanvas.vue
❌ Search logic → SearchBar.vue + providerStore
❌ Filter UI → FilterPanel.vue + filterStore
❌ Provider display → ProviderList.vue + ProviderCard.vue
❌ Provider details → ProviderDetails.vue
❌ Marker management → MapCanvas.vue
❌ Direct map manipulation → MapCanvas.vue

---

## Core Application Flow (MUST PRESERVE)

### 1. Initial Load (No Onboarding)

```javascript
mounted() {
  // 1. Initialize stores
  this.providerStore = useProviderStore();
  this.mapStore = useMapStore();
  this.filterStore = useFilterStore();

  // 2. Load regional centers
  await this.fetchRegionalCenters();

  // 3. Get user ZIP from geolocation
  const zipCode = await this.getUserZipCode();

  // 4. Load providers for that ZIP's Regional Center
  if (zipCode) {
    await this.providerStore.searchByZipCode(zipCode);
  }

  // 5. Map displays automatically via MapCanvas component
  // 6. Regional Center polygon displays via handleMapReady()
}
```

### 2. With Onboarding Flow

```javascript
mounted() {
  if (this.showOnboarding) {
    // Show onboarding overlay
    // User enters ZIP code
    // User answers questions
    // onboardingComplete() called
  }
}

onboardingComplete(data) {
  // 1. Get ZIP from onboarding
  const zipCode = data.zipCode;

  // 2. Load providers for that ZIP's RC
  await this.providerStore.searchByZipCode(zipCode);

  // 3. Apply any additional filters from onboarding
  this.filterStore.applyOnboardingFilters(data);

  // 4. Hide onboarding, show map
  this.showOnboarding = false;
}
```

### 3. Search Flow

```javascript
handleNewSearch(searchData) {
  // SearchBar emits search event
  // MapView orchestrates the search via providerStore

  if (searchData.type === 'zip') {
    // ZIP search → Regional Center endpoint
    await this.providerStore.searchByZipCode(searchData.query);
  } else {
    // Address search → Comprehensive search
    await this.providerStore.searchProviders({
      searchText: searchData.query,
      lat: searchData.location?.lat,
      lng: searchData.location?.lng
    });
  }

  // Map and list update automatically via store reactivity
}
```

---

## Key Architectural Principles

### 1. Single Source of Truth
- **Provider data**: providerStore.providers
- **Map state**: mapStore
- **Filter state**: filterStore
- **User state**: MapView.userData (could be extracted to userStore)

### 2. Unidirectional Data Flow
```
User Action → Component Event → MapView Handler → Store Action → Store State Change → Components React
```

### 3. Component Responsibility
Each component has ONE clear job:
- MapCanvas: Render map and markers
- SearchBar: Handle search input
- FilterPanel: Handle filter UI
- ProviderList/Card: Display providers
- MapView: Orchestrate everything

### 4. Regional Center is Location-Based
- ZIP code determines Regional Center (automatic)
- NOT a filter or choice
- Displayed automatically based on search/user location

---

## Next Steps

1. ✅ Created this cleanup plan
2. ⏳ Run grep analysis to verify dead code
3. ⏳ Remove dead methods incrementally
4. ⏳ Test after each removal
5. ⏳ Fix Regional Center filter confusion
6. ⏳ Verify onboarding flow
7. ⏳ Document final architecture

**Estimated Total Time**: 8-12 hours
**Priority**: Medium (code works, this is housekeeping)
**Risk**: Low (with incremental testing)
