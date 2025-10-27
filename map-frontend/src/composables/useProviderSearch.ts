/**
 * Provider Search Composable
 * Handles all provider searching and fetching logic
 * Week 3: Now delegates to Pinia providerStore for centralized state
 * Maintains backward compatibility as a composable wrapper
 */

import { computed } from 'vue';
import { useProviderStore } from '@/stores/providerStore';
import type {
  Provider,
  SearchParams,
  RegionalCenterInfo,
  ProviderSearchResult
} from '@/stores/providerStore';

// Re-export types for backward compatibility
export type { Provider, SearchParams, RegionalCenterInfo, ProviderSearchResult };

export function useProviderSearch(apiBaseUrl: string) {
  // Get the Pinia store instance
  const store = useProviderStore();

  // Set API base URL in store
  if (apiBaseUrl) {
    store.setApiBaseUrl(apiBaseUrl);
  }

  // Return store state and methods directly
  // This maintains the same API as before, but now uses centralized Pinia state
  const providers = computed(() => store.providers);
  const loading = computed(() => store.loading);
  const error = computed(() => store.error);
  const searchLocation = computed({
    get: () => store.searchLocation,
    set: (value: string) => { store.searchLocation = value; }
  });
  const searchCoordinates = computed(() => store.searchCoordinates);
  const regionalCenterInfo = computed(() => store.regionalCenterInfo);

  // Computed
  const providerCount = computed(() => store.providerCount);
  const providersWithCoordinates = computed(() => store.providersWithCoordinates);
  const hasProviders = computed(() => store.hasProviders);

  // Delegate methods to store
  // All complex logic is now in the store, these are just pass-through methods

  /**
   * Search by ZIP code specifically
   * Delegates to providerStore.searchByZipCode
   */
  async function searchByZipCode(zipCode: string, filters?: Partial<SearchParams>) {
    return store.searchByZipCode(zipCode, filters);
  }

  /**
   * Search by coordinates with radius
   * Delegates to providerStore.searchByLocation
   */
  async function searchByLocation(
    lat: number,
    lng: number,
    radius: number = 25,
    filters?: Partial<SearchParams>
  ) {
    return store.searchByLocation(lat, lng, radius, filters);
  }

  /**
   * Clear all search results and state
   * Delegates to providerStore.clearProviders
   */
  function clearSearch() {
    store.clearProviders();
  }

  /**
   * Get provider by ID
   * Delegates to providerStore.getProviderById
   */
  function getProviderById(id: number): Provider | undefined {
    return store.getProviderById(id);
  }

  /**
   * Filter current providers by criteria (client-side)
   * Delegates to providerStore.filterProviders
   */
  function filterProviders(predicate: (provider: Provider) => boolean): Provider[] {
    return store.filterProviders(predicate);
  }

  /**
   * Select a provider
   * Delegates to providerStore.selectProvider
   */
  function selectProvider(id: number | null) {
    store.selectProvider(id);
  }

  return {
    // State (as computed from store)
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

    // Methods (delegated to store)
    searchByZipCode,
    searchByLocation,
    clearSearch,
    getProviderById,
    filterProviders,
    selectProvider
  };
}
