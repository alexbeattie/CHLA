/**
 * Tests for filterStore
 * Pinia store for filter state and user data
 */

import { describe, it, expect, beforeEach } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import { useFilterStore } from '@/stores/filterStore';

describe('filterStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  describe('Initialization', () => {
    it('should initialize with default filter options', () => {
      const store = useFilterStore();

      expect(store.filterOptions.acceptsInsurance).toBe(false);
      expect(store.filterOptions.acceptsRegionalCenter).toBe(false);
      expect(store.filterOptions.acceptsPrivatePay).toBe(false);
      expect(store.filterOptions.matchesAge).toBe(false);
      expect(store.filterOptions.matchesDiagnosis).toBe(false);
      expect(store.filterOptions.matchesTherapy).toBe(false);
      expect(store.filterOptions.showOnlyFavorites).toBe(false);
    });

    it('should initialize with empty user data', () => {
      const store = useFilterStore();

      expect(store.userData.insurance).toBeUndefined();
      expect(store.userData.age).toBeUndefined();
      expect(store.userData.diagnosis).toBeUndefined();
      expect(store.userData.therapy).toBeUndefined();
    });

    it('should initialize with empty available options', () => {
      const store = useFilterStore();

      expect(store.availableTherapyTypes).toEqual([]);
      expect(store.availableAgeGroups).toEqual([]);
      expect(store.availableDiagnoses).toEqual([]);
      expect(store.availableInsuranceTypes).toEqual([]);
    });

    it('should initialize computed properties correctly', () => {
      const store = useFilterStore();

      expect(store.hasActiveFilters).toBe(false);
      expect(store.activeFilterCount).toBe(0);
      expect(store.hasUserData).toBe(false);
      expect(store.filterParams).toEqual({});
    });
  });

  describe('toggleFilter', () => {
    it('should toggle a filter on and off', () => {
      const store = useFilterStore();

      expect(store.filterOptions.matchesAge).toBe(false);

      store.toggleFilter('matchesAge');
      expect(store.filterOptions.matchesAge).toBe(true);

      store.toggleFilter('matchesAge');
      expect(store.filterOptions.matchesAge).toBe(false);
    });

    it('should handle mutual exclusivity for insurance filters', () => {
      const store = useFilterStore();

      // Enable acceptsInsurance
      store.toggleFilter('acceptsInsurance');
      expect(store.filterOptions.acceptsInsurance).toBe(true);
      expect(store.filterOptions.acceptsRegionalCenter).toBe(false);
      expect(store.filterOptions.acceptsPrivatePay).toBe(false);

      // Enable acceptsRegionalCenter (should disable acceptsInsurance)
      store.toggleFilter('acceptsRegionalCenter');
      expect(store.filterOptions.acceptsInsurance).toBe(false);
      expect(store.filterOptions.acceptsRegionalCenter).toBe(true);
      expect(store.filterOptions.acceptsPrivatePay).toBe(false);

      // Enable acceptsPrivatePay (should disable acceptsRegionalCenter)
      store.toggleFilter('acceptsPrivatePay');
      expect(store.filterOptions.acceptsInsurance).toBe(false);
      expect(store.filterOptions.acceptsRegionalCenter).toBe(false);
      expect(store.filterOptions.acceptsPrivatePay).toBe(true);
    });

    it('should not affect other filters when toggling insurance', () => {
      const store = useFilterStore();

      store.toggleFilter('matchesAge');
      store.toggleFilter('matchesDiagnosis');

      store.toggleFilter('acceptsInsurance');

      expect(store.filterOptions.matchesAge).toBe(true);
      expect(store.filterOptions.matchesDiagnosis).toBe(true);
    });
  });

  describe('setFilter', () => {
    it('should set a filter to a specific value', () => {
      const store = useFilterStore();

      store.setFilter('matchesAge', true);
      expect(store.filterOptions.matchesAge).toBe(true);

      store.setFilter('matchesAge', false);
      expect(store.filterOptions.matchesAge).toBe(false);
    });

    it('should set multiple filters independently', () => {
      const store = useFilterStore();

      store.setFilter('matchesAge', true);
      store.setFilter('matchesDiagnosis', true);
      store.setFilter('matchesTherapy', false);

      expect(store.filterOptions.matchesAge).toBe(true);
      expect(store.filterOptions.matchesDiagnosis).toBe(true);
      expect(store.filterOptions.matchesTherapy).toBe(false);
    });
  });

  describe('updateUserData', () => {
    it('should update user data with partial data', () => {
      const store = useFilterStore();

      store.updateUserData({ age: '3-5' });
      expect(store.userData.age).toBe('3-5');
      expect(store.userData.insurance).toBeUndefined();

      store.updateUserData({ diagnosis: 'Autism' });
      expect(store.userData.age).toBe('3-5');
      expect(store.userData.diagnosis).toBe('Autism');
    });

    it('should update user data with complete data', () => {
      const store = useFilterStore();

      store.updateUserData({
        insurance: 'Regional Center',
        age: '3-5',
        diagnosis: 'Autism',
        therapy: 'ABA'
      });

      expect(store.userData.insurance).toBe('Regional Center');
      expect(store.userData.age).toBe('3-5');
      expect(store.userData.diagnosis).toBe('Autism');
      expect(store.userData.therapy).toBe('ABA');
    });

    it('should overwrite existing user data', () => {
      const store = useFilterStore();

      store.updateUserData({ age: '3-5' });
      expect(store.userData.age).toBe('3-5');

      store.updateUserData({ age: '6-12' });
      expect(store.userData.age).toBe('6-12');
    });
  });

  describe('setAvailableOptions', () => {
    it('should set available therapy types', () => {
      const store = useFilterStore();

      const therapyTypes = ['ABA', 'OT', 'Speech Therapy'];
      store.setAvailableOptions({ therapyTypes });

      expect(store.availableTherapyTypes).toEqual(therapyTypes);
    });

    it('should set available age groups', () => {
      const store = useFilterStore();

      const ageGroups = ['0-2', '3-5', '6-12', '13-17', '18+'];
      store.setAvailableOptions({ ageGroups });

      expect(store.availableAgeGroups).toEqual(ageGroups);
    });

    it('should set available diagnoses', () => {
      const store = useFilterStore();

      const diagnoses = ['Autism', 'ADHD', 'Down Syndrome'];
      store.setAvailableOptions({ diagnoses });

      expect(store.availableDiagnoses).toEqual(diagnoses);
    });

    it('should set available insurance types', () => {
      const store = useFilterStore();

      const insuranceTypes = ['Regional Center', 'Private Pay', 'Insurance'];
      store.setAvailableOptions({ insuranceTypes });

      expect(store.availableInsuranceTypes).toEqual(insuranceTypes);
    });

    it('should set multiple option types at once', () => {
      const store = useFilterStore();

      store.setAvailableOptions({
        therapyTypes: ['ABA', 'OT'],
        ageGroups: ['3-5', '6-12'],
        diagnoses: ['Autism'],
        insuranceTypes: ['Regional Center']
      });

      expect(store.availableTherapyTypes).toEqual(['ABA', 'OT']);
      expect(store.availableAgeGroups).toEqual(['3-5', '6-12']);
      expect(store.availableDiagnoses).toEqual(['Autism']);
      expect(store.availableInsuranceTypes).toEqual(['Regional Center']);
    });
  });

  describe('applyOnboardingFilters', () => {
    it('should enable Regional Center filter for regional center insurance', () => {
      const store = useFilterStore();

      store.updateUserData({ insurance: 'Regional Center' });
      store.applyOnboardingFilters();

      expect(store.filterOptions.acceptsRegionalCenter).toBe(true);
      expect(store.filterOptions.acceptsInsurance).toBe(false);
      expect(store.filterOptions.acceptsPrivatePay).toBe(false);
    });

    it('should enable private pay filter for private insurance', () => {
      const store = useFilterStore();

      store.updateUserData({ insurance: 'Private Pay' });
      store.applyOnboardingFilters();

      expect(store.filterOptions.acceptsPrivatePay).toBe(true);
      expect(store.filterOptions.acceptsInsurance).toBe(false);
      expect(store.filterOptions.acceptsRegionalCenter).toBe(false);
    });

    it('should enable insurance filter for other insurance types', () => {
      const store = useFilterStore();

      store.updateUserData({ insurance: 'Blue Cross' });
      store.applyOnboardingFilters();

      expect(store.filterOptions.acceptsInsurance).toBe(true);
      expect(store.filterOptions.acceptsRegionalCenter).toBe(false);
      expect(store.filterOptions.acceptsPrivatePay).toBe(false);
    });

    it('should enable age filter when age is set', () => {
      const store = useFilterStore();

      store.updateUserData({ age: '3-5' });
      store.applyOnboardingFilters();

      expect(store.filterOptions.matchesAge).toBe(true);
    });

    it('should enable diagnosis filter when diagnosis is set', () => {
      const store = useFilterStore();

      store.updateUserData({ diagnosis: 'Autism' });
      store.applyOnboardingFilters();

      expect(store.filterOptions.matchesDiagnosis).toBe(true);
    });

    it('should enable therapy filter when therapy is set', () => {
      const store = useFilterStore();

      store.updateUserData({ therapy: 'ABA' });
      store.applyOnboardingFilters();

      expect(store.filterOptions.matchesTherapy).toBe(true);
    });

    it('should enable multiple filters based on user data', () => {
      const store = useFilterStore();

      store.updateUserData({
        insurance: 'Regional Center',
        age: '3-5',
        diagnosis: 'Autism',
        therapy: 'ABA'
      });

      store.applyOnboardingFilters();

      expect(store.filterOptions.acceptsRegionalCenter).toBe(true);
      expect(store.filterOptions.matchesAge).toBe(true);
      expect(store.filterOptions.matchesDiagnosis).toBe(true);
      expect(store.filterOptions.matchesTherapy).toBe(true);
    });
  });

  describe('buildFilterParams / filterParams', () => {
    it('should build empty params when no filters active', () => {
      const store = useFilterStore();

      const params = store.buildFilterParams();
      expect(params).toEqual({});
      expect(store.filterParams).toEqual({});
    });

    it('should build params for insurance filter', () => {
      const store = useFilterStore();

      store.toggleFilter('acceptsInsurance');

      const params = store.buildFilterParams();
      expect(params.insurance).toBe('insurance');
    });

    it('should build params for regional center filter', () => {
      const store = useFilterStore();

      store.toggleFilter('acceptsRegionalCenter');

      const params = store.buildFilterParams();
      expect(params.insurance).toBe('regional center');
    });

    it('should build params for private pay filter', () => {
      const store = useFilterStore();

      store.toggleFilter('acceptsPrivatePay');

      const params = store.buildFilterParams();
      expect(params.insurance).toBe('private pay');
    });

    it('should build params for age filter with user data', () => {
      const store = useFilterStore();

      store.updateUserData({ age: '3-5' });
      store.toggleFilter('matchesAge');

      const params = store.buildFilterParams();
      expect(params.age).toBe('3-5');
    });

    it('should not include age param if filter enabled but no user data', () => {
      const store = useFilterStore();

      store.toggleFilter('matchesAge');

      const params = store.buildFilterParams();
      expect(params.age).toBeUndefined();
    });

    it('should build params for diagnosis filter with user data', () => {
      const store = useFilterStore();

      store.updateUserData({ diagnosis: 'Autism' });
      store.toggleFilter('matchesDiagnosis');

      const params = store.buildFilterParams();
      expect(params.diagnosis).toBe('Autism');
    });

    it('should build params for therapy filter with user data', () => {
      const store = useFilterStore();

      store.updateUserData({ therapy: 'ABA' });
      store.toggleFilter('matchesTherapy');

      const params = store.buildFilterParams();
      expect(params.therapy).toBe('ABA');
    });

    it('should build complete params for multiple filters', () => {
      const store = useFilterStore();

      store.updateUserData({
        age: '3-5',
        diagnosis: 'Autism',
        therapy: 'ABA'
      });

      store.toggleFilter('acceptsRegionalCenter');
      store.toggleFilter('matchesAge');
      store.toggleFilter('matchesDiagnosis');
      store.toggleFilter('matchesTherapy');

      const params = store.buildFilterParams();
      expect(params.insurance).toBe('regional center');
      expect(params.age).toBe('3-5');
      expect(params.diagnosis).toBe('Autism');
      expect(params.therapy).toBe('ABA');
    });
  });

  describe('resetFilters', () => {
    it('should reset all filter options to default', () => {
      const store = useFilterStore();

      // Set some filters
      store.toggleFilter('acceptsRegionalCenter');
      store.toggleFilter('matchesAge');
      store.toggleFilter('matchesDiagnosis');

      // Reset
      store.resetFilters();

      expect(store.filterOptions.acceptsInsurance).toBe(false);
      expect(store.filterOptions.acceptsRegionalCenter).toBe(false);
      expect(store.filterOptions.acceptsPrivatePay).toBe(false);
      expect(store.filterOptions.matchesAge).toBe(false);
      expect(store.filterOptions.matchesDiagnosis).toBe(false);
      expect(store.filterOptions.matchesTherapy).toBe(false);
      expect(store.filterOptions.showOnlyFavorites).toBe(false);
    });

    it('should not reset user data', () => {
      const store = useFilterStore();

      store.updateUserData({ age: '3-5', diagnosis: 'Autism' });
      store.toggleFilter('matchesAge');

      store.resetFilters();

      expect(store.userData.age).toBe('3-5');
      expect(store.userData.diagnosis).toBe('Autism');
    });
  });

  describe('resetUserData', () => {
    it('should reset all user data to undefined', () => {
      const store = useFilterStore();

      // Set user data
      store.updateUserData({
        insurance: 'Regional Center',
        age: '3-5',
        diagnosis: 'Autism',
        therapy: 'ABA'
      });

      // Reset
      store.resetUserData();

      expect(store.userData.insurance).toBeUndefined();
      expect(store.userData.age).toBeUndefined();
      expect(store.userData.diagnosis).toBeUndefined();
      expect(store.userData.therapy).toBeUndefined();
    });

    it('should not reset filter options', () => {
      const store = useFilterStore();

      store.toggleFilter('matchesAge');
      store.updateUserData({ age: '3-5' });

      store.resetUserData();

      expect(store.filterOptions.matchesAge).toBe(true);
    });
  });

  describe('clearAll', () => {
    it('should clear all filters and user data', () => {
      const store = useFilterStore();

      // Set filters and user data
      store.updateUserData({
        insurance: 'Regional Center',
        age: '3-5',
        diagnosis: 'Autism'
      });
      store.toggleFilter('acceptsRegionalCenter');
      store.toggleFilter('matchesAge');
      store.toggleFilter('matchesDiagnosis');

      // Clear all
      store.clearAll();

      // Verify filters cleared
      expect(store.filterOptions.acceptsRegionalCenter).toBe(false);
      expect(store.filterOptions.matchesAge).toBe(false);
      expect(store.filterOptions.matchesDiagnosis).toBe(false);

      // Verify user data cleared
      expect(store.userData.insurance).toBeUndefined();
      expect(store.userData.age).toBeUndefined();
      expect(store.userData.diagnosis).toBeUndefined();
    });
  });

  describe('Computed properties', () => {
    it('should calculate hasActiveFilters correctly', () => {
      const store = useFilterStore();

      expect(store.hasActiveFilters).toBe(false);

      store.toggleFilter('matchesAge');
      expect(store.hasActiveFilters).toBe(true);

      store.resetFilters();
      expect(store.hasActiveFilters).toBe(false);

      store.toggleFilter('acceptsRegionalCenter');
      expect(store.hasActiveFilters).toBe(true);
    });

    it('should calculate activeFilterCount correctly', () => {
      const store = useFilterStore();

      expect(store.activeFilterCount).toBe(0);

      store.toggleFilter('matchesAge');
      expect(store.activeFilterCount).toBe(1);

      store.toggleFilter('matchesDiagnosis');
      expect(store.activeFilterCount).toBe(2);

      store.toggleFilter('acceptsRegionalCenter');
      expect(store.activeFilterCount).toBe(3);

      store.resetFilters();
      expect(store.activeFilterCount).toBe(0);
    });

    it('should calculate hasUserData correctly', () => {
      const store = useFilterStore();

      expect(store.hasUserData).toBe(false);

      store.updateUserData({ age: '3-5' });
      expect(store.hasUserData).toBe(true);

      store.resetUserData();
      expect(store.hasUserData).toBe(false);

      store.updateUserData({ diagnosis: 'Autism' });
      expect(store.hasUserData).toBe(true);
    });

    it('should update filterParams reactively', () => {
      const store = useFilterStore();

      expect(store.filterParams).toEqual({});

      store.toggleFilter('acceptsRegionalCenter');
      expect(store.filterParams.insurance).toBe('regional center');

      store.updateUserData({ age: '3-5' });
      store.toggleFilter('matchesAge');
      expect(store.filterParams.age).toBe('3-5');
      expect(store.filterParams.insurance).toBe('regional center');

      store.resetFilters();
      expect(store.filterParams).toEqual({});
    });
  });

  describe('Edge cases', () => {
    it('should handle case-insensitive insurance matching', () => {
      const store = useFilterStore();

      store.updateUserData({ insurance: 'REGIONAL CENTER' });
      store.applyOnboardingFilters();

      expect(store.filterOptions.acceptsRegionalCenter).toBe(true);
    });

    it('should handle partial insurance name matching', () => {
      const store = useFilterStore();

      store.updateUserData({ insurance: 'Regional Center of Orange County' });
      store.applyOnboardingFilters();

      expect(store.filterOptions.acceptsRegionalCenter).toBe(true);
    });

    it('should not enable filters if user data is undefined', () => {
      const store = useFilterStore();

      store.applyOnboardingFilters();

      expect(store.filterOptions.acceptsInsurance).toBe(false);
      expect(store.filterOptions.matchesAge).toBe(false);
      expect(store.filterOptions.matchesDiagnosis).toBe(false);
      expect(store.filterOptions.matchesTherapy).toBe(false);
    });
  });
});
