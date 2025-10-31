# MapView.vue Refactoring Plan - Safe & Incremental

## Current State Analysis

**File:** `MapView.vue` (7,444 lines)
- Template: 498 lines (reasonable)
- Script: 5,538 lines (THE PROBLEM)
- Style: 1,406 lines (can be extracted to Tailwind later)
- Imports: 20 components

## The Problem

A 7,444-line file is:
- Hard to understand
- Difficult to test
- Slow to load in editor
- Easy to break
- Impossible to collaborate on

## Core Principle: **ONE SMALL CHANGE AT A TIME**

Each step:
1. Extract ONE piece
2. Test it works
3. Commit
4. Move to next piece

If anything breaks, rollback is ONE commit away.

---

## Phase 1: Analysis (Week 1, Day 1-2)

### Step 1.1: Map the Territory (2 hours)
**Goal:** Understand what's in the file without changing anything

**Tasks:**
- [ ] List all methods (how many?)
- [ ] List all computed properties
- [ ] List all data properties
- [ ] Identify dependencies between them
- [ ] Find which methods are only used once vs reused

**Output:** A map of what's in the file

**Command:**
```bash
# Count methods
grep -c "^    [a-zA-Z].*() {" src/views/MapView.vue

# List method names
grep "^    [a-zA-Z].*() {" src/views/MapView.vue | head -20

# Find computed properties
sed -n '/computed: {/,/^  }/p' src/views/MapView.vue | grep "^    [a-zA-Z]" | wc -l
```

### Step 1.2: Identify Safe Extraction Candidates (1 hour)
**Goal:** Find methods that can be extracted without breaking things

**Safe candidates are methods that:**
- ✅ Don't use `this.$refs` (DOM-dependent)
- ✅ Don't use `this.$router` or `this.$route` (routing-dependent)
- ✅ Are pure functions (input → output)
- ✅ Are utility functions (formatPhone, formatAddress, etc.)

**Unsafe candidates:**
- ❌ Methods using lifecycle hooks
- ❌ Methods accessing DOM directly
- ❌ Methods with complex component state dependencies

### Step 1.3: Create Test File (30 min)
**Goal:** Ensure we can test that the app still works

**Create:** `scripts/test-mapview.sh`
```bash
#!/bin/bash
echo "Testing MapView..."

# Test 1: File compiles
cd map-frontend && npm run build 2>&1 | grep -q "built in" && echo "✅ Build passes" || echo "❌ Build fails"

# Test 2: No linter errors
npx eslint src/views/MapView.vue 2>&1 | grep -q "0 problems" && echo "✅ No lint errors" || echo "⚠️ Has lint errors"

# Test 3: Dev server starts
timeout 30 npm run dev > /tmp/test.log 2>&1 &
sleep 10
curl -s http://localhost:3000 | grep -q "Map Location Finder" && echo "✅ App loads" || echo "❌ App fails to load"
pkill -f "vite.*development"

echo "Testing complete!"
```

**DON'T EXTRACT ANYTHING YET** - Just prepare.

---

## Phase 2: Extract Utilities (Week 1, Day 3-5)

### Step 2.1: Extract Format Functions (2 hours)
**Goal:** Move simple formatting functions to utils

**Target methods (EXAMPLES - we'll identify real ones):**
- `formatPhone(phone)` - Format phone numbers
- `formatAddress(address)` - Format addresses
- `formatDistance(distance)` - Format distances

**Process:**
1. Create `src/utils/formatting.js`
2. Copy ONE function
3. Export it
4. Import it in MapView
5. Replace usage
6. Test with `./scripts/test-mapview.sh`
7. Commit: "refactor: Extract formatPhone to utils"
8. Repeat for next function

**Example:**
```javascript
// src/utils/formatting.js
export function formatPhone(phone) {
  if (!phone) return '';
  const digits = phone.replace(/\D/g, '');
  if (digits.length === 10) {
    return `(${digits.slice(0, 3)}) ${digits.slice(3, 6)}-${digits.slice(6)}`;
  }
  return phone;
}

// In MapView.vue
import { formatPhone } from '@/utils/formatting'

// Replace: this.formatPhone(phone)
// With: formatPhone(phone)
```

**Testing after each:**
```bash
./scripts/test-mapview.sh
# If passes: git commit
# If fails: git reset --hard
```

### Step 2.2: Extract Validation Functions (2 hours)
**Goal:** Move validation logic to utils

**Target methods:**
- ZIP code validation
- Email validation
- Phone validation

**Same process as 2.1**

### Step 2.3: Extract Calculation Functions (2 hours)
**Goal:** Move pure calculations to utils

**Target methods:**
- Distance calculations
- Coordinate transformations
- Data filtering logic

---

## Phase 3: Extract Composables (Week 2)

### Step 3.1: Extract Geolocation Logic (4 hours)
**Goal:** Move geolocation into composable

**Target:** All `geolocation` related code

**Create:** `src/composables/useGeolocation.js` (probably already exists - check first!)

**Process:**
1. Check if `useGeolocation` exists
2. If yes, ensure MapView uses it properly
3. If no, create it with ONE piece at a time
4. Test after each piece

### Step 3.2: Extract Filter Logic (4 hours)
**Goal:** Move filter state and logic to composable/store

**Current:** Filter logic scattered in MapView
**Target:** `src/composables/useFilters.js` or use existing Pinia store

### Step 3.3: Extract Search Logic (4 hours)
**Goal:** Consolidate search functionality

---

## Phase 4: Extract UI Components (Week 3)

### Step 4.1: Extract Individual Sections (1 hour each)
**Goal:** Turn large template sections into components

**IMPORTANT:** Don't extract everything at once!

**One section at a time:**

1. **UserProfileSection.vue** (if not already component)
   - The profile card at top of sidebar
   - Test: Does profile still display?
   - Commit

2. **LocationNotice.vue** 
   - The "no location detected" notice
   - Test: Does notice still show?
   - Commit

3. **FilterSection.vue** (if not FilterPanel)
   - Filter checkboxes and controls
   - Test: Do filters still work?
   - Commit

**Each extraction:**
```vue
<!-- Before in MapView.vue -->
<div class="profile-section">
  <!-- 50 lines of profile UI -->
</div>

<!-- After in MapView.vue -->
<user-profile-section 
  :profile="userData" 
  @edit="handleEditProfile" 
/>

<!-- New file: UserProfileSection.vue -->
<template>
  <div class="profile-section">
    <!-- same 50 lines -->
  </div>
</template>
```

---

## Phase 5: Reduce CSS (Week 4)

### Step 5.1: Audit Current CSS (2 hours)
**Goal:** Understand what CSS is actually used

```bash
# Find unused CSS classes
grep -o "class=\"[^\"]*\"" src/views/MapView.vue | sort -u > /tmp/classes-used.txt
grep -o "\.[a-z-]*" src/views/MapView.vue | sort -u > /tmp/classes-defined.txt
```

### Step 5.2: Extract Common Styles (2 hours)
**Goal:** Move reusable styles to global or component files

### Step 5.3: Remove Duplicate Styles (2 hours)
**Goal:** Consolidate repeated styles

---

## Testing Strategy

### After EVERY Change:

1. **Visual Test** (30 seconds)
   ```bash
   npm run dev
   # Open http://localhost:3000
   # Click around, test basic features
   ```

2. **Build Test** (1 minute)
   ```bash
   npm run build
   # Should succeed
   ```

3. **Quick Test Script** (1 minute)
   ```bash
   ./scripts/test-mapview.sh
   ```

### Before Committing:

```bash
# 1. Check linter
npm run lint src/views/MapView.vue

# 2. Check types (if using TypeScript)
# npm run type-check

# 3. Test in browser manually

# 4. Commit with clear message
git add .
git commit -m "refactor: Extract formatPhone utility

- Move formatPhone from MapView to utils/formatting.js
- No functional changes
- All tests passing
- Lines reduced: 7444 → 7430"
```

---

## Success Metrics

### After Phase 2 (Utilities):
- **Target:** 7,444 → 7,200 lines (-244)
- **Benefit:** Testable utility functions
- **Risk:** Low (pure functions)

### After Phase 3 (Composables):
- **Target:** 7,200 → 6,500 lines (-700)
- **Benefit:** Reusable logic
- **Risk:** Medium (state management)

### After Phase 4 (Components):
- **Target:** 6,500 → 4,500 lines (-2,000)
- **Benefit:** Maintainable UI pieces
- **Risk:** Medium (component boundaries)

### After Phase 5 (CSS):
- **Target:** 4,500 → 3,500 lines (-1,000)
- **Benefit:** Cleaner styles
- **Risk:** Low (visual only)

**Final Target:** 3,500 lines (53% reduction)

---

## Emergency Procedures

### If Something Breaks:

1. **Don't panic** - You have git!

2. **Check what changed:**
   ```bash
   git diff
   ```

3. **Rollback the last change:**
   ```bash
   git reset --hard HEAD~1
   ```

4. **Restart server:**
   ```bash
   pkill node
   cd map-frontend && npm run dev
   ```

5. **Test again:**
   - Open http://localhost:3000
   - If works: you rolled back successfully
   - If broken: roll back one more: `git reset --hard HEAD~2`

### If Completely Lost:

```bash
# Nuclear option - go back to known good state
git log --oneline -20
# Find the commit BEFORE you started refactoring
git reset --hard <commit-hash>
```

---

## Rules of Engagement

### DO:
✅ Extract ONE thing at a time
✅ Test after EVERY change
✅ Commit after EVERY successful extraction
✅ Write clear commit messages
✅ Keep changes small (<100 lines per commit)
✅ Document what you're doing

### DON'T:
❌ Extract multiple things at once
❌ Change logic while extracting
❌ Refactor + add features at same time
❌ Skip testing
❌ Make commits with "WIP" or "fix stuff"
❌ Touch working code unless extracting it

---

## Starting Point Checklist

Before you start ANY extraction:

- [ ] App works perfectly right now
- [ ] Server runs without errors
- [ ] You can load the map
- [ ] You can search for providers
- [ ] All features work
- [ ] Git status is clean (`git status`)
- [ ] You've committed current working state

**If ANY of above is NO, fix it FIRST before refactoring!**

---

## Timeline Estimate

- **Phase 1 (Analysis):** 2 days
- **Phase 2 (Utilities):** 3 days
- **Phase 3 (Composables):** 5 days
- **Phase 4 (Components):** 5 days
- **Phase 5 (CSS):** 3 days

**Total:** ~18 days of actual work (3-4 weeks calendar time)

**But:** You can stop after ANY phase and still have improvements!

---

## Next Steps

1. **Review this plan** - Does it make sense?
2. **Run Phase 1.1** - Map what's in the file
3. **Identify first extraction target** - Start small!
4. **Create test script** - Ensure you can verify changes
5. **Start with ONE utility function** - The simplest one

**Want to proceed?** Let's start with Phase 1.1 - mapping what's actually in MapView.vue
