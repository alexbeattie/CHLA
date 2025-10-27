# Week 4 Kickoff: Component Extraction

**Date:** October 26, 2025
**Status:** 🟢 Ready to Start
**Prerequisites:** ✅ Week 1-3 Complete (221 tests passing)

---

## 🎯 Week 4 Goal

Break down the 6,681-line `MapView.vue` into focused, testable sub-components while maintaining full functionality. Each component will use Pinia stores directly for state management.

**Target:** Reduce `MapView.vue` from 6,681 lines to ~500 lines (orchestration only)

---

## 📋 Current State

### What We Have ✅
- **Utilities** (Week 1): Geocoding, coordinates, distance calculations
- **Composables** (Week 2): useProviderSearch, useMapState, useFilterState, useRegionalCenter
- **Stores** (Week 3): providerStore, mapStore, filterStore
- **Tests:** 221 passing tests with 100% coverage

### What MapView.vue Currently Does
The 6,681-line component handles:
1. Map rendering (Mapbox GL)
2. Search functionality (ZIP and location)
3. Filter management
4. Provider list display
5. Provider detail panels
6. Directions and routing
7. User location tracking
8. Onboarding flow
9. UI state management
10. Event handling

### The Problem
All this functionality is in one massive file, making it:
- ❌ Hard to understand
- ❌ Difficult to test
- ❌ Risky to modify
- ❌ Performance issues
- ❌ Poor code reusability

---

## 🎨 Component Architecture

### New Component Structure

```
src/components/map/
├── MapView.vue (500 lines - orchestration only)
├── map/
│   ├── MapCanvas.vue          # Mapbox GL map container
│   └── UserLocationMarker.vue # User location display
├── search/
│   ├── SearchBar.vue           # Search input with autocomplete
│   └── SearchResults.vue       # Results counter and status
├── providers/
│   ├── ProviderList.vue        # Scrollable provider list
│   ├── ProviderCard.vue        # Individual provider card
│   ├── ProviderDetails.vue     # Detailed provider info panel
│   └── ProviderMarker.vue      # Map marker for provider
├── filters/
│   ├── FilterPanel.vue         # Filter controls panel
│   └── FilterChip.vue          # Individual filter chip
├── directions/
│   ├── DirectionsPanel.vue     # Directions display
│   └── DirectionsRoute.vue     # Route visualization
└── onboarding/
    ├── OnboardingFlow.vue      # Multi-step onboarding
    └── OnboardingStep.vue      # Individual step component
```

---

## 📦 Component Specifications

### 1. MapCanvas.vue (Priority: HIGH)
**Purpose:** Render the Mapbox GL map
**Lines:** ~200
**Responsibilities:**
- Initialize Mapbox GL map
- Handle map events (move, zoom, click)
- Render provider markers
- Render user location
- Render directions route

**Props:**
- `providers: Provider[]`
- `selectedProviderId: number | null`
- `userLocation: Coordinates | null`
- `directionsRoute: Route | null`
- `viewport: MapViewport`

**Events:**
- `@update:viewport`
- `@marker-click`
- `@map-ready`

**Store Usage:**
- `mapStore` - viewport, bounds, map ready state
- `providerStore` - providers data

**Tests:** 15 tests
- Map initialization
- Viewport updates
- Marker rendering
- Event handling
- User location display

---

### 2. SearchBar.vue (Priority: HIGH)
**Purpose:** Search input with ZIP/location search
**Lines:** ~150
**Responsibilities:**
- Search input field
- ZIP code validation
- Location autocomplete
- Search button
- Loading state display

**Props:**
- `loading: boolean`
- `placeholder: string`

**Events:**
- `@search`
- `@clear`

**Store Usage:**
- `providerStore` - searchByZipCode, searchByLocation
- `mapStore` - setUserLocation

**Tests:** 12 tests
- Input validation
- ZIP search
- Location search
- Clear functionality
- Loading states

---

### 3. ProviderList.vue (Priority: HIGH)
**Purpose:** Scrollable list of provider results
**Lines:** ~180
**Responsibilities:**
- Display provider cards
- Handle scrolling
- Show empty state
- Show loading state
- Handle selection

**Props:**
- `providers: Provider[]`
- `selectedProviderId: number | null`
- `loading: boolean`

**Events:**
- `@select-provider`
- `@scroll-to-provider`

**Store Usage:**
- `providerStore` - providers, selectProvider
- `filterStore` - filterParams

**Tests:** 10 tests
- List rendering
- Selection handling
- Empty state
- Loading state
- Scroll behavior

---

### 4. ProviderCard.vue (Priority: MEDIUM)
**Purpose:** Individual provider card in list
**Lines:** ~120
**Responsibilities:**
- Display provider info
- Show distance
- Handle click
- Show selected state
- Display insurance badges

**Props:**
- `provider: Provider`
- `selected: boolean`
- `distance: number | null`

**Events:**
- `@click`

**Store Usage:**
- None (presentational component)

**Tests:** 8 tests
- Rendering
- Click handling
- Selected state
- Distance display
- Badge display

---

### 5. ProviderDetails.vue (Priority: MEDIUM)
**Purpose:** Detailed provider information panel
**Lines:** ~200
**Responsibilities:**
- Display full provider details
- Show contact information
- Display services
- Get directions button
- Close button

**Props:**
- `provider: Provider`

**Events:**
- `@close`
- `@get-directions`

**Store Usage:**
- `mapStore` - getDirectionsTo

**Tests:** 10 tests
- Detail rendering
- Contact info display
- Get directions
- Close handling
- Service display

---

### 6. FilterPanel.vue (Priority: MEDIUM)
**Purpose:** Filter controls and management
**Lines:** ~150
**Responsibilities:**
- Display filter toggles
- Handle filter changes
- Show active filter count
- Reset filters
- Apply filters

**Props:**
- None (uses store directly)

**Events:**
- `@filter-change`
- `@apply-filters`

**Store Usage:**
- `filterStore` - all filter state and actions

**Tests:** 12 tests
- Filter toggling
- Mutual exclusivity
- Reset functionality
- Apply filters
- Active count

---

### 7. DirectionsPanel.vue (Priority: LOW)
**Purpose:** Display directions information
**Lines:** ~100
**Responsibilities:**
- Show route distance
- Show route duration
- Display turn-by-turn
- Close directions

**Props:**
- `route: Route | null`
- `from: Coordinates`
- `to: Coordinates`

**Events:**
- `@close`

**Store Usage:**
- `mapStore` - clearDirections

**Tests:** 8 tests
- Route display
- Distance formatting
- Duration formatting
- Close handling

---

### 8. OnboardingFlow.vue (Priority: LOW)
**Purpose:** Multi-step user onboarding
**Lines:** ~180
**Responsibilities:**
- Display onboarding steps
- Handle step navigation
- Collect user data
- Apply onboarding filters

**Props:**
- None (uses store directly)

**Events:**
- `@complete`
- `@skip`

**Store Usage:**
- `filterStore` - updateUserData, applyOnboardingFilters

**Tests:** 10 tests
- Step navigation
- Data collection
- Complete handling
- Skip handling
- Filter application

---

## 🗓️ Week 4 Timeline

### Day 1-2: Map Components (6 hours)
**Priority: HIGH**
1. Create `MapCanvas.vue`
   - Extract map initialization logic
   - Handle viewport updates
   - Render markers
   - Write 15 tests
2. Create `UserLocationMarker.vue`
   - Display user location
   - Handle accuracy circle
   - Write 5 tests

**Deliverable:** Working map with markers ✅

---

### Day 3-4: Search & List Components (8 hours)
**Priority: HIGH**
1. Create `SearchBar.vue`
   - Search input
   - ZIP validation
   - Write 12 tests
2. Create `ProviderList.vue`
   - Scrollable list
   - Loading states
   - Write 10 tests
3. Create `ProviderCard.vue`
   - Card display
   - Selection handling
   - Write 8 tests

**Deliverable:** Search and list working ✅

---

### Day 4-5: Details & Filters (6 hours)
**Priority: MEDIUM**
1. Create `ProviderDetails.vue`
   - Detail panel
   - Contact info
   - Write 10 tests
2. Create `FilterPanel.vue`
   - Filter controls
   - Apply/reset
   - Write 12 tests

**Deliverable:** Details and filtering working ✅

---

### Day 5: Optional Components (4 hours)
**Priority: LOW**
1. Create `DirectionsPanel.vue` (optional)
   - Route display
   - Write 8 tests
2. Create `OnboardingFlow.vue` (optional)
   - Onboarding wizard
   - Write 10 tests

**Deliverable:** Full feature parity ✅

---

### Day 5: Integration & Testing (4 hours)
1. Update `MapView.vue` to use all components
2. Remove old code
3. Run full test suite
4. Manual testing
5. Performance check

**Deliverable:** 6,681 → ~500 lines ✅

**Total Estimated Time:** 24-28 hours (1 week at 4-5 hours/day)

---

## 🧪 Testing Strategy

### Component Tests
Each component will have:
- **Rendering tests** - Component displays correctly
- **Props tests** - Props are handled properly
- **Event tests** - Events are emitted correctly
- **Store integration tests** - Store actions work
- **Edge case tests** - Error states, empty states

### Integration Tests
- MapView orchestration works
- Component communication works
- Store updates propagate
- Events flow correctly

### Target Coverage
- **85%+ code coverage** on all new components
- **100% critical path coverage**
- All existing functionality preserved

---

## 🎯 Success Criteria

### Must Have ✅
1. MapView.vue reduced to ~500 lines
2. All functionality preserved
3. All 221+ tests passing
4. No performance regression
5. Clean component boundaries

### Nice to Have 🎁
1. Improved performance (smaller components)
2. Better code reusability
3. Easier to add new features
4. Component documentation
5. Storybook stories for components

---

## 🚧 Migration Strategy

### Phase 1: Extract Without Breaking (Days 1-3)
1. Create components in `src/components/map/`
2. Keep MapView.vue working
3. Test components in isolation
4. No changes to MapView.vue yet

### Phase 2: Integrate Components (Days 4-5)
1. Import components into MapView.vue
2. Replace template sections with components
3. Remove old template code
4. Test after each replacement

### Phase 3: Clean Up (Day 5)
1. Remove unused code from MapView.vue
2. Update imports
3. Final testing
4. Performance check

**Key Principle:** Always keep MapView.vue working! Test after every change.

---

## 🔍 Code Review Checklist

Before marking a component complete:

- [ ] Component is under 250 lines
- [ ] Has clear single responsibility
- [ ] Props are well-typed
- [ ] Events are documented
- [ ] Uses stores appropriately
- [ ] Has 10+ passing tests
- [ ] No console errors
- [ ] Accessible (ARIA labels)
- [ ] Responsive design
- [ ] Performance checked

---

## 📊 Expected Metrics

### Before Week 4
- MapView.vue: 6,681 lines
- Components: 0
- Test Coverage: Store & composable tests only
- Performance: Baseline

### After Week 4
- MapView.vue: ~500 lines (92% reduction!)
- Components: 8-10 new components
- Test Coverage: 85%+ on all components
- Performance: Improved (smaller reactive surface)

---

## 🎨 Component Design Patterns

### Pattern 1: Presentation vs Container
**Presentation Components:**
- ProviderCard.vue
- FilterChip.vue
- OnboardingStep.vue

**Container Components:**
- ProviderList.vue
- FilterPanel.vue
- OnboardingFlow.vue

### Pattern 2: Store Integration
Components should:
1. Import stores directly (not through composables)
2. Use computed refs for reactive state
3. Call store actions for mutations
4. Emit events for parent communication

```typescript
import { useProviderStore } from '@/stores';

export default {
  setup() {
    const providerStore = useProviderStore();

    const providers = computed(() => providerStore.providers);

    function handleSearch() {
      providerStore.searchByZipCode(zipCode.value);
    }

    return { providers, handleSearch };
  }
};
```

### Pattern 3: Event Communication
Components emit events up, parents handle them:

```vue
<!-- Child -->
<template>
  <button @click="$emit('select', provider.id)">Select</button>
</template>

<!-- Parent -->
<template>
  <ProviderCard @select="handleSelect" />
</template>
```

---

## 🚀 Benefits After Week 4

### Developer Experience
- **Faster development** - Work on small components
- **Easier debugging** - Isolated component issues
- **Better testing** - Test components independently
- **Code reusability** - Use components elsewhere

### Maintainability
- **Clear structure** - Easy to find code
- **Single responsibility** - Each component does one thing
- **Easier refactoring** - Change components independently
- **Better documentation** - Component-level docs

### Performance
- **Smaller reactive surface** - Only relevant data in each component
- **Better code splitting** - Lazy load components
- **Easier optimization** - Profile individual components

---

## 📝 Documentation Plan

### Component Documentation
Each component will have:
1. **README.md** - Purpose, usage, examples
2. **Props documentation** - Type, required, default
3. **Events documentation** - When emitted, payload
4. **Store usage** - Which stores, which actions
5. **Examples** - Code snippets

### API Documentation
- Component APIs in `docs/COMPONENTS.md`
- Store integration patterns
- Event flow diagrams
- Migration guide for existing code

---

## ⚠️ Potential Challenges

### Challenge 1: Component Communication
**Issue:** Components need to communicate
**Solution:** Use events + store updates, avoid prop drilling

### Challenge 2: Shared State
**Issue:** Multiple components need same data
**Solution:** Access stores directly, don't duplicate state

### Challenge 3: Testing Complexity
**Issue:** Components depend on stores
**Solution:** Use Pinia testing utilities, fresh store per test

### Challenge 4: Performance
**Issue:** Too many components might impact performance
**Solution:** Profile before/after, use lazy loading if needed

---

## 🎓 Learning Outcomes

By the end of Week 4, you'll have:
- ✅ Mastered Vue component extraction
- ✅ Learned Pinia store integration patterns
- ✅ Written comprehensive component tests
- ✅ Built reusable, maintainable components
- ✅ Improved application performance
- ✅ Reduced technical debt significantly

---

## 🔗 Related Documents

- [Week 3 Completion Report](./WEEK_3_COMPLETION.md)
- [MapView Refactor Plan](./MAPVIEW_REFACTOR_PLAN.md)
- [Quick Reference Guide](./QUICK_REFERENCE.md)
- [Store Architecture](../map-frontend/src/stores/README.md)

---

## 📋 Pre-Week 4 Checklist

Before starting Week 4:
- [x] Week 1-3 complete
- [x] All 221 tests passing
- [x] Stores working correctly
- [x] Git is clean
- [x] Documentation up to date
- [ ] MapView.vue analyzed for component boundaries
- [ ] Component structure planned
- [ ] Test strategy defined

---

**Week 4 Status:** 🟢 **READY TO START**
**Prerequisites:** ✅ **ALL MET**
**Estimated Duration:** 24-28 hours (1 week)

Let's break down that 6,681-line monster! 🚀
