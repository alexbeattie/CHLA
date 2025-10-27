# Vue 3 Composables

**Created:** October 26, 2025
**Purpose:** Reusable business logic extracted from MapView.vue
**Pattern:** Composition API + TypeScript
**Test Coverage:** 117 tests (100% passing)

---

## 📚 Overview

This directory contains Vue 3 composables that encapsulate business logic for the ABA Provider Map application. Each composable is:

- ✅ **Fully typed** with TypeScript
- ✅ **Unit tested** with Vitest
- ✅ **Reusable** across components
- ✅ **Well-documented** with JSDoc comments
- ✅ **Production-ready**

---

## 🗂️ Composables

### 1. `useProviderSearch`

**Purpose:** Provider fetching and search logic
**Lines:** 268
**Tests:** 19

**Features:**
- ZIP code search with regional center filtering
- Location-based radius search
- Comprehensive text search
- Filter parameter building
- Multiple search methods
- Response format normalization

**Usage:**
```typescript
import { useProviderSearch } from '@/composables';

const {
  providers,
  loading,
  error,
  searchByZipCode,
  searchByLocation,
  providerCount
} = useProviderSearch('http://localhost:8000');

// Search by ZIP code
await searchByZipCode('91769', {
  insurance: 'regional center',
  age: '3-5'
});

// Search by location
await searchByLocation(34.0522, -118.2437, 25);

console.log(`Found ${providerCount.value} providers`);
```

**Key Methods:**
- `searchProviders(params)` - Main search method
- `searchByZipCode(zipCode, filters)` - ZIP-specific search
- `searchByLocation(lat, lng, radius, filters)` - Coordinate search
- `searchWithFilters(filters)` - Comprehensive search
- `clearSearch()` - Reset state
- `getProviderById(id)` - Get single provider
- `filterProviders(predicate)` - Client-side filtering

**State:**
- `providers` - Array of providers
- `loading` - Loading state
- `error` - Error message
- `regionalCenterInfo` - Current regional center
- `searchCoordinates` - Last search coordinates

**Computed:**
- `providerCount` - Number of providers
- `providersWithCoordinates` - Providers with lat/lng
- `hasProviders` - Boolean check

---

### 2. `useFilterState`

**Purpose:** Filter state management
**Lines:** 237
**Tests:** 30

**Features:**
- Filter toggle with mutual exclusivity
- User onboarding data integration
- Filter parameter building
- Active filter counting
- Available options management

**Usage:**
```typescript
import { useFilterState } from '@/composables';

const {
  filterOptions,
  userData,
  hasActiveFilters,
  activeFilterCount,
  toggleFilter,
  updateUserData,
  buildFilterParams,
  applyOnboardingFilters
} = useFilterState();

// Toggle filter
toggleFilter('acceptsRegionalCenter');

// Update user data from onboarding
updateUserData({
  insurance: 'Regional Center',
  age: '3-5',
  diagnosis: 'Autism'
});

// Apply filters based on user data
applyOnboardingFilters();

// Build params for API call
const params = buildFilterParams();
// { insurance: 'regional center', age: '3-5', diagnosis: 'Autism' }
```

**Filter Options:**
- `acceptsInsurance` - Accepts insurance
- `acceptsRegionalCenter` - Accepts regional center funding
- `acceptsPrivatePay` - Accepts private pay
- `matchesAge` - Filter by age group
- `matchesDiagnosis` - Filter by diagnosis
- `matchesTherapy` - Filter by therapy type
- `showOnlyFavorites` - Show only favorites

**Mutual Exclusivity:**
Insurance filters are mutually exclusive. Enabling one disables others.

---

### 3. `useMapState`

**Purpose:** Map viewport and UI state management
**Lines:** 285
**Tests:** 39

**Features:**
- Viewport and bounds management
- Provider selection and hover state
- UI panel toggles
- User location handling
- Directions API integration
- Map style switching

**Usage:**
```typescript
import { useMapState } from '@/composables';

const {
  viewport,
  uiState,
  selectedProviderId,
  userLocation,
  centerOn,
  selectProvider,
  setUserLocation,
  getDirectionsTo,
  hasDirections
} = useMapState();

// Center map on location
centerOn({ lat: 34.0522, lng: -118.2437 }, 12);

// Select a provider
selectProvider(123);

// Set user location
setUserLocation({ lat: 34.0, lng: -118.0 }, 10);

// Get directions
await getDirectionsTo(
  { lat: 34.1, lng: -118.1 },
  'mapbox-token'
);

console.log(`Has directions: ${hasDirections.value}`);
```

**Viewport:**
- `center` - Map center coordinates
- `zoom` - Zoom level
- `bearing` - Map rotation
- `pitch` - Map tilt

**UI State:**
- `showFilters` - Show filter panel
- `showProviderDetails` - Show provider details
- `showOnboarding` - Show onboarding flow
- `showDirections` - Show directions panel
- `sidebarExpanded` - Sidebar expanded
- `mapStyle` - Map style (streets/satellite/outdoors)

**Key Methods:**
- `setViewport(viewport)` - Update viewport
- `centerOn(coords, zoom)` - Center on coordinates
- `fitBounds(bounds, padding)` - Fit to bounds
- `selectProvider(id)` - Select provider
- `setUserLocation(coords, accuracy)` - Set user location
- `getDirectionsTo(coords, token)` - Get directions
- `resetMap()` - Reset to initial state

---

### 4. `useRegionalCenter`

**Purpose:** Regional center data and boundary management
**Lines:** 287
**Tests:** 29

**Features:**
- Regional center fetching
- ZIP code lookup
- Boundary generation
- GeoJSON export for map display
- Nearest regional center by coordinates

**Usage:**
```typescript
import { useRegionalCenter } from '@/composables';

const {
  regionalCenters,
  currentRegionalCenter,
  regionalCenterName,
  findByZipCode,
  generateApproximateBoundary,
  getHighlightGeoJSON,
  isZipInRegionalCenter
} = useRegionalCenter('http://localhost:8000');

// Find regional center by ZIP
const rc = await findByZipCode('91769');
console.log(`Found: ${regionalCenterName.value}`);

// Generate boundary for highlighting
await generateApproximateBoundary(
  rc.zip_codes,
  'mapbox-token'
);

// Get GeoJSON for map
const geoJSON = getHighlightGeoJSON();

// Check if ZIP is in current RC
if (isZipInRegionalCenter('91769')) {
  console.log('ZIP is in regional center');
}
```

**Key Methods:**
- `fetchRegionalCenters()` - Fetch all RCs
- `findByZipCode(zipCode)` - Find RC by ZIP
- `getById(id)` - Get RC by ID
- `setRegionalCenter(rc)` - Set current RC
- `clearRegionalCenter()` - Clear state
- `isZipInRegionalCenter(zipCode)` - Check if ZIP in RC
- `generateApproximateBoundary(zipCodes, token)` - Generate boundary
- `getHighlightGeoJSON()` - Get GeoJSON for map
- `findNearestToCoordinates(lat, lng, token)` - Find nearest RC

**State:**
- `regionalCenters` - All regional centers
- `currentRegionalCenter` - Currently selected RC
- `regionalCenterBoundary` - Boundary polygon
- `loading` - Loading state
- `error` - Error message

---

## 🧪 Testing

All composables have comprehensive unit tests using Vitest.

**Run tests:**
```bash
npm test                 # Run all tests
npm run test:ui          # Run with UI
npm run test:coverage    # Run with coverage report
```

**Test files:**
- `src/tests/composables/useProviderSearch.spec.ts`
- `src/tests/composables/useFilterState.spec.ts`
- `src/tests/composables/useMapState.spec.ts`
- `src/tests/composables/useRegionalCenter.spec.ts`

**Coverage:** 117 tests, 100% passing

---

## 📖 Documentation

- **Integration Guide:** `/docs/COMPOSABLES_INTEGRATION_GUIDE.md`
- **Refactoring Plan:** `/docs/MAPVIEW_REFACTOR_PLAN.md`
- **Session Summary:** `/docs/SESSION_SUMMARY.md`

---

## 🎯 Design Patterns

### Composition API Pattern

Each composable follows the Vue 3 Composition API pattern:

```typescript
export function useExample() {
  // State (reactive refs)
  const data = ref([]);
  const loading = ref(false);

  // Computed properties
  const count = computed(() => data.value.length);

  // Methods
  async function fetchData() {
    loading.value = true;
    // ... fetch logic
    loading.value = false;
  }

  // Return public API
  return {
    // State
    data,
    loading,
    // Computed
    count,
    // Methods
    fetchData
  };
}
```

### Separation of Concerns

Each composable handles a specific domain:
- **Provider Search** - Data fetching
- **Filter State** - User preferences
- **Map State** - UI and viewport
- **Regional Center** - Geographic boundaries

### Reactive State Management

All state is reactive using Vue's `ref()` and `reactive()`:
- Changes automatically propagate to components
- Computed properties update automatically
- No manual state synchronization needed

---

## 🔄 Integration Example

Here's how to use multiple composables together in a component:

```vue
<template>
  <div class="map-container">
    <!-- Search bar -->
    <input
      v-model="searchText"
      @keyup.enter="handleSearch"
      placeholder="Enter ZIP code"
    />

    <!-- Filter panel -->
    <div v-if="uiState.showFilters">
      <button
        :class="{ active: filterOptions.acceptsRegionalCenter }"
        @click="toggleFilter('acceptsRegionalCenter')"
      >
        Regional Center
      </button>
    </div>

    <!-- Regional center info -->
    <div v-if="hasRegionalCenter">
      <h3>{{ regionalCenterName }}</h3>
      <p>{{ providerCount }} providers</p>
    </div>

    <!-- Provider list -->
    <div
      v-for="provider in providers"
      :key="provider.id"
      @click="selectProvider(provider.id)"
    >
      {{ provider.name }}
    </div>

    <!-- Loading -->
    <div v-if="loading">Searching...</div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import {
  useProviderSearch,
  useFilterState,
  useMapState,
  useRegionalCenter
} from '@/composables';

const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8000';

// Initialize composables
const providerSearch = useProviderSearch(apiUrl);
const filterState = useFilterState();
const mapState = useMapState();
const regionalCenter = useRegionalCenter(apiUrl);

// Destructure for template
const { providers, loading, providerCount } = providerSearch;
const { filterOptions, toggleFilter } = filterState;
const { uiState, selectProvider } = mapState;
const { hasRegionalCenter, regionalCenterName } = regionalCenter;

// Local state
const searchText = ref('');

// Search handler
async function handleSearch() {
  const query = searchText.value.trim();
  if (!query) return;

  // Find regional center
  await regionalCenter.findByZipCode(query);

  // Build filters
  const filters = filterState.buildFilterParams();

  // Search providers
  await providerSearch.searchByZipCode(query, filters);

  // Center map on results
  if (providerSearch.providersWithCoordinates.value.length > 0) {
    const first = providerSearch.providersWithCoordinates.value[0];
    mapState.centerOn({
      lat: first.latitude!,
      lng: first.longitude!
    }, 12);
  }
}
</script>
```

---

## 🚀 Best Practices

### 1. Always Initialize in `setup()`

```typescript
// ✅ Good
setup() {
  const providerSearch = useProviderSearch(apiUrl);
  return { providerSearch };
}

// ❌ Bad - don't create in methods
methods: {
  search() {
    const providerSearch = useProviderSearch(apiUrl);
  }
}
```

### 2. Destructure Carefully

```typescript
// ✅ Good - maintains reactivity
const { providers, loading } = useProviderSearch(apiUrl);

// ⚠️ Loses reactivity for primitives
const { loading } = useProviderSearch(apiUrl);
// Use loading.value in template

// ✅ Better for primitives - use computed
const loading = computed(() => providerSearch.loading.value);
```

### 3. Combine Multiple Composables

```typescript
// ✅ Good - combine related composables
async function searchWithAllFeatures(zipCode: string) {
  const rc = await regionalCenter.findByZipCode(zipCode);
  const filters = filterState.buildFilterParams();
  await providerSearch.searchByZipCode(zipCode, filters);

  if (rc) {
    await regionalCenter.generateApproximateBoundary(
      rc.zip_codes,
      mapboxToken
    );
  }
}
```

### 4. Handle Errors Gracefully

```typescript
// ✅ Good - check for errors
const result = await providerSearch.searchByZipCode('91769');
if (providerSearch.error.value) {
  console.error('Search failed:', providerSearch.error.value);
  // Show error to user
}
```

### 5. Clean Up When Needed

```typescript
// ✅ Good - clear state when appropriate
onUnmounted(() => {
  providerSearch.clearSearch();
  regionalCenter.clearRegionalCenter();
});
```

---

## 📝 TypeScript Interfaces

All composables export TypeScript interfaces for type safety:

```typescript
// From useProviderSearch
import type {
  Provider,
  SearchParams,
  RegionalCenterInfo,
  ProviderSearchResult
} from '@/composables/useProviderSearch';

// From useFilterState
import type {
  FilterOptions,
  UserData
} from '@/composables/useFilterState';

// From useMapState
import type {
  MapViewport,
  MapBounds,
  MapUIState
} from '@/composables/useMapState';

// From useRegionalCenter
import type {
  RegionalCenter,
  RegionalCenterBoundary
} from '@/composables/useRegionalCenter';
```

---

## 🔧 Configuration

Composables use environment variables for API configuration:

```bash
# .env.development
VITE_API_URL=http://localhost:8000
VITE_MAPBOX_TOKEN=your-mapbox-token
```

---

## 🐛 Troubleshooting

### Issue: Reactivity not working

**Solution:** Ensure you're using `.value` for refs in script, but not in template:

```typescript
// Script
console.log(providers.value); // ✅ Use .value

// Template
<div>{{ providers }}</div> <!-- ✅ No .value -->
```

### Issue: Composable not updating

**Solution:** Check that state is properly returned from composable:

```typescript
export function useExample() {
  const data = ref([]);

  // ✅ Must return for reactivity
  return { data };
}
```

### Issue: Tests failing

**Solution:** Ensure mocks are properly reset between tests:

```typescript
beforeEach(() => {
  vi.clearAllMocks();
  composable = useProviderSearch(apiUrl);
});

afterEach(() => {
  vi.resetAllMocks();
});
```

---

## 📊 Performance

All composables are optimized for performance:

- **Lazy evaluation:** Computed properties only recalculate when dependencies change
- **Minimal re-renders:** State updates trigger only necessary component updates
- **Efficient filtering:** Client-side filtering uses efficient array methods
- **Request debouncing:** Consider adding debouncing for user input

---

## 🔮 Future Enhancements

Planned improvements for composables:

1. **Caching layer** - Cache search results to reduce API calls
2. **Optimistic updates** - Update UI before API confirms
3. **Request cancellation** - Cancel in-flight requests when new search starts
4. **Pagination** - Support for large result sets
5. **Real-time updates** - WebSocket support for live data
6. **Offline support** - IndexedDB caching for offline access

---

## 📚 Additional Resources

- [Vue 3 Composition API Docs](https://vuejs.org/guide/extras/composition-api-faq.html)
- [Vitest Documentation](https://vitest.dev/)
- [TypeScript Handbook](https://www.typescriptlang.org/docs/)
- [Mapbox GL JS API](https://docs.mapbox.com/mapbox-gl-js/api/)

---

**Questions?** Check the integration guide at `/docs/COMPOSABLES_INTEGRATION_GUIDE.md`
