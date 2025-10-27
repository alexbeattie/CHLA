/**
 * Tests for SearchBar component
 * Week 4: Component Extraction
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { mount } from '@vue/test-utils';
import { setActivePinia, createPinia } from 'pinia';
import { nextTick } from 'vue';
import SearchBar from '@/components/map/SearchBar.vue';
import { useProviderStore } from '@/stores/providerStore';
import { useMapStore } from '@/stores/mapStore';

describe('SearchBar', () => {
  let wrapper: any;
  let providerStore: any;
  let mapStore: any;

  beforeEach(() => {
    setActivePinia(createPinia());
    providerStore = useProviderStore();
    mapStore = useMapStore();

    // Mock store methods
    vi.spyOn(providerStore, 'searchByZipCode').mockResolvedValue({
      providers: [
        { id: 1, name: 'Provider 1', latitude: 34.0, longitude: -118.0 }
      ],
      count: 1
    });

    vi.spyOn(providerStore, 'searchByLocation').mockResolvedValue({
      providers: [
        { id: 2, name: 'Provider 2', latitude: 34.1, longitude: -118.1 }
      ],
      count: 1,
      center: { lat: 34.1, lng: -118.1 }
    });

    vi.spyOn(providerStore, 'clearProviders').mockImplementation(() => {
      providerStore.providers = [];
    });

    vi.spyOn(mapStore, 'centerOn').mockImplementation(() => {});
    vi.spyOn(mapStore, 'setUserLocation').mockImplementation(() => {});
  });

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
    }
    vi.clearAllMocks();
  });

  describe('Component Rendering', () => {
    it('should render search input', () => {
      wrapper = mount(SearchBar);

      expect(wrapper.find('.search-input').exists()).toBe(true);
    });

    it('should render with default placeholder', () => {
      wrapper = mount(SearchBar);

      const input = wrapper.find('.search-input');
      expect(input.attributes('placeholder')).toBe('Enter city or ZIP code');
    });

    it('should render with custom placeholder', () => {
      wrapper = mount(SearchBar, {
        props: {
          placeholder: 'Search for providers'
        }
      });

      const input = wrapper.find('.search-input');
      expect(input.attributes('placeholder')).toBe('Search for providers');
    });

    it('should render search button', () => {
      wrapper = mount(SearchBar);

      expect(wrapper.find('.search-btn').exists()).toBe(true);
    });

    it('should not show clear button when input is empty', () => {
      wrapper = mount(SearchBar);

      expect(wrapper.find('.clear-btn').exists()).toBe(false);
    });

    it('should show clear button when input has value', async () => {
      wrapper = mount(SearchBar);

      const input = wrapper.find('.search-input');
      await input.setValue('90210');
      await nextTick();

      expect(wrapper.find('.clear-btn').exists()).toBe(true);
    });
  });

  describe('ZIP Code Search', () => {
    it('should validate 5-digit ZIP code', async () => {
      wrapper = mount(SearchBar);

      const input = wrapper.find('.search-input');
      await input.setValue('90210');
      await nextTick();

      const searchBtn = wrapper.find('.search-btn');
      await searchBtn.trigger('click');
      await nextTick();

      expect(providerStore.searchByZipCode).toHaveBeenCalledWith('90210');
    });

    it('should show error for invalid ZIP code (too short)', async () => {
      wrapper = mount(SearchBar);

      const input = wrapper.find('.search-input');
      await input.setValue('902');
      await nextTick();

      const searchBtn = wrapper.find('.search-btn');
      await searchBtn.trigger('click');
      await nextTick();

      expect(wrapper.find('.validation-message.error').exists()).toBe(true);
      expect(wrapper.text()).toContain('ZIP code must be 5 digits');
    });

    it('should show error for invalid ZIP code (too long)', async () => {
      wrapper = mount(SearchBar);

      const input = wrapper.find('.search-input');
      await input.setValue('902101');
      await nextTick();

      const searchBtn = wrapper.find('.search-btn');
      await searchBtn.trigger('click');
      await nextTick();

      expect(wrapper.find('.validation-message.error').exists()).toBe(true);
      expect(wrapper.text()).toContain('Invalid ZIP code format');
    });

    it('should emit search event on ZIP code search', async () => {
      wrapper = mount(SearchBar);

      const input = wrapper.find('.search-input');
      await input.setValue('90210');
      await nextTick();

      const searchBtn = wrapper.find('.search-btn');
      await searchBtn.trigger('click');
      await nextTick();
      await nextTick(); // Wait for async search to complete

      expect(wrapper.emitted('search')).toBeTruthy();
      expect(wrapper.emitted('search')[0][0]).toMatchObject({
        query: '90210',
        type: 'zip'
      });
    });

    it('should center map on ZIP search results', async () => {
      wrapper = mount(SearchBar);

      const input = wrapper.find('.search-input');
      await input.setValue('90210');
      await nextTick();

      const searchBtn = wrapper.find('.search-btn');
      await searchBtn.trigger('click');
      await nextTick();
      await nextTick();

      expect(mapStore.centerOn).toHaveBeenCalledWith(
        { lat: 34.0, lng: -118.0 },
        11
      );
    });

    it('should show results summary for ZIP search', async () => {
      wrapper = mount(SearchBar);

      const input = wrapper.find('.search-input');
      await input.setValue('90210');
      await nextTick();

      const searchBtn = wrapper.find('.search-btn');
      await searchBtn.trigger('click');
      await nextTick();
      await nextTick();

      expect(wrapper.find('.results-summary').exists()).toBe(true);
      expect(wrapper.text()).toContain('Found 1 provider');
      expect(wrapper.text()).toContain('in ZIP code 90210');
    });
  });

  describe('Location Search', () => {
    it('should search by location for text input', async () => {
      wrapper = mount(SearchBar);

      const input = wrapper.find('.search-input');
      await input.setValue('Los Angeles');
      await nextTick();

      const searchBtn = wrapper.find('.search-btn');
      await searchBtn.trigger('click');
      await nextTick();
      await nextTick();

      expect(providerStore.searchByLocation).toHaveBeenCalledWith('Los Angeles');
    });

    it('should show error for short location search', async () => {
      wrapper = mount(SearchBar);

      const input = wrapper.find('.search-input');
      await input.setValue('LA');
      await nextTick();

      const searchBtn = wrapper.find('.search-btn');
      await searchBtn.trigger('click');
      await nextTick();

      expect(wrapper.find('.validation-message.error').exists()).toBe(true);
      expect(wrapper.text()).toContain('Please enter at least 3 characters');
    });

    it('should emit search event on location search', async () => {
      wrapper = mount(SearchBar);

      const input = wrapper.find('.search-input');
      await input.setValue('Los Angeles');
      await nextTick();

      const searchBtn = wrapper.find('.search-btn');
      await searchBtn.trigger('click');
      await nextTick();
      await nextTick();

      expect(wrapper.emitted('search')).toBeTruthy();
      expect(wrapper.emitted('search')[0][0]).toMatchObject({
        query: 'Los Angeles',
        type: 'location'
      });
    });

    it('should set user location and center map on location search', async () => {
      wrapper = mount(SearchBar);

      const input = wrapper.find('.search-input');
      await input.setValue('Los Angeles');
      await nextTick();

      const searchBtn = wrapper.find('.search-btn');
      await searchBtn.trigger('click');
      await nextTick();
      await nextTick();

      expect(mapStore.setUserLocation).toHaveBeenCalledWith({
        lat: 34.1,
        lng: -118.1
      });

      expect(mapStore.centerOn).toHaveBeenCalledWith(
        { lat: 34.1, lng: -118.1 },
        12
      );
    });

    it('should show results summary for location search', async () => {
      wrapper = mount(SearchBar);

      const input = wrapper.find('.search-input');
      await input.setValue('Los Angeles');
      await nextTick();

      const searchBtn = wrapper.find('.search-btn');
      await searchBtn.trigger('click');
      await nextTick();
      await nextTick();

      expect(wrapper.find('.results-summary').exists()).toBe(true);
      expect(wrapper.text()).toContain('Found 1 provider');
      expect(wrapper.text()).toContain('near Los Angeles');
    });
  });

  describe('Clear Functionality', () => {
    it('should clear input when clear button is clicked', async () => {
      wrapper = mount(SearchBar);

      const input = wrapper.find('.search-input');
      await input.setValue('90210');
      await nextTick();

      const clearBtn = wrapper.find('.clear-btn');
      await clearBtn.trigger('click');
      await nextTick();

      expect(wrapper.vm.searchQuery).toBe('');
    });

    it('should emit clear event when cleared', async () => {
      wrapper = mount(SearchBar);

      const input = wrapper.find('.search-input');
      await input.setValue('90210');
      await nextTick();

      const clearBtn = wrapper.find('.clear-btn');
      await clearBtn.trigger('click');
      await nextTick();

      expect(wrapper.emitted('clear')).toBeTruthy();
    });

    it('should clear validation messages when cleared', async () => {
      wrapper = mount(SearchBar);

      const input = wrapper.find('.search-input');
      await input.setValue('902'); // Invalid ZIP
      await nextTick();

      const searchBtn = wrapper.find('.search-btn');
      await searchBtn.trigger('click');
      await nextTick();

      expect(wrapper.find('.validation-message').exists()).toBe(true);

      const clearBtn = wrapper.find('.clear-btn');
      await clearBtn.trigger('click');
      await nextTick();

      expect(wrapper.find('.validation-message').exists()).toBe(false);
    });

    it('should clear results summary when cleared', async () => {
      wrapper = mount(SearchBar);

      const input = wrapper.find('.search-input');
      await input.setValue('90210');
      await nextTick();

      const searchBtn = wrapper.find('.search-btn');
      await searchBtn.trigger('click');
      await nextTick();
      await nextTick();

      expect(wrapper.find('.results-summary').exists()).toBe(true);

      const clearBtn = wrapper.find('.clear-btn');
      await clearBtn.trigger('click');
      await nextTick();

      expect(wrapper.find('.results-summary').exists()).toBe(false);
    });

    it('should call providerStore.clearProviders when cleared', async () => {
      wrapper = mount(SearchBar);

      const input = wrapper.find('.search-input');
      await input.setValue('90210');
      await nextTick();

      const clearBtn = wrapper.find('.clear-btn');
      await clearBtn.trigger('click');
      await nextTick();

      expect(providerStore.clearProviders).toHaveBeenCalled();
    });
  });

  describe('Enter Key Search', () => {
    it('should search when Enter key is pressed', async () => {
      wrapper = mount(SearchBar);

      const input = wrapper.find('.search-input');
      await input.setValue('90210');
      await input.trigger('keyup.enter');
      await nextTick();
      await nextTick();

      expect(providerStore.searchByZipCode).toHaveBeenCalledWith('90210');
    });
  });

  describe('Loading State', () => {
    it('should disable input during loading', async () => {
      wrapper = mount(SearchBar);

      providerStore.loading = true;
      await nextTick();

      const input = wrapper.find('.search-input');
      expect(input.attributes('disabled')).toBeDefined();
    });

    it('should disable search button during loading', async () => {
      wrapper = mount(SearchBar);

      providerStore.loading = true;
      await nextTick();

      const searchBtn = wrapper.find('.search-btn');
      expect(searchBtn.attributes('disabled')).toBeDefined();
    });

    it('should show spinner during loading', async () => {
      wrapper = mount(SearchBar);

      const input = wrapper.find('.search-input');
      await input.setValue('90210');
      await nextTick();

      providerStore.loading = true;
      await nextTick();

      expect(wrapper.find('.spinner-border').exists()).toBe(true);
      expect(wrapper.find('.bi-search').exists()).toBe(false);
    });
  });

  describe('Validation Messages', () => {
    it('should emit validation-error event on validation failure', async () => {
      wrapper = mount(SearchBar);

      const input = wrapper.find('.search-input');
      await input.setValue('902');
      await nextTick();

      const searchBtn = wrapper.find('.search-btn');
      await searchBtn.trigger('click');
      await nextTick();

      expect(wrapper.emitted('validation-error')).toBeTruthy();
      expect(wrapper.emitted('validation-error')[0][0]).toBe('ZIP code must be 5 digits');
    });

    it('should clear validation on new input', async () => {
      wrapper = mount(SearchBar, {
        props: {
          debounceDelay: 0 // Disable debounce for testing
        }
      });

      const input = wrapper.find('.search-input');
      await input.setValue('902');
      await nextTick();

      const searchBtn = wrapper.find('.search-btn');
      await searchBtn.trigger('click');
      await nextTick();

      expect(wrapper.find('.validation-message').exists()).toBe(true);

      await input.setValue('90210');
      await input.trigger('input');
      await nextTick();

      expect(wrapper.find('.validation-message').exists()).toBe(false);
    });
  });

  describe('Props', () => {
    it('should not show results summary when showResultsSummary is false', async () => {
      wrapper = mount(SearchBar, {
        props: {
          showResultsSummary: false
        }
      });

      const input = wrapper.find('.search-input');
      await input.setValue('90210');
      await nextTick();

      const searchBtn = wrapper.find('.search-btn');
      await searchBtn.trigger('click');
      await nextTick();
      await nextTick();

      expect(wrapper.find('.results-summary').exists()).toBe(false);
    });

    it('should auto-focus when autoFocus is true', async () => {
      wrapper = mount(SearchBar, {
        props: {
          autoFocus: true
        },
        attachTo: document.body
      });

      await new Promise(resolve => setTimeout(resolve, 150));

      expect(document.activeElement).toBe(wrapper.find('.search-input').element);

      wrapper.unmount();
    });
  });

  describe('Error Handling', () => {
    it('should handle search error gracefully', async () => {
      providerStore.searchByZipCode.mockRejectedValueOnce(
        new Error('API Error')
      );

      wrapper = mount(SearchBar);

      const input = wrapper.find('.search-input');
      await input.setValue('90210');
      await nextTick();

      const searchBtn = wrapper.find('.search-btn');
      await searchBtn.trigger('click');
      await nextTick();
      await nextTick();

      expect(wrapper.find('.validation-message.error').exists()).toBe(true);
      expect(wrapper.text()).toContain('API Error');
    });

    it('should emit validation-error on search failure', async () => {
      providerStore.searchByZipCode.mockRejectedValueOnce(
        new Error('Network error')
      );

      wrapper = mount(SearchBar);

      const input = wrapper.find('.search-input');
      await input.setValue('90210');
      await nextTick();

      const searchBtn = wrapper.find('.search-btn');
      await searchBtn.trigger('click');
      await nextTick();
      await nextTick();

      expect(wrapper.emitted('validation-error')).toBeTruthy();
    });
  });

  describe('Results Pluralization', () => {
    it('should show singular "provider" for 1 result', async () => {
      providerStore.searchByZipCode.mockResolvedValueOnce({
        providers: [{ id: 1, name: 'Provider 1' }],
        count: 1
      });

      wrapper = mount(SearchBar);

      const input = wrapper.find('.search-input');
      await input.setValue('90210');
      await nextTick();

      const searchBtn = wrapper.find('.search-btn');
      await searchBtn.trigger('click');
      await nextTick();
      await nextTick();

      expect(wrapper.text()).toContain('Found 1 provider');
    });

    it('should show plural "providers" for multiple results', async () => {
      providerStore.searchByZipCode.mockResolvedValueOnce({
        providers: [
          { id: 1, name: 'Provider 1' },
          { id: 2, name: 'Provider 2' }
        ],
        count: 2
      });

      wrapper = mount(SearchBar);

      const input = wrapper.find('.search-input');
      await input.setValue('90210');
      await nextTick();

      const searchBtn = wrapper.find('.search-btn');
      await searchBtn.trigger('click');
      await nextTick();
      await nextTick();

      expect(wrapper.text()).toContain('Found 2 providers');
    });
  });

  describe('Exposed Methods', () => {
    it('should expose focus method', async () => {
      wrapper = mount(SearchBar, {
        attachTo: document.body
      });

      await nextTick();

      wrapper.vm.focus();
      await nextTick();

      expect(document.activeElement).toBe(wrapper.find('.search-input').element);

      wrapper.unmount();
    });
  });
});
