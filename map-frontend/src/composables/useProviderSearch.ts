/**
 * Provider Search Composable
 * Handles all provider searching and fetching logic
 * Extracted from MapView.vue as part of Week 2 refactoring
 */

import { ref, computed } from 'vue';
import axios from 'axios';
import { isValidZipCode } from '@/utils/map';

export interface Provider {
  id: number;
  name: string;
  address: string;
  latitude: number | null;
  longitude: number | null;
  phone: string | null;
  website: string | null;
  email: string | null;
  insurance_accepted: string;
  therapy_types: string[] | null;
  age_groups: string[] | null;
  diagnoses_treated: string[] | null;
  description: string | null;
  type: string | null;
  // Add other fields as needed
}

export interface SearchParams {
  zipCode?: string;
  lat?: number;
  lng?: number;
  radius?: number;
  insurance?: string;
  therapy?: string;
  age?: string;
  diagnosis?: string;
  searchText?: string;
}

export interface RegionalCenterInfo {
  id: number;
  name: string;
  zip_codes: string[];
}

export interface ProviderSearchResult {
  providers: Provider[];
  count: number;
  regional_center?: RegionalCenterInfo;
}

export function useProviderSearch(apiBaseUrl: string) {
  // State
  const providers = ref<Provider[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);
  const searchLocation = ref('');
  const searchCoordinates = ref<{ lat: number; lng: number } | null>(null);
  const regionalCenterInfo = ref<RegionalCenterInfo | null>(null);

  // Computed
  const providerCount = computed(() => providers.value.length);

  const providersWithCoordinates = computed(() =>
    providers.value.filter(p => p.latitude && p.longitude)
  );

  const hasProviders = computed(() => providers.value.length > 0);

  /**
   * Search providers using appropriate endpoint
   * Uses regional center filtering for ZIP codes, radius search for addresses
   */
  async function searchProviders(params: SearchParams): Promise<ProviderSearchResult | null> {
    loading.value = true;
    error.value = null;
    regionalCenterInfo.value = null;

    try {
      const isZipSearch = params.zipCode && isValidZipCode(params.zipCode);
      let url: string;
      const queryParams = new URLSearchParams();

      if (isZipSearch) {
        // Use regional center-based filtering for ZIP searches
        url = `${apiBaseUrl}/api/providers-v2/by_regional_center/`;
        queryParams.append('zip_code', params.zipCode!);

        console.log(`🎯 Using REGIONAL CENTER filtering for ZIP: ${params.zipCode}`);
      } else {
        // Use comprehensive search for address/coordinate searches
        url = `${apiBaseUrl}/api/providers-v2/comprehensive_search/`;

        if (params.lat && params.lng) {
          queryParams.append('lat', params.lat.toString());
          queryParams.append('lng', params.lng.toString());
          queryParams.append('radius', (params.radius || 25).toString());
        }

        if (params.searchText) {
          queryParams.append('q', params.searchText);
          queryParams.append('location', params.searchText);
        }
      }

      // Add common filters
      if (params.insurance) {
        queryParams.append('insurance', params.insurance);
      }

      if (params.therapy) {
        queryParams.append('therapy', params.therapy);
      }

      if (params.age) {
        queryParams.append('age', params.age);
      }

      if (params.diagnosis) {
        queryParams.append('diagnosis', params.diagnosis);
      }

      // Make API call
      console.log(`🔍 Fetching providers from: ${url}?${queryParams.toString()}`);
      const response = await axios.get(`${url}?${queryParams.toString()}`);

      // Handle different response formats
      let result: ProviderSearchResult;

      if (isZipSearch && response.data && response.data.results) {
        // Regional center endpoint returns {results: [...], count: N, regional_center: {...}}
        providers.value = response.data.results || [];
        regionalCenterInfo.value = response.data.regional_center || null;

        result = {
          providers: providers.value,
          count: response.data.count || providers.value.length,
          regional_center: regionalCenterInfo.value || undefined
        };

        console.log(
          `✅ Loaded ${providers.value.length} providers from regional center: ${regionalCenterInfo.value?.name || 'Unknown'}`
        );
      } else if (Array.isArray(response.data)) {
        // Comprehensive search returns array directly
        providers.value = response.data;

        result = {
          providers: providers.value,
          count: providers.value.length
        };

        console.log(`✅ Loaded ${providers.value.length} providers from comprehensive search`);
      } else {
        // Fallback for unexpected format
        providers.value = [];
        result = { providers: [], count: 0 };
        console.warn('Unexpected response format:', response.data);
      }

      return result;

    } catch (err: any) {
      error.value = err.message || 'Failed to fetch providers';
      providers.value = [];
      console.error('❌ Provider search error:', err);
      return null;
    } finally {
      loading.value = false;
    }
  }

  /**
   * Search by ZIP code specifically
   * Convenience method that uses regional center filtering
   */
  async function searchByZipCode(zipCode: string, filters?: Partial<SearchParams>) {
    if (!isValidZipCode(zipCode)) {
      error.value = 'Invalid ZIP code format';
      return null;
    }

    return searchProviders({
      zipCode,
      ...filters
    });
  }

  /**
   * Search by coordinates with radius
   * Uses comprehensive search endpoint
   */
  async function searchByLocation(
    lat: number,
    lng: number,
    radius: number = 25,
    filters?: Partial<SearchParams>
  ) {
    searchCoordinates.value = { lat, lng };

    return searchProviders({
      lat,
      lng,
      radius,
      ...filters
    });
  }

  /**
   * Search with multiple filters
   * Determines best endpoint based on available parameters
   */
  async function searchWithFilters(filters: SearchParams) {
    return searchProviders(filters);
  }

  /**
   * Clear all search results and state
   */
  function clearSearch() {
    providers.value = [];
    searchLocation.value = '';
    searchCoordinates.value = null;
    regionalCenterInfo.value = null;
    error.value = null;
  }

  /**
   * Get provider by ID
   */
  function getProviderById(id: number): Provider | undefined {
    return providers.value.find(p => p.id === id);
  }

  /**
   * Filter current providers by criteria (client-side)
   * Useful for quick filtering without re-fetching
   */
  function filterProviders(predicate: (provider: Provider) => boolean): Provider[] {
    return providers.value.filter(predicate);
  }

  return {
    // State
    providers,
    loading,
    error,
    searchLocation,
    searchCoordinates,
    regionalCenterInfo,

    // Computed
    providerCount,
    providersWithCoordinates,
    hasProviders,

    // Methods
    searchProviders,
    searchByZipCode,
    searchByLocation,
    searchWithFilters,
    clearSearch,
    getProviderById,
    filterProviders
  };
}
