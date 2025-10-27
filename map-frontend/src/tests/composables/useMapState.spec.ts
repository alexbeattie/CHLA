/**
 * Tests for useMapState composable
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import { useMapState } from '@/composables/useMapState';

describe('useMapState', () => {
  let composable: ReturnType<typeof useMapState>;

  beforeEach(() => {
    setActivePinia(createPinia());
    composable = useMapState();
  });

  describe('Initialization', () => {
    it('should initialize with default viewport', () => {
      expect(composable.viewport.value.center).toEqual({ lat: 34.0522, lng: -118.2437 });
      expect(composable.viewport.value.zoom).toBe(10);
      expect(composable.viewport.value.bearing).toBe(0);
      expect(composable.viewport.value.pitch).toBe(0);
    });

    it('should initialize with default UI state', () => {
      expect(composable.uiState.value.showFilters).toBe(false);
      expect(composable.uiState.value.showProviderDetails).toBe(false);
      expect(composable.uiState.value.showOnboarding).toBe(false);
      expect(composable.uiState.value.showDirections).toBe(false);
      expect(composable.uiState.value.sidebarExpanded).toBe(true);
      expect(composable.uiState.value.mapStyle).toBe('streets');
    });

    it('should initialize with null states', () => {
      expect(composable.bounds.value).toBe(null);
      expect(composable.selectedProviderId.value).toBe(null);
      expect(composable.hoveredProviderId.value).toBe(null);
      expect(composable.userLocation.value).toBe(null);
      expect(composable.directionsRoute.value).toBe(null);
    });

    it('should initialize with correct loading states', () => {
      expect(composable.mapLoading.value).toBe(true);
      expect(composable.mapReady.value).toBe(false);
    });
  });

  describe('Viewport management', () => {
    it('should update viewport', () => {
      composable.setViewport({
        center: { lat: 34.0, lng: -118.0 },
        zoom: 12
      });

      expect(composable.viewport.value.center).toEqual({ lat: 34.0, lng: -118.0 });
      expect(composable.viewport.value.zoom).toBe(12);
    });

    it('should partially update viewport', () => {
      composable.setViewport({ zoom: 15 });

      expect(composable.viewport.value.zoom).toBe(15);
      expect(composable.viewport.value.center).toEqual({ lat: 34.0522, lng: -118.2437 });
    });

    it('should center on coordinates', () => {
      composable.centerOn({ lat: 33.0, lng: -117.0 });

      expect(composable.viewport.value.center).toEqual({ lat: 33.0, lng: -117.0 });
    });

    it('should center on coordinates with zoom', () => {
      composable.centerOn({ lat: 33.0, lng: -117.0 }, 14);

      expect(composable.viewport.value.center).toEqual({ lat: 33.0, lng: -117.0 });
      expect(composable.viewport.value.zoom).toBe(14);
    });

    it('should fit to bounds', () => {
      const bounds = {
        north: 35.0,
        south: 33.0,
        east: -117.0,
        west: -119.0
      };

      composable.fitBounds(bounds);

      expect(composable.bounds.value).toEqual(bounds);
    });

    it('should update bounds', () => {
      const bounds = {
        north: 34.1,
        south: 33.9,
        east: -118.1,
        west: -118.3
      };

      composable.updateBounds(bounds);

      expect(composable.bounds.value).toEqual(bounds);
    });
  });

  describe('Provider selection', () => {
    it('should select a provider', () => {
      composable.selectProvider(123);

      expect(composable.selectedProviderId.value).toBe(123);
      expect(composable.uiState.value.showProviderDetails).toBe(true);
      expect(composable.hasSelectedProvider.value).toBe(true);
    });

    it('should clear selection', () => {
      composable.selectProvider(123);
      expect(composable.selectedProviderId.value).toBe(123);

      composable.clearSelection();

      expect(composable.selectedProviderId.value).toBe(null);
      expect(composable.uiState.value.showProviderDetails).toBe(false);
      expect(composable.hasSelectedProvider.value).toBe(false);
    });

    it('should allow null selection', () => {
      composable.selectProvider(123);
      composable.selectProvider(null);

      expect(composable.selectedProviderId.value).toBe(null);
      expect(composable.uiState.value.showProviderDetails).toBe(false);
    });

    it('should hover over provider', () => {
      composable.hoverProvider(456);

      expect(composable.hoveredProviderId.value).toBe(456);
      expect(composable.hasHoveredProvider.value).toBe(true);
    });

    it('should clear hover', () => {
      composable.hoverProvider(456);
      composable.hoverProvider(null);

      expect(composable.hoveredProviderId.value).toBe(null);
      expect(composable.hasHoveredProvider.value).toBe(false);
    });
  });

  describe('UI state management', () => {
    it('should toggle UI elements', () => {
      expect(composable.uiState.value.showFilters).toBe(false);

      composable.toggleUI('showFilters');
      expect(composable.uiState.value.showFilters).toBe(true);

      composable.toggleUI('showFilters');
      expect(composable.uiState.value.showFilters).toBe(false);
    });

    it('should toggle sidebar', () => {
      expect(composable.uiState.value.sidebarExpanded).toBe(true);

      composable.toggleSidebar();
      expect(composable.uiState.value.sidebarExpanded).toBe(false);

      composable.toggleSidebar();
      expect(composable.uiState.value.sidebarExpanded).toBe(true);
    });

    it('should set map style', () => {
      composable.setMapStyle('satellite');
      expect(composable.uiState.value.mapStyle).toBe('satellite');

      composable.setMapStyle('outdoors');
      expect(composable.uiState.value.mapStyle).toBe('outdoors');

      composable.setMapStyle('streets');
      expect(composable.uiState.value.mapStyle).toBe('streets');
    });

    it('should show and hide onboarding', () => {
      composable.showOnboarding();
      expect(composable.uiState.value.showOnboarding).toBe(true);

      composable.hideOnboarding();
      expect(composable.uiState.value.showOnboarding).toBe(false);
    });

    it('should set map ready state', () => {
      expect(composable.mapReady.value).toBe(false);
      expect(composable.mapLoading.value).toBe(true);

      composable.setMapReady();

      expect(composable.mapReady.value).toBe(true);
      expect(composable.mapLoading.value).toBe(false);
    });
  });

  describe('User location', () => {
    it('should set user location', () => {
      const coords = { lat: 34.0, lng: -118.0 };
      composable.setUserLocation(coords, 10);

      expect(composable.userLocation.value).toEqual(coords);
      expect(composable.userLocationAccuracy.value).toBe(10);
      expect(composable.hasUserLocation.value).toBe(true);
    });

    it('should set user location without accuracy', () => {
      const coords = { lat: 34.0, lng: -118.0 };
      composable.setUserLocation(coords);

      expect(composable.userLocation.value).toEqual(coords);
      expect(composable.userLocationAccuracy.value).toBe(null);
    });

    it('should clear user location', () => {
      composable.setUserLocation({ lat: 34.0, lng: -118.0 }, 10);

      composable.clearUserLocation();

      expect(composable.userLocation.value).toBe(null);
      expect(composable.userLocationAccuracy.value).toBe(null);
      expect(composable.hasUserLocation.value).toBe(false);
    });

    it('should center on user location', () => {
      const coords = { lat: 34.5, lng: -118.5 };
      composable.setUserLocation(coords);

      composable.centerOnUserLocation();

      expect(composable.viewport.value.center).toEqual(coords);
      expect(composable.viewport.value.zoom).toBe(14); // Default zoom
    });

    it('should center on user location with custom zoom', () => {
      const coords = { lat: 34.5, lng: -118.5 };
      composable.setUserLocation(coords);

      composable.centerOnUserLocation(16);

      expect(composable.viewport.value.center).toEqual(coords);
      expect(composable.viewport.value.zoom).toBe(16);
    });

    it('should not center if no user location', () => {
      const originalCenter = composable.viewport.value.center;

      composable.centerOnUserLocation();

      expect(composable.viewport.value.center).toEqual(originalCenter);
    });
  });

  describe('Directions', () => {
    const mockRoute = {
      distance: 5000,
      duration: 600,
      geometry: {
        type: 'LineString',
        coordinates: [
          [-118.2437, 34.0522],
          [-118.2537, 34.0622]
        ]
      }
    };

    const from = { lat: 34.0522, lng: -118.2437 };
    const to = { lat: 34.0622, lng: -118.2537 };

    it('should set directions', () => {
      composable.setDirections(mockRoute, from, to);

      expect(composable.directionsRoute.value).toEqual(mockRoute);
      expect(composable.directionsFrom.value).toEqual(from);
      expect(composable.directionsTo.value).toEqual(to);
      expect(composable.uiState.value.showDirections).toBe(true);
      expect(composable.hasDirections.value).toBe(true);
    });

    it('should clear directions', () => {
      composable.setDirections(mockRoute, from, to);

      composable.clearDirections();

      expect(composable.directionsRoute.value).toBe(null);
      expect(composable.directionsFrom.value).toBe(null);
      expect(composable.directionsTo.value).toBe(null);
      expect(composable.uiState.value.showDirections).toBe(false);
      expect(composable.hasDirections.value).toBe(false);
    });

    it('should get directions to provider', async () => {
      // Mock fetch for Mapbox Directions API
      const mockResponse = {
        routes: [mockRoute]
      };

      global.fetch = vi.fn().mockResolvedValue({
        json: async () => mockResponse
      });

      composable.setUserLocation(from);

      const result = await composable.getDirectionsTo(to, 'test-token');

      expect(result).toEqual(mockRoute);
      expect(composable.directionsRoute.value).toEqual(mockRoute);
      expect(composable.directionsFrom.value).toEqual(from);
      expect(composable.directionsTo.value).toEqual(to);

      // Verify fetch was called with correct URL
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('api.mapbox.com/directions')
      );
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining(`${from.lng},${from.lat}`)
      );
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining(`${to.lng},${to.lat}`)
      );
    });

    it('should return null if no user location for directions', async () => {
      const result = await composable.getDirectionsTo(to, 'test-token');

      expect(result).toBe(null);
      expect(composable.directionsRoute.value).toBe(null);
    });

    it('should handle directions API errors', async () => {
      global.fetch = vi.fn().mockRejectedValue(new Error('Network error'));

      composable.setUserLocation(from);

      const result = await composable.getDirectionsTo(to, 'test-token');

      expect(result).toBe(null);
      expect(composable.directionsRoute.value).toBe(null);
    });

    it('should return null if no routes returned', async () => {
      global.fetch = vi.fn().mockResolvedValue({
        json: async () => ({ routes: [] })
      });

      composable.setUserLocation(from);

      const result = await composable.getDirectionsTo(to, 'test-token');

      expect(result).toBe(null);
    });
  });

  describe('resetMap', () => {
    it('should reset map to initial state', () => {
      // Change various states
      composable.setViewport({
        center: { lat: 35.0, lng: -119.0 },
        zoom: 15,
        bearing: 45,
        pitch: 60
      });
      composable.selectProvider(123);
      composable.setDirections(
        { distance: 1000 },
        { lat: 34.0, lng: -118.0 },
        { lat: 34.1, lng: -118.1 }
      );

      // Reset
      composable.resetMap();

      // Verify reset
      expect(composable.viewport.value.center).toEqual({ lat: 34.0522, lng: -118.2437 });
      expect(composable.viewport.value.zoom).toBe(10);
      expect(composable.viewport.value.bearing).toBe(0);
      expect(composable.viewport.value.pitch).toBe(0);
      expect(composable.selectedProviderId.value).toBe(null);
      expect(composable.directionsRoute.value).toBe(null);
    });
  });

  describe('Computed properties', () => {
    it('should calculate hasSelectedProvider', () => {
      expect(composable.hasSelectedProvider.value).toBe(false);

      composable.selectProvider(123);
      expect(composable.hasSelectedProvider.value).toBe(true);

      composable.clearSelection();
      expect(composable.hasSelectedProvider.value).toBe(false);
    });

    it('should calculate hasHoveredProvider', () => {
      expect(composable.hasHoveredProvider.value).toBe(false);

      composable.hoverProvider(456);
      expect(composable.hasHoveredProvider.value).toBe(true);

      composable.hoverProvider(null);
      expect(composable.hasHoveredProvider.value).toBe(false);
    });

    it('should calculate hasUserLocation', () => {
      expect(composable.hasUserLocation.value).toBe(false);

      composable.setUserLocation({ lat: 34.0, lng: -118.0 });
      expect(composable.hasUserLocation.value).toBe(true);

      composable.clearUserLocation();
      expect(composable.hasUserLocation.value).toBe(false);
    });

    it('should calculate hasDirections', () => {
      expect(composable.hasDirections.value).toBe(false);

      composable.setDirections(
        { distance: 1000 },
        { lat: 34.0, lng: -118.0 },
        { lat: 34.1, lng: -118.1 }
      );
      expect(composable.hasDirections.value).toBe(true);

      composable.clearDirections();
      expect(composable.hasDirections.value).toBe(false);
    });

    it('should calculate currentCenter', () => {
      expect(composable.currentCenter.value).toEqual({ lat: 34.0522, lng: -118.2437 });

      composable.centerOn({ lat: 35.0, lng: -119.0 });
      expect(composable.currentCenter.value).toEqual({ lat: 35.0, lng: -119.0 });
    });

    it('should calculate currentZoom', () => {
      expect(composable.currentZoom.value).toBe(10);

      composable.setViewport({ zoom: 15 });
      expect(composable.currentZoom.value).toBe(15);
    });
  });
});
