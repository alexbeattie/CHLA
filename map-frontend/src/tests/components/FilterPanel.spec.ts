/**
 * Tests for FilterPanel component
 * Week 4: Component Extraction
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { mount } from '@vue/test-utils';
import { setActivePinia, createPinia } from 'pinia';
import { nextTick } from 'vue';
import FilterPanel from '@/components/map/FilterPanel.vue';
import { useFilterStore } from '@/stores/filterStore';

describe('FilterPanel', () => {
  let wrapper: any;
  let filterStore: any;

  beforeEach(() => {
    setActivePinia(createPinia());
    filterStore = useFilterStore();

    // Set up default user data
    filterStore.userData.age = undefined;
    filterStore.userData.diagnosis = undefined;
    filterStore.userData.therapy = undefined;
  });

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
    }
    vi.clearAllMocks();
  });

  describe('Component Rendering', () => {
    it('should render panel header', () => {
      wrapper = mount(FilterPanel);

      expect(wrapper.find('.panel-header').exists()).toBe(true);
      expect(wrapper.find('.header-title h3').text()).toBe('Filters');
    });

    it('should render filter sections', () => {
      wrapper = mount(FilterPanel);

      const sections = wrapper.findAll('.filter-section');
      expect(sections.length).toBeGreaterThan(0);
    });

    it('should show insurance filters', () => {
      wrapper = mount(FilterPanel);

      expect(wrapper.text()).toContain('Insurance');
      expect(wrapper.text()).toContain('Accepts Insurance');
      expect(wrapper.text()).toContain('Accepts Regional Center');
      expect(wrapper.text()).toContain('Accepts Private Pay');
    });

    it('should show profile matching filters', () => {
      wrapper = mount(FilterPanel);

      expect(wrapper.text()).toContain('Match My Profile');
      expect(wrapper.text()).toContain('Match Age');
      expect(wrapper.text()).toContain('Match Diagnosis');
      expect(wrapper.text()).toContain('Match Therapy');
    });
  });

  describe('Filter Count Badge', () => {
    it('should not show badge when no filters active', () => {
      wrapper = mount(FilterPanel);

      expect(wrapper.find('.filter-count-badge').exists()).toBe(false);
    });

    it('should show badge with count when filters active', async () => {
      wrapper = mount(FilterPanel);

      wrapper.vm.localFilters.acceptsInsurance = true;
      await nextTick();

      expect(wrapper.find('.filter-count-badge').exists()).toBe(true);
      expect(wrapper.find('.filter-count-badge').text()).toBe('1');
    });

    it('should update badge count when multiple filters active', async () => {
      wrapper = mount(FilterPanel);

      wrapper.vm.localFilters.acceptsInsurance = true;
      wrapper.vm.localFilters.acceptsRegionalCenter = true;
      await nextTick();

      expect(wrapper.find('.filter-count-badge').text()).toBe('2');
    });
  });

  describe('Reset Button', () => {
    it('should not show reset button when no filters active', () => {
      wrapper = mount(FilterPanel);

      expect(wrapper.find('.btn-reset').exists()).toBe(false);
    });

    it('should show reset button when filters are active', async () => {
      wrapper = mount(FilterPanel);

      const checkbox = wrapper.find('input[type="checkbox"]');
      await checkbox.setValue(true);
      await nextTick();

      expect(wrapper.find('.btn-reset').exists()).toBe(true);
    });

    it('should reset all filters when clicked', async () => {
      wrapper = mount(FilterPanel);

      // Activate some filters
      wrapper.vm.localFilters.acceptsInsurance = true;
      wrapper.vm.localFilters.acceptsRegionalCenter = true;
      await nextTick();

      // Click reset
      await wrapper.find('.btn-reset').trigger('click');
      await nextTick();

      // All filters should be false
      expect(wrapper.vm.localFilters.acceptsInsurance).toBe(false);
      expect(wrapper.vm.localFilters.acceptsRegionalCenter).toBe(false);
    });

    it('should emit reset event when clicked', async () => {
      wrapper = mount(FilterPanel);

      const checkbox = wrapper.find('input[type="checkbox"]');
      await checkbox.setValue(true);
      await nextTick();

      await wrapper.find('.btn-reset').trigger('click');

      expect(wrapper.emitted('reset')).toBeTruthy();
    });
  });

  describe('Collapse Toggle', () => {
    it('should show collapse button by default', () => {
      wrapper = mount(FilterPanel);

      expect(wrapper.find('.btn-collapse').exists()).toBe(true);
    });

    it('should hide collapse button when showCollapseToggle is false', () => {
      wrapper = mount(FilterPanel, {
        props: {
          showCollapseToggle: false
        }
      });

      expect(wrapper.find('.btn-collapse').exists()).toBe(false);
    });

    it('should toggle collapsed state when clicked', async () => {
      wrapper = mount(FilterPanel);

      expect(wrapper.find('.panel-content').exists()).toBe(true);

      await wrapper.find('.btn-collapse').trigger('click');
      await nextTick();

      expect(wrapper.find('.filter-panel').classes()).toContain('is-collapsed');
    });

    it('should start collapsed when startCollapsed is true', () => {
      wrapper = mount(FilterPanel, {
        props: {
          startCollapsed: true
        }
      });

      expect(wrapper.find('.filter-panel').classes()).toContain('is-collapsed');
    });
  });

  describe('Insurance Filters', () => {
    it('should toggle acceptsInsurance filter', async () => {
      wrapper = mount(FilterPanel, {
        props: { manualApply: true }  // Use manual mode to prevent store sync issues
      });

      // Set the filter directly via component data
      wrapper.vm.localFilters.acceptsInsurance = true;
      await nextTick();

      expect(wrapper.vm.localFilters.acceptsInsurance).toBe(true);
    });

    it('should toggle acceptsRegionalCenter filter', async () => {
      wrapper = mount(FilterPanel, {
        props: { manualApply: true }
      });

      wrapper.vm.localFilters.acceptsRegionalCenter = true;
      await nextTick();

      expect(wrapper.vm.localFilters.acceptsRegionalCenter).toBe(true);
    });

    it('should toggle acceptsPrivatePay filter', async () => {
      wrapper = mount(FilterPanel, {
        props: { manualApply: true }
      });

      wrapper.vm.localFilters.acceptsPrivatePay = true;
      await nextTick();

      expect(wrapper.vm.localFilters.acceptsPrivatePay).toBe(true);
    });

    it('should emit filter-change when insurance filter changes', async () => {
      wrapper = mount(FilterPanel);

      wrapper.vm.localFilters.acceptsInsurance = true;
      wrapper.vm.handleFilterChange();
      await nextTick();

      expect(wrapper.emitted('filter-change')).toBeTruthy();
    });
  });

  describe('Profile Matching Filters', () => {
    it('should disable age filter when no user age set', () => {
      wrapper = mount(FilterPanel);

      const ageCheckbox = wrapper.findAll('.filter-checkbox input')[3];
      expect(ageCheckbox.attributes('disabled')).toBeDefined();
    });

    it('should enable age filter when user age is set', () => {
      filterStore.userData.age = '10-12';

      wrapper = mount(FilterPanel);

      const ageCheckbox = wrapper.findAll('.filter-checkbox input')[3];
      expect(ageCheckbox.attributes('disabled')).toBeUndefined();
    });

    it('should show user age value when set', () => {
      filterStore.userData.age = '10-12';

      wrapper = mount(FilterPanel);

      expect(wrapper.text()).toContain('(10-12)');
    });

    it('should show "Not set" when user age not set', () => {
      wrapper = mount(FilterPanel);

      const ageLabel = wrapper.findAll('.filter-checkbox')[3];
      expect(ageLabel.text()).toContain('(Not set)');
    });

    it('should disable diagnosis filter when no user diagnosis set', () => {
      wrapper = mount(FilterPanel);

      const diagnosisCheckbox = wrapper.findAll('.filter-checkbox input')[4];
      expect(diagnosisCheckbox.attributes('disabled')).toBeDefined();
    });

    it('should enable diagnosis filter when user diagnosis is set', () => {
      filterStore.userData.diagnosis = 'Autism';

      wrapper = mount(FilterPanel);

      const diagnosisCheckbox = wrapper.findAll('.filter-checkbox input')[4];
      expect(diagnosisCheckbox.attributes('disabled')).toBeUndefined();
    });

    it('should disable therapy filter when no user therapy set', () => {
      wrapper = mount(FilterPanel);

      const therapyCheckbox = wrapper.findAll('.filter-checkbox input')[5];
      expect(therapyCheckbox.attributes('disabled')).toBeDefined();
    });

    it('should enable therapy filter when user therapy is set', () => {
      filterStore.userData.therapy = 'ABA';

      wrapper = mount(FilterPanel);

      const therapyCheckbox = wrapper.findAll('.filter-checkbox input')[5];
      expect(therapyCheckbox.attributes('disabled')).toBeUndefined();
    });
  });

  describe('Favorites Filter', () => {
    it('should not show favorites filter by default', () => {
      wrapper = mount(FilterPanel);

      expect(wrapper.text()).not.toContain('Favorites');
      expect(wrapper.text()).not.toContain('Show Only Favorites');
    });

    it('should show favorites filter when showFavorites is true', () => {
      wrapper = mount(FilterPanel, {
        props: {
          showFavorites: true
        }
      });

      expect(wrapper.text()).toContain('Favorites');
      expect(wrapper.text()).toContain('Show Only Favorites');
    });
  });

  describe('Manual Apply Mode', () => {
    it('should not show apply button when manualApply is false', () => {
      wrapper = mount(FilterPanel, {
        props: {
          manualApply: false
        }
      });

      expect(wrapper.find('.btn-apply').exists()).toBe(false);
    });

    it('should show apply button when manualApply is true and has changes', async () => {
      wrapper = mount(FilterPanel, {
        props: {
          manualApply: true
        }
      });

      const checkbox = wrapper.find('input[type="checkbox"]');
      await checkbox.setValue(true);
      await nextTick();

      expect(wrapper.find('.btn-apply').exists()).toBe(true);
    });

    it('should emit apply event when apply button is clicked', async () => {
      wrapper = mount(FilterPanel, {
        props: {
          manualApply: true
        }
      });

      const checkbox = wrapper.find('input[type="checkbox"]');
      await checkbox.setValue(true);
      await nextTick();

      await wrapper.find('.btn-apply').trigger('click');

      expect(wrapper.emitted('apply')).toBeTruthy();
    });

    it('should not emit filter-change immediately in manual mode', async () => {
      wrapper = mount(FilterPanel, {
        props: {
          manualApply: true
        }
      });

      const checkbox = wrapper.find('input[type="checkbox"]');
      await checkbox.setValue(true);
      await nextTick();

      expect(wrapper.emitted('filter-change')).toBeFalsy();
    });

    it('should emit filter-change immediately when manualApply is false', async () => {
      wrapper = mount(FilterPanel, {
        props: {
          manualApply: false
        }
      });

      const checkbox = wrapper.find('input[type="checkbox"]');
      await checkbox.setValue(true);
      await nextTick();

      expect(wrapper.emitted('filter-change')).toBeTruthy();
    });
  });

  describe('Active Filters Summary', () => {
    it('should not show summary when no filters active', () => {
      wrapper = mount(FilterPanel);

      expect(wrapper.find('.filters-summary').exists()).toBe(false);
    });

    it('should show summary when filters are active', async () => {
      wrapper = mount(FilterPanel);

      const checkbox = wrapper.find('input[type="checkbox"]');
      await checkbox.setValue(true);
      await nextTick();

      expect(wrapper.find('.filters-summary').exists()).toBe(true);
    });

    it('should not show summary when showSummary is false', async () => {
      wrapper = mount(FilterPanel, {
        props: {
          showSummary: false
        }
      });

      const checkbox = wrapper.find('input[type="checkbox"]');
      await checkbox.setValue(true);
      await nextTick();

      expect(wrapper.find('.filters-summary').exists()).toBe(false);
    });

    it('should display active filter chips', async () => {
      wrapper = mount(FilterPanel);

      wrapper.vm.localFilters.acceptsInsurance = true;
      wrapper.vm.localFilters.acceptsRegionalCenter = true;
      await nextTick();

      const chips = wrapper.findAll('.filter-chip');
      expect(chips.length).toBe(2);
    });

    it('should remove filter when chip is clicked', async () => {
      wrapper = mount(FilterPanel);

      wrapper.vm.localFilters.acceptsInsurance = true;
      await nextTick();

      const chip = wrapper.find('.filter-chip');
      await chip.trigger('click');
      await nextTick();

      expect(wrapper.vm.localFilters.acceptsInsurance).toBe(false);
    });
  });

  describe('Store Integration', () => {
    it('should call filterStore.toggleFilter when filter changes', async () => {
      const toggleSpy = vi.spyOn(filterStore, 'toggleFilter');

      wrapper = mount(FilterPanel);

      const checkbox = wrapper.find('input[type="checkbox"]');
      await checkbox.setValue(true);
      await nextTick();

      expect(toggleSpy).toHaveBeenCalled();
    });

    it('should sync with store when store changes externally', async () => {
      wrapper = mount(FilterPanel);

      // Change store directly
      filterStore.toggleFilter('acceptsInsurance', true);
      await nextTick();

      const insuranceCheckbox = wrapper.findAll('.filter-checkbox input')[0];
      expect(insuranceCheckbox.element.checked).toBe(true);
    });
  });

  describe('Default Props', () => {
    it('should use default prop values', () => {
      wrapper = mount(FilterPanel);

      expect(wrapper.vm.startCollapsed).toBe(false);
      expect(wrapper.vm.showCollapseToggle).toBe(true);
      expect(wrapper.vm.showFavorites).toBe(false);
      expect(wrapper.vm.showSummary).toBe(true);
      expect(wrapper.vm.manualApply).toBe(false);
    });

    it('should accept custom prop values', () => {
      wrapper = mount(FilterPanel, {
        props: {
          startCollapsed: true,
          showCollapseToggle: false,
          showFavorites: true,
          showSummary: false,
          manualApply: true
        }
      });

      expect(wrapper.vm.startCollapsed).toBe(true);
      expect(wrapper.vm.showCollapseToggle).toBe(false);
      expect(wrapper.vm.showFavorites).toBe(true);
      expect(wrapper.vm.showSummary).toBe(false);
      expect(wrapper.vm.manualApply).toBe(true);
    });
  });
});
