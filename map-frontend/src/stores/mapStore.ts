/**
 * Map Store
 * Centralized state management for map viewport and UI
 * Week 3: Pinia Store Architecture
 */

import { defineStore } from 'pinia';
import { ref, computed, reactive } from 'vue';

export interface Coordinates {
  lat: number;
  lng: number;
}

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

export const useMapStore = defineStore('map', () => {
  // ==================== STATE ====================

  // Viewport state
  const viewport = reactive<MapViewport>({
    center: { lat: 34.0522, lng: -118.2437 }, // LA center
    zoom: 10,
    bearing: 0,
    pitch: 0
  });

  // Map bounds
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

  // Selected and hovered providers
  const selectedProviderId = ref<number | null>(null);
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

  // ==================== GETTERS ====================

  const hasSelectedProvider = computed(() => selectedProviderId.value !== null);
  const hasHoveredProvider = computed(() => hoveredProviderId.value !== null);
  const hasUserLocation = computed(() => userLocation.value !== null);
  const hasDirections = computed(() => directionsRoute.value !== null);

  const currentCenter = computed(() => viewport.center);
  const currentZoom = computed(() => viewport.zoom);

  // ==================== ACTIONS ====================

  /**
   * Update map viewport
   */
  function setViewport(newViewport: Partial<MapViewport>) {
    Object.assign(viewport, newViewport);
    console.log(`🗺️ [Store] Updated viewport:`, newViewport);
  }

  /**
   * Center map on coordinates
   */
  function centerOn(coords: Coordinates, zoom?: number) {
    viewport.center = coords;
    if (zoom !== undefined) {
      viewport.zoom = zoom;
    }
    console.log(`📍 [Store] Centered map on: ${coords.lat}, ${coords.lng}`);
  }

  /**
   * Fit map to bounds
   */
  function fitBounds(newBounds: MapBounds, padding?: number) {
    bounds.value = newBounds;
    console.log('🗺️ [Store] Fitting map to bounds:', newBounds);
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
      console.log(`📍 [Store] Selected provider: ${providerId}`);
    } else {
      uiState.showProviderDetails = false;
      console.log('📍 [Store] Cleared provider selection');
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
      console.log(`🎛️ [Store] Toggled ${element}: ${uiState[element]}`);
    }
  }

  /**
   * Set map style
   */
  function setMapStyle(style: 'streets' | 'satellite' | 'outdoors') {
    uiState.mapStyle = style;
    console.log(`🎨 [Store] Changed map style to: ${style}`);
  }

  /**
   * Set user location from geolocation
   */
  function setUserLocation(coords: Coordinates, accuracy?: number) {
    userLocation.value = coords;
    userLocationAccuracy.value = accuracy || null;
    console.log(`📍 [Store] Set user location: ${coords.lat}, ${coords.lng}`);
  }

  /**
   * Clear user location
   */
  function clearUserLocation() {
    userLocation.value = null;
    userLocationAccuracy.value = null;
    console.log('🧹 [Store] Cleared user location');
  }

  /**
   * Center map on user's location
   */
  function centerOnUserLocation(zoom: number = 14) {
    if (userLocation.value) {
      centerOn(userLocation.value, zoom);
    } else {
      console.warn('⚠️ [Store] No user location available');
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
    console.log('🧭 [Store] Set directions route');
  }

  /**
   * Clear directions
   */
  function clearDirections() {
    directionsRoute.value = null;
    directionsFrom.value = null;
    directionsTo.value = null;
    uiState.showDirections = false;
    console.log('🧹 [Store] Cleared directions');
  }

  /**
   * Get directions to provider using Mapbox API
   */
  async function getDirectionsTo(
    providerCoords: Coordinates,
    mapboxToken: string
  ): Promise<any | null> {
    if (!userLocation.value) {
      console.warn('⚠️ [Store] User location not available for directions');
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
      console.error('❌ [Store] Error getting directions:', error);
      return null;
    }
  }

  /**
   * Mark map as ready
   */
  function setMapReady() {
    mapReady.value = true;
    mapLoading.value = false;
    console.log('✅ [Store] Map is ready');
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
    console.log('🔄 [Store] Reset map to initial state');
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

  // ==================== RETURN ====================

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

    // Getters
    hasSelectedProvider,
    hasHoveredProvider,
    hasUserLocation,
    hasDirections,
    currentCenter,
    currentZoom,

    // Actions
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
});
