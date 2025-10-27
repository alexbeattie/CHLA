# Frontend Update: Regional Center-Based Provider Filtering

## Problem

Currently, when searching by ZIP code (e.g., 91769 Pomona), the map shows:
- ❌ ALL providers within 25-mile radius (~100+ providers)
- ❌ Regional center boundary is not visually highlighted

**What we want:**
- ✅ ONLY providers within the regional center's service area (~64 providers for San Gabriel/Pomona)
- ✅ Regional center polygon/boundary visually highlighted on map
- ✅ Accurate count of providers in that specific regional center

---

## Solution Overview

### Backend (✅ COMPLETED - commit 87b5d42)

New API endpoint created:
```
GET /api/providers-v2/by_regional_center/?zip_code=91769
```

**Response:**
```json
{
  "count": 64,
  "regional_center": {
    "id": 20,
    "name": "San Gabriel/Pomona Regional Center",
    "zip_codes": ["91766", "91767", "91768", "91769", ...]
  },
  "results": [
    // Array of providers in this regional center
  ]
}
```

### Frontend (⏳ TODO)

Need to update `/Users/alexbeattie/Developer/CHLA/map-frontend/src/views/MapView.vue`

---

## Frontend Changes Needed

### Change #1: Update fetchProviders() Method

**Location:** `MapView.vue` lines 2324-2491

**Current Code:**
```javascript
async fetchProviders() {
    let queryParams = new URLSearchParams();

    // Current: Uses radius-based filtering
    if (searchLat && searchLng) {
        queryParams.append("lat", searchLat);
        queryParams.append("lng", searchLng);
        const searchRadius = this.radius || 25; // 25 mile radius
        queryParams.append("radius", searchRadius);
    }

    // ... filters ...

    const url = `${this.getApiRoot()}/api/providers-v2/comprehensive_search/?${queryParams.toString()}`;
    const response = await axios.get(url);
    this.providers = response.data;
}
```

**New Code:**
```javascript
async fetchProviders() {
    // Check if we have a ZIP code search
    const searchZip = this.searchLocation;  // Or however ZIP is stored

    // First, try to get regional center for this ZIP
    let regionalCenter = null;
    if (searchZip && /^\d{5}$/.test(searchZip)) {
        try {
            const rcResponse = await axios.get(
                `${this.getApiRoot()}/api/regional-centers/by_zip_code/?zip_code=${searchZip}`
            );
            regionalCenter = rcResponse.data;

            // Store for highlighting
            this.currentRegionalCenter = regionalCenter;
        } catch (e) {
            console.log('No regional center found for ZIP, falling back to radius search');
        }
    }

    // If we found a regional center, use RC-based filtering
    if (regionalCenter && regionalCenter.id) {
        let queryParams = new URLSearchParams();
        queryParams.append("zip_code", searchZip);

        // Apply additional filters
        if (this.filterOptions.acceptsInsurance) {
            queryParams.append("insurance", "insurance");
        }
        if (this.filterOptions.acceptsRegionalCenter) {
            queryParams.append("insurance", "regional center");
        }
        if (this.filterOptions.acceptsPrivatePay) {
            queryParams.append("insurance", "private pay");
        }

        // ... other filters (age, diagnosis, therapy) ...

        const url = `${this.getApiRoot()}/api/providers-v2/by_regional_center/?${queryParams.toString()}`;
        const response = await axios.get(url);

        this.providers = response.data.results;
        this.providerCount = response.data.count;
        this.regionalCenterInfo = response.data.regional_center;

        // Update map bounds to regional center
        this.highlightRegionalCenter(regionalCenter);
    } else {
        // Fallback: Use existing radius-based search
        // ... existing code ...
    }
}
```

### Change #2: Add Regional Center Highlighting

**Location:** Add new method to `MapView.vue`

```javascript
highlightRegionalCenter(regionalCenter) {
    if (!this.map || !regionalCenter) return;

    // Remove existing highlight if any
    if (this.map.getLayer('highlighted-rc-fill')) {
        this.map.removeLayer('highlighted-rc-fill');
    }
    if (this.map.getLayer('highlighted-rc-outline')) {
        this.map.removeLayer('highlighted-rc-outline');
    }
    if (this.map.getSource('highlighted-rc')) {
        this.map.removeSource('highlighted-rc');
    }

    // Get the regional center's service area geometry
    // Option 1: If service_area geometry is available
    if (regionalCenter.service_area_geojson) {
        this.map.addSource('highlighted-rc', {
            type: 'geojson',
            data: regionalCenter.service_area_geojson
        });
    }
    // Option 2: Use ZIP code coloring (existing approach)
    else if (regionalCenter.zip_codes) {
        // Highlight the ZIPs that belong to this regional center
        this.highlightRegionalCenterZips(regionalCenter.zip_codes);
        return;
    }

    // Add fill layer
    this.map.addLayer({
        id: 'highlighted-rc-fill',
        type: 'fill',
        source: 'highlighted-rc',
        paint: {
            'fill-color': '#4caf50',  // Green for San Gabriel, customize per RC
            'fill-opacity': 0.3
        }
    });

    // Add outline layer
    this.map.addLayer({
        id: 'highlighted-rc-outline',
        type: 'line',
        source: 'highlighted-rc',
        paint: {
            'line-color': '#2e7d32',
            'line-width': 3
        }
    });
}

highlightRegionalCenterZips(zipCodes) {
    // This uses the existing ZIP code layer approach
    // Update the paint property to highlight specific ZIPs

    if (!this.map.getLayer('la-zip-codes-colored-fill')) {
        console.warn('ZIP layer not loaded yet');
        return;
    }

    // Create a match expression to highlight the RC's ZIPs
    const zipSet = new Set(zipCodes);

    // Update the fill layer to show highlighted ZIPs more prominently
    this.map.setPaintProperty('la-zip-codes-colored-fill', 'fill-opacity', [
        'case',
        ['in', ['get', 'ZIP'], ['literal', Array.from(zipSet)]],
        0.5,  // Highlighted ZIPs more opaque
        0.1   // Other ZIPs faded
    ]);

    // Update outline for highlighted ZIPs
    this.map.setPaintProperty('la-zip-codes-colored-outline', 'line-width', [
        'case',
        ['in', ['get', 'ZIP'], ['literal', Array.from(zipSet)]],
        2.5,  // Highlighted ZIPs thicker outline
        0.5   // Other ZIPs thin outline
    ]);
}
```

### Change #3: Add UI Feedback

**Location:** Update provider count display

```javascript
// In template
<div v-if="regionalCenterInfo" class="regional-center-info">
    <h3>{{ regionalCenterInfo.name }}</h3>
    <p>Showing {{ providerCount }} providers in this regional center</p>
    <button @click="clearRegionalCenterFilter">Show all providers (radius search)</button>
</div>
```

```javascript
// In methods
clearRegionalCenterFilter() {
    this.currentRegionalCenter = null;
    this.regionalCenterInfo = null;

    // Reset ZIP highlighting
    if (this.map.getLayer('la-zip-codes-colored-fill')) {
        this.map.setPaintProperty('la-zip-codes-colored-fill', 'fill-opacity', 0.2);
        this.map.setPaintProperty('la-zip-codes-colored-outline', 'line-width', 1.2);
    }

    // Re-run search with radius-based filtering
    this.fetchProviders();
}
```

---

## Testing Checklist

After implementing frontend changes:

### Test ZIP 91769 (Pomona)
- [ ] Search for ZIP 91769
- [ ] Should call `/api/providers-v2/by_regional_center/?zip_code=91769`
- [ ] Should return ~64 providers (not 100+)
- [ ] Should highlight San Gabriel/Pomona RC ZIPs on map
- [ ] Should show regional center name in UI
- [ ] Provider markers should only be within RC ZIPs

### Test ZIP 91101 (Pasadena)
- [ ] Search for ZIP 91101
- [ ] Should call `/api/providers-v2/by_regional_center/?zip_code=91101`
- [ ] Should return providers in Eastern LA RC
- [ ] Should highlight Eastern LA RC ZIPs
- [ ] Filtering by insurance/therapy should work

### Test Fallback
- [ ] Search for a ZIP without regional center mapping
- [ ] Should fall back to radius-based search
- [ ] Should still show providers within 25 miles

### Test Filters
- [ ] Regional Center filter should work with new endpoint
- [ ] Insurance filter should work
- [ ] Therapy type filter should work
- [ ] Age filter should work

---

## File Locations

**Backend (✅ Done):**
- `/Users/alexbeattie/Developer/CHLA/maplocation/locations/views.py` (lines 1255-1357)

**Frontend (⏳ TODO):**
- `/Users/alexbeattie/Developer/CHLA/map-frontend/src/views/MapView.vue`
  - Line ~2400: `fetchProviders()` method
  - Add new methods: `highlightRegionalCenter()`, `highlightRegionalCenterZips()`, `clearRegionalCenterFilter()`
  - Update template to show regional center info

**Data Files:**
- `/Users/alexbeattie/Developer/CHLA/map-frontend/src/assets/all-zip-codes-los-angeles.geojson` (existing ZIP boundaries)

---

## Regional Center Colors (For Highlighting)

Use these colors to match existing ZIP coloring:

| Regional Center | Color Code | Color Name |
|----------------|------------|------------|
| San Gabriel/Pomona | `#4caf50` | Green |
| Eastern LA (Pasadena) | `#ff9800` | Orange |
| North LA | `#f1c40f` | Yellow |
| Harbor | `#00bcd4` | Cyan |
| Westside | `#e91e63` | Pink |
| South Central LA | `#f44336` | Red |
| Frank D. Lanterman | `#9c27b0` | Purple |

---

## Benefits

1. **Accurate Results** - Users see only providers in their regional center
2. **Better UX** - Clear visual indication of regional center boundary
3. **Fewer Markers** - Map is less cluttered (64 vs 100+)
4. **Faster Performance** - Smaller result set to render
5. **RC-Specific Info** - Can show RC contact info, services, etc.

---

## Alternative: Quick Fix (Simpler Approach)

If full frontend refactor is too complex, you can use this simpler approach:

**Just change the API call:**

```javascript
// In fetchProviders(), replace:
const url = `${this.getApiRoot()}/api/providers-v2/comprehensive_search/?${queryParams.toString()}`;

// With:
const searchZip = this.searchLocation;
const url = /^\d{5}$/.test(searchZip)
    ? `${this.getApiRoot()}/api/providers-v2/by_regional_center/?zip_code=${searchZip}&${queryParams.toString()}`
    : `${this.getApiRoot()}/api/providers-v2/comprehensive_search/?${queryParams.toString()}`;
```

This one-line change will:
- ✅ Use regional center filtering for ZIP searches
- ✅ Fall back to radius search for address searches
- ❌ Won't highlight the regional center (but you can add that later)

---

## Summary

- ✅ **Backend API ready** (commit 87b5d42)
- ⏳ **Frontend changes needed** (update MapView.vue)
- 📝 **Expected result**: ZIP 91769 shows 64 providers in San Gabriel/Pomona RC only
- 🎨 **Visual improvement**: Highlighted regional center boundary on map
