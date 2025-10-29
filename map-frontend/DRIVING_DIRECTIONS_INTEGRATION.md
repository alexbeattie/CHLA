# Driving Directions Feature - Integration Guide

## What's Been Created

### 1. Mapbox Directions Service (`src/services/mapboxDirections.ts`)
- `getDrivingDirections()` - Get full route with turn-by-turn steps
- `getDrivingDistance()` - Get driving distance only (faster)
- Helper functions for formatting distances, durations, and maneuver icons

### 2. Updated ProviderCard Component
- Added "Directions" button next to distance
- Emits `get-directions` event when clicked
- Shows "driving" label on distance

### 3. DirectionsPanel Component (`src/components/map/DirectionsPanel.vue`)
- Embedded panel showing turn-by-turn directions
- Displays route summary (distance + time)
- Shows destination info
- Lists all steps with icons
- Responsive design (fullscreen on mobile)

## What Needs to Be Integrated in MapView

### Step 1: Import Dependencies

Add to top of MapView.vue `<script>`:

```typescript
import { getDrivingDirections, getDrivingDistance } from '@/services/mapboxDirections';
import DirectionsPanel from '@/components/map/DirectionsPanel.vue';
```

### Step 2: Add DirectionsPanel to Template

Add after the MapCanvas component:

```vue
<!-- Directions Panel -->
<directions-panel
  :visible="showDirections"
  :directions="currentDirections"
  :destination="directionsDestination"
  :loading="directionsLoading"
  :error="directionsError"
  @close="closeDirections"
  @retry="retryDirections"
/>
```

### Step 3: Add State Variables

Add to reactive state:

```typescript
const showDirections = ref(false);
const currentDirections = ref(null);
const directionsDestination = ref(null);
const directionsLoading = ref(false);
const directionsError = ref(null);
const directionsRoute = ref(null); // For map overlay
```

### Step 4: Replace Straight-Line Distance Calculation

Find where providers get distances calculated (likely in a computed or method).

**Before:**
```typescript
// Using Haversine formula
import { calculateDistance } from '@/utils/map/coordinates';
provider.distance = calculateDistance(userLat, userLng, providerLat, providerLng);
```

**After:**
```typescript
// Fetch driving distances for all providers
async function fetchDrivingDistances(providers, origin) {
  const promises = providers.map(async (provider) => {
    if (!provider.latitude || !provider.longitude) return provider;

    try {
      const distance = await getDrivingDistance(
        [origin.lng, origin.lat],
        [provider.longitude, provider.latitude]
      );
      return { ...provider, distance };
    } catch (error) {
      console.error(`Failed to get distance for provider ${provider.id}:`, error);
      // Fallback to straight-line distance
      const straightLine = calculateDistance(
        origin.lat, origin.lng,
        provider.latitude, provider.longitude
      );
      return { ...provider, distance: straightLine };
    }
  });

  return await Promise.all(promises);
}
```

### Step 5: Handle "Get Directions" Click

Add handler for ProviderCard's `get-directions` event:

```typescript
async function handleGetDirections(provider) {
  console.log('🗺️ Getting directions to:', provider.name);

  // Clear existing directions
  closeDirections();

  // Validate provider has coordinates
  if (!provider.latitude || !provider.longitude) {
    directionsError.value = 'Provider location not available';
    showDirections.value = true;
    return;
  }

  // Validate user location
  if (!userLocation.value) {
    directionsError.value = 'Your location is required for directions';
    showDirections.value = true;
    return;
  }

  // Show loading state
  showDirections.value = true;
  directionsLoading.value = true;
  directionsError.value = null;

  // Set destination info
  directionsDestination.value = {
    name: provider.name,
    address: formatProviderAddress(provider)
  };

  try {
    // Fetch directions
    const directions = await getDrivingDirections(
      [userLocation.value.lng, userLocation.value.lat],
      [provider.longitude, provider.latitude]
    );

    currentDirections.value = directions;
    directionsRoute.value = directions.route;

    // Draw route on map
    drawRouteOnMap(directions.route);

    // Fit map to show entire route
    fitMapToRoute(directions.route);

  } catch (error) {
    console.error('Error fetching directions:', error);
    directionsError.value = 'Could not calculate route. Please try again.';
  } finally {
    directionsLoading.value = false;
  }
}

function closeDirections() {
  showDirections.value = false;
  currentDirections.value = null;
  directionsDestination.value = null;
  directionsError.value = null;

  // Remove route from map
  removeRouteFromMap();
}

function retryDirections() {
  if (directionsDestination.value) {
    // Re-trigger directions for the same destination
    // You'll need to store the provider reference
    handleGetDirections(lastDirectionsProvider.value);
  }
}
```

### Step 6: Draw Route on Map

Add map overlay functions:

```typescript
function drawRouteOnMap(route) {
  if (!map.value) return;

  // Remove existing route if any
  removeRouteFromMap();

  // Add route source
  map.value.addSource('route', {
    type: 'geojson',
    data: {
      type: 'Feature',
      properties: {},
      geometry: route.geometry
    }
  });

  // Add route layer (background/outline)
  map.value.addLayer({
    id: 'route-outline',
    type: 'line',
    source: 'route',
    layout: {
      'line-join': 'round',
      'line-cap': 'round'
    },
    paint: {
      'line-color': '#1d4ed8',
      'line-width': 8,
      'line-opacity': 0.3
    }
  });

  // Add route layer (main line)
  map.value.addLayer({
    id: 'route',
    type: 'line',
    source: 'route',
    layout: {
      'line-join': 'round',
      'line-cap': 'round'
    },
    paint: {
      'line-color': '#2563eb',
      'line-width': 5
    }
  });
}

function removeRouteFromMap() {
  if (!map.value) return;

  if (map.value.getLayer('route')) {
    map.value.removeLayer('route');
  }
  if (map.value.getLayer('route-outline')) {
    map.value.removeLayer('route-outline');
  }
  if (map.value.getSource('route')) {
    map.value.removeSource('route');
  }
}

function fitMapToRoute(route) {
  if (!map.value || !route.geometry) return;

  const coordinates = route.geometry.coordinates;

  // Create bounds
  const bounds = coordinates.reduce((bounds, coord) => {
    return bounds.extend(coord);
  }, new mapboxgl.LngLatBounds(coordinates[0], coordinates[0]));

  // Fit map to bounds
  map.value.fitBounds(bounds, {
    padding: { top: 100, bottom: 100, left: 100, right: 500 }, // Account for directions panel
    duration: 1000
  });
}
```

### Step 7: Wire Up ProviderCard Event

Find where ProviderCard is rendered and add the event handler:

```vue
<provider-card
  v-for="provider in filteredProviders"
  :key="provider.id"
  :provider="provider"
  :distance="provider.distance"
  :selected="selectedProviderId === provider.id"
  @click="handleProviderClick"
  @select="handleProviderSelect"
  @get-directions="handleGetDirections"
/>
```

### Step 8: Helper Function for Address Formatting

```typescript
function formatProviderAddress(provider) {
  if (!provider.address) return '';

  if (typeof provider.address === 'object') {
    const parts = [];
    if (provider.address.street) parts.push(provider.address.street);
    if (provider.address.city) parts.push(provider.address.city);
    if (provider.address.state) parts.push(provider.address.state);
    if (provider.address.zip) parts.push(provider.address.zip);
    return parts.join(', ');
  }

  return provider.address;
}
```

## Testing Checklist

- [ ] Click search and enter a ZIP code
- [ ] Verify providers show driving distance (not straight-line)
- [ ] Click "Directions" button on a provider card
- [ ] Verify DirectionsPanel appears on the right
- [ ] Verify route is drawn on map in blue
- [ ] Verify map zooms to show full route
- [ ] Verify turn-by-turn steps are displayed
- [ ] Click X to close directions
- [ ] Verify route disappears from map
- [ ] Test on mobile (panel should appear at bottom)
- [ ] Test error handling (invalid location, no route found)

## Performance Optimization (Optional)

To avoid too many API calls:

1. **Debounce distance calculations**: Only calculate distances for visible providers
2. **Cache results**: Store calculated distances in localStorage
3. **Limit concurrent requests**: Process providers in batches
4. **Use straight-line for initial display**: Show straight-line immediately, then update with driving distance

```typescript
// Batch processing example
async function fetchDistancesInBatches(providers, origin, batchSize = 5) {
  const results = [];

  for (let i = 0; i < providers.length; i += batchSize) {
    const batch = providers.slice(i, i + batchSize);
    const batchResults = await Promise.all(
      batch.map(p => getDrivingDistance([origin.lng, origin.lat], [p.longitude, p.latitude]))
    );
    results.push(...batchResults);

    // Small delay to avoid rate limiting
    await new Promise(resolve => setTimeout(resolve, 100));
  }

  return results;
}
```

## Mobile Considerations

The DirectionsPanel is responsive:
- Desktop: Appears as floating panel on right
- Mobile: Appears as bottom sheet (slides up from bottom)
- Automatically adjusts map padding to account for panel

## Notes

- Mapbox Directions API has rate limits (check your plan)
- Consider caching directions for frequently searched routes
- Route geometry is in GeoJSON format (standard for Mapbox)
- All distances are in miles, durations in minutes
- Turn-by-turn instructions come from Mapbox API

## Files Modified/Created

✅ Created:
- `src/services/mapboxDirections.ts`
- `src/components/map/DirectionsPanel.vue`

✅ Modified:
- `src/components/map/ProviderCard.vue`

⏳ To Modify:
- `src/views/MapView.vue` (integration steps above)
