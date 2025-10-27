/**
 * Regional Center Composable
 * Handles regional center data, boundaries, and ZIP code lookups
 * Extracted from MapView.vue as part of Week 2 refactoring
 */

import { ref, computed } from 'vue';
import axios from 'axios';
import { isValidZipCode } from '@/utils/map';

export interface RegionalCenter {
  id: number;
  name: string;
  regional_center: string;
  zip_codes: string[];
  service_area?: string;
  phone?: string;
  website?: string;
  email?: string;
}

export interface RegionalCenterBoundary {
  type: 'Polygon' | 'MultiPolygon';
  coordinates: number[][][] | number[][][][];
}

export function useRegionalCenter(apiBaseUrl: string) {
  // State
  const regionalCenters = ref<RegionalCenter[]>([]);
  const currentRegionalCenter = ref<RegionalCenter | null>(null);
  const regionalCenterBoundary = ref<RegionalCenterBoundary | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);

  // Computed
  const hasRegionalCenter = computed(() => currentRegionalCenter.value !== null);
  const regionalCenterName = computed(() => currentRegionalCenter.value?.regional_center || 'Unknown');
  const regionalCenterZipCodes = computed(() => currentRegionalCenter.value?.zip_codes || []);
  const hasBoundary = computed(() => regionalCenterBoundary.value !== null);

  /**
   * Fetch all regional centers
   */
  async function fetchRegionalCenters(): Promise<RegionalCenter[]> {
    loading.value = true;
    error.value = null;

    try {
      const response = await axios.get(`${apiBaseUrl}/api/regional-centers/`);
      regionalCenters.value = response.data;
      console.log(`✅ Loaded ${regionalCenters.value.length} regional centers`);
      return regionalCenters.value;
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch regional centers';
      console.error('❌ Regional center fetch error:', err);
      return [];
    } finally {
      loading.value = false;
    }
  }

  /**
   * Find regional center by ZIP code
   */
  async function findByZipCode(zipCode: string): Promise<RegionalCenter | null> {
    if (!isValidZipCode(zipCode)) {
      error.value = 'Invalid ZIP code format';
      return null;
    }

    loading.value = true;
    error.value = null;

    try {
      const response = await axios.get(`${apiBaseUrl}/api/regional-centers/by_zip_code/`, {
        params: { zip_code: zipCode }
      });

      if (response.data) {
        currentRegionalCenter.value = response.data;
        console.log(`🎯 Found regional center for ZIP ${zipCode}:`, response.data.regional_center);
        return response.data;
      }

      error.value = `No regional center found for ZIP ${zipCode}`;
      return null;
    } catch (err: any) {
      error.value = err.response?.data?.error || err.message || 'Failed to find regional center';
      console.error('❌ Regional center lookup error:', err);
      return null;
    } finally {
      loading.value = false;
    }
  }

  /**
   * Get regional center by ID
   */
  async function getById(id: number): Promise<RegionalCenter | null> {
    loading.value = true;
    error.value = null;

    try {
      const response = await axios.get(`${apiBaseUrl}/api/regional-centers/${id}/`);
      currentRegionalCenter.value = response.data;
      return response.data;
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch regional center';
      console.error('❌ Regional center fetch error:', err);
      return null;
    } finally {
      loading.value = false;
    }
  }

  /**
   * Set current regional center
   */
  function setRegionalCenter(rc: RegionalCenter | null) {
    currentRegionalCenter.value = rc;
    if (rc) {
      console.log(`📍 Current regional center: ${rc.regional_center}`);
    }
  }

  /**
   * Clear current regional center
   */
  function clearRegionalCenter() {
    currentRegionalCenter.value = null;
    regionalCenterBoundary.value = null;
    error.value = null;
  }

  /**
   * Check if a ZIP code is within current regional center
   */
  function isZipInRegionalCenter(zipCode: string): boolean {
    if (!currentRegionalCenter.value || !currentRegionalCenter.value.zip_codes) {
      return false;
    }

    return currentRegionalCenter.value.zip_codes.includes(zipCode);
  }

  /**
   * Get boundary polygon for regional center (if available)
   * This would fetch GeoJSON boundary data if implemented
   */
  async function fetchBoundary(regionalCenterId: number): Promise<RegionalCenterBoundary | null> {
    loading.value = true;
    error.value = null;

    try {
      // TODO: Implement when backend has boundary endpoint
      // For now, generate approximate boundary from ZIP codes
      console.log(`⚠️ Boundary fetching not yet implemented for RC ${regionalCenterId}`);
      return null;
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch boundary';
      console.error('❌ Boundary fetch error:', err);
      return null;
    } finally {
      loading.value = false;
    }
  }

  /**
   * Generate approximate boundary polygon from ZIP codes
   * Uses ZIP code centroids to create a convex hull
   * This is a temporary solution until proper boundary data is available
   */
  async function generateApproximateBoundary(
    zipCodes: string[],
    mapboxToken: string
  ): Promise<RegionalCenterBoundary | null> {
    if (zipCodes.length === 0) {
      return null;
    }

    try {
      // Geocode all ZIP codes to get their centroids
      const centroids: { lat: number; lng: number }[] = [];

      for (const zip of zipCodes) {
        const url = `https://api.mapbox.com/geocoding/v5/mapbox.places/${zip}.json?types=postcode&access_token=${mapboxToken}`;
        const response = await fetch(url);
        const data = await response.json();

        if (data.features && data.features.length > 0) {
          const [lng, lat] = data.features[0].center;
          centroids.push({ lat, lng });
        }
      }

      if (centroids.length < 3) {
        console.warn('Not enough ZIP codes to generate boundary');
        return null;
      }

      // Create a simple convex hull from centroids
      // For production, use a proper convex hull algorithm
      const boundary: RegionalCenterBoundary = {
        type: 'Polygon',
        coordinates: [centroids.map(c => [c.lng, c.lat])]
      };

      regionalCenterBoundary.value = boundary;
      console.log(`✅ Generated approximate boundary from ${centroids.length} ZIP codes`);
      return boundary;
    } catch (err: any) {
      error.value = err.message || 'Failed to generate boundary';
      console.error('❌ Boundary generation error:', err);
      return null;
    }
  }

  /**
   * Highlight regional center on map (returns GeoJSON for display)
   */
  function getHighlightGeoJSON(): any {
    if (!regionalCenterBoundary.value) {
      return null;
    }

    return {
      type: 'Feature',
      properties: {
        name: regionalCenterName.value,
        fill: 'rgba(66, 153, 225, 0.2)',
        stroke: 'rgba(66, 153, 225, 0.8)',
        'stroke-width': 2
      },
      geometry: regionalCenterBoundary.value
    };
  }

  /**
   * Find nearest regional center to coordinates
   * Useful for "where am I?" functionality
   */
  async function findNearestToCoordinates(
    lat: number,
    lng: number,
    mapboxToken: string
  ): Promise<RegionalCenter | null> {
    try {
      // Reverse geocode to get ZIP code
      const url = `https://api.mapbox.com/geocoding/v5/mapbox.places/${lng},${lat}.json?types=postcode&access_token=${mapboxToken}`;
      const response = await fetch(url);
      const data = await response.json();

      if (data.features && data.features.length > 0) {
        const zipCode = data.features[0].text;
        return await findByZipCode(zipCode);
      }

      return null;
    } catch (err: any) {
      error.value = err.message || 'Failed to find nearest regional center';
      console.error('❌ Nearest RC lookup error:', err);
      return null;
    }
  }

  return {
    // State
    regionalCenters,
    currentRegionalCenter,
    regionalCenterBoundary,
    loading,
    error,

    // Computed
    hasRegionalCenter,
    regionalCenterName,
    regionalCenterZipCodes,
    hasBoundary,

    // Methods
    fetchRegionalCenters,
    findByZipCode,
    getById,
    setRegionalCenter,
    clearRegionalCenter,
    isZipInRegionalCenter,
    fetchBoundary,
    generateApproximateBoundary,
    getHighlightGeoJSON,
    findNearestToCoordinates
  };
}
