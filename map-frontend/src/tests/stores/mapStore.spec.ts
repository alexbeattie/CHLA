/**
 * Tests for mapStore
 * Pinia store for map viewport and UI state
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import { useMapStore } from '@/stores/mapStore';

describe('mapStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  describe('Initialization', () => {
    it('should initialize with default viewport', () => {
      const store = useMapStore();

      expect(store.viewport.center).toEqual({ lat: 34.0522, lng: -118.2437 });
      expect(store.viewport.zoom).toBe(10);
      expect(store.viewport.bearing).toBe(0);
      expect(store.viewport.pitch).toBe(0);
    });

    it('should initialize with default UI state', () => {
      const store = useMapStore();

      expect(store.uiState.showFilters).toBe(false);
      expect(store.uiState.showProviderDetails).toBe(false);
      expect(store.uiState.showOnboarding).toBe(false);
      expect(store.uiState.showDirections).toBe(false);
      expect(store.uiState.sidebarExpanded).toBe(true);
      expect(store.uiState.mapStyle).toBe('streets');
    });

    it('should initialize with null states', () => {
      const store = useMapStore();

      expect(store.bounds).toBe(null);
      expect(store.selectedProviderId).toBe(null);
      expect(store.hoveredProviderId).toBe(null);
      expect(store.userLocation).toBe(null);
      expect(store.directionsRoute).toBe(null);
    });

    it('should initialize with correct loading states', () => {
      const store = useMapStore();

      expect(store.mapLoading).toBe(true);
      expect(store.mapReady).toBe(false);
    });
  });

  describe('Viewport management', () => {
    it('should update viewport', () => {
      const store = useMapStore();

      store.setViewport({
        center: { lat: 34.0, lng: -118.0 },
        zoom: 12
      });

      expect(store.viewport.center).toEqual({ lat: 34.0, lng: -118.0 });
      expect(store.viewport.zoom).toBe(12);
    });

    it('should partially update viewport', () => {
      const store = useMapStore();

      store.setViewport({ zoom: 15 });

      expect(store.viewport.zoom).toBe(15);
      expect(store.viewport.center).toEqual({ lat: 34.0522, lng: -118.2437 });
    });

    it('should center on coordinates', () => {
      const store = useMapStore();

      store.centerOn({ lat: 33.0, lng: -117.0 });

      expect(store.viewport.center).toEqual({ lat: 33.0, lng: -117.0 });
    });

    it('should center on coordinates with zoom', () => {
      const store = useMapStore();

      store.centerOn({ lat: 33.0, lng: -117.0 }, 14);

      expect(store.viewport.center).toEqual({ lat: 33.0, lng: -117.0 });
      expect(store.viewport.zoom).toBe(14);
    });

    it('should fit to bounds', () => {
      const store = useMapStore();

      const bounds = {
        north: 35.0,
        south: 33.0,
        east: -117.0,
        west: -119.0
      };

      store.fitBounds(bounds);

      expect(store.bounds).toEqual(bounds);
    });

    it('should update bounds', () => {
      const store = useMapStore();

      const bounds = {
        north: 34.1,
        south: 33.9,
        east: -118.1,
        west: -118.3
      };

      store.updateBounds(bounds);

      expect(store.bounds).toEqual(bounds);
    });
  });

  describe('Provider selection', () => {
    it('should select a provider', () => {
      const store = useMapStore();

      store.selectProvider(123);

      expect(store.selectedProviderId).toBe(123);
      expect(store.uiState.showProviderDetails).toBe(true);
      expect(store.hasSelectedProvider).toBe(true);
    });

    it('should clear selection', () => {
      const store = useMapStore();

      store.selectProvider(123);
      store.clearSelection();

      expect(store.selectedProviderId).toBe(null);
      expect(store.uiState.showProviderDetails).toBe(false);
      expect(store.hasSelectedProvider).toBe(false);
    });

    it('should allow null selection', () => {
      const store = useMapStore();

      store.selectProvider(123);
      store.selectProvider(null);

      expect(store.selectedProviderId).toBe(null);
      expect(store.uiState.showProviderDetails).toBe(false);
    });

    it('should hover over provider', () => {
      const store = useMapStore();

      store.hoverProvider(456);

      expect(store.hoveredProviderId).toBe(456);
      expect(store.hasHoveredProvider).toBe(true);
    });

    it('should clear hover', () => {
      const store = useMapStore();

      store.hoverProvider(456);
      store.hoverProvider(null);

      expect(store.hoveredProviderId).toBe(null);
      expect(store.hasHoveredProvider).toBe(false);
    });
  });

  describe('UI state management', () => {
    it('should toggle UI elements', () => {
      const store = useMapStore();

      expect(store.uiState.showFilters).toBe(false);

      store.toggleUI('showFilters');
      expect(store.uiState.showFilters).toBe(true);

      store.toggleUI('showFilters');
      expect(store.uiState.showFilters).toBe(false);
    });

    it('should toggle sidebar', () => {
      const store = useMapStore();

      expect(store.uiState.sidebarExpanded).toBe(true);

      store.toggleSidebar();
      expect(store.uiState.sidebarExpanded).toBe(false);

      store.toggleSidebar();
      expect(store.uiState.sidebarExpanded).toBe(true);
    });

    it('should set map style', () => {
      const store = useMapStore();

      store.setMapStyle('satellite');
      expect(store.uiState.mapStyle).toBe('satellite');

      store.setMapStyle('outdoors');
      expect(store.uiState.mapStyle).toBe('outdoors');

      store.setMapStyle('streets');
      expect(store.uiState.mapStyle).toBe('streets');
    });

    it('should show and hide onboarding', () => {
      const store = useMapStore();

      store.showOnboarding();
      expect(store.uiState.showOnboarding).toBe(true);

      store.hideOnboarding();
      expect(store.uiState.showOnboarding).toBe(false);
    });

    it('should set map ready state', () => {
      const store = useMapStore();

      expect(store.mapReady).toBe(false);
      expect(store.mapLoading).toBe(true);

      store.setMapReady();

      expect(store.mapReady).toBe(true);
      expect(store.mapLoading).toBe(false);
    });
  });

  describe('User location', () => {
    it('should set user location', () => {
      const store = useMapStore();

      const coords = { lat: 34.0, lng: -118.0 };
      store.setUserLocation(coords, 10);

      expect(store.userLocation).toEqual(coords);
      expect(store.userLocationAccuracy).toBe(10);
      expect(store.hasUserLocation).toBe(true);
    });

    it('should set user location without accuracy', () => {
      const store = useMapStore();

      const coords = { lat: 34.0, lng: -118.0 };
      store.setUserLocation(coords);

      expect(store.userLocation).toEqual(coords);
      expect(store.userLocationAccuracy).toBe(null);
    });

    it('should clear user location', () => {
      const store = useMapStore();

      store.setUserLocation({ lat: 34.0, lng: -118.0 }, 10);
      store.clearUserLocation();

      expect(store.userLocation).toBe(null);
      expect(store.userLocationAccuracy).toBe(null);
      expect(store.hasUserLocation).toBe(false);
    });

    it('should center on user location', () => {
      const store = useMapStore();

      const coords = { lat: 34.5, lng: -118.5 };
      store.setUserLocation(coords);

      store.centerOnUserLocation();

      expect(store.viewport.center).toEqual(coords);
      expect(store.viewport.zoom).toBe(14);
    });

    it('should center on user location with custom zoom', () => {
      const store = useMapStore();

      const coords = { lat: 34.5, lng: -118.5 };
      store.setUserLocation(coords);

      store.centerOnUserLocation(16);

      expect(store.viewport.center).toEqual(coords);
      expect(store.viewport.zoom).toBe(16);
    });

    it('should not center if no user location', () => {
      const store = useMapStore();

      const originalCenter = { ...store.viewport.center };

      store.centerOnUserLocation();

      expect(store.viewport.center).toEqual(originalCenter);
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
      const store = useMapStore();

      store.setDirections(mockRoute, from, to);

      expect(store.directionsRoute).toEqual(mockRoute);
      expect(store.directionsFrom).toEqual(from);
      expect(store.directionsTo).toEqual(to);
      expect(store.uiState.showDirections).toBe(true);
      expect(store.hasDirections).toBe(true);
    });

    it('should clear directions', () => {
      const store = useMapStore();

      store.setDirections(mockRoute, from, to);
      store.clearDirections();

      expect(store.directionsRoute).toBe(null);
      expect(store.directionsFrom).toBe(null);
      expect(store.directionsTo).toBe(null);
      expect(store.uiState.showDirections).toBe(false);
      expect(store.hasDirections).toBe(false);
    });

    it('should get directions to provider', async () => {
      const store = useMapStore();

      const mockResponse = {
        routes: [mockRoute]
      };

      global.fetch = vi.fn().mockResolvedValue({
        json: async () => mockResponse
      });

      store.setUserLocation(from);

      const result = await store.getDirectionsTo(to, 'test-token');

      expect(result).toEqual(mockRoute);
      expect(store.directionsRoute).toEqual(mockRoute);
      expect(store.directionsFrom).toEqual(from);
      expect(store.directionsTo).toEqual(to);

      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('api.mapbox.com/directions')
      );
    });

    it('should return null if no user location for directions', async () => {
      const store = useMapStore();

      const result = await store.getDirectionsTo(to, 'test-token');

      expect(result).toBe(null);
      expect(store.directionsRoute).toBe(null);
    });

    it('should handle directions API errors', async () => {
      const store = useMapStore();

      global.fetch = vi.fn().mockRejectedValue(new Error('Network error'));

      store.setUserLocation(from);

      const result = await store.getDirectionsTo(to, 'test-token');

      expect(result).toBe(null);
      expect(store.directionsRoute).toBe(null);
    });

    it('should return null if no routes returned', async () => {
      const store = useMapStore();

      global.fetch = vi.fn().mockResolvedValue({
        json: async () => ({ routes: [] })
      });

      store.setUserLocation(from);

      const result = await store.getDirectionsTo(to, 'test-token');

      expect(result).toBe(null);
    });
  });

  describe('resetMap', () => {
    it('should reset map to initial state', () => {
      const store = useMapStore();

      // Change various states
      store.setViewport({
        center: { lat: 35.0, lng: -119.0 },
        zoom: 15,
        bearing: 45,
        pitch: 60
      });
      store.selectProvider(123);
      store.setDirections(
        { distance: 1000 },
        { lat: 34.0, lng: -118.0 },
        { lat: 34.1, lng: -118.1 }
      );

      // Reset
      store.resetMap();

      // Verify reset
      expect(store.viewport.center).toEqual({ lat: 34.0522, lng: -118.2437 });
      expect(store.viewport.zoom).toBe(10);
      expect(store.viewport.bearing).toBe(0);
      expect(store.viewport.pitch).toBe(0);
      expect(store.selectedProviderId).toBe(null);
      expect(store.directionsRoute).toBe(null);
    });
  });

  describe('Computed properties', () => {
    it('should calculate hasSelectedProvider', () => {
      const store = useMapStore();

      expect(store.hasSelectedProvider).toBe(false);

      store.selectProvider(123);
      expect(store.hasSelectedProvider).toBe(true);

      store.clearSelection();
      expect(store.hasSelectedProvider).toBe(false);
    });

    it('should calculate hasHoveredProvider', () => {
      const store = useMapStore();

      expect(store.hasHoveredProvider).toBe(false);

      store.hoverProvider(456);
      expect(store.hasHoveredProvider).toBe(true);

      store.hoverProvider(null);
      expect(store.hasHoveredProvider).toBe(false);
    });

    it('should calculate hasUserLocation', () => {
      const store = useMapStore();

      expect(store.hasUserLocation).toBe(false);

      store.setUserLocation({ lat: 34.0, lng: -118.0 });
      expect(store.hasUserLocation).toBe(true);

      store.clearUserLocation();
      expect(store.hasUserLocation).toBe(false);
    });

    it('should calculate hasDirections', () => {
      const store = useMapStore();

      expect(store.hasDirections).toBe(false);

      store.setDirections(
        { distance: 1000 },
        { lat: 34.0, lng: -118.0 },
        { lat: 34.1, lng: -118.1 }
      );
      expect(store.hasDirections).toBe(true);

      store.clearDirections();
      expect(store.hasDirections).toBe(false);
    });

    it('should calculate currentCenter', () => {
      const store = useMapStore();

      expect(store.currentCenter).toEqual({ lat: 34.0522, lng: -118.2437 });

      store.centerOn({ lat: 35.0, lng: -119.0 });
      expect(store.currentCenter).toEqual({ lat: 35.0, lng: -119.0 });
    });

    it('should calculate currentZoom', () => {
      const store = useMapStore();

      expect(store.currentZoom).toBe(10);

      store.setViewport({ zoom: 15 });
      expect(store.currentZoom).toBe(15);
    });
  });
});
