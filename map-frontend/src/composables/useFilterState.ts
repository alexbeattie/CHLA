/**
 * Filter State Composable
 * Manages all filter state and logic
 * Week 3: Now delegates to Pinia filterStore for centralized state
 * Maintains backward compatibility as a composable wrapper
 */

import { computed } from 'vue';
import { useFilterStore } from '@/stores/filterStore';
import type {
  FilterOptions,
  UserData
} from '@/stores/filterStore';

// Re-export types for backward compatibility
export type { FilterOptions, UserData };

export function useFilterState() {
  // Get the Pinia store instance
  const store = useFilterStore();

  // Return store state as computed properties
  // This maintains reactivity while using centralized state
  const filterOptions = computed(() => store.filterOptions);
  const userData = computed(() => store.userData);
  const availableTherapyTypes = computed(() => store.availableTherapyTypes);
  const availableAgeGroups = computed(() => store.availableAgeGroups);
  const availableDiagnoses = computed(() => store.availableDiagnoses);
  const availableInsuranceTypes = computed(() => store.availableInsuranceTypes);

  // Computed properties (delegated to store)
  const hasActiveFilters = computed(() => store.hasActiveFilters);
  const activeFilterCount = computed(() => store.activeFilterCount);
  const hasUserData = computed(() => store.hasUserData);
  const filterParams = computed(() => store.filterParams);

  // Delegate all methods to store
  // All complex logic is now in the store, these are just pass-through methods

  /**
   * Build query parameters object from active filters
   * Delegates to filterStore.buildFilterParams
   */
  function buildFilterParams() {
    return store.buildFilterParams();
  }

  /**
   * Reset all filters to default state
   * Delegates to filterStore.resetFilters
   */
  function resetFilters() {
    return store.resetFilters();
  }

  /**
   * Reset user data
   * Delegates to filterStore.resetUserData
   */
  function resetUserData() {
    return store.resetUserData();
  }

  /**
   * Toggle a specific filter on/off
   * Delegates to filterStore.toggleFilter
   */
  function toggleFilter(filterName: keyof FilterOptions) {
    return store.toggleFilter(filterName);
  }

  /**
   * Set filter from external source
   * Delegates to filterStore.setFilter
   */
  function setFilter(filterName: keyof FilterOptions, value: boolean) {
    return store.setFilter(filterName, value);
  }

  /**
   * Update user data from onboarding
   * Delegates to filterStore.updateUserData
   */
  function updateUserData(data: Partial<UserData>) {
    return store.updateUserData(data);
  }

  /**
   * Load filter options from API or config
   * Delegates to filterStore.setAvailableOptions
   */
  function setAvailableOptions(options: {
    therapyTypes?: string[];
    ageGroups?: string[];
    diagnoses?: string[];
    insuranceTypes?: string[];
  }) {
    return store.setAvailableOptions(options);
  }

  /**
   * Apply filters from onboarding data
   * Delegates to filterStore.applyOnboardingFilters
   */
  function applyOnboardingFilters() {
    return store.applyOnboardingFilters();
  }

  /**
   * Clear all filters and user data
   * Delegates to filterStore.clearAll
   */
  function clearAll() {
    return store.clearAll();
  }

  return {
    // State (as computed from store)
    filterOptions,
    userData,
    availableTherapyTypes,
    availableAgeGroups,
    availableDiagnoses,
    availableInsuranceTypes,

    // Computed
    hasActiveFilters,
    activeFilterCount,
    hasUserData,
    filterParams,

    // Methods (delegated to store)
    buildFilterParams,
    resetFilters,
    resetUserData,
    toggleFilter,
    setFilter,
    updateUserData,
    setAvailableOptions,
    applyOnboardingFilters,
    clearAll
  };
}
