/**
 * Map State Composable
 * Manages map display state, viewport, and UI state
 * Extracted from MapView.vue as part of Week 2 refactoring
 */

import { ref, computed, reactive } from 'vue';
import type { Coordinates } from '@/utils/map';

export interface MapViewport {
  center: Coordinates;
  zoom: number;
  bearing?: number;
  pitch?: number;
}

export interface MapBounds {
  north: number;
  south: number;
  east: number;
  west: number;
}

export interface MapUIState {
  showFilters: boolean;
  showProviderDetails: boolean;
  showOnboarding: boolean;
  showDirections: boolean;
  sidebarExpanded: boolean;
  mapStyle: 'streets' | 'satellite' | 'outdoors';
}

export function useMapState() {
  // Viewport state
  const viewport = reactive<MapViewport>({
    center: { lat: 34.0522, lng: -118.2437 }, // LA center
    zoom: 10,
    bearing: 0,
    pitch: 0
  });

  // Map bounds (updated as user pans/zooms)
  const bounds = ref<MapBounds | null>(null);

  // UI state
  const uiState = reactive<MapUIState>({
    showFilters: false,
    showProviderDetails: false,
    showOnboarding: false,
    showDirections: false,
    sidebarExpanded: true,
    mapStyle: 'streets'
  });

  // Selected provider
  const selectedProviderId = ref<number | null>(null);

  // Hovered provider (for highlighting)
  const hoveredProviderId = ref<number | null>(null);

  // Map loading state
  const mapLoading = ref(true);
  const mapReady = ref(false);

  // User location
  const userLocation = ref<Coordinates | null>(null);
  const userLocationAccuracy = ref<number | null>(null);

  // Directions state
  const directionsRoute = ref<any>(null);
  const directionsFrom = ref<Coordinates | null>(null);
  const directionsTo = ref<Coordinates | null>(null);

  // Computed properties
  const hasSelectedProvider = computed(() => selectedProviderId.value !== null);
  const hasHoveredProvider = computed(() => hoveredProviderId.value !== null);
  const hasUserLocation = computed(() => userLocation.value !== null);
  const hasDirections = computed(() => directionsRoute.value !== null);

  const currentCenter = computed(() => viewport.center);
  const currentZoom = computed(() => viewport.zoom);

  /**
   * Update map viewport
   */
  function setViewport(newViewport: Partial<MapViewport>) {
    Object.assign(viewport, newViewport);
  }

  /**
   * Center map on coordinates
   */
  function centerOn(coords: Coordinates, zoom?: number) {
    viewport.center = coords;
    if (zoom !== undefined) {
      viewport.zoom = zoom;
    }
  }

  /**
   * Fit map to bounds
   */
  function fitBounds(newBounds: MapBounds, padding?: number) {
    bounds.value = newBounds;
    // Map implementation will handle actual fitting
    console.log('Fitting map to bounds:', newBounds);
  }

  /**
   * Update map bounds (called by map component)
   */
  function updateBounds(newBounds: MapBounds) {
    bounds.value = newBounds;
  }

  /**
   * Select a provider
   */
  function selectProvider(providerId: number | null) {
    selectedProviderId.value = providerId;
    if (providerId !== null) {
      uiState.showProviderDetails = true;
    }
  }

  /**
   * Clear provider selection
   */
  function clearSelection() {
    selectedProviderId.value = null;
    uiState.showProviderDetails = false;
  }

  /**
   * Hover over a provider marker
   */
  function hoverProvider(providerId: number | null) {
    hoveredProviderId.value = providerId;
  }

  /**
   * Toggle UI element visibility
   */
  function toggleUI(element: keyof MapUIState) {
    if (typeof uiState[element] === 'boolean') {
      uiState[element] = !uiState[element];
    }
  }

  /**
   * Set map style
   */
  function setMapStyle(style: 'streets' | 'satellite' | 'outdoors') {
    uiState.mapStyle = style;
  }

  /**
   * Set user location from geolocation
   */
  function setUserLocation(coords: Coordinates, accuracy?: number) {
    userLocation.value = coords;
    userLocationAccuracy.value = accuracy || null;
  }

  /**
   * Clear user location
   */
  function clearUserLocation() {
    userLocation.value = null;
    userLocationAccuracy.value = null;
  }

  /**
   * Center map on user's location
   */
  function centerOnUserLocation(zoom: number = 14) {
    if (userLocation.value) {
      centerOn(userLocation.value, zoom);
    }
  }

  /**
   * Set directions route
   */
  function setDirections(route: any, from: Coordinates, to: Coordinates) {
    directionsRoute.value = route;
    directionsFrom.value = from;
    directionsTo.value = to;
    uiState.showDirections = true;
  }

  /**
   * Clear directions
   */
  function clearDirections() {
    directionsRoute.value = null;
    directionsFrom.value = null;
    directionsTo.value = null;
    uiState.showDirections = false;
  }

  /**
   * Get directions to provider
   */
  async function getDirectionsTo(providerCoords: Coordinates, mapboxToken: string) {
    if (!userLocation.value) {
      console.warn('User location not available for directions');
      return null;
    }

    try {
      const from = userLocation.value;
      const to = providerCoords;

      // Call Mapbox Directions API
      const url = `https://api.mapbox.com/directions/v5/mapbox/driving/${from.lng},${from.lat};${to.lng},${to.lat}?geometries=geojson&access_token=${mapboxToken}`;

      const response = await fetch(url);
      const data = await response.json();

      if (data.routes && data.routes.length > 0) {
        const route = data.routes[0];
        setDirections(route, from, to);
        return route;
      }

      return null;
    } catch (error) {
      console.error('Error getting directions:', error);
      return null;
    }
  }

  /**
   * Mark map as ready
   */
  function setMapReady() {
    mapReady.value = true;
    mapLoading.value = false;
  }

  /**
   * Reset map to initial state
   */
  function resetMap() {
    viewport.center = { lat: 34.0522, lng: -118.2437 };
    viewport.zoom = 10;
    viewport.bearing = 0;
    viewport.pitch = 0;
    clearSelection();
    clearDirections();
  }

  /**
   * Toggle sidebar
   */
  function toggleSidebar() {
    uiState.sidebarExpanded = !uiState.sidebarExpanded;
  }

  /**
   * Show onboarding flow
   */
  function showOnboarding() {
    uiState.showOnboarding = true;
  }

  /**
   * Hide onboarding flow
   */
  function hideOnboarding() {
    uiState.showOnboarding = false;
  }

  return {
    // State
    viewport,
    bounds,
    uiState,
    selectedProviderId,
    hoveredProviderId,
    mapLoading,
    mapReady,
    userLocation,
    userLocationAccuracy,
    directionsRoute,
    directionsFrom,
    directionsTo,

    // Computed
    hasSelectedProvider,
    hasHoveredProvider,
    hasUserLocation,
    hasDirections,
    currentCenter,
    currentZoom,

    // Methods
    setViewport,
    centerOn,
    fitBounds,
    updateBounds,
    selectProvider,
    clearSelection,
    hoverProvider,
    toggleUI,
    setMapStyle,
    setUserLocation,
    clearUserLocation,
    centerOnUserLocation,
    setDirections,
    clearDirections,
    getDirectionsTo,
    setMapReady,
    resetMap,
    toggleSidebar,
    showOnboarding,
    hideOnboarding
  };
}
