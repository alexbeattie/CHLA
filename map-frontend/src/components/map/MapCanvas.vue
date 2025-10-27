<template>
  <div ref="mapContainer" class="map-canvas"></div>
</template>

<script>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue';
import mapboxgl from 'mapbox-gl';
import 'mapbox-gl/dist/mapbox-gl.css';
import { useMapStore } from '@/stores/mapStore';
import { useProviderStore } from '@/stores/providerStore';

/**
 * MapCanvas Component
 * Renders the Mapbox GL map with markers, user location, and directions
 * Week 4: Component Extraction
 */
export default {
  name: 'MapCanvas',

  props: {
    // Mapbox access token
    mapboxToken: {
      type: String,
      required: true
    },
    // Initial center coordinates
    center: {
      type: Object,
      default: () => ({ lat: 34.0522, lng: -118.2437 })
    },
    // Initial zoom level
    zoom: {
      type: Number,
      default: 10
    },
    // Map style URL
    mapStyle: {
      type: String,
      default: 'mapbox://styles/mapbox/streets-v12'
    }
  },

  emits: [
    'map-ready',
    'map-error',
    'marker-click',
    'viewport-change'
  ],

  setup(props, { emit }) {
    const mapContainer = ref(null);
    const map = ref(null);
    const markers = ref([]);
    const userLocationMarker = ref(null);
    const directionsLayer = ref(null);
    const isUpdatingMarkers = ref(false);

    const mapStore = useMapStore();
    const providerStore = useProviderStore();

    /**
     * Initialize the Mapbox map
     */
    const initializeMap = () => {
      console.log('🗺️ MapCanvas: Initializing Mapbox map...');

      if (!mapContainer.value) {
        console.error('❌ MapCanvas: Map container not found!');
        return;
      }

      try {
        // Set Mapbox access token
        mapboxgl.accessToken = props.mapboxToken;

        // Create map instance
        map.value = new mapboxgl.Map({
          container: mapContainer.value,
          style: props.mapStyle,
          center: [props.center.lng, props.center.lat],
          zoom: props.zoom,
          duration: 0 // Immediate initial positioning
        });

        console.log('✅ MapCanvas: Map instance created');

        // Add navigation controls
        map.value.addControl(
          new mapboxgl.NavigationControl(),
          'top-right'
        );

        // Add geolocation control
        map.value.addControl(
          new mapboxgl.GeolocateControl({
            positionOptions: {
              enableHighAccuracy: true
            },
            fitBoundsOptions: {
              maxZoom: 15
            },
            trackUserLocation: false,
            showAccuracyCircle: true
          }),
          'top-right'
        );

        // Map load event
        map.value.on('load', () => {
          console.log('✅ MapCanvas: Map loaded successfully');
          mapStore.setMapReady();
          emit('map-ready', map.value);
          updateMarkers();
          updateUserLocation();
          updateDirections();
        });

        // Map move event - update store
        map.value.on('moveend', () => {
          const center = map.value.getCenter();
          const zoom = map.value.getZoom();

          mapStore.setViewport({
            center: { lat: center.lat, lng: center.lng },
            zoom
          });

          emit('viewport-change', {
            center: { lat: center.lat, lng: center.lng },
            zoom
          });
        });

        // Error handling
        map.value.on('error', (error) => {
          console.error('❌ MapCanvas: Map error:', error);
          emit('map-error', error);
        });

        map.value.on('styleimagemissing', (error) => {
          console.warn('⚠️ MapCanvas: Style image missing:', error);
        });

      } catch (error) {
        console.error('❌ MapCanvas: Error creating map:', error);
        emit('map-error', error);
      }
    };

    /**
     * Update provider markers on the map
     */
    const updateMarkers = () => {
      if (!map.value) return;

      // Prevent concurrent updates
      if (isUpdatingMarkers.value) {
        console.log('⚠️ MapCanvas: Marker update already in progress, skipping');
        return;
      }

      isUpdatingMarkers.value = true;

      console.log(`🗺️ MapCanvas: Updating markers (${providerStore.providersWithCoordinates.length} providers)`);

      // Remove existing markers
      markers.value.forEach(marker => marker.remove());
      markers.value = [];

      // Add markers for providers with coordinates
      providerStore.providersWithCoordinates.forEach(provider => {
        const el = document.createElement('div');
        el.className = 'provider-marker';
        el.style.width = '30px';
        el.style.height = '30px';
        el.style.borderRadius = '50%';
        el.style.cursor = 'pointer';
        el.style.border = '2px solid white';
        el.style.boxShadow = '0 2px 4px rgba(0,0,0,0.3)';

        // Color based on selection
        if (providerStore.selectedProviderId === provider.id) {
          el.style.backgroundColor = '#2563eb'; // Blue for selected
        } else {
          el.style.backgroundColor = '#ef4444'; // Red for unselected
        }

        // Create marker
        const marker = new mapboxgl.Marker(el)
          .setLngLat([provider.longitude, provider.latitude])
          .addTo(map.value);

        // Click handler
        el.addEventListener('click', () => {
          console.log(`📍 MapCanvas: Marker clicked for provider ${provider.id}`);
          providerStore.selectProvider(provider.id);
          mapStore.selectProvider(provider.id);
          emit('marker-click', provider);
        });

        markers.value.push(marker);
      });

      // Release the lock
      isUpdatingMarkers.value = false;
    };

    /**
     * Update user location marker
     */
    const updateUserLocation = () => {
      if (!map.value) return;

      const userLoc = mapStore.userLocation;

      // Remove existing user location marker
      if (userLocationMarker.value) {
        userLocationMarker.value.remove();
        userLocationMarker.value = null;
      }

      // Add new user location marker if we have coordinates
      if (userLoc) {
        console.log(`📍 MapCanvas: Adding user location marker at ${userLoc.lat}, ${userLoc.lng}`);

        const el = document.createElement('div');
        el.className = 'user-location-marker';
        el.style.width = '20px';
        el.style.height = '20px';
        el.style.borderRadius = '50%';
        el.style.backgroundColor = '#3b82f6';
        el.style.border = '3px solid white';
        el.style.boxShadow = '0 2px 6px rgba(0,0,0,0.4)';

        userLocationMarker.value = new mapboxgl.Marker(el)
          .setLngLat([userLoc.lng, userLoc.lat])
          .addTo(map.value);
      }
    };

    /**
     * Update directions route on the map
     */
    const updateDirections = () => {
      if (!map.value || !map.value.isStyleLoaded()) return;

      const route = mapStore.directionsRoute;

      // Remove existing directions layer
      if (directionsLayer.value) {
        if (map.value.getLayer('route')) {
          map.value.removeLayer('route');
        }
        if (map.value.getSource('route')) {
          map.value.removeSource('route');
        }
        directionsLayer.value = null;
      }

      // Add new directions route if available
      if (route && route.geometry) {
        console.log('🧭 MapCanvas: Adding directions route to map');

        map.value.addSource('route', {
          type: 'geojson',
          data: {
            type: 'Feature',
            properties: {},
            geometry: route.geometry
          }
        });

        map.value.addLayer({
          id: 'route',
          type: 'line',
          source: 'route',
          layout: {
            'line-join': 'round',
            'line-cap': 'round'
          },
          paint: {
            'line-color': '#3b82f6',
            'line-width': 5,
            'line-opacity': 0.75
          }
        });

        directionsLayer.value = 'route';

        // Fit bounds to show entire route
        const coordinates = route.geometry.coordinates;
        const bounds = coordinates.reduce((bounds, coord) => {
          return bounds.extend(coord);
        }, new mapboxgl.LngLatBounds(coordinates[0], coordinates[0]));

        map.value.fitBounds(bounds, {
          padding: 50
        });
      }
    };

    /**
     * Center map on specific coordinates
     */
    const centerOn = (coords, zoom = null) => {
      if (!map.value) return;

      console.log(`🗺️ MapCanvas: Centering map on ${coords.lat}, ${coords.lng}`);

      map.value.flyTo({
        center: [coords.lng, coords.lat],
        zoom: zoom || map.value.getZoom(),
        essential: true,
        duration: 1500
      });
    };

    /**
     * Fit map to bounds
     */
    const fitBounds = (bounds, padding = 50) => {
      if (!map.value) return;

      console.log('🗺️ MapCanvas: Fitting map to bounds');

      const mapboxBounds = new mapboxgl.LngLatBounds(
        [bounds.west, bounds.south],
        [bounds.east, bounds.north]
      );

      map.value.fitBounds(mapboxBounds, { padding });
    };

    // Watch for provider changes
    // Use shallow watch and check array length to avoid excessive re-renders
    watch(
      () => [providerStore.providers.length, providerStore.providersWithCoordinates.length],
      () => {
        nextTick(() => {
          updateMarkers();
        });
      }
    );

    // Watch for selected provider changes
    watch(
      () => providerStore.selectedProviderId,
      () => {
        nextTick(() => {
          updateMarkers();
        });
      }
    );

    // Watch for user location changes
    watch(
      () => mapStore.userLocation,
      () => {
        nextTick(() => {
          updateUserLocation();
        });
      },
      { deep: true }
    );

    // Watch for directions changes
    watch(
      () => mapStore.directionsRoute,
      () => {
        nextTick(() => {
          updateDirections();
        });
      },
      { deep: true }
    );

    // Watch for viewport changes from store
    watch(
      () => mapStore.viewport,
      (newViewport) => {
        if (map.value && newViewport) {
          const currentCenter = map.value.getCenter();
          const currentZoom = map.value.getZoom();

          // Only update if significantly different (avoid infinite loops)
          const centerDiff = Math.abs(currentCenter.lat - newViewport.center.lat) +
                           Math.abs(currentCenter.lng - newViewport.center.lng);
          const zoomDiff = Math.abs(currentZoom - newViewport.zoom);

          if (centerDiff > 0.001 || zoomDiff > 0.1) {
            map.value.jumpTo({
              center: [newViewport.center.lng, newViewport.center.lat],
              zoom: newViewport.zoom
            });
          }
        }
      },
      { deep: true }
    );

    // Lifecycle hooks
    onMounted(() => {
      nextTick(() => {
        initializeMap();
      });
    });

    onBeforeUnmount(() => {
      // Cleanup markers
      markers.value.forEach(marker => marker.remove());
      if (userLocationMarker.value) {
        userLocationMarker.value.remove();
      }

      // Remove map
      if (map.value) {
        map.value.remove();
        map.value = null;
      }

      console.log('🗺️ MapCanvas: Cleaned up map and markers');
    });

    // Expose methods for parent component
    return {
      mapContainer,
      map,
      centerOn,
      fitBounds,
      updateMarkers,
      updateUserLocation,
      updateDirections
    };
  }
};
</script>

<style scoped>
.map-canvas {
  width: 100%;
  height: 100%;
  position: relative;
}

/* Provider marker styles */
:deep(.provider-marker) {
  transition: all 0.2s ease;
}

:deep(.provider-marker:hover) {
  transform: scale(1.2);
  z-index: 1;
}

/* User location marker styles */
:deep(.user-location-marker) {
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(59, 130, 246, 0.7);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(59, 130, 246, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(59, 130, 246, 0);
  }
}
</style>
