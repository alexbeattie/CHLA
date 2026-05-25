<template>
  <div ref="mapContainer" class="map-canvas"></div>
</template>

<script>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from "vue";
import mapboxgl from "mapbox-gl";
import "mapbox-gl/dist/mapbox-gl.css";
import { useMapStore } from "@/stores/mapStore";
import { useProviderStore } from "@/stores/providerStore";
import { useLocationStore } from "@/stores/locationStore";
import { createMinimalPopup } from "@/utils/popup-minimal.js";

/**
 * MapCanvas Component
 * Renders the Mapbox GL map with markers, user location, and directions
 * Week 4: Component Extraction
 */
export default {
  name: "MapCanvas",

  props: {
    // Mapbox access token
    mapboxToken: {
      type: String,
      required: true,
    },
    // Initial center coordinates
    center: {
      type: Object,
      default: () => ({ lat: 34.0522, lng: -118.2437 }),
    },
    // Initial zoom level
    zoom: {
      type: Number,
      default: 10,
    },
    // Map style URL
    mapStyle: {
      type: String,
      default: "mapbox://styles/mapbox/streets-v12",
    },
  },

  emits: ["map-ready", "map-error", "marker-click", "viewport-change"],

  setup(props, { emit }) {
    const mapContainer = ref(null);
    const map = ref(null);
    const markers = ref([]);
    const locationMarkers = ref([]);
    const userLocationMarker = ref(null);
    const directionsLayer = ref(null);
    const isUpdatingMarkers = ref(false);

    const mapStore = useMapStore();
    const providerStore = useProviderStore();
    const locationStore = useLocationStore();

    /**
     * Initialize the Mapbox map
     */
    const initializeMap = () => {
      console.log("🗺️ MapCanvas: Initializing Mapbox map...");

      if (!mapContainer.value) {
        console.error("❌ MapCanvas: Map container not found!");
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
          duration: 0, // Immediate initial positioning
        });

        console.log("✅ MapCanvas: Map instance created");

        // Add navigation controls
        map.value.addControl(new mapboxgl.NavigationControl(), "top-right");

        // Add geolocation control
        map.value.addControl(
          new mapboxgl.GeolocateControl({
            positionOptions: {
              enableHighAccuracy: true,
            },
            fitBoundsOptions: {
              maxZoom: 15,
            },
            trackUserLocation: false,
            showAccuracyCircle: true,
          }),
          "top-right"
        );

        // Map load event
        map.value.on("load", () => {
          console.log("✅ MapCanvas: Map loaded successfully");
          mapStore.setMapReady();
          emit("map-ready", map.value);
          updateMarkers();
          updateLocationMarkers();
          updateUserLocation();
          updateDirections();
        });

        // Map move event - update store
        map.value.on("moveend", () => {
          const center = map.value.getCenter();
          const zoom = map.value.getZoom();

          mapStore.setViewport({
            center: { lat: center.lat, lng: center.lng },
            zoom,
          });

          emit("viewport-change", {
            center: { lat: center.lat, lng: center.lng },
            zoom,
          });
        });

        // Error handling
        map.value.on("error", (error) => {
          console.error("❌ MapCanvas: Map error:", error);
          emit("map-error", error);
        });

        map.value.on("styleimagemissing", (error) => {
          console.warn("⚠️ MapCanvas: Style image missing:", error);
        });
      } catch (error) {
        console.error("❌ MapCanvas: Error creating map:", error);
        emit("map-error", error);
      }
    };

    /**
     * Update provider markers on the map
     */
    const updateMarkers = () => {
      if (!map.value) return;

      // Prevent concurrent updates
      if (isUpdatingMarkers.value) {
        console.log(
          "⚠️ MapCanvas: Marker update already in progress, skipping"
        );
        return;
      }

      isUpdatingMarkers.value = true;

      console.log(
        `🗺️ MapCanvas: Updating markers (${providerStore.providersWithCoordinates.length} providers)`
      );

      // Track existing markers by provider ID for efficient updates
      const existingMarkers = new Map();
      markers.value.forEach((marker) => {
        if (marker._providerId) {
          existingMarkers.set(marker._providerId, marker);
        }
      });

      const newMarkers = [];
      const currentProviderIds = new Set();

      // Add or update markers for current providers
      providerStore.providersWithCoordinates.forEach((provider) => {
        currentProviderIds.add(provider.id);

        // Check if marker already exists
        const existingMarker = existingMarkers.get(provider.id);

        if (existingMarker) {
          // Update existing marker color
          const el = existingMarker.getElement();
          const isSelected = providerStore.selectedProviderId === provider.id;
          el.classList.toggle("is-selected", isSelected);
          el.style.zIndex = isSelected ? "1001" : "1000";
          newMarkers.push(existingMarker);
        } else {
          // Create new marker
          const el = document.createElement("div");
          el.className = "provider-marker map-marker-icon";
          el.innerHTML =
            '<span class="marker-face"><i class="bi bi-hospital-fill" aria-hidden="true"></i></span>';
          // DON'T set position - let Mapbox handle it (absolute positioning to coordinates)
          el.style.zIndex = "1000";

          // Color based on selection
          if (providerStore.selectedProviderId === provider.id) {
            el.classList.add("is-selected");
            el.style.zIndex = "1001";
          }
          el.setAttribute("aria-label", `Provider: ${provider.name}`);

          // Create popup for this marker
          const popupHTML = createMinimalPopup(provider);

          const popup = new mapboxgl.Popup({
            offset: 25,
            maxWidth: "300px",
            closeButton: true,
            closeOnClick: true, // Close when clicking elsewhere on map
            className: "minimal-popup",
          }).setHTML(popupHTML);

          // Create marker (convert coordinates to numbers)
          const marker = new mapboxgl.Marker(el)
            .setLngLat([
              parseFloat(provider.longitude),
              parseFloat(provider.latitude),
            ])
            .setPopup(popup)
            .addTo(map.value);

          // Store provider ID on marker for tracking
          marker._providerId = provider.id;

          // Click handler
          el.addEventListener("click", (e) => {
            e.stopPropagation();
            console.log(
              `📍 MapCanvas: Marker clicked for provider ${provider.id}`
            );

            // Close all other popups first
            markers.value.forEach((m) => {
              if (m !== marker && m.getPopup().isOpen()) {
                m.getPopup().remove();
              }
            });
            locationMarkers.value.forEach((locationMarker) => {
              if (locationMarker.getPopup().isOpen()) {
                locationMarker.getPopup().remove();
              }
            });

            providerStore.selectProvider(provider.id);
            mapStore.selectProvider(provider.id);
            emit("marker-click", provider);

            // Toggle the popup for this marker
            marker.togglePopup();
          });

          newMarkers.push(marker);
        }
      });

      // Remove markers for providers that no longer exist
      existingMarkers.forEach((marker, providerId) => {
        if (!currentProviderIds.has(providerId)) {
          marker.remove();
        }
      });

      markers.value = newMarkers;

      // Release the lock
      isUpdatingMarkers.value = false;
    };

    /**
     * Update only marker colors when selection changes (no recreation)
     */
    const updateMarkerSelection = () => {
      if (!map.value) return;

      markers.value.forEach((marker) => {
        const el = marker.getElement();
        const isSelected =
          marker._providerId === providerStore.selectedProviderId;
        el.classList.toggle("is-selected", isSelected);
        el.style.zIndex = isSelected ? "1001" : "1000";
      });
    };

    /**
     * Update inclusive playground location markers on the map
     */
    const updateLocationMarkers = () => {
      if (!map.value) return;

      const visibleLocations = locationStore.showInclusivePlaygrounds
        ? locationStore.locationsWithCoordinates
        : [];

      const existingMarkers = new Map();
      locationMarkers.value.forEach((marker) => {
        if (marker._locationId) {
          existingMarkers.set(marker._locationId, marker);
        }
      });

      const newMarkers = [];
      const currentLocationIds = new Set();

      visibleLocations.forEach((location) => {
        currentLocationIds.add(location.id);
        const existingMarker = existingMarkers.get(location.id);

        if (existingMarker) {
          newMarkers.push(existingMarker);
          return;
        }

        const el = document.createElement("div");
        el.className = "location-marker playground-marker map-marker-icon";
        el.innerHTML =
          '<span class="marker-face"><i class="bi bi-tree-fill" aria-hidden="true"></i></span>';
        el.style.zIndex = "999";
        el.setAttribute("aria-label", `Inclusive playground: ${location.name}`);

        const popup = new mapboxgl.Popup({
          offset: 25,
          maxWidth: "300px",
          closeButton: true,
          closeOnClick: true,
          className: "minimal-popup",
        }).setHTML(createLocationPopup(location));

        const marker = new mapboxgl.Marker(el)
          .setLngLat([
            parseFloat(location.longitude),
            parseFloat(location.latitude),
          ])
          .setPopup(popup)
          .addTo(map.value);

        marker._locationId = location.id;

        el.addEventListener("click", (event) => {
          event.stopPropagation();
          markers.value.forEach((providerMarker) => {
            if (providerMarker.getPopup().isOpen()) {
              providerMarker.getPopup().remove();
            }
          });
          locationMarkers.value.forEach((locationMarker) => {
            if (
              locationMarker !== marker &&
              locationMarker.getPopup().isOpen()
            ) {
              locationMarker.getPopup().remove();
            }
          });
          locationStore.selectLocation(location.id);
          marker.togglePopup();
        });

        newMarkers.push(marker);
      });

      existingMarkers.forEach((marker, locationId) => {
        if (!currentLocationIds.has(locationId)) {
          marker.remove();
        }
      });

      locationMarkers.value = newMarkers;
    };

    const createLocationPopup = (location) => {
      const lat = location.latitude || 0;
      const lng = location.longitude || 0;
      const address = [
        location.address,
        location.city,
        location.state,
        location.zip_code,
      ]
        .filter(Boolean)
        .join(", ");
      const googleMapsUrl = `https://www.google.com/maps/dir/?api=1&destination=${lat},${lng}`;
      const appleMapsUrl = `https://maps.apple.com/?daddr=${lat},${lng}`;

      return `
        <article class="kindd-map-popup kindd-map-popup--playground">
          <div class="kindd-map-popup__eyebrow">
            <span class="kindd-map-popup__icon kindd-map-popup__icon--playground">
              <i class="bi bi-tree-fill" aria-hidden="true"></i>
            </span>
            <span>Inclusive Playground</span>
          </div>
          <h3 class="kindd-map-popup__title">${escapeHtml(location.name)}</h3>
          ${
            address
              ? `<p class="kindd-map-popup__line"><i class="bi bi-geo-alt-fill" aria-hidden="true"></i><span>${escapeHtml(
                  address
                )}</span></p>`
              : ""
          }
          <div class="kindd-map-popup__actions">
            <a href="${googleMapsUrl}" target="_blank" rel="noopener" class="kindd-map-popup__button kindd-map-popup__button--primary">Google Maps</a>
            <a href="${appleMapsUrl}" target="_blank" rel="noopener" class="kindd-map-popup__button kindd-map-popup__button--dark">Apple Maps</a>
          </div>
        </article>
      `;
    };

    const escapeHtml = (value) =>
      String(value || "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");

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
        console.log(
          `📍 MapCanvas: Adding user location marker at ${userLoc.lat}, ${userLoc.lng}`
        );

        const el = document.createElement("div");
        el.className = "user-location-marker";
        el.innerHTML = '<span class="user-location-core"></span>';
        el.setAttribute("aria-label", "Your location");

        userLocationMarker.value = new mapboxgl.Marker(el)
          .setLngLat([parseFloat(userLoc.lng), parseFloat(userLoc.lat)])
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
        if (map.value.getLayer("route")) {
          map.value.removeLayer("route");
        }
        if (map.value.getSource("route")) {
          map.value.removeSource("route");
        }
        directionsLayer.value = null;
      }

      // Add new directions route if available
      if (route && route.geometry) {
        console.log("🧭 MapCanvas: Adding directions route to map");

        map.value.addSource("route", {
          type: "geojson",
          data: {
            type: "Feature",
            properties: {},
            geometry: route.geometry,
          },
        });

        map.value.addLayer({
          id: "route",
          type: "line",
          source: "route",
          layout: {
            "line-join": "round",
            "line-cap": "round",
          },
          paint: {
            "line-color": "#3b82f6",
            "line-width": 5,
            "line-opacity": 0.75,
          },
        });

        directionsLayer.value = "route";

        // Fit bounds to show entire route
        const coordinates = route.geometry.coordinates;
        const bounds = coordinates.reduce((bounds, coord) => {
          return bounds.extend(coord);
        }, new mapboxgl.LngLatBounds(coordinates[0], coordinates[0]));

        map.value.fitBounds(bounds, {
          padding: 50,
        });
      }
    };

    /**
     * Center map on specific coordinates with smooth easing
     */
    const centerOn = (coords, zoom = null) => {
      if (!map.value) return;

      console.log(
        `🗺️ MapCanvas: Centering map on ${coords.lat}, ${coords.lng}`
      );

      // Use easeTo instead of flyTo for smoother, gentler animation
      map.value.easeTo({
        center: [coords.lng, coords.lat],
        zoom: zoom || map.value.getZoom(),
        duration: 2000, // Slower 2 second animation for smooth feeling
        easing(t) {
          // Bezier curve for smooth ease-in-out (starts slow, speeds up, ends slow)
          return t * t * (3.0 - 2.0 * t);
        },
      });
    };

    /**
     * Fit map to bounds
     */
    const fitBounds = (bounds, padding = 50) => {
      if (!map.value) return;

      console.log("🗺️ MapCanvas: Fitting map to bounds");

      const mapboxBounds = new mapboxgl.LngLatBounds(
        [bounds.west, bounds.south],
        [bounds.east, bounds.north]
      );

      map.value.fitBounds(mapboxBounds, { padding });
    };

    // Watch for provider changes
    // Use shallow watch and check array length to avoid excessive re-renders
    watch(
      () => [
        providerStore.providers.length,
        providerStore.providersWithCoordinates.length,
      ],
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
          updateMarkerSelection();
        });
      }
    );

    // Watch for inclusive playground location changes
    watch(
      () => [
        locationStore.locations.length,
        locationStore.locationsWithCoordinates.length,
        locationStore.showInclusivePlaygrounds,
      ],
      () => {
        nextTick(() => {
          updateLocationMarkers();
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
          const centerDiff =
            Math.abs(currentCenter.lat - newViewport.center.lat) +
            Math.abs(currentCenter.lng - newViewport.center.lng);
          const zoomDiff = Math.abs(currentZoom - newViewport.zoom);

          if (centerDiff > 0.001 || zoomDiff > 0.1) {
            map.value.jumpTo({
              center: [newViewport.center.lng, newViewport.center.lat],
              zoom: newViewport.zoom,
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
      markers.value.forEach((marker) => marker.remove());
      locationMarkers.value.forEach((marker) => marker.remove());
      if (userLocationMarker.value) {
        userLocationMarker.value.remove();
      }

      // Remove map
      if (map.value) {
        map.value.remove();
        map.value = null;
      }

      console.log("🗺️ MapCanvas: Cleaned up map and markers");
    });

    /**
     * Open popup for a specific provider (for mobile sidebar interaction)
     */
    const openProviderPopup = (providerId) => {
      console.log(`🗺️ MapCanvas: Opening popup for provider ${providerId}`);

      // Find the marker for this provider
      const marker = markers.value.find((m) => m._providerId === providerId);

      if (marker) {
        // Close all other popups first
        markers.value.forEach((m) => {
          if (m !== marker && m.getPopup().isOpen()) {
            m.getPopup().remove();
          }
        });

        // Open this marker's popup
        marker.togglePopup();
        console.log(`✅ MapCanvas: Opened popup for provider ${providerId}`);
      } else {
        console.warn(
          `⚠️ MapCanvas: Could not find marker for provider ${providerId}`
        );
      }
    };

    // Expose methods for parent component
    return {
      mapContainer,
      map,
      centerOn,
      fitBounds,
      updateMarkers,
      updateLocationMarkers,
      updateUserLocation,
      updateDirections,
      openProviderPopup,
    };
  },
};
</script>

<style scoped>
.map-canvas {
  width: 100%;
  height: 100%;
  position: relative;
}

/* Shared marker styles */
:deep(.map-marker-icon) {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

:deep(.marker-face) {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #ffffff;
  box-shadow: 0 2px 7px rgba(15, 23, 42, 0.3);
  color: #ffffff;
  transition: background-color 0.2s ease, box-shadow 0.2s ease,
    opacity 0.2s ease, transform 0.2s ease;
}

:deep(.marker-face i) {
  font-size: 14px;
  line-height: 1;
}

/* Services/providers: red rounded-square medical marker */
:deep(.provider-marker .marker-face) {
  background: #dc2626;
  border-radius: 11px;
}

:deep(.provider-marker.is-selected .marker-face) {
  background: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.2),
    0 3px 10px rgba(15, 23, 42, 0.35);
}

:deep(.provider-marker:hover .marker-face) {
  transform: scale(1.16);
}

/* Inclusive playgrounds: green pin with tree icon */
:deep(.playground-marker) {
  width: 30px;
  height: 30px;
}

:deep(.playground-marker .marker-face) {
  background: #16a34a;
  border-radius: 50% 50% 50% 10px;
  transform: rotate(-45deg);
}

:deep(.playground-marker .marker-face i) {
  transform: rotate(45deg);
}

:deep(.playground-marker:hover .marker-face) {
  transform: rotate(-45deg) scale(1.12);
}

/* User location: blue dot with an animated accuracy ring */
:deep(.user-location-marker) {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2px solid #ffffff;
  background: #2563eb;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.3);
  animation: pulse 2s infinite;
}

/* Popup styles - ensure they appear above markers */
:deep(.mapboxgl-popup) {
  z-index: 2000 !important;
}

:deep(.mapboxgl-popup-content) {
  padding: 0 !important;
  box-shadow: 0 12px 34px rgba(15, 23, 42, 0.22) !important;
  border-radius: 18px !important;
  border: 2px solid rgba(0, 72, 119, 0.16) !important;
  overflow: hidden;
}

:deep(.mapboxgl-popup-close-button) {
  width: 28px;
  height: 28px;
  margin: 7px;
  padding: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.92);
  color: #475569;
  font-size: 21px;
  line-height: 25px;
  z-index: 2001;
}

:deep(.mapboxgl-popup-close-button:hover) {
  background-color: #eef7fb;
  color: #004877;
}

:deep(.mapboxgl-popup-tip) {
  z-index: 1999;
}

:deep(.kindd-map-popup) {
  width: min(286px, calc(100vw - 48px));
  padding: 14px;
  box-sizing: border-box;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbfd 100%);
  color: #1f2937;
}

:deep(.kindd-map-popup__eyebrow) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 9px;
  margin-bottom: 8px;
  border-radius: 999px;
  background: #eaf6fb;
  color: #004877;
  font-size: 11px;
  font-weight: 750;
  letter-spacing: 0.02em;
}

:deep(.kindd-map-popup__icon) {
  width: 19px;
  height: 19px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #dc2626;
  color: #ffffff;
  font-size: 12px;
}

:deep(.kindd-map-popup__icon--playground) {
  background: #16a34a;
}

:deep(.kindd-map-popup__title) {
  margin: 0 34px 9px 0;
  color: #111827;
  font-family: var(--kindd-font-family);
  font-size: 18px;
  font-weight: 760;
  line-height: 1.15;
  letter-spacing: -0.02em;
  overflow-wrap: anywhere;
}

:deep(.kindd-map-popup__line),
:deep(.kindd-map-popup__description),
:deep(.kindd-map-popup__distance) {
  margin: 0 0 10px;
  color: #475569;
  font-size: 13px;
  line-height: 1.45;
}

:deep(.kindd-map-popup__line) {
  display: flex;
  gap: 8px;
}

:deep(.kindd-map-popup__line i) {
  margin-top: 2px;
  color: #0d9ddb;
  flex: 0 0 auto;
}

:deep(.kindd-map-popup__meta) {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 10px 0;
}

:deep(.kindd-map-popup__link) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #004877;
  font-size: 12.5px;
  font-weight: 700;
  text-decoration: none;
}

:deep(.kindd-map-popup__link:hover) {
  color: #0d9ddb;
}

:deep(.kindd-map-popup__actions) {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 7px;
  margin-top: 12px;
}

:deep(.kindd-map-popup__button) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 32px;
  padding: 7px 9px;
  border: 1px solid rgba(0, 72, 119, 0.14);
  border-radius: 10px;
  color: #004877;
  background: #f5fbfe;
  font-size: 11.5px;
  font-weight: 750;
  text-decoration: none;
}

:deep(.kindd-map-popup__button--primary) {
  border-color: rgba(13, 157, 219, 0.25);
  background: #e9f7fc;
  color: #004877;
}

:deep(.kindd-map-popup__button--dark) {
  border-color: rgba(31, 41, 55, 0.16);
  background: #f3f4f6;
  color: #1f2937;
}

:deep(.kindd-map-popup__button--success) {
  grid-column: 1 / -1;
  border-color: rgba(0, 72, 119, 0.18);
  background: #ffffff;
  color: #004877;
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
