# Quick Reference Guide - MapView Refactoring

**Last Updated:** October 26, 2025
**Status:** Week 2 Complete ✅
**Next:** Week 3 - Pinia Stores

---

## 📁 Project Structure

```
CHLA/
├── maplocation/                    # Django backend
│   ├── locations/
│   │   ├── views.py               # API endpoints
│   │   ├── models.py              # RegionalCenter, ProviderV2
│   │   └── management/commands/   # ZIP population commands
│   └── data/                      # Excel provider imports
│
├── map-frontend/                   # Vue 3 frontend
│   ├── src/
│   │   ├── composables/           # ✨ NEW: Reusable business logic
│   │   │   ├── useProviderSearch.ts
│   │   │   ├── useFilterState.ts
│   │   │   ├── useMapState.ts
│   │   │   ├── useRegionalCenter.ts
│   │   │   ├── index.ts
│   │   │   └── README.md          # ✨ API Reference
│   │   │
│   │   ├── tests/                 # ✨ NEW: Unit tests
│   │   │   ├── setup.ts
│   │   │   └── composables/
│   │   │       ├── useProviderSearch.spec.ts
│   │   │       ├── useFilterState.spec.ts
│   │   │       ├── useMapState.spec.ts
│   │   │       └── useRegionalCenter.spec.ts
│   │   │
│   │   ├── utils/
│   │   │   └── map/               # Week 1: Utilities
│   │   │       ├── coordinates.ts
│   │   │       ├── geocoding.ts
│   │   │       └── index.ts
│   │   │
│   │   └── views/
│   │       └── MapView.vue        # 6,681 lines (to be refactored)
│   │
│   ├── vite.config.js             # ✨ Updated: Test config
│   └── package.json               # ✨ Updated: Test scripts
│
└── docs/                           # Comprehensive documentation
    ├── MAPVIEW_REFACTOR_PLAN.md   # 5-week roadmap
    ├── COMPOSABLES_INTEGRATION_GUIDE.md  # How to integrate
    ├── WEEK_2_COMPLETION.md       # Week 2 report
    ├── SESSION_SUMMARY.md         # Full history
    ├── TECHNICAL_DEBT.md          # Known issues
    ├── QUICK_REFERENCE.md         # ✨ This file
    └── FRONTEND_RC_FILTERING.md   # RC filtering guide
```

---

## 🚀 Quick Start Commands

### Development
```bash
# Backend (Django)
cd maplocation
source venv/bin/activate
python manage.py runserver

# Frontend (Vue)
cd map-frontend
npm run dev

# Tests
cd map-frontend
npm test                    # Run all tests
npm run test:ui             # Interactive UI
npm run test:coverage       # With coverage
```

### Git Workflow
```bash
# Check status
git status
git log --oneline -10

# Commit pattern
git add .
git commit -m "feat: description

Details here

🤖 Generated with Claude Code
Co-Authored-By: Claude <noreply@anthropic.com>"

git push origin main
```

---

## 📚 Documentation Index

### For Developers

| Document | Purpose | When to Read |
|----------|---------|--------------|
| **MAPVIEW_REFACTOR_PLAN.md** | 5-week roadmap | Starting refactoring |
| **composables/README.md** | API reference | Using composables |
| **COMPOSABLES_INTEGRATION_GUIDE.md** | Integration patterns | Integrating composables |
| **TECHNICAL_DEBT.md** | Known issues | Before deploying |

### For Management

| Document | Purpose |
|----------|---------|
| **WEEK_2_COMPLETION.md** | Sprint report |
| **SESSION_SUMMARY.md** | Full history |
| **QUICK_REFERENCE.md** | This file |

---

## 🎯 Refactoring Progress

### ✅ Week 1: Utilities (Complete)
**Status:** DONE
**Deliverables:**
- 3 utility files (408 lines)
- coordinates.ts, geocoding.ts, index.ts

### ✅ Week 2: Composables (Complete)
**Status:** DONE
**Deliverables:**
- 4 composables (1,143 lines)
- 117 tests (100% passing)
- 900+ lines documentation

### 🔜 Week 3: Pinia Stores (Next)
**Status:** PLANNED
**Tasks:**
- Install Pinia
- Create 3 stores
- Migrate state
- Write tests

### 🔜 Week 4: Components (Planned)
**Status:** PLANNED
**Tasks:**
- Extract UI components
- Create component tests
- Integrate with stores

### 🔜 Week 5: Integration (Planned)
**Status:** PLANNED
**Tasks:**
- Remove old code
- Final testing
- Performance optimization
- Production deployment

---

## 🧩 Composables Quick Reference

### useProviderSearch
**Purpose:** Provider fetching and search
**Key Methods:**
- `searchByZipCode(zipCode, filters)`
- `searchByLocation(lat, lng, radius, filters)`
- `clearSearch()`

**State:**
- `providers` - Array of providers
- `loading` - Loading state
- `error` - Error message
- `providerCount` - Number of providers

### useFilterState
**Purpose:** Filter state management
**Key Methods:**
- `toggleFilter(filterName)`
- `updateUserData(data)`
- `buildFilterParams()`
- `applyOnboardingFilters()`

**State:**
- `filterOptions` - Filter toggles
- `userData` - User onboarding data
- `hasActiveFilters` - Boolean
- `activeFilterCount` - Number

### useMapState
**Purpose:** Map viewport and UI state
**Key Methods:**
- `centerOn(coords, zoom)`
- `selectProvider(id)`
- `setUserLocation(coords, accuracy)`
- `getDirectionsTo(coords, token)`

**State:**
- `viewport` - Center, zoom, bearing, pitch
- `uiState` - Panel visibility
- `selectedProviderId` - Selected provider
- `userLocation` - User's location

### useRegionalCenter
**Purpose:** Regional center data
**Key Methods:**
- `findByZipCode(zipCode)`
- `generateApproximateBoundary(zipCodes, token)`
- `getHighlightGeoJSON()`
- `isZipInRegionalCenter(zipCode)`

**State:**
- `regionalCenters` - All RCs
- `currentRegionalCenter` - Current RC
- `regionalCenterBoundary` - Boundary polygon
- `regionalCenterName` - Name

---

## 🔌 API Endpoints

### Backend (Django)

**Regional Center Filtering:**
```
GET /api/providers-v2/by_regional_center/
  ?zip_code=91769
  &insurance=regional+center
  &age=3-5
  &diagnosis=Autism
```

**Comprehensive Search:**
```
GET /api/providers-v2/comprehensive_search/
  ?lat=34.0522
  &lng=-118.2437
  &radius=25
  &q=search+text
```

**Regional Center Lookup:**
```
GET /api/regional-centers/by_zip_code/?zip_code=91769
```

---

## 🧪 Testing Quick Reference

### Run Tests
```bash
npm test                    # All tests
npm test -- useProviderSearch  # Specific file
npm run test:ui             # Interactive mode
```

### Write Tests
```typescript
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { useProviderSearch } from '@/composables';
import axios from 'axios';

vi.mock('axios');

describe('useProviderSearch', () => {
  let composable;

  beforeEach(() => {
    vi.clearAllMocks();
    composable = useProviderSearch('http://localhost:8000');
  });

  it('should search by ZIP code', async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: { results: [...], count: 64 }
    });

    await composable.searchByZipCode('91769');

    expect(composable.providers.value.length).toBe(64);
  });
});
```

---

## 🔍 Common Tasks

### Add a New Composable
1. Create file in `/map-frontend/src/composables/`
2. Follow existing pattern (state → computed → methods → return)
3. Export from `index.ts`
4. Write tests in `/src/tests/composables/`
5. Update `composables/README.md`

### Use Composables in Component
```vue
<script setup lang="ts">
import { useProviderSearch, useFilterState } from '@/composables';

const apiUrl = import.meta.env.VITE_API_URL;
const providerSearch = useProviderSearch(apiUrl);
const filterState = useFilterState();

// Destructure for template
const { providers, loading } = providerSearch;
const { filterOptions, toggleFilter } = filterState;
</script>
```

### Debug Tests
```bash
npm run test:ui              # Interactive UI
npm test -- --reporter=verbose  # Detailed output
```

---

## 📊 Metrics Dashboard

### Code
- **Composables:** 4 files, 1,143 lines
- **Tests:** 117 tests, 100% passing
- **Utilities:** 3 files, 408 lines
- **MapView:** 6,681 lines (to be refactored)

### Quality
- **TypeScript:** 100%
- **Test Coverage:** High
- **Breaking Changes:** 0
- **Production Ready:** Yes ✅

### Documentation
- **Total Docs:** 7 files
- **Total Lines:** 2,000+
- **Guides:** 3
- **References:** 2

---

## ⚡ Performance Tips

### Composables
- Composables are lightweight (~1KB each)
- Computed properties are cached
- State updates trigger minimal re-renders
- No global state pollution

### Testing
- Tests run in ~50ms
- Parallel execution
- Fast feedback loop
- Hot reload in test:ui

---

## 🐛 Common Issues & Solutions

### Issue: Tests Failing
**Solution:** Check mocks are reset
```typescript
beforeEach(() => {
  vi.clearAllMocks();
});
```

### Issue: Reactivity Not Working
**Solution:** Use `.value` in script
```typescript
console.log(providers.value);  // ✅
console.log(providers);         // ❌
```

### Issue: Type Errors
**Solution:** Import types
```typescript
import type { Provider, SearchParams } from '@/composables';
```

### Issue: API Not Working
**Solution:** Check environment variables
```bash
# .env.development
VITE_API_URL=http://localhost:8000
VITE_MAPBOX_TOKEN=your-token
```

---

## 🎯 Success Criteria Checklist

### Week 2 ✅
- [x] 4 composables extracted
- [x] 117 tests passing
- [x] Full TypeScript types
- [x] Comprehensive documentation
- [x] Zero breaking changes
- [x] Production ready

### Week 3 🔜
- [ ] Pinia installed
- [ ] 3 stores created
- [ ] State migrated
- [ ] Store tests written
- [ ] Documentation updated

---

## 🔗 External Resources

- [Vue 3 Composition API](https://vuejs.org/guide/extras/composition-api-faq.html)
- [Vitest Documentation](https://vitest.dev/)
- [Pinia Documentation](https://pinia.vuejs.org/)
- [TypeScript Handbook](https://www.typescriptlang.org/docs/)
- [Mapbox GL JS API](https://docs.mapbox.com/mapbox-gl-js/api/)

---

## 📞 Getting Help

### Documentation
1. Check `/docs/` for guides
2. Read `composables/README.md` for API reference
3. Review test examples in `/src/tests/composables/`

### Testing
1. Run `npm run test:ui` for interactive debugging
2. Check test output for specific errors
3. Verify mocks are properly configured

### Composables
1. Review existing composable patterns
2. Check TypeScript types
3. Ensure proper return values

---

## 🎉 Recent Wins

✅ **Week 2 Complete** - Composables + tests + docs
✅ **117 Tests** - All passing, <50ms execution
✅ **Zero Breaking Changes** - Backward compatible
✅ **Production Ready** - Ready to integrate

---

**Next Session:** Week 3 - Pinia Store Setup
**Estimated Time:** 8-10 hours
**Confidence:** High ⭐⭐⭐⭐⭐
