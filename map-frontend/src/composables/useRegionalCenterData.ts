/**
 * useRegionalCenterData Composable
 * Manages LA Regional Center data, selection, and visualization
 * Extracted from MapView.vue to reduce complexity
 */

import { ref, computed } from 'vue';
import axios from 'axios';
import {
  LA_REGIONAL_CENTERS,
  getRegionalCenterColor,
  getRegionalCenterCoordinates,
  getRegionalCenterNames,
} from '@/constants/regionalCenters';
import { Coordinates, calculateDistance, findNearestPoint } from '@/utils/map/coordinates';

export interface RegionalCenter {
  id: string;
  name: string;
  center_id?: string;
  regional_center?: string;
  phone?: string;
  address?: string;
  website?: string;
  service_areas?: string[];
  zip_codes?: string[];
  office_type?: string;
  county_served?: string;
  suite?: string;
  city?: string;
  state?: string;
  zip_code?: string;
  address_street?: string;
  latitude?: number;
  longitude?: number;
  lat?: number;
  lng?: number;
  coordinates?: {
    lat: number;
    lng: number;
  };
}

export function useRegionalCenterData() {
  // State
  const regionalCenters = ref<RegionalCenter[]>([]);
  const selectedRegionalCenters = ref<Record<string, boolean>>({});
  const loading = ref(false);
  const error = ref<string | null>(null);

  // Initialize selection state with all centers selected
  const initializeSelection = () => {
    const names = getRegionalCenterNames();
    names.forEach((name) => {
      selectedRegionalCenters.value[name] = true;
    });
  };

  // Computed
  const selectedCenters = computed(() => {
    return regionalCenters.value.filter(
      (center) => selectedRegionalCenters.value[center.name]
    );
  });

  const allSelected = computed(() => {
    return Object.values(selectedRegionalCenters.value).every((selected) => selected);
  });

  const noneSelected = computed(() => {
    return Object.values(selectedRegionalCenters.value).every((selected) => !selected);
  });

  /**
   * Fetch regional centers from API
   */
  async function fetchRegionalCenters(): Promise<void> {
    loading.value = true;
    error.value = null;

    try {
      const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8000';
      const response = await axios.get(`${apiBaseUrl}/api/regional-centers/service_area_boundaries/`, {
        headers: { Accept: 'application/json' },
      });

      if (response.data && response.data.features) {
        regionalCenters.value = response.data.features.map((feature: any) => ({
          id: feature.properties.center_id || feature.properties.name,
          name: feature.properties.name,
          center_id: feature.properties.center_id,
          regional_center: feature.properties.name,
          phone: feature.properties.phone,
          address: feature.properties.address,
          website: feature.properties.website,
          service_areas: feature.properties.service_areas,
          zip_codes: feature.properties.zip_codes,
          office_type: feature.properties.office_type,
          county_served: feature.properties.county_served,
          suite: feature.properties.suite,
          city: feature.properties.city,
          state: feature.properties.state,
          zip_code: feature.properties.zip_code,
          address_street: feature.properties.address_street,
          latitude: feature.properties.latitude,
          longitude: feature.properties.longitude,
          coordinates: feature.properties.latitude && feature.properties.longitude
            ? {
                lat: feature.properties.latitude,
                lng: feature.properties.longitude,
              }
            : getRegionalCenterCoordinates(feature.properties.name),
        }));

        // Initialize selection state if not already done
        if (Object.keys(selectedRegionalCenters.value).length === 0) {
          initializeSelection();
        }
      }
    } catch (err: any) {
      error.value = err.message || 'Failed to load regional centers';
      console.error('Error fetching regional centers:', err);
    } finally {
      loading.value = false;
    }
  }

  /**
   * Find regional center by ZIP code
   */
  function findByZipCode(zipCode: string): RegionalCenter | null {
    return (
      regionalCenters.value.find((center) =>
        center.zip_codes?.includes(zipCode)
      ) || null
    );
  }

  /**
   * Find regional center by ID
   */
  function findById(id: string): RegionalCenter | null {
    return regionalCenters.value.find((center) => center.id === id || center.center_id === id) || null;
  }

  /**
   * Find regional center by name
   */
  function findByName(name: string): RegionalCenter | null {
    return regionalCenters.value.find((center) => center.name === name) || null;
  }

  /**
   * Find nearest regional center to coordinates
   */
  function findNearestToCoordinates(coords: Coordinates): { center: RegionalCenter; distance: number } | null {
    if (regionalCenters.value.length === 0) return null;

    // Convert centers to coordinate format
    const centersWithCoords = regionalCenters.value
      .map((center) => {
        const centerCoords = center.coordinates || getRegionalCenterCoordinates(center.name);
        if (!centerCoords) return null;

        return {
          ...center,
          lat: centerCoords.lat,
          lng: centerCoords.lng,
        };
      })
      .filter((c): c is RegionalCenter & { lat: number; lng: number } => c !== null);

    const result = findNearestPoint(coords, centersWithCoords);
    if (!result) return null;

    return {
      center: result.point,
      distance: result.distance,
    };
  }

  /**
   * Get sorted list of centers by distance from a point
   */
  function getSortedByDistance(coords: Coordinates): Array<{ center: RegionalCenter; distance: number }> {
    const centersWithCoords = regionalCenters.value
      .map((center) => {
        const centerCoords = center.coordinates || getRegionalCenterCoordinates(center.name);
        if (!centerCoords) return null;

        const distance = calculateDistance(
          coords.lat,
          coords.lng,
          centerCoords.lat,
          centerCoords.lng
        );

        return {
          center,
          distance,
        };
      })
      .filter((item): item is { center: RegionalCenter; distance: number } => item !== null);

    return centersWithCoords.sort((a, b) => a.distance - b.distance);
  }

  /**
   * Toggle a regional center's visibility
   */
  function toggleCenter(name: string) {
    selectedRegionalCenters.value[name] = !selectedRegionalCenters.value[name];
  }

  /**
   * Select all regional centers
   */
  function selectAll() {
    const names = getRegionalCenterNames();
    names.forEach((name) => {
      selectedRegionalCenters.value[name] = true;
    });
  }

  /**
   * Deselect all regional centers
   */
  function deselectAll() {
    const names = getRegionalCenterNames();
    names.forEach((name) => {
      selectedRegionalCenters.value[name] = false;
    });
  }

  /**
   * Get color for a regional center
   */
  function getCenterColor(name: string): string {
    return getRegionalCenterColor(name);
  }

  /**
   * Get coordinates for a regional center
   */
  function getCenterCoordinates(name: string): Coordinates | null {
    const center = findByName(name);
    if (center?.coordinates) {
      return center.coordinates;
    }
    return getRegionalCenterCoordinates(name);
  }

  /**
   * Get list of regional centers with colors for legend
   */
  function getCentersForLegend() {
    return getRegionalCenterNames().map((name) => ({
      name,
      color: getCenterColor(name),
      selected: selectedRegionalCenters.value[name] ?? false,
    }));
  }

  return {
    // State
    regionalCenters,
    selectedRegionalCenters,
    loading,
    error,

    // Computed
    selectedCenters,
    allSelected,
    noneSelected,

    // Methods
    fetchRegionalCenters,
    findByZipCode,
    findById,
    findByName,
    findNearestToCoordinates,
    getSortedByDistance,
    toggleCenter,
    selectAll,
    deselectAll,
    getCenterColor,
    getCenterCoordinates,
    getCentersForLegend,
  };
}
