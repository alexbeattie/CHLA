/**
 * Map State Composable
 * Manages map display state, viewport, and UI state
 * Week 3: Now delegates to Pinia mapStore for centralized state
 * Maintains backward compatibility as a composable wrapper
 */

import { computed } from 'vue';
import { useMapStore } from '@/stores/mapStore';
import type { Coordinates } from '@/utils/map';
import type {
  MapViewport,
  MapBounds,
  MapUIState
} from '@/stores/mapStore';

// Re-export types for backward compatibility
export type { MapViewport, MapBounds, MapUIState };

export function useMapState() {
  // Get the Pinia store instance
  const store = useMapStore();

  // Return store state as computed properties
  // This maintains reactivity while using centralized state
  const viewport = computed(() => store.viewport);
  const bounds = computed(() => store.bounds);
  const uiState = computed(() => store.uiState);
  const selectedProviderId = computed(() => store.selectedProviderId);
  const hoveredProviderId = computed(() => store.hoveredProviderId);
  const mapLoading = computed(() => store.mapLoading);
  const mapReady = computed(() => store.mapReady);
  const userLocation = computed(() => store.userLocation);
  const userLocationAccuracy = computed(() => store.userLocationAccuracy);
  const directionsRoute = computed(() => store.directionsRoute);
  const directionsFrom = computed(() => store.directionsFrom);
  const directionsTo = computed(() => store.directionsTo);

  // Computed properties (delegated to store)
  const hasSelectedProvider = computed(() => store.hasSelectedProvider);
  const hasHoveredProvider = computed(() => store.hasHoveredProvider);
  const hasUserLocation = computed(() => store.hasUserLocation);
  const hasDirections = computed(() => store.hasDirections);
  const currentCenter = computed(() => store.currentCenter);
  const currentZoom = computed(() => store.currentZoom);

  // Delegate all methods to store
  // All complex logic is now in the store, these are just pass-through methods

  /**
   * Update map viewport - delegates to mapStore.setViewport
   */
  function setViewport(newViewport: Partial<MapViewport>) {
    return store.setViewport(newViewport);
  }

  /**
   * Center map on coordinates - delegates to mapStore.centerOn
   */
  function centerOn(coords: Coordinates, zoom?: number) {
    return store.centerOn(coords, zoom);
  }

  /**
   * Fit map to bounds - delegates to mapStore.fitBounds
   */
  function fitBounds(newBounds: MapBounds, padding?: number) {
    return store.fitBounds(newBounds, padding);
  }

  /**
   * Update map bounds - delegates to mapStore.updateBounds
   */
  function updateBounds(newBounds: MapBounds) {
    return store.updateBounds(newBounds);
  }

  /**
   * Select a provider - delegates to mapStore.selectProvider
   */
  function selectProvider(providerId: number | null) {
    return store.selectProvider(providerId);
  }

  /**
   * Clear provider selection - delegates to mapStore.clearSelection
   */
  function clearSelection() {
    return store.clearSelection();
  }

  /**
   * Hover over a provider marker - delegates to mapStore.hoverProvider
   */
  function hoverProvider(providerId: number | null) {
    return store.hoverProvider(providerId);
  }

  /**
   * Toggle UI element visibility - delegates to mapStore.toggleUI
   */
  function toggleUI(element: keyof MapUIState) {
    return store.toggleUI(element);
  }

  /**
   * Set map style - delegates to mapStore.setMapStyle
   */
  function setMapStyle(style: 'streets' | 'satellite' | 'outdoors') {
    return store.setMapStyle(style);
  }

  /**
   * Set user location - delegates to mapStore.setUserLocation
   */
  function setUserLocation(coords: Coordinates, accuracy?: number) {
    return store.setUserLocation(coords, accuracy);
  }

  /**
   * Clear user location - delegates to mapStore.clearUserLocation
   */
  function clearUserLocation() {
    return store.clearUserLocation();
  }

  /**
   * Center map on user's location - delegates to mapStore.centerOnUserLocation
   */
  function centerOnUserLocation(zoom: number = 14) {
    return store.centerOnUserLocation(zoom);
  }

  /**
   * Set directions route - delegates to mapStore.setDirections
   */
  function setDirections(route: any, from: Coordinates, to: Coordinates) {
    return store.setDirections(route, from, to);
  }

  /**
   * Clear directions - delegates to mapStore.clearDirections
   */
  function clearDirections() {
    return store.clearDirections();
  }

  /**
   * Get directions to provider - delegates to mapStore.getDirectionsTo
   */
  async function getDirectionsTo(providerCoords: Coordinates, mapboxToken: string) {
    return store.getDirectionsTo(providerCoords, mapboxToken);
  }

  /**
   * Mark map as ready - delegates to mapStore.setMapReady
   */
  function setMapReady() {
    return store.setMapReady();
  }

  /**
   * Reset map to initial state - delegates to mapStore.resetMap
   */
  function resetMap() {
    return store.resetMap();
  }

  /**
   * Toggle sidebar - delegates to mapStore.toggleSidebar
   */
  function toggleSidebar() {
    return store.toggleSidebar();
  }

  /**
   * Show onboarding flow - delegates to mapStore.showOnboarding
   */
  function showOnboarding() {
    return store.showOnboarding();
  }

  /**
   * Hide onboarding flow - delegates to mapStore.hideOnboarding
   */
  function hideOnboarding() {
    return store.hideOnboarding();
  }

  return {
    // State (as computed from store)
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

    // Methods (delegated to store)
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
