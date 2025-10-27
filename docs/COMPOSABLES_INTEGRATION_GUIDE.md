# Composables Integration Guide

**Created:** October 26, 2025
**Purpose:** Guide for integrating Week 2 composables into MapView.vue
**Approach:** Hybrid/Strangler Fig Pattern (new code alongside old)

---

## 📋 Overview

Week 2 refactoring created 4 composables that extract business logic from MapView.vue:

1. `useProviderSearch` - Provider fetching and search
2. `useFilterState` - Filter management
3. `useMapState` - Map viewport and UI state
4. `useRegionalCenter` - Regional center data and boundaries

This guide shows how to integrate them **without breaking existing code**.

---

## 🎯 Integration Strategy

### Phase 1: Add Composables (This Week)
- Import composables into MapView.vue
- Initialize composables in `setup()` method
- Use composables for **new features only**
- Keep existing code working

### Phase 2: Gradual Migration (Week 3)
- Replace old methods with composable methods
- Remove duplicate state
- Test each migration step

### Phase 3: Cleanup (Week 5)
- Remove all old code
- Finalize integration
- Performance testing

---

## 🔧 Step-by-Step Integration

### Step 1: Convert MapView to Composition API (Hybrid)

Vue 3 supports using both Options API and Composition API together:

```vue
<script lang="ts">
import { defineComponent } from 'vue';
import {
  useProviderSearch,
  useFilterState,
  useMapState,
  useRegionalCenter
} from '@/composables';

export default defineComponent({
  name: 'MapView',

  // NEW: Add setup() method
  setup() {
    // Initialize composables
    const providerSearch = useProviderSearch(process.env.VUE_APP_API_URL || 'http://localhost:8000');
    const filterState = useFilterState();
    const mapState = useMapState();
    const regionalCenter = useRegionalCenter(process.env.VUE_APP_API_URL || 'http://localhost:8000');

    // Expose to template
    return {
      // Provider search
      providerSearch,

      // Filter state
      filterState,

      // Map state
      mapState,

      // Regional center
      regionalCenter
    };
  },

  // OLD: Keep existing Options API code
  data() {
    return {
      providers: [],
      loading: false,
      // ... all existing data
    };
  },

  methods: {
    // ... all existing methods
  }
});
</script>
```

### Step 2: Use Composables for New Features

Example: Add regional center highlighting using new composable:

```vue
<template>
  <!-- Existing map code -->

  <!-- NEW: Regional center boundary layer -->
  <div v-if="regionalCenter.hasBoundary" class="regional-center-info">
    <div class="info-box">
      <h3>📍 {{ regionalCenter.regionalCenterName }}</h3>
      <p>{{ regionalCenter.regionalCenterZipCodes.length }} ZIP codes</p>
    </div>
  </div>
</template>

<script>
export default defineComponent({
  setup() {
    const regionalCenter = useRegionalCenter(apiUrl);

    return { regionalCenter };
  },

  methods: {
    // NEW: Fetch regional center when ZIP searched
    async handleZipSearch(zipCode) {
      // Use new composable
      const rc = await this.regionalCenter.findByZipCode(zipCode);

      if (rc) {
        // Generate boundary for highlighting
        await this.regionalCenter.generateApproximateBoundary(
          rc.zip_codes,
          this.mapboxToken
        );
      }

      // Still call old method for backward compatibility
      await this.searchProviders();
    }
  }
});
</script>
```

### Step 3: Gradually Replace Old State

Example: Migrate filter state:

```vue
<script>
export default defineComponent({
  setup() {
    const filterState = useFilterState();
    return { filterState };
  },

  computed: {
    // NEW: Use composable
    hasActiveFilters() {
      return this.filterState.hasActiveFilters;
    },

    // OLD: Keep for now, mark for removal
    // TODO: Remove in Week 5
    // hasActiveFiltersOld() {
    //   return this.acceptsInsurance || this.acceptsRegionalCenter;
    // }
  },

  methods: {
    // NEW: Use composable method
    resetFilters() {
      this.filterState.resetFilters();
      // Call old method to ensure compatibility
      this.clearAllFilters(); // TODO: Remove in Week 5
    }
  }
});
</script>
```

---

## 📝 Example: Complete Integration

Here's a complete example showing how MapView.vue would look with composables:

```vue
<template>
  <div class="map-view">
    <!-- Search bar -->
    <div class="search-container">
      <input
        v-model="searchText"
        @keyup.enter="handleSearch"
        placeholder="Enter ZIP code or address"
      />
    </div>

    <!-- Filter panel -->
    <div class="filters">
      <button
        :class="{ active: filterState.filterOptions.acceptsRegionalCenter }"
        @click="filterState.toggleFilter('acceptsRegionalCenter')"
      >
        Regional Center
      </button>

      <button
        :class="{ active: filterState.filterOptions.acceptsInsurance }"
        @click="filterState.toggleFilter('acceptsInsurance')"
      >
        Insurance
      </button>

      <div v-if="filterState.hasActiveFilters" class="filter-count">
        {{ filterState.activeFilterCount }} active filters
      </div>
    </div>

    <!-- Regional center info -->
    <div v-if="regionalCenter.hasRegionalCenter" class="rc-info">
      <h3>{{ regionalCenter.regionalCenterName }}</h3>
      <p>Showing {{ providerSearch.providerCount }} providers</p>
    </div>

    <!-- Provider list -->
    <div class="provider-list">
      <div
        v-for="provider in providerSearch.providers"
        :key="provider.id"
        @click="mapState.selectProvider(provider.id)"
        :class="{ selected: mapState.selectedProviderId === provider.id }"
      >
        {{ provider.name }}
      </div>
    </div>

    <!-- Loading state -->
    <div v-if="providerSearch.loading" class="loading">
      Searching providers...
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref } from 'vue';
import {
  useProviderSearch,
  useFilterState,
  useMapState,
  useRegionalCenter
} from '@/composables';

export default defineComponent({
  name: 'MapView',

  setup() {
    const apiUrl = process.env.VUE_APP_API_URL || 'http://localhost:8000';

    // Initialize composables
    const providerSearch = useProviderSearch(apiUrl);
    const filterState = useFilterState();
    const mapState = useMapState();
    const regionalCenter = useRegionalCenter(apiUrl);

    // Local state
    const searchText = ref('');
    const mapboxToken = ref(process.env.VUE_APP_MAPBOX_TOKEN || '');

    // Search handler
    async function handleSearch() {
      const query = searchText.value.trim();

      if (!query) return;

      // Check if ZIP code
      if (/^\d{5}$/.test(query)) {
        // ZIP code search
        console.log('🎯 ZIP code search:', query);

        // Find regional center
        const rc = await regionalCenter.findByZipCode(query);

        // Search providers in regional center
        const filters = filterState.buildFilterParams();
        await providerSearch.searchByZipCode(query, filters);

        // Generate boundary for highlighting
        if (rc && mapboxToken.value) {
          await regionalCenter.generateApproximateBoundary(
            rc.zip_codes,
            mapboxToken.value
          );
        }

        // Center map on results
        if (providerSearch.providersWithCoordinates.length > 0) {
          const firstProvider = providerSearch.providersWithCoordinates[0];
          mapState.centerOn({
            lat: firstProvider.latitude!,
            lng: firstProvider.longitude!
          }, 12);
        }
      } else {
        // Address search
        console.log('📍 Address search:', query);

        const filters = filterState.buildFilterParams();
        await providerSearch.searchWithFilters({
          searchText: query,
          radius: 25,
          ...filters
        });
      }
    }

    return {
      // Composables
      providerSearch,
      filterState,
      mapState,
      regionalCenter,

      // Local state
      searchText,
      mapboxToken,

      // Methods
      handleSearch
    };
  },

  // OLD OPTIONS API CODE BELOW
  // Keep for backward compatibility during migration
  // TODO: Remove in Week 5 after full migration

  data() {
    return {
      // ... existing data
    };
  },

  methods: {
    // ... existing methods
  }
});
</script>
```

---

## 🧪 Testing Composables

### Unit Testing Example

Create `tests/composables/useProviderSearch.spec.ts`:

```typescript
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { useProviderSearch } from '@/composables';
import axios from 'axios';

vi.mock('axios');

describe('useProviderSearch', () => {
  const apiUrl = 'http://localhost:8000';
  let composable: ReturnType<typeof useProviderSearch>;

  beforeEach(() => {
    composable = useProviderSearch(apiUrl);
  });

  it('should initialize with empty providers', () => {
    expect(composable.providers.value).toEqual([]);
    expect(composable.loading.value).toBe(false);
    expect(composable.error.value).toBe(null);
  });

  it('should search by ZIP code', async () => {
    const mockResponse = {
      data: {
        results: [{ id: 1, name: 'Test Provider' }],
        count: 1,
        regional_center: {
          id: 20,
          name: 'San Gabriel/Pomona',
          zip_codes: ['91769']
        }
      }
    };

    vi.mocked(axios.get).mockResolvedValue(mockResponse);

    const result = await composable.searchByZipCode('91769');

    expect(result).toEqual(mockResponse.data);
    expect(composable.providers.value.length).toBe(1);
    expect(composable.regionalCenterInfo.value?.name).toBe('San Gabriel/Pomona');
  });

  it('should handle ZIP code search errors', async () => {
    vi.mocked(axios.get).mockRejectedValue(new Error('Network error'));

    const result = await composable.searchByZipCode('91769');

    expect(result).toBe(null);
    expect(composable.error.value).toBe('Network error');
    expect(composable.providers.value).toEqual([]);
  });
});
```

---

## 🔍 Common Patterns

### Pattern 1: Combining Multiple Composables

```typescript
setup() {
  const providerSearch = useProviderSearch(apiUrl);
  const filterState = useFilterState();
  const regionalCenter = useRegionalCenter(apiUrl);

  // Combined search function
  async function searchWithAllFilters(zipCode: string) {
    // Get regional center
    const rc = await regionalCenter.findByZipCode(zipCode);

    // Build filters
    const filters = filterState.buildFilterParams();

    // Search providers
    await providerSearch.searchByZipCode(zipCode, filters);

    return {
      providers: providerSearch.providers.value,
      regionalCenter: rc
    };
  }

  return { searchWithAllFilters };
}
```

### Pattern 2: Reactive Computed from Composables

```typescript
setup() {
  const providerSearch = useProviderSearch(apiUrl);
  const filterState = useFilterState();

  // Computed property combining composable state
  const filteredProviderCount = computed(() => {
    if (!filterState.hasActiveFilters) {
      return providerSearch.providerCount.value;
    }

    // Client-side filtering if needed
    return providerSearch.providers.value.filter(p => {
      if (filterState.filterOptions.acceptsRegionalCenter) {
        return p.insurance_accepted?.includes('Regional Center');
      }
      return true;
    }).length;
  });

  return { filteredProviderCount };
}
```

### Pattern 3: Watch for Changes

```typescript
import { watch } from 'vue';

setup() {
  const filterState = useFilterState();
  const providerSearch = useProviderSearch(apiUrl);

  // Re-search when filters change
  watch(
    () => filterState.filterOptions,
    async (newFilters, oldFilters) => {
      if (newFilters !== oldFilters) {
        console.log('Filters changed, re-searching...');
        const params = filterState.buildFilterParams();
        await providerSearch.searchWithFilters(params);
      }
    },
    { deep: true }
  );

  return { filterState, providerSearch };
}
```

---

## ⚠️ Migration Warnings

### Don't Do This ❌

```typescript
// DON'T mix old data() with composable state
data() {
  return {
    providers: [] // OLD
  };
},
setup() {
  const providerSearch = useProviderSearch(apiUrl);
  return { providerSearch }; // providerSearch.providers is NEW
}
// Now you have TWO providers sources - confusing!
```

### Do This Instead ✅

```typescript
setup() {
  const providerSearch = useProviderSearch(apiUrl);

  // Use only composable state
  return {
    providers: providerSearch.providers // Expose directly
  };
}
```

---

## 📊 Migration Checklist

### Week 2 (Current)
- [x] Create all composables
- [x] Write comprehensive documentation
- [ ] Add composables to MapView.vue setup()
- [ ] Test composables work alongside old code
- [ ] Use composables for 1-2 new features (RC highlighting)

### Week 3 (Next Week)
- [ ] Migrate filter state to composable
- [ ] Migrate provider search to composable
- [ ] Migrate map state to composable
- [ ] Remove duplicate data() properties
- [ ] Update all methods to use composables

### Week 4
- [ ] Extract UI components
- [ ] Pass composables as props to components
- [ ] Test all features still work

### Week 5
- [ ] Remove ALL old Options API code
- [ ] Final testing
- [ ] Performance benchmarks
- [ ] Deploy to production

---

## 🎉 Benefits Already Achieved

Even without full integration, we now have:

✅ **1,143 lines of reusable code** - Can be used in other components
✅ **Type-safe interfaces** - Full TypeScript support
✅ **Testable logic** - Easy to unit test
✅ **Documentation** - Clear API for each composable
✅ **Future-proof** - Vue 3 Composition API is the future

---

## 📚 Resources

- [Vue 3 Composition API](https://vuejs.org/guide/extras/composition-api-faq.html)
- [Using Composition API with Options API](https://vuejs.org/guide/extras/composition-api-faq.html#can-i-use-both-apis-together)
- [Strangler Fig Pattern](https://martinfowler.com/bliki/StranglerFigApplication.html)
- Week 1: `/docs/MAPVIEW_REFACTOR_PLAN.md`

---

**Next Step:** Add composables to MapView.vue and use for regional center highlighting feature.
