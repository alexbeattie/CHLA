/**
 * Provider Store
 * Centralized state management for provider data
 * Week 3: Pinia Store Architecture
 */

import { defineStore } from 'pinia';
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
}

export interface SearchParams {
  zipCode?: string;
  lat?: number;
  lng?: number;
  radius?: number;
  insurance?: string;
  therapy?: string;  // Single therapy (legacy from onboarding)
  therapies?: string[];  // Multiple therapies (multi-select filter)
  age?: string;
  diagnosis?: string;  // Single diagnosis (legacy from onboarding)
  diagnoses?: string[];  // Multiple diagnoses (multi-select filter)
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

export const useProviderStore = defineStore('provider', () => {
  // ==================== STATE ====================

  const providers = ref<Provider[]>([]);
  const selectedProvider = ref<Provider | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);
  const searchLocation = ref('');
  const searchCoordinates = ref<{ lat: number; lng: number } | null>(null);
  const regionalCenterInfo = ref<RegionalCenterInfo | null>(null);

  // API base URL - use VITE_API_BASE_URL for consistency
  const apiBaseUrl = ref(import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL || 'http://localhost:8000');

  // ==================== GETTERS ====================

  const providerCount = computed(() => providers.value.length);

  const providersWithCoordinates = computed(() =>
    providers.value.filter(p => p.latitude !== null && p.longitude !== null)
  );

  const hasProviders = computed(() => providers.value.length > 0);

  const selectedProviderId = computed(() => selectedProvider.value?.id || null);

  const hasRegionalCenter = computed(() => regionalCenterInfo.value !== null);

  const regionalCenterName = computed(() =>
    regionalCenterInfo.value?.name || 'Unknown'
  );

  // ==================== ACTIONS ====================

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
        url = `${apiBaseUrl.value}/api/providers-v2/by_regional_center/`;
        queryParams.append('zip_code', params.zipCode!);

        console.log(`🎯 [Store] Using REGIONAL CENTER filtering for ZIP: ${params.zipCode}`);
      } else {
        // Use comprehensive search for address/coordinate searches
        url = `${apiBaseUrl.value}/api/providers-v2/comprehensive_search/`;

        if (params.lat && params.lng) {
          queryParams.append('lat', params.lat.toString());
          queryParams.append('lng', params.lng.toString());
          queryParams.append('radius', (params.radius || 50).toString());
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

      // Handle single therapy (from onboarding legacy)
      if (params.therapy) {
        queryParams.append('therapy', params.therapy);
      }

      // Handle multiple therapies (from multi-select filter)
      if (params.therapies && Array.isArray(params.therapies)) {
        params.therapies.forEach((therapy: string) => {
          queryParams.append('therapy', therapy);
        });
      }

      if (params.age) {
        queryParams.append('age', params.age);
      }

      // Handle single diagnosis (from onboarding legacy)
      if (params.diagnosis) {
        queryParams.append('diagnosis', params.diagnosis);
      }

      // Handle multiple diagnoses (from multi-select filter)
      if (params.diagnoses && Array.isArray(params.diagnoses)) {
        params.diagnoses.forEach((diagnosis: string) => {
          queryParams.append('diagnosis', diagnosis);
        });
      }

      // Make API call
      console.log(`🔍 [Store] Fetching providers from: ${url}?${queryParams.toString()}`);
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
          `✅ [Store] Loaded ${providers.value.length} providers from regional center: ${regionalCenterInfo.value?.name || 'Unknown'}`
        );
      } else if (Array.isArray(response.data)) {
        // Comprehensive search returns array directly
        providers.value = response.data;

        result = {
          providers: providers.value,
          count: providers.value.length
        };

        console.log(`✅ [Store] Loaded ${providers.value.length} providers from comprehensive search`);
      } else {
        // Fallback for unexpected format
        providers.value = [];
        result = { providers: [], count: 0 };
        console.warn('[Store] Unexpected response format:', response.data);
      }

      return result;

    } catch (err: any) {
      error.value = err.message || 'Failed to fetch providers';
      providers.value = [];
      console.error('❌ [Store] Provider search error:', err);
      return null;
    } finally {
      loading.value = false;
    }
  }

  /**
   * Search by ZIP code specifically
   * Convenience method that uses regional center filtering
   */
  async function searchByZipCode(
    zipCode: string,
    filters?: Partial<SearchParams>
  ): Promise<ProviderSearchResult | null> {
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
  ): Promise<ProviderSearchResult | null> {
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
  async function searchWithFilters(filters: SearchParams): Promise<ProviderSearchResult | null> {
    return searchProviders(filters);
  }

  /**
   * Select a provider by ID
   */
  function selectProvider(id: number | null) {
    if (id === null) {
      selectedProvider.value = null;
      return;
    }

    const provider = providers.value.find(p => p.id === id);
    if (provider) {
      selectedProvider.value = provider;
      console.log(`📍 [Store] Selected provider: ${provider.name}`);
    } else {
      console.warn(`⚠️ [Store] Provider with ID ${id} not found`);
    }
  }

  /**
   * Get provider by ID
   */
  function getProviderById(id: number): Provider | undefined {
    return providers.value.find(p => p.id === id);
  }

  /**
   * Filter providers by predicate (client-side)
   */
  function filterProviders(predicate: (provider: Provider) => boolean): Provider[] {
    return providers.value.filter(predicate);
  }

  /**
   * Load ALL providers from database (no filters)
   */
  async function loadAllProviders(): Promise<ProviderSearchResult | null> {
    loading.value = true;
    error.value = null;
    regionalCenterInfo.value = null;

    try {
      const url = `${apiBaseUrl.value}/api/providers-v2/`;
      console.log(`🌍 [Store] Loading ALL providers from: ${url}`);

      const response = await axios.get(url);

      // Handle response
      const allProviders = Array.isArray(response.data) ? response.data : response.data.results || [];
      providers.value = allProviders;

      console.log(`✅ [Store] Loaded ${allProviders.length} total providers`);

      const result: ProviderSearchResult = {
        providers: allProviders,
        count: allProviders.length
      };

      return result;
    } catch (err: any) {
      error.value = err.response?.data?.message || err.message || 'Failed to load providers';
      console.error('❌ [Store] Error loading all providers:', err);
      return null;
    } finally {
      loading.value = false;
    }
  }

  /**
   * Clear all provider data and state
   */
  function clearProviders() {
    providers.value = [];
    selectedProvider.value = null;
    searchLocation.value = '';
    searchCoordinates.value = null;
    regionalCenterInfo.value = null;
    error.value = null;
    console.log('🧹 [Store] Cleared all provider data');
  }

  /**
   * Set API base URL (useful for testing)
   */
  function setApiBaseUrl(url: string) {
    apiBaseUrl.value = url;
  }

  // ==================== RETURN ====================

  return {
    // State
    providers,
    selectedProvider,
    loading,
    error,
    searchLocation,
    searchCoordinates,
    regionalCenterInfo,

    // Getters
    providerCount,
    providersWithCoordinates,
    hasProviders,
    selectedProviderId,
    hasRegionalCenter,
    regionalCenterName,

    // Actions
    searchProviders,
    searchByZipCode,
    searchByLocation,
    searchWithFilters,
    loadAllProviders,
    selectProvider,
    getProviderById,
    filterProviders,
    clearProviders,
    setApiBaseUrl
  };
});
