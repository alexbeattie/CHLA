# Week 3 Kickoff - Pinia Store Architecture

**Date:** October 26, 2025
**Sprint:** Week 3 of 5-week MapView Refactoring
**Developer:** Alex Beattie
**Assistant:** Claude Code
**Status:** 🚀 STARTING

---

## 🎯 Week 3 Goals

### Primary Objectives
- ✅ Install and configure Pinia state management
- ✅ Create 3 production-ready stores (provider, map, filter)
- ✅ Migrate state from composables to stores
- ✅ Write comprehensive tests for all stores
- ✅ Document store patterns and usage

### Success Criteria
- [ ] Pinia properly configured in Vue app
- [ ] 3 stores created with full TypeScript types
- [ ] State successfully migrated from composables
- [ ] All store tests passing (target: 90+ tests)
- [ ] Zero breaking changes maintained
- [ ] Composables updated to use stores
- [ ] Documentation complete

---

## 📋 Week 2 Recap

### Completed Deliverables
- ✅ 4 composables (1,143 lines)
- ✅ 117 tests (100% passing)
- ✅ 2,400+ lines of documentation
- ✅ Zero breaking changes

### Foundation Built
- Composables provide business logic layer
- Tests ensure reliability
- Documentation guides integration
- Ready for centralized state management

---

## 🏗️ Week 3 Architecture

### Current State (Week 2)
```
MapView.vue (6,681 lines)
    ↓ uses
Composables (1,143 lines)
    ├── useProviderSearch
    ├── useFilterState
    ├── useMapState
    └── useRegionalCenter
```

### Target State (Week 3)
```
MapView.vue (6,681 lines)
    ↓ uses
Composables (updated)
    ↓ use
Pinia Stores (new)
    ├── providerStore
    ├── mapStore
    └── filterStore
```

### Why Pinia?

**Benefits:**
- **Centralized State:** Single source of truth
- **DevTools:** Vue DevTools integration
- **TypeScript:** Full type inference
- **Composition API:** Consistent with composables
- **SSR Ready:** Server-side rendering support
- **Lightweight:** Only ~1KB

**Comparison with Composables:**
| Feature | Composables | Pinia Stores |
|---------|-------------|--------------|
| Local state | ✅ Yes | ❌ No |
| Shared state | ⚠️ Limited | ✅ Yes |
| DevTools | ❌ No | ✅ Yes |
| Persistence | ❌ Manual | ✅ Plugin |
| Time travel | ❌ No | ✅ Yes |

---

## 📦 Planned Deliverables

### 1. Pinia Infrastructure
- Install Pinia package
- Configure in main.ts
- Set up TypeScript types
- Configure DevTools

### 2. Three Core Stores

#### A. Provider Store (`providerStore.ts`)
**Responsibilities:**
- Provider data management
- Search functionality
- Regional center integration
- Provider selection

**State:**
- `providers: Provider[]`
- `selectedProvider: Provider | null`
- `regionalCenterInfo: RegionalCenterInfo | null`
- `loading: boolean`
- `error: string | null`

**Getters:**
- `providerCount`
- `providersWithCoordinates`
- `selectedProviderId`
- `hasProviders`

**Actions:**
- `searchByZipCode(zipCode, filters)`
- `searchByLocation(lat, lng, radius, filters)`
- `selectProvider(id)`
- `clearProviders()`

#### B. Map Store (`mapStore.ts`)
**Responsibilities:**
- Map viewport state
- UI panel visibility
- User location
- Directions

**State:**
- `viewport: MapViewport`
- `bounds: MapBounds | null`
- `userLocation: Coordinates | null`
- `uiState: MapUIState`
- `mapReady: boolean`

**Getters:**
- `currentCenter`
- `currentZoom`
- `hasUserLocation`
- `hasDirections`

**Actions:**
- `setViewport(viewport)`
- `centerOn(coords, zoom)`
- `setUserLocation(coords, accuracy)`
- `getDirectionsTo(coords, token)`
- `toggleUI(panel)`

#### C. Filter Store (`filterStore.ts`)
**Responsibilities:**
- Filter state management
- User data from onboarding
- Filter parameters building

**State:**
- `filterOptions: FilterOptions`
- `userData: UserData`
- `availableOptions: AvailableOptions`

**Getters:**
- `hasActiveFilters`
- `activeFilterCount`
- `filterParams` (for API calls)

**Actions:**
- `toggleFilter(name)`
- `updateUserData(data)`
- `resetFilters()`
- `applyOnboardingFilters()`

### 3. Store Tests
**Target:** 90+ tests across 3 stores
- Provider store: ~35 tests
- Map store: ~30 tests
- Filter store: ~25 tests

### 4. Documentation
- Store usage guide
- Migration patterns
- Testing examples
- Best practices

---

## 🔄 Migration Strategy

### Phase 1: Install & Configure (1 hour)
1. Install Pinia
2. Configure in main.ts
3. Set up TypeScript
4. Verify DevTools

### Phase 2: Create Stores (3 hours)
1. Create providerStore.ts
2. Create mapStore.ts
3. Create filterStore.ts
4. Export from index.ts

### Phase 3: Write Tests (3 hours)
1. Set up Pinia testing utilities
2. Write provider store tests
3. Write map store tests
4. Write filter store tests

### Phase 4: Update Composables (2 hours)
1. Update composables to use stores
2. Maintain backward compatibility
3. Test integration
4. Update documentation

### Phase 5: Documentation (1 hour)
1. Create store usage guide
2. Document migration patterns
3. Update QUICK_REFERENCE.md
4. Create completion report

**Total Estimated Time:** 10 hours

---

## 🎨 Store Design Patterns

### Pattern 1: Setup Store (Composition API Style)
```typescript
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

export const useProviderStore = defineStore('provider', () => {
  // State
  const providers = ref<Provider[]>([]);
  const loading = ref(false);

  // Getters
  const providerCount = computed(() => providers.value.length);

  // Actions
  async function searchProviders(params) {
    loading.value = true;
    try {
      // Logic here
    } finally {
      loading.value = false;
    }
  }

  return {
    // State
    providers,
    loading,
    // Getters
    providerCount,
    // Actions
    searchProviders
  };
});
```

### Pattern 2: Store Testing
```typescript
import { setActivePinia, createPinia } from 'pinia';
import { describe, it, expect, beforeEach } from 'vitest';
import { useProviderStore } from '@/stores/providerStore';

describe('providerStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  it('should initialize with empty providers', () => {
    const store = useProviderStore();
    expect(store.providers).toEqual([]);
  });

  it('should search providers', async () => {
    const store = useProviderStore();
    await store.searchByZipCode('91769');
    expect(store.providers.length).toBeGreaterThan(0);
  });
});
```

### Pattern 3: Store Composition
```typescript
// In composable or component
import { useProviderStore } from '@/stores/providerStore';
import { useFilterStore } from '@/stores/filterStore';

export function useProviderSearch() {
  const providerStore = useProviderStore();
  const filterStore = useFilterStore();

  async function searchWithFilters(zipCode: string) {
    const filters = filterStore.filterParams;
    await providerStore.searchByZipCode(zipCode, filters);
  }

  return { searchWithFilters };
}
```

---

## 📊 Expected Metrics

### Code Metrics
| Metric | Target | Status |
|--------|--------|--------|
| Store files | 4 (3 stores + index) | 🔜 Pending |
| TypeScript lines | 800-1,000 | 🔜 Pending |
| Test files | 3 | 🔜 Pending |
| Total tests | 90+ | 🔜 Pending |
| Test pass rate | 100% | 🔜 Pending |

### Quality Metrics
| Metric | Target | Status |
|--------|--------|--------|
| Type safety | 100% | 🔜 Pending |
| Breaking changes | 0 | 🔜 Pending |
| DevTools integration | Yes | 🔜 Pending |
| Documentation | Comprehensive | 🔜 Pending |

---

## 🚧 Challenges & Solutions

### Challenge 1: Circular Dependencies
**Issue:** Stores might reference each other
**Solution:** Use store composition pattern, access stores within actions

### Challenge 2: Testing with Pinia
**Issue:** Need to set up Pinia for tests
**Solution:** Use `setActivePinia(createPinia())` in beforeEach

### Challenge 3: Migration Without Breaking Changes
**Issue:** Composables currently work standalone
**Solution:** Update composables to use stores, but keep exports the same

### Challenge 4: TypeScript Types
**Issue:** Complex type inference with Pinia
**Solution:** Explicit return types, use `storeToRefs` for reactivity

---

## 🔍 Quality Checklist

### Store Implementation
- [ ] Uses setup store pattern (Composition API style)
- [ ] Full TypeScript types
- [ ] Clear state/getters/actions separation
- [ ] Error handling in all actions
- [ ] Loading states for async operations
- [ ] Proper cleanup in actions

### Testing
- [ ] All actions tested
- [ ] All getters tested
- [ ] Error scenarios covered
- [ ] Loading states verified
- [ ] State mutations validated
- [ ] Integration between stores tested

### Documentation
- [ ] JSDoc comments on all methods
- [ ] Usage examples provided
- [ ] Type interfaces exported
- [ ] Migration guide written
- [ ] Best practices documented

---

## 📚 Resources

### Pinia Documentation
- [Getting Started](https://pinia.vuejs.org/getting-started.html)
- [Setup Stores](https://pinia.vuejs.org/core-concepts/#setup-stores)
- [Testing](https://pinia.vuejs.org/cookbook/testing.html)
- [TypeScript](https://pinia.vuejs.org/cookbook/migration-v1-v2.html#typescript)

### Related Documentation
- `/docs/MAPVIEW_REFACTOR_PLAN.md` - Week 3 section
- `/docs/COMPOSABLES_INTEGRATION_GUIDE.md` - Composable patterns
- `/map-frontend/src/composables/README.md` - Composable API

---

## 🎯 Success Definition

Week 3 will be considered successful when:

1. ✅ **Pinia is installed and configured**
   - DevTools working
   - TypeScript configured
   - No console errors

2. ✅ **Three stores are production-ready**
   - Full TypeScript types
   - All methods implemented
   - Error handling complete

3. ✅ **90+ tests passing**
   - Provider store tested
   - Map store tested
   - Filter store tested

4. ✅ **Zero breaking changes**
   - Composables still work
   - MapView.vue unchanged
   - All existing tests pass

5. ✅ **Documentation complete**
   - Store usage guide
   - Migration patterns
   - API reference

---

## 🚀 Let's Begin!

**Next Steps:**
1. Install Pinia
2. Configure in main.ts
3. Create first store (providerStore)
4. Write tests
5. Repeat for other stores

**Expected Duration:** 10 hours
**Confidence Level:** High ⭐⭐⭐⭐⭐

Let's build a rock-solid state management layer! 🎊
