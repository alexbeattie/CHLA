# MapView.vue Refactoring Progress

## Executive Summary
**Goal**: Reduce MapView.vue from 6,690 lines to ~2,900 lines (56% reduction)
**Current**: 6,432 lines (258 lines removed so far - 3.9% reduction)
**Strategy**: Gradual modernization - Extract to composables/stores while keeping Options API

---

## ✅ Completed Tasks

### 1. Fixed Critical Bugs
- ✅ Fixed `LA_COUNTY_CENTER` undefined warning (added constants to component data)
- ✅ Fixed marker pinning issues:
  - Added z-index to keep markers above map layers
  - Prevented unnecessary marker recreation
  - Markers now persist properly across updates

### 2. Comprehensive Analysis
- ✅ Analyzed entire 6,690-line MapView.vue file
- ✅ Identified all functional areas and dead code
- ✅ Created prioritized refactoring plan
- ✅ Documented ~835 lines for useMapLayers extraction
- ✅ Documented ~723 lines for usePopups extraction
- ✅ Documented ~600 lines for fetchProviders extraction

### 3. Phase 1: Dead Code Removal (258 lines removed)
- ✅ Removed `updateMarkers()` method (78 lines)
- ✅ Removed `createSingleMarker()` method (175 lines)
- ✅ Removed `useNewComponents` flag (2 lines)
- ✅ Removed dead comments (3 lines)

**File size reduced from 6,690 → 6,432 lines**

---

## 📋 Remaining Work

### Phase 2: Extract Heavy Logic (~2,158 lines)

#### A. Create `useMapLayers` Composable (~835 lines)
Extract from MapView.vue:
- `addServiceAreasToMap()` (200 lines)
- `removeServiceAreasFromMap()` (40 lines)
- `addLAZipCodesToMap()` (79 lines)
- `removeLAZipCodesFromMap()` (29 lines)
- `addLARegionalCentersToMap()` (235 lines)
- `removeLARegionalCentersFromMap()` (74 lines)
- `addColoredLAZipsLayer()` (153 lines)
- `updateRegionalCenterHighlighting()` (25 lines)

**Complexity**: High - requires careful state management
**Estimated time**: 6-8 hours

#### B. Create `usePopups` Composable (~723 lines)
Extract from MapView.vue:
- `createSimplePopup()` (309 lines)
- `createRegionalCenterPopup()` (286 lines)
- `formatHours()` (28 lines)
- `formatHoursObject()` (22 lines)
- `formatDescription()` (20 lines)
- `formatInsurance()` (29 lines)
- `formatLanguages()` (29 lines)

**Complexity**: Medium - mostly HTML generation
**Estimated time**: 4 hours

#### C. Refactor `fetchProviders` to Store (~600 lines)
Currently lines 2507-3054 in MapView.vue - massive 547-line method!

Move to `providerStore` as actions:
- `searchByLocation(lat, lng, radius)`
- `searchByZip(zipCode)`
- `applyFilters(filters)`
- Coordinate validation logic

**Complexity**: High - complex API logic
**Estimated time**: 4-6 hours

### Phase 3: Store Architecture (~200-300 lines)

#### A. Create `regionalCenterStore`
State to extract from MapView:
- `selectedRegionalCenters` (object)
- `laRegionalCentersData` (GeoJSON)
- `zipToCenter` (lookup map)
- `matchedRegionalCenter` (user's RC)

Actions:
- `toggleCenterSelection(name)`
- `updateVisibility(centerName)`
- `findByZip(zipCode)`

**Estimated time**: 3 hours

#### B. Create `uiStore`
State to extract from MapView:
- `showFundingInfo`
- `showOnboarding`
- `showMobileSidebar`
- `showMobileSearch`
- `showUserMenu`
- `showServiceAreas`
- `pinServiceAreas`
- `showLARegionalCenters`
- `zipViewOnly`

Actions:
- `toggleModal(modalName)`
- `toggleSidebar()`
- `toggleOverlay(overlayName)`

**Estimated time**: 2 hours

### Phase 4: Utilities & Polish (~100-200 lines)

#### A. Extract Utilities
- `utils/map/popupFormatters.ts` (formatHours, formatInsurance, etc.)
- `utils/map/colorUtils.ts` (hslToHex)
- `utils/map/laRegionalCenters.ts` (constants)
- Consolidate coordinate validation

**Estimated time**: 2 hours

#### B. Enhance Existing Composables
- Add geocoding consolidation to `useGeolocation`
- Add bounds calculation to existing map composables

**Estimated time**: 2 hours

---

## Architecture After Refactoring

```
MapView.vue (~2,900 lines)
├── Template (~500 lines)
├── Script (~1,900 lines)
│   ├── Composables (thin wrappers)
│   │   ├── useMapLayers()
│   │   ├── usePopups()
│   │   ├── useGeolocation()
│   │   └── useRegionalCenterData()
│   ├── Stores (via composables)
│   │   ├── providerStore
│   │   ├── mapStore
│   │   ├── filterStore
│   │   ├── regionalCenterStore (new)
│   │   └── uiStore (new)
│   └── Event handlers (orchestration only)
└── Styles (~500 lines)
```

---

## Benefits of This Approach

### Immediate Benefits
1. ✅ **Smaller files** - Easier to navigate and understand
2. ✅ **Reusable logic** - Composables can be used in other components
3. ✅ **Better testing** - Test composables and stores independently
4. ✅ **Clear separation** - Each file has one responsibility
5. ✅ **Backward compatible** - Can use composables in Options API

### Future Benefits
6. **Easy Composition API migration** - When MapView is smaller, converting to `<script setup>` is trivial
7. **TypeScript benefits** - Better type inference with composables
8. **Performance** - Better tree-shaking and code-splitting
9. **Team scaling** - Multiple developers can work on different composables

---

## Next Steps

### Recommended Order:
1. ✅ **Done**: Remove dead code
2. **Next**: Create `usePopups` composable (easiest, 723 lines, 4 hours)
3. **Then**: Refactor `fetchProviders` to store (600 lines, 4-6 hours)
4. **Then**: Create `useMapLayers` composable (835 lines, 6-8 hours)
5. **Then**: Create new stores (regionalCenterStore, uiStore, 5 hours)
6. **Finally**: Extract utilities and polish (4 hours)

**Total estimated time**: 23-27 hours of focused work

---

## Notes

- All composables follow existing TypeScript patterns in `/src/composables/`
- Composables act as wrappers around Pinia stores when needed
- MapView.vue stays in Options API during extraction
- Final conversion to Composition API happens after file is <3,000 lines
- All changes maintain backward compatibility

---

## Questions to Discuss

1. Should we start with `usePopups` (easiest) or `useMapLayers` (biggest impact)?
2. Do you want to review each composable before integrating?
3. Should we write tests as we go or after extraction?
4. Any specific pain points in MapView you want addressed first?

---

**Generated**: 2025-10-27
**File**: MapView.vue (4,734 lines - DOWN FROM 6,690!)
**Target**: ~2,900 lines (56% reduction)
**Progress**: 1,956/3,790 lines removed (29.3% complete!)

## ⚡ MAJOR UPDATE - Significant Progress Made!

We've completed Phases 1, 2A, and 2B with excellent results:
- ✅ Phase 1: Dead code removal (258 lines)
- ✅ Phase 2A: usePopups composable created (1,139 lines extracted)
- ✅ Phase 2B: fetchProviders refactored (525 lines simplified)

See `REFACTORING_SUMMARY.md` for detailed breakdown of achievements.
