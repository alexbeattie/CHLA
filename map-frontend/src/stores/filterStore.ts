/**
 * Filter Store
 * Centralized state management for filter state and user data
 * Week 3: Pinia Store Architecture
 */

import { defineStore } from 'pinia';
import { ref, computed, reactive } from 'vue';

export interface FilterOptions {
  acceptsInsurance: boolean;
  acceptsPrivatePay: boolean;
  matchesAge: boolean;
  matchesDiagnosis: boolean;
  matchesTherapy: boolean;
  showOnlyFavorites: boolean;
}

export interface UserData {
  insurance?: string;
  age?: string;
  diagnosis?: string;
  therapy?: string;
}

export const useFilterStore = defineStore('filter', () => {
  // ==================== STATE ====================

  // Filter toggle state
  const filterOptions = reactive<FilterOptions>({
    acceptsInsurance: false,
    acceptsPrivatePay: false,
    matchesAge: false,
    matchesDiagnosis: false,
    matchesTherapy: false,
    showOnlyFavorites: false
  });

  // User onboarding data
  const userData = reactive<UserData>({
    insurance: undefined,
    age: undefined,
    diagnosis: undefined,
    therapy: undefined
  });

  // Available filter options (from backend or config)
  const availableTherapyTypes = ref<string[]>([]);
  const availableAgeGroups = ref<string[]>([]);
  const availableDiagnoses = ref<string[]>([]);
  const availableInsuranceTypes = ref<string[]>([]);

  // ==================== GETTERS ====================

  const hasActiveFilters = computed(() => {
    return (
      filterOptions.acceptsInsurance ||
      filterOptions.acceptsPrivatePay ||
      filterOptions.matchesAge ||
      filterOptions.matchesDiagnosis ||
      filterOptions.matchesTherapy ||
      filterOptions.showOnlyFavorites
    );
  });

  const activeFilterCount = computed(() => {
    let count = 0;
    if (filterOptions.acceptsInsurance) count++;
    if (filterOptions.acceptsPrivatePay) count++;
    if (filterOptions.matchesAge) count++;
    if (filterOptions.matchesDiagnosis) count++;
    if (filterOptions.matchesTherapy) count++;
    if (filterOptions.showOnlyFavorites) count++;
    return count;
  });

  const hasUserData = computed(() => {
    return !!(
      userData.insurance ||
      userData.age ||
      userData.diagnosis ||
      userData.therapy
    );
  });

  /**
   * Build query parameters object from active filters
   * Used for API requests
   */
  const filterParams = computed(() => {
    const params: Record<string, string> = {};

    // Insurance filters
    if (filterOptions.acceptsInsurance) {
      params.insurance = 'insurance';
    } else if (filterOptions.acceptsPrivatePay) {
      params.insurance = 'private pay';
    }

    // User data filters (from onboarding)
    if (filterOptions.matchesAge && userData.age) {
      params.age = userData.age;
    }

    if (filterOptions.matchesDiagnosis && userData.diagnosis) {
      params.diagnosis = userData.diagnosis;
    }

    if (filterOptions.matchesTherapy && userData.therapy) {
      params.therapy = userData.therapy;
    }

    return params;
  });

  // ==================== ACTIONS ====================

  /**
   * Build query parameters object from active filters
   * Method version for compatibility
   */
  function buildFilterParams() {
    return filterParams.value;
  }

  /**
   * Reset all filters to default state
   */
  function resetFilters() {
    filterOptions.acceptsInsurance = false;
    filterOptions.acceptsPrivatePay = false;
    filterOptions.matchesAge = false;
    filterOptions.matchesDiagnosis = false;
    filterOptions.matchesTherapy = false;
    filterOptions.showOnlyFavorites = false;
    console.log('🧹 [Store] Reset all filters');
  }

  /**
   * Reset user data (from onboarding)
   */
  function resetUserData() {
    userData.insurance = undefined;
    userData.age = undefined;
    userData.diagnosis = undefined;
    userData.therapy = undefined;
    console.log('🧹 [Store] Reset user data');
  }

  /**
   * Toggle a specific filter on/off
   */
  function toggleFilter(filterName: keyof FilterOptions) {
    if (typeof filterOptions[filterName] === 'boolean') {
      filterOptions[filterName] = !filterOptions[filterName];

      // Handle mutual exclusivity for insurance filters
      if (filterName === 'acceptsInsurance' && filterOptions[filterName]) {
        filterOptions.acceptsPrivatePay = false;
      } else if (filterName === 'acceptsPrivatePay' && filterOptions[filterName]) {
        filterOptions.acceptsInsurance = false;
      }

      console.log(`🎛️ [Store] Toggled filter ${filterName}: ${filterOptions[filterName]}`);
    }
  }

  /**
   * Set filter from external source (e.g., onboarding flow)
   */
  function setFilter(filterName: keyof FilterOptions, value: boolean) {
    if (typeof filterOptions[filterName] === 'boolean') {
      filterOptions[filterName] = value;
      console.log(`🎛️ [Store] Set filter ${filterName}: ${value}`);
    }
  }

  /**
   * Update user data from onboarding
   */
  function updateUserData(data: Partial<UserData>) {
    Object.assign(userData, data);
    console.log('📝 [Store] Updated user data:', data);
  }

  /**
   * Load filter options from API or config
   */
  function setAvailableOptions(options: {
    therapyTypes?: string[];
    ageGroups?: string[];
    diagnoses?: string[];
    insuranceTypes?: string[];
  }) {
    if (options.therapyTypes) {
      availableTherapyTypes.value = options.therapyTypes;
    }
    if (options.ageGroups) {
      availableAgeGroups.value = options.ageGroups;
    }
    if (options.diagnoses) {
      availableDiagnoses.value = options.diagnoses;
    }
    if (options.insuranceTypes) {
      availableInsuranceTypes.value = options.insuranceTypes;
    }
    console.log('📋 [Store] Set available filter options');
  }

  /**
   * Apply filters from onboarding data
   * Automatically enables relevant filters based on user data
   */
  function applyOnboardingFilters() {
    if (userData.insurance) {
      const insuranceLower = userData.insurance.toLowerCase();
      if (insuranceLower.includes('private')) {
        filterOptions.acceptsPrivatePay = true;
      } else {
        filterOptions.acceptsInsurance = true;
      }
    }

    if (userData.age) {
      filterOptions.matchesAge = true;
    }

    if (userData.diagnosis) {
      filterOptions.matchesDiagnosis = true;
    }

    if (userData.therapy) {
      filterOptions.matchesTherapy = true;
    }

    console.log('✅ [Store] Applied onboarding filters');
  }

  /**
   * Clear all filters and user data
   */
  function clearAll() {
    resetFilters();
    resetUserData();
    console.log('🧹 [Store] Cleared all filter data');
  }

  // ==================== RETURN ====================

  return {
    // State
    filterOptions,
    userData,
    availableTherapyTypes,
    availableAgeGroups,
    availableDiagnoses,
    availableInsuranceTypes,

    // Getters
    hasActiveFilters,
    activeFilterCount,
    hasUserData,
    filterParams,

    // Actions
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
});
