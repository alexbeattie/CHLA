/**
 * Tests for useFilterState composable
 * Week 3: Updated to work with Pinia store-backed composables
 */

import { describe, it, expect, beforeEach } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import { useFilterState } from '@/composables/useFilterState';

describe('useFilterState', () => {
  let composable: ReturnType<typeof useFilterState>;

  beforeEach(() => {
    // Create fresh Pinia instance for each test
    setActivePinia(createPinia());
    composable = useFilterState();
  });

  describe('Initialization', () => {
    it('should initialize with all filters disabled', () => {
      expect(composable.filterOptions.value.acceptsInsurance).toBe(false);
      expect(composable.filterOptions.value.acceptsRegionalCenter).toBe(false);
      expect(composable.filterOptions.value.acceptsPrivatePay).toBe(false);
      expect(composable.filterOptions.value.matchesAge).toBe(false);
      expect(composable.filterOptions.value.matchesDiagnosis).toBe(false);
      expect(composable.filterOptions.value.matchesTherapy).toBe(false);
      expect(composable.filterOptions.value.showOnlyFavorites).toBe(false);
    });

    it('should initialize with empty user data', () => {
      expect(composable.userData.value.insurance).toBeUndefined();
      expect(composable.userData.value.age).toBeUndefined();
      expect(composable.userData.value.diagnosis).toBeUndefined();
      expect(composable.userData.value.therapy).toBeUndefined();
    });

    it('should initialize computed properties correctly', () => {
      expect(composable.hasActiveFilters.value).toBe(false);
      expect(composable.activeFilterCount.value).toBe(0);
      expect(composable.hasUserData.value).toBe(false);
    });
  });

  describe('toggleFilter', () => {
    it('should toggle a single filter on and off', () => {
      expect(composable.filterOptions.value.acceptsInsurance).toBe(false);

      composable.toggleFilter('acceptsInsurance');
      expect(composable.filterOptions.value.acceptsInsurance).toBe(true);

      composable.toggleFilter('acceptsInsurance');
      expect(composable.filterOptions.value.acceptsInsurance).toBe(false);
    });

    it('should enforce mutual exclusivity for insurance filters', () => {
      // Enable regional center
      composable.toggleFilter('acceptsRegionalCenter');
      expect(composable.filterOptions.value.acceptsRegionalCenter).toBe(true);

      // Enable insurance (should disable regional center)
      composable.toggleFilter('acceptsInsurance');
      expect(composable.filterOptions.value.acceptsInsurance).toBe(true);
      expect(composable.filterOptions.value.acceptsRegionalCenter).toBe(false);

      // Enable private pay (should disable insurance)
      composable.toggleFilter('acceptsPrivatePay');
      expect(composable.filterOptions.value.acceptsPrivatePay).toBe(true);
      expect(composable.filterOptions.value.acceptsInsurance).toBe(false);
    });

    it('should not affect mutual exclusivity when disabling', () => {
      composable.toggleFilter('acceptsInsurance');
      expect(composable.filterOptions.value.acceptsInsurance).toBe(true);

      // Disable insurance
      composable.toggleFilter('acceptsInsurance');
      expect(composable.filterOptions.value.acceptsInsurance).toBe(false);
      expect(composable.filterOptions.value.acceptsRegionalCenter).toBe(false);
      expect(composable.filterOptions.value.acceptsPrivatePay).toBe(false);
    });
  });

  describe('setFilter', () => {
    it('should set filter to specific value', () => {
      composable.setFilter('matchesAge', true);
      expect(composable.filterOptions.value.matchesAge).toBe(true);

      composable.setFilter('matchesAge', false);
      expect(composable.filterOptions.value.matchesAge).toBe(false);
    });

    it('should not enforce mutual exclusivity when using setFilter', () => {
      // Note: setFilter doesn't enforce mutual exclusivity
      // This is intentional for programmatic control
      composable.setFilter('acceptsInsurance', true);
      composable.setFilter('acceptsRegionalCenter', true);

      expect(composable.filterOptions.value.acceptsInsurance).toBe(true);
      expect(composable.filterOptions.value.acceptsRegionalCenter).toBe(true);
    });
  });

  describe('resetFilters', () => {
    it('should reset all filters to default state', () => {
      // Enable some filters
      composable.toggleFilter('acceptsInsurance');
      composable.toggleFilter('matchesAge');
      composable.toggleFilter('showOnlyFavorites');

      expect(composable.activeFilterCount.value).toBe(3);

      // Reset
      composable.resetFilters();

      expect(composable.filterOptions.value.acceptsInsurance).toBe(false);
      expect(composable.filterOptions.value.matchesAge).toBe(false);
      expect(composable.filterOptions.value.showOnlyFavorites).toBe(false);
      expect(composable.activeFilterCount.value).toBe(0);
    });
  });

  describe('User data management', () => {
    it('should update user data', () => {
      composable.updateUserData({
        insurance: 'Regional Center',
        age: '3-5',
        diagnosis: 'Autism'
      });

      expect(composable.userData.value.insurance).toBe('Regional Center');
      expect(composable.userData.value.age).toBe('3-5');
      expect(composable.userData.value.diagnosis).toBe('Autism');
      expect(composable.hasUserData.value).toBe(true);
    });

    it('should partially update user data', () => {
      composable.updateUserData({ insurance: 'Regional Center' });
      expect(composable.userData.value.insurance).toBe('Regional Center');
      expect(composable.userData.value.age).toBeUndefined();

      composable.updateUserData({ age: '6-12' });
      expect(composable.userData.value.insurance).toBe('Regional Center');
      expect(composable.userData.value.age).toBe('6-12');
    });

    it('should reset user data', () => {
      composable.updateUserData({
        insurance: 'Insurance',
        age: '3-5',
        diagnosis: 'Autism',
        therapy: 'ABA'
      });

      expect(composable.hasUserData.value).toBe(true);

      composable.resetUserData();

      expect(composable.userData.value.insurance).toBeUndefined();
      expect(composable.userData.value.age).toBeUndefined();
      expect(composable.userData.value.diagnosis).toBeUndefined();
      expect(composable.userData.value.therapy).toBeUndefined();
      expect(composable.hasUserData.value).toBe(false);
    });
  });

  describe('buildFilterParams', () => {
    it('should build empty params when no filters active', () => {
      const params = composable.buildFilterParams();
      expect(Object.keys(params).length).toBe(0);
    });

    it('should include insurance filter', () => {
      composable.toggleFilter('acceptsInsurance');
      const params = composable.buildFilterParams();

      expect(params.insurance).toBe('insurance');
    });

    it('should include regional center filter', () => {
      composable.toggleFilter('acceptsRegionalCenter');
      const params = composable.buildFilterParams();

      expect(params.insurance).toBe('regional center');
    });

    it('should include private pay filter', () => {
      composable.toggleFilter('acceptsPrivatePay');
      const params = composable.buildFilterParams();

      expect(params.insurance).toBe('private pay');
    });

    it('should include user data filters when enabled', () => {
      composable.updateUserData({
        age: '3-5',
        diagnosis: 'Autism',
        therapy: 'ABA'
      });

      composable.setFilter('matchesAge', true);
      composable.setFilter('matchesDiagnosis', true);
      composable.setFilter('matchesTherapy', true);

      const params = composable.buildFilterParams();

      expect(params.age).toBe('3-5');
      expect(params.diagnosis).toBe('Autism');
      expect(params.therapy).toBe('ABA');
    });

    it('should not include user data filters when disabled', () => {
      composable.updateUserData({
        age: '3-5',
        diagnosis: 'Autism',
        therapy: 'ABA'
      });

      // Filters are disabled by default
      const params = composable.buildFilterParams();

      expect(params.age).toBeUndefined();
      expect(params.diagnosis).toBeUndefined();
      expect(params.therapy).toBeUndefined();
    });

    it('should not include user data filters when data is missing', () => {
      composable.setFilter('matchesAge', true);
      composable.setFilter('matchesDiagnosis', true);

      // No user data provided
      const params = composable.buildFilterParams();

      expect(params.age).toBeUndefined();
      expect(params.diagnosis).toBeUndefined();
    });
  });

  describe('applyOnboardingFilters', () => {
    it('should enable filters based on user data', () => {
      composable.updateUserData({
        insurance: 'Regional Center',
        age: '3-5',
        diagnosis: 'Autism',
        therapy: 'ABA'
      });

      composable.applyOnboardingFilters();

      expect(composable.filterOptions.value.acceptsRegionalCenter).toBe(true);
      expect(composable.filterOptions.value.matchesAge).toBe(true);
      expect(composable.filterOptions.value.matchesDiagnosis).toBe(true);
      expect(composable.filterOptions.value.matchesTherapy).toBe(true);
    });

    it('should detect regional center insurance', () => {
      composable.updateUserData({ insurance: 'regional center' });
      composable.applyOnboardingFilters();

      expect(composable.filterOptions.value.acceptsRegionalCenter).toBe(true);
      expect(composable.filterOptions.value.acceptsInsurance).toBe(false);
      expect(composable.filterOptions.value.acceptsPrivatePay).toBe(false);
    });

    it('should detect private pay insurance', () => {
      composable.updateUserData({ insurance: 'private pay' });
      composable.applyOnboardingFilters();

      expect(composable.filterOptions.value.acceptsPrivatePay).toBe(true);
      expect(composable.filterOptions.value.acceptsInsurance).toBe(false);
      expect(composable.filterOptions.value.acceptsRegionalCenter).toBe(false);
    });

    it('should detect general insurance', () => {
      composable.updateUserData({ insurance: 'Blue Cross' });
      composable.applyOnboardingFilters();

      expect(composable.filterOptions.value.acceptsInsurance).toBe(true);
      expect(composable.filterOptions.value.acceptsRegionalCenter).toBe(false);
      expect(composable.filterOptions.value.acceptsPrivatePay).toBe(false);
    });

    it('should not enable filters for missing user data', () => {
      composable.updateUserData({ insurance: 'Regional Center' });
      composable.applyOnboardingFilters();

      expect(composable.filterOptions.value.acceptsRegionalCenter).toBe(true);
      expect(composable.filterOptions.value.matchesAge).toBe(false);
      expect(composable.filterOptions.value.matchesDiagnosis).toBe(false);
    });
  });

  describe('Computed properties', () => {
    it('should calculate hasActiveFilters correctly', () => {
      expect(composable.hasActiveFilters.value).toBe(false);

      composable.toggleFilter('acceptsInsurance');
      expect(composable.hasActiveFilters.value).toBe(true);

      composable.toggleFilter('matchesAge');
      expect(composable.hasActiveFilters.value).toBe(true);

      composable.resetFilters();
      expect(composable.hasActiveFilters.value).toBe(false);
    });

    it('should calculate activeFilterCount correctly', () => {
      expect(composable.activeFilterCount.value).toBe(0);

      composable.toggleFilter('acceptsInsurance');
      expect(composable.activeFilterCount.value).toBe(1);

      composable.toggleFilter('matchesAge');
      expect(composable.activeFilterCount.value).toBe(2);

      composable.toggleFilter('matchesDiagnosis');
      expect(composable.activeFilterCount.value).toBe(3);

      composable.toggleFilter('acceptsInsurance');
      expect(composable.activeFilterCount.value).toBe(2);
    });

    it('should calculate hasUserData correctly', () => {
      expect(composable.hasUserData.value).toBe(false);

      composable.updateUserData({ age: '3-5' });
      expect(composable.hasUserData.value).toBe(true);

      composable.resetUserData();
      expect(composable.hasUserData.value).toBe(false);
    });
  });

  describe('setAvailableOptions', () => {
    it('should set available therapy types', () => {
      const therapyTypes = ['ABA', 'Speech Therapy', 'OT'];
      composable.setAvailableOptions({ therapyTypes });

      expect(composable.availableTherapyTypes.value).toEqual(therapyTypes);
    });

    it('should set available age groups', () => {
      const ageGroups = ['0-2', '3-5', '6-12', '13-17', '18+'];
      composable.setAvailableOptions({ ageGroups });

      expect(composable.availableAgeGroups.value).toEqual(ageGroups);
    });

    it('should set multiple options at once', () => {
      const options = {
        therapyTypes: ['ABA', 'OT'],
        ageGroups: ['3-5', '6-12'],
        diagnoses: ['Autism', 'ADHD'],
        insuranceTypes: ['Regional Center', 'Insurance', 'Private Pay']
      };

      composable.setAvailableOptions(options);

      expect(composable.availableTherapyTypes.value).toEqual(options.therapyTypes);
      expect(composable.availableAgeGroups.value).toEqual(options.ageGroups);
      expect(composable.availableDiagnoses.value).toEqual(options.diagnoses);
      expect(composable.availableInsuranceTypes.value).toEqual(options.insuranceTypes);
    });
  });
});
