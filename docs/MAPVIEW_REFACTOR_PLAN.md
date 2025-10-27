# MapView.vue Refactoring Plan

## Current State Analysis

**File:** `/Users/alexbeattie/Developer/CHLA/map-frontend/src/views/MapView.vue`
- **Total lines:** 6,681
- **Template:** Lines 1-578
- **Script:** Lines 579-6,500+
- **Methods:** ~78 methods
- **Data properties:** 60+ reactive properties

**Problems:**
1. ❌ Too large to understand or maintain
2. ❌ Difficult to test individual features
3. ❌ Performance issues (large reactive state)
4. ❌ Hard to debug
5. ❌ Risky to make changes

---

## Refactoring Strategy: Incremental & Safe

We'll use a **strangler fig pattern** - gradually extract functionality while keeping the original working.

### Principles:
1. ✅ **One change at a time** - test after each step
2. ✅ **No breaking changes** - original file works throughout
3. ✅ **TypeScript composables** - better type safety
4. ✅ **Pinia stores** - centralized state management
5. ✅ **Component extraction** - UI into smaller pieces

---

## Phase 1: Extract Utility Functions (Week 1)

### Step 1.1: Create Utils Directory

**Create:** `src/utils/map/`

```
src/utils/map/
├── geocoding.ts          # Address/ZIP geocoding
├── coordinates.ts        # Coordinate validation & bounds checking
├── distance.ts           # Distance calculations
└── formatters.ts         # Data formatting utilities
```

**Files to create:**

#### `src/utils/map/geocoding.ts`
```typescript
export interface GeocodeResult {
  lat: number;
  lng: number;
  formatted_address: string;
  zip_code?: string;
}

export async function geocodeAddress(
  address: string,
  mapboxToken: string
): Promise<GeocodeResult | null> {
  // Extract geocoding logic from MapView
  // Currently around line 2400-2500
}

export async function geocodeZip(
  zipCode: string,
  mapboxToken: string
): Promise<GeocodeResult | null> {
  // Extract ZIP geocoding
}

export function isValidZipCode(zip: string): boolean {
  return /^\d{5}$/.test(zip);
}
```

#### `src/utils/map/coordinates.ts`
```typescript
export interface CoordinateBounds {
  minLat: number;
  maxLat: number;
  minLng: number;
  maxLng: number;
}

export const CA_BOUNDS: CoordinateBounds = {
  minLat: 32,
  maxLat: 42,
  minLng: -125,
  maxLng: -114
};

export function isWithinBounds(
  lat: number,
  lng: number,
  bounds: CoordinateBounds = CA_BOUNDS
): boolean {
  return (
    lat >= bounds.minLat &&
    lat <= bounds.maxLat &&
    lng >= bounds.minLng &&
    lng <= bounds.maxLng
  );
}

export function validateCoordinates(lat: any, lng: any): {
  lat: number;
  lng: number;
} | null {
  // Extract coordinate validation logic
  // Currently scattered throughout MapView
}
```

**Benefits:**
- Easy to test
- Reusable across components
- No breaking changes (just extracted, not removed yet)

---

## Phase 2: Create Composables (Week 2)

### Step 2.1: Map State Composable

**Create:** `src/composables/useMapState.ts`

```typescript
import { ref, computed } from 'vue';
import type { Map as MapboxMap } from 'mapbox-gl';

export function useMapState() {
  const map = ref<MapboxMap | null>(null);
  const mapLoaded = ref(false);
  const mapCenter = ref<[number, number]>([-118.2437, 34.0522]); // LA
  const mapZoom = ref(10);

  const isMapReady = computed(() => map.value !== null && mapLoaded.value);

  function setMap(mapInstance: MapboxMap) {
    map.value = mapInstance;
  }

  function setMapLoaded(loaded: boolean) {
    mapLoaded.value = loaded;
  }

  function updateMapView(center: [number, number], zoom: number) {
    if (!map.value) return;
    map.value.flyTo({ center, zoom });
    mapCenter.value = center;
    mapZoom.value = zoom;
  }

  return {
    map,
    mapLoaded,
    mapCenter,
    mapZoom,
    isMapReady,
    setMap,
    setMapLoaded,
    updateMapView
  };
}
```

### Step 2.2: Provider Search Composable

**Create:** `src/composables/useProviderSearch.ts`

```typescript
import { ref, computed } from 'vue';
import axios from 'axios';
import type { Provider } from '@/types/provider';

export function useProviderSearch(apiBaseUrl: string) {
  const providers = ref<Provider[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);
  const searchLocation = ref('');
  const searchCoordinates = ref<{ lat: number; lng: number } | null>(null);

  const providerCount = computed(() => providers.value.length);

  async function searchProviders(params: {
    zipCode?: string;
    lat?: number;
    lng?: number;
    radius?: number;
    insurance?: string;
    therapy?: string;
  }) {
    loading.value = true;
    error.value = null;

    try {
      const queryParams = new URLSearchParams();

      // Use regional center filtering for ZIP searches
      if (params.zipCode && /^\d{5}$/.test(params.zipCode)) {
        queryParams.append('zip_code', params.zipCode);

        if (params.insurance) {
          queryParams.append('insurance', params.insurance);
        }

        const url = `${apiBaseUrl}/api/providers-v2/by_regional_center/?${queryParams}`;
        const response = await axios.get(url);

        providers.value = response.data.results || [];
        return response.data;
      }

      // Fall back to radius search for lat/lng
      if (params.lat && params.lng) {
        queryParams.append('lat', params.lat.toString());
        queryParams.append('lng', params.lng.toString());
        queryParams.append('radius', (params.radius || 25).toString());
      }

      if (params.insurance) {
        queryParams.append('insurance', params.insurance);
      }

      const url = `${apiBaseUrl}/api/providers-v2/comprehensive_search/?${queryParams}`;
      const response = await axios.get(url);

      providers.value = response.data.results || response.data || [];
      return response.data;

    } catch (err: any) {
      error.value = err.message;
      providers.value = [];
      return null;
    } finally {
      loading.value = false;
    }
  }

  function clearSearch() {
    providers.value = [];
    searchLocation.value = '';
    searchCoordinates.value = null;
    error.value = null;
  }

  return {
    providers,
    loading,
    error,
    searchLocation,
    searchCoordinates,
    providerCount,
    searchProviders,
    clearSearch
  };
}
```

### Step 2.3: Filter State Composable

**Create:** `src/composables/useFilterState.ts`

```typescript
import { ref, reactive, computed } from 'vue';

export interface FilterOptions {
  acceptsInsurance: boolean;
  acceptsRegionalCenter: boolean;
  acceptsPrivatePay: boolean;
  matchesAge: boolean;
  matchesDiagnosis: boolean;
  therapyTypes: string[];
  ageGroups: string[];
}

export function useFilterState() {
  const filterOptions = reactive<FilterOptions>({
    acceptsInsurance: false,
    acceptsRegionalCenter: false,
    acceptsPrivatePay: false,
    matchesAge: false,
    matchesDiagnosis: false,
    therapyTypes: [],
    ageGroups: []
  });

  const hasActiveFilters = computed(() => {
    return (
      filterOptions.acceptsInsurance ||
      filterOptions.acceptsRegionalCenter ||
      filterOptions.acceptsPrivatePay ||
      filterOptions.matchesAge ||
      filterOptions.matchesDiagnosis ||
      filterOptions.therapyTypes.length > 0 ||
      filterOptions.ageGroups.length > 0
    );
  });

  function resetFilters() {
    filterOptions.acceptsInsurance = false;
    filterOptions.acceptsRegionalCenter = false;
    filterOptions.acceptsPrivatePay = false;
    filterOptions.matchesAge = false;
    filterOptions.matchesDiagnosis = false;
    filterOptions.therapyTypes = [];
    filterOptions.ageGroups = [];
  }

  function toggleFilter(filterName: keyof FilterOptions) {
    if (typeof filterOptions[filterName] === 'boolean') {
      (filterOptions[filterName] as boolean) = !(filterOptions[filterName] as boolean);
    }
  }

  return {
    filterOptions,
    hasActiveFilters,
    resetFilters,
    toggleFilter
  };
}
```

---

## Phase 3: Create Pinia Stores (Week 3)

### Step 3.1: Provider Store

**Create:** `src/stores/providerStore.ts`

```typescript
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import axios from 'axios';
import type { Provider } from '@/types/provider';

export const useProviderStore = defineStore('provider', () => {
  // State
  const providers = ref<Provider[]>([]);
  const selectedProvider = ref<Provider | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);

  // Getters
  const providerCount = computed(() => providers.value.length);
  const providersWithCoordinates = computed(() =>
    providers.value.filter(p => p.latitude && p.longitude)
  );

  // Actions
  async function fetchProviders(params: {
    zipCode?: string;
    regionalCenterId?: number;
    filters?: any;
  }) {
    loading.value = true;
    error.value = null;

    try {
      let url = '';
      const queryParams = new URLSearchParams();

      if (params.zipCode) {
        url = `${import.meta.env.VITE_API_BASE_URL}/api/providers-v2/by_regional_center/`;
        queryParams.append('zip_code', params.zipCode);
      } else if (params.regionalCenterId) {
        url = `${import.meta.env.VITE_API_BASE_URL}/api/providers-v2/by_regional_center/`;
        queryParams.append('regional_center_id', params.regionalCenterId.toString());
      }

      // Add filters
      if (params.filters) {
        Object.entries(params.filters).forEach(([key, value]) => {
          if (value) queryParams.append(key, value as string);
        });
      }

      const response = await axios.get(`${url}?${queryParams}`);
      providers.value = response.data.results || response.data || [];

      return response.data;
    } catch (err: any) {
      error.value = err.message;
      throw err;
    } finally {
      loading.value = false;
    }
  }

  function selectProvider(provider: Provider | null) {
    selectedProvider.value = provider;
  }

  function clearProviders() {
    providers.value = [];
    selectedProvider.value = null;
    error.value = null;
  }

  return {
    providers,
    selectedProvider,
    loading,
    error,
    providerCount,
    providersWithCoordinates,
    fetchProviders,
    selectProvider,
    clearProviders
  };
});
```

### Step 3.2: Map Store

**Create:** `src/stores/mapStore.ts`

```typescript
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { Map as MapboxMap } from 'mapbox-gl';

export const useMapStore = defineStore('map', () => {
  const map = ref<MapboxMap | null>(null);
  const mapLoaded = ref(false);
  const mapCenter = ref<[number, number]>([-118.2437, 34.0522]);
  const mapZoom = ref(10);
  const showRegionalCenters = ref(false);
  const highlightedRegionalCenter = ref<number | null>(null);

  const isReady = computed(() => map.value !== null && mapLoaded.value);

  function setMap(mapInstance: MapboxMap) {
    map.value = mapInstance;
  }

  function setLoaded() {
    mapLoaded.value = true;
  }

  function flyTo(center: [number, number], zoom: number) {
    if (!map.value) return;
    map.value.flyTo({ center, zoom, duration: 1000 });
    mapCenter.value = center;
    mapZoom.value = zoom;
  }

  function toggleRegionalCenters() {
    showRegionalCenters.value = !showRegionalCenters.value;
  }

  function highlightRegionalCenter(id: number | null) {
    highlightedRegionalCenter.value = id;
  }

  return {
    map,
    mapLoaded,
    mapCenter,
    mapZoom,
    showRegionalCenters,
    highlightedRegionalCenter,
    isReady,
    setMap,
    setLoaded,
    flyTo,
    toggleRegionalCenters,
    highlightRegionalCenter
  };
});
```

---

## Phase 4: Extract Components (Week 4)

### Step 4.1: Component Structure

```
src/components/map/
├── MapContainer.vue           # Main map display
├── MapControls.vue            # Zoom, pan controls
├── MapMarkers.vue             # Provider markers
├── RegionalCenterLayer.vue    # RC polygon overlay
└── MapPopup.vue               # Marker popup

src/components/search/
├── SearchBar.vue              # Search input
├── FilterPanel.vue            # All filters
├── FilterPills.vue            # Quick filter pills
└── SearchResults.vue          # Results list

src/components/provider/
├── ProviderCard.vue           # Provider detail card
├── ProviderList.vue           # List view
└── ProviderPopup.vue          # Map popup content

src/components/onboarding/
└── OnboardingFlow.vue         # Already extracted!
```

### Step 4.2: MapContainer Component

**Create:** `src/components/map/MapContainer.vue`

```vue
<template>
  <div ref="mapContainer" class="map-container"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import mapboxgl from 'mapbox-gl';
import { useMapStore } from '@/stores/mapStore';

const props = defineProps<{
  accessToken: string;
  center?: [number, number];
  zoom?: number;
}>();

const emit = defineEmits<{
  mapLoaded: [];
  mapClick: [{ lat: number; lng: number }];
}>();

const mapContainer = ref<HTMLDivElement | null>(null);
const mapStore = useMapStore();

onMounted(() => {
  if (!mapContainer.value) return;

  mapboxgl.accessToken = props.accessToken;

  const map = new mapboxgl.Map({
    container: mapContainer.value,
    style: 'mapbox://styles/mapbox/streets-v12',
    center: props.center || [-118.2437, 34.0522],
    zoom: props.zoom || 10
  });

  map.on('load', () => {
    mapStore.setMap(map);
    mapStore.setLoaded();
    emit('mapLoaded');
  });

  map.on('click', (e) => {
    emit('mapClick', { lat: e.lngLat.lat, lng: e.lngLat.lng });
  });
});

// Watch for center/zoom changes
watch(() => [props.center, props.zoom], ([newCenter, newZoom]) => {
  if (newCenter && newZoom && mapStore.isReady) {
    mapStore.flyTo(newCenter as [number, number], newZoom as number);
  }
});
</script>

<style scoped>
.map-container {
  width: 100%;
  height: 100%;
}
</style>
```

---

## Phase 5: Gradual Integration (Week 5)

### Step 5.1: Update MapView to Use Composables

**In MapView.vue, replace data properties:**

```vue
<script>
import { useProviderSearch } from '@/composables/useProviderSearch';
import { useFilterState } from '@/composables/useFilterState';
import { useMapState } from '@/composables/useMapState';

export default {
  setup() {
    const { providers, searchProviders, loading } = useProviderSearch(
      import.meta.env.VITE_API_BASE_URL
    );

    const { filterOptions, hasActiveFilters, resetFilters } = useFilterState();

    const { map, mapLoaded, updateMapView } = useMapState();

    return {
      providers,
      searchProviders,
      loading,
      filterOptions,
      hasActiveFilters,
      resetFilters,
      map,
      mapLoaded,
      updateMapView
    };
  },

  // Keep existing data() for now, gradually remove as we migrate
  data() {
    // ... existing data, but commented out what's moved to composables
  }
}
</script>
```

### Step 5.2: Test Each Migration

After each change:
1. Run dev server
2. Test all functionality
3. Fix any issues
4. Commit with clear message
5. Move to next piece

---

## Testing Strategy

### Unit Tests (New)

Create tests for extracted utilities:

```typescript
// src/utils/map/__tests__/coordinates.test.ts
import { describe, it, expect } from 'vitest';
import { isWithinBounds, CA_BOUNDS } from '../coordinates';

describe('isWithinBounds', () => {
  it('should return true for LA coordinates', () => {
    expect(isWithinBounds(34.0522, -118.2437, CA_BOUNDS)).toBe(true);
  });

  it('should return false for coordinates outside CA', () => {
    expect(isWithinBounds(40.7128, -74.0060, CA_BOUNDS)).toBe(false); // NYC
  });
});
```

### Integration Tests

Test composables with Vue Test Utils:

```typescript
// src/composables/__tests__/useProviderSearch.test.ts
import { describe, it, expect, vi } from 'vitest';
import { useProviderSearch } from '../useProviderSearch';

describe('useProviderSearch', () => {
  it('should search by ZIP code', async () => {
    const { searchProviders, providers } = useProviderSearch('http://localhost');

    await searchProviders({ zipCode: '91769' });

    expect(providers.value.length).toBeGreaterThan(0);
  });
});
```

---

## Migration Checklist

### Week 1: Utils ✅ COMPLETE
- [x] Create `src/utils/map/` directory
- [x] Extract geocoding.ts
- [x] Extract coordinates.ts
- [x] Extract distance.ts
- [x] Extract formatters.ts
- [x] Write unit tests (72 tests passing)
- [x] Update MapView to import utils (but keep existing code)

### Week 2: Composables ✅ COMPLETE
- [x] Create `src/composables/` directory
- [x] Create useMapState.ts
- [x] Create useProviderSearch.ts
- [x] Create useFilterState.ts
- [x] Create useRegionalCenter.ts
- [x] Write composable tests (88 tests passing)
- [x] Start using in MapView (hybrid approach)

### Week 3: Stores ✅ COMPLETE
- [x] Set up Pinia
- [x] Create providerStore.ts (311 lines)
- [x] Create mapStore.ts (308 lines)
- [x] Create filterStore.ts (269 lines)
- [x] Migrate state management (composables delegate to stores)
- [x] Test store integration (221 tests passing - 107 store + 114 composable)

### Week 4: Components
- [ ] Create MapContainer.vue
- [ ] Create SearchBar.vue
- [ ] Create FilterPanel.vue
- [ ] Create ProviderCard.vue
- [ ] Test each component
- [ ] Integrate into MapView

### Week 5: Final Migration
- [ ] Remove old code from MapView
- [ ] Update all imports
- [ ] Final testing
- [ ] Performance check
- [ ] Deploy

---

## Benefits After Refactoring

1. **Maintainability**
   - 6,681 lines → ~300 lines per file
   - Clear separation of concerns
   - Easy to find code

2. **Testability**
   - Unit tests for utilities
   - Component tests
   - Store tests
   - Integration tests

3. **Performance**
   - Smaller reactive state
   - Better code splitting
   - Lazy-loaded components

4. **Developer Experience**
   - TypeScript autocomplete
   - Better error messages
   - Easier onboarding

5. **Reusability**
   - Composables can be used elsewhere
   - Components are standalone
   - Stores are global

---

## Rollback Plan

If something breaks:
1. Each phase is in a separate branch
2. Original MapView.vue is never deleted until the end
3. Can run old and new side-by-side
4. Feature flags for gradual rollout

---

## Next Steps

1. **Review this plan** - Make sure you're comfortable with the approach
2. **Set up TypeScript** - If not already configured
3. **Create Week 1 branch** - `git checkout -b refactor/week1-utils`
4. **Start with utils extraction** - Lowest risk, high value

Let me know when you're ready to start, and I'll help implement each phase! 🚀
