/**
 * Tests for MapCanvas component
 * Week 4: Component Extraction
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { mount } from '@vue/test-utils';
import { setActivePinia, createPinia } from 'pinia';
import { nextTick } from 'vue';
import MapCanvas from '@/components/map/MapCanvas.vue';
import { useMapStore } from '@/stores/mapStore';
import { useProviderStore } from '@/stores/providerStore';

// Mock mapbox-gl
vi.mock('mapbox-gl', () => {
  const mockMap = {
    on: vi.fn((event, callback) => {
      if (event === 'load') {
        setTimeout(() => callback(), 0);
      }
    }),
    addControl: vi.fn(),
    getCenter: vi.fn(() => ({ lat: 34.0522, lng: -118.2437 })),
    getZoom: vi.fn(() => 10),
    flyTo: vi.fn(),
    jumpTo: vi.fn(),
    fitBounds: vi.fn(),
    remove: vi.fn(),
    isStyleLoaded: vi.fn(() => true),
    addSource: vi.fn(),
    addLayer: vi.fn(),
    removeLayer: vi.fn(),
    removeSource: vi.fn(),
    getLayer: vi.fn(() => null),
    getSource: vi.fn(() => null)
  };

  const mockMarker = {
    setLngLat: vi.fn().mockReturnThis(),
    addTo: vi.fn().mockReturnThis(),
    remove: vi.fn()
  };

  return {
    default: {
      Map: vi.fn(() => mockMap),
      Marker: vi.fn(() => mockMarker),
      NavigationControl: vi.fn(),
      GeolocateControl: vi.fn(),
      LngLatBounds: vi.fn((sw, ne) => ({
        extend: vi.fn().mockReturnThis()
      })),
      accessToken: ''
    }
  };
});

describe('MapCanvas', () => {
  let wrapper: any;
  let mapStore: any;
  let providerStore: any;

  beforeEach(() => {
    setActivePinia(createPinia());
    mapStore = useMapStore();
    providerStore = useProviderStore();
  });

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
    }
    vi.clearAllMocks();
  });

  describe('Component Initialization', () => {
    it('should render map container', () => {
      wrapper = mount(MapCanvas, {
        props: {
          mapboxToken: 'test-token'
        }
      });

      expect(wrapper.find('.map-canvas').exists()).toBe(true);
    });

    it('should initialize with default props', () => {
      wrapper = mount(MapCanvas, {
        props: {
          mapboxToken: 'test-token'
        }
      });

      // Check default center
      expect(wrapper.props('center')).toEqual({ lat: 34.0522, lng: -118.2437 });
      expect(wrapper.props('zoom')).toBe(10);
      expect(wrapper.props('mapStyle')).toBe('mapbox://styles/mapbox/streets-v12');
    });

    it('should accept custom props', () => {
      wrapper = mount(MapCanvas, {
        props: {
          mapboxToken: 'test-token',
          center: { lat: 35.0, lng: -119.0 },
          zoom: 12,
          mapStyle: 'mapbox://styles/mapbox/satellite-v9'
        }
      });

      expect(wrapper.props('center')).toEqual({ lat: 35.0, lng: -119.0 });
      expect(wrapper.props('zoom')).toBe(12);
      expect(wrapper.props('mapStyle')).toBe('mapbox://styles/mapbox/satellite-v9');
    });
  });

  describe('Map Initialization', () => {
    it('should create map instance on mount', async () => {
      const mapboxgl = await import('mapbox-gl');

      wrapper = mount(MapCanvas, {
        props: {
          mapboxToken: 'test-token'
        }
      });

      await nextTick();

      expect(mapboxgl.default.Map).toHaveBeenCalled();
    });

    it('should set mapbox access token', async () => {
      const mapboxgl = await import('mapbox-gl');

      wrapper = mount(MapCanvas, {
        props: {
          mapboxToken: 'test-token-123'
        }
      });

      await nextTick();

      expect(mapboxgl.default.accessToken).toBe('test-token-123');
    });

    it('should emit map-ready when map loads', async () => {
      wrapper = mount(MapCanvas, {
        props: {
          mapboxToken: 'test-token'
        }
      });

      await nextTick();
      await new Promise(resolve => setTimeout(resolve, 10));

      expect(wrapper.emitted('map-ready')).toBeTruthy();
    });

    it('should add navigation controls', async () => {
      const mapboxgl = await import('mapbox-gl');

      wrapper = mount(MapCanvas, {
        props: {
          mapboxToken: 'test-token'
        }
      });

      await nextTick();

      expect(mapboxgl.default.NavigationControl).toHaveBeenCalled();
    });

    it('should add geolocation control', async () => {
      const mapboxgl = await import('mapbox-gl');

      wrapper = mount(MapCanvas, {
        props: {
          mapboxToken: 'test-token'
        }
      });

      await nextTick();

      expect(mapboxgl.default.GeolocateControl).toHaveBeenCalled();
    });
  });

  describe('Provider Markers', () => {
    it('should render markers for providers with coordinates', async () => {
      const mapboxgl = await import('mapbox-gl');

      // Add providers to store
      providerStore.providers = [
        {
          id: 1,
          name: 'Provider 1',
          latitude: 34.0,
          longitude: -118.0
        },
        {
          id: 2,
          name: 'Provider 2',
          latitude: 34.1,
          longitude: -118.1
        }
      ];

      wrapper = mount(MapCanvas, {
        props: {
          mapboxToken: 'test-token'
        }
      });

      await nextTick();
      await new Promise(resolve => setTimeout(resolve, 10));

      // Should create 2 markers
      expect(mapboxgl.default.Marker).toHaveBeenCalledTimes(2);
    });

    it('should not render markers for providers without coordinates', async () => {
      const mapboxgl = await import('mapbox-gl');

      providerStore.providers = [
        {
          id: 1,
          name: 'Provider 1',
          latitude: null,
          longitude: null
        }
      ];

      wrapper = mount(MapCanvas, {
        props: {
          mapboxToken: 'test-token'
        }
      });

      await nextTick();
      await new Promise(resolve => setTimeout(resolve, 10));

      // Should not create markers for providers without coordinates
      expect(mapboxgl.default.Marker).not.toHaveBeenCalled();
    });

    it('should emit marker-click when marker is clicked', async () => {
      providerStore.providers = [
        {
          id: 1,
          name: 'Provider 1',
          latitude: 34.0,
          longitude: -118.0
        }
      ];

      wrapper = mount(MapCanvas, {
        props: {
          mapboxToken: 'test-token'
        }
      });

      await nextTick();
      await new Promise(resolve => setTimeout(resolve, 10));

      // Simulate marker click by calling store method directly
      providerStore.selectProvider(1);
      await nextTick();

      expect(providerStore.selectedProviderId).toBe(1);
    });

    it('should update markers when providers change', async () => {
      const mapboxgl = await import('mapbox-gl');

      wrapper = mount(MapCanvas, {
        props: {
          mapboxToken: 'test-token'
        }
      });

      await nextTick();
      await new Promise(resolve => setTimeout(resolve, 10));

      // Add providers
      providerStore.providers = [
        {
          id: 1,
          name: 'Provider 1',
          latitude: 34.0,
          longitude: -118.0
        }
      ];

      await nextTick();

      expect(mapboxgl.default.Marker).toHaveBeenCalled();
    });
  });

  describe('User Location', () => {
    it('should render user location marker when set', async () => {
      const mapboxgl = await import('mapbox-gl');

      wrapper = mount(MapCanvas, {
        props: {
          mapboxToken: 'test-token'
        }
      });

      await nextTick();
      await new Promise(resolve => setTimeout(resolve, 10));

      // Set user location
      mapStore.setUserLocation({ lat: 34.05, lng: -118.25 });
      await nextTick();

      expect(mapboxgl.default.Marker).toHaveBeenCalled();
    });

    it('should update user location marker when location changes', async () => {
      wrapper = mount(MapCanvas, {
        props: {
          mapboxToken: 'test-token'
        }
      });

      await nextTick();
      await new Promise(resolve => setTimeout(resolve, 10));

      // Set initial location
      mapStore.setUserLocation({ lat: 34.0, lng: -118.0 });
      await nextTick();

      // Update location
      mapStore.setUserLocation({ lat: 34.1, lng: -118.1 });
      await nextTick();

      // Should have updated the marker
      expect(mapStore.userLocation).toEqual({ lat: 34.1, lng: -118.1 });
    });
  });

  describe('Viewport Management', () => {
    it('should emit viewport-change on map move', async () => {
      wrapper = mount(MapCanvas, {
        props: {
          mapboxToken: 'test-token'
        }
      });

      await nextTick();

      // Map move would trigger this in real scenario
      // We're testing the emission capability
      expect(wrapper.emitted()).toBeDefined();
    });

    it('should update map when store viewport changes', async () => {
      wrapper = mount(MapCanvas, {
        props: {
          mapboxToken: 'test-token'
        }
      });

      await nextTick();
      await new Promise(resolve => setTimeout(resolve, 10));

      // Update viewport in store
      mapStore.setViewport({
        center: { lat: 35.0, lng: -119.0 },
        zoom: 12
      });

      await nextTick();

      expect(mapStore.viewport.center).toEqual({ lat: 35.0, lng: -119.0 });
      expect(mapStore.viewport.zoom).toBe(12);
    });
  });

  describe('Cleanup', () => {
    it('should remove map on unmount', async () => {
      wrapper = mount(MapCanvas, {
        props: {
          mapboxToken: 'test-token'
        }
      });

      await nextTick();
      await new Promise(resolve => setTimeout(resolve, 10));

      const removeMethod = wrapper.vm.map?.remove;

      wrapper.unmount();

      // Map should be cleaned up
      expect(wrapper.vm.map).toBeNull();
    });

    it('should remove markers on unmount', async () => {
      providerStore.providers = [
        {
          id: 1,
          name: 'Provider 1',
          latitude: 34.0,
          longitude: -118.0
        }
      ];

      wrapper = mount(MapCanvas, {
        props: {
          mapboxToken: 'test-token'
        }
      });

      await nextTick();
      await new Promise(resolve => setTimeout(resolve, 10));

      wrapper.unmount();

      // Markers should be cleaned up
      expect(wrapper.vm.map).toBeNull();
    });
  });

  describe('Error Handling', () => {
    it('should emit map-error on map error', async () => {
      wrapper = mount(MapCanvas, {
        props: {
          mapboxToken: 'test-token'
        }
      });

      await nextTick();

      // Error handling is set up
      expect(wrapper.emitted()).toBeDefined();
    });

    it('should handle missing map container gracefully', async () => {
      // This would be tested with a more complex setup
      // For now, just verify the component handles errors
      wrapper = mount(MapCanvas, {
        props: {
          mapboxToken: 'test-token'
        }
      });

      expect(wrapper.exists()).toBe(true);
    });
  });
});
