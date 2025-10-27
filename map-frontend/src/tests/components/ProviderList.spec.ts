/**
 * Tests for ProviderList component
 * Week 4: Component Extraction
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { mount } from '@vue/test-utils';
import { setActivePinia, createPinia } from 'pinia';
import { nextTick } from 'vue';
import ProviderList from '@/components/map/ProviderList.vue';
import ProviderCard from '@/components/map/ProviderCard.vue';
import { useProviderStore } from '@/stores/providerStore';
import { useMapStore } from '@/stores/mapStore';

describe('ProviderList', () => {
  let wrapper: any;
  let providerStore: any;
  let mapStore: any;

  const mockProviders = [
    {
      id: 1,
      name: 'ABC Therapy',
      address: '123 Main St',
      latitude: 34.0522,
      longitude: -118.2437,
      phone: '3105551234',
      website: 'https://abc.com',
      email: 'info@abc.com',
      insurance_accepted: 'Insurance',
      therapy_types: ['ABA'],
      age_groups: ['Children'],
      diagnoses_treated: ['Autism'],
      description: 'Therapy center',
      type: 'Therapy Center'
    },
    {
      id: 2,
      name: 'XYZ Clinic',
      address: '456 Oak Ave',
      latitude: 34.1,
      longitude: -118.3,
      phone: '3105555678',
      website: 'https://xyz.com',
      email: 'info@xyz.com',
      insurance_accepted: 'Regional Center',
      therapy_types: ['Speech'],
      age_groups: ['Adolescents'],
      diagnoses_treated: ['ADHD'],
      description: 'Medical clinic',
      type: 'Medical Clinic'
    }
  ];

  beforeEach(() => {
    setActivePinia(createPinia());
    providerStore = useProviderStore();
    mapStore = useMapStore();

    // Set up default store state
    providerStore.providers = [];
    providerStore.loading = false;
    providerStore.selectedProvider = null;

    mapStore.userLocation = { lat: 34.0522, lng: -118.2437 };
  });

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
    }
    vi.clearAllMocks();
  });

  describe('Component Rendering', () => {
    it('should render list header', () => {
      wrapper = mount(ProviderList);

      expect(wrapper.find('.list-header').exists()).toBe(true);
      expect(wrapper.find('.list-title').text()).toContain('Providers');
    });

    it('should render with providers from store', () => {
      providerStore.providers = mockProviders;

      wrapper = mount(ProviderList);

      expect(wrapper.findAllComponents(ProviderCard).length).toBe(2);
    });

    it('should render with providers from props', () => {
      wrapper = mount(ProviderList, {
        props: {
          providers: mockProviders
        }
      });

      expect(wrapper.findAllComponents(ProviderCard).length).toBe(2);
    });

    it('should show provider count', () => {
      providerStore.providers = mockProviders;

      wrapper = mount(ProviderList);

      expect(wrapper.find('.provider-count').text()).toBe('2 results');
    });

    it('should show singular "result" for one provider', () => {
      providerStore.providers = [mockProviders[0]];

      wrapper = mount(ProviderList);

      expect(wrapper.find('.provider-count').text()).toBe('1 result');
    });
  });

  describe('Loading State', () => {
    it('should show loading state when loading', () => {
      providerStore.loading = true;

      wrapper = mount(ProviderList);

      expect(wrapper.find('.loading-state').exists()).toBe(true);
      expect(wrapper.find('.spinner-border').exists()).toBe(true);
      expect(wrapper.text()).toContain('Finding providers');
    });

    it('should use loading prop over store', () => {
      providerStore.loading = false;

      wrapper = mount(ProviderList, {
        props: {
          loading: true
        }
      });

      expect(wrapper.find('.loading-state').exists()).toBe(true);
    });

    it('should not show provider list when loading', () => {
      providerStore.loading = true;
      providerStore.providers = mockProviders;

      wrapper = mount(ProviderList);

      expect(wrapper.find('.list-container').exists()).toBe(false);
      expect(wrapper.findAllComponents(ProviderCard).length).toBe(0);
    });
  });

  describe('Empty State', () => {
    it('should show empty state when no providers', () => {
      providerStore.providers = [];

      wrapper = mount(ProviderList);

      expect(wrapper.find('.empty-state').exists()).toBe(true);
      expect(wrapper.find('.empty-title').text()).toBe('No providers found');
    });

    it('should use custom empty title', () => {
      wrapper = mount(ProviderList, {
        props: {
          providers: [],
          emptyTitle: 'Custom Title'
        }
      });

      expect(wrapper.find('.empty-title').text()).toBe('Custom Title');
    });

    it('should use custom empty message', () => {
      wrapper = mount(ProviderList, {
        props: {
          providers: [],
          emptyMessage: 'Custom message'
        }
      });

      expect(wrapper.find('.empty-message').text()).toBe('Custom message');
    });

    it('should render empty-actions slot', () => {
      wrapper = mount(ProviderList, {
        props: {
          providers: []
        },
        slots: {
          'empty-actions': '<button>Clear Filters</button>'
        }
      });

      expect(wrapper.html()).toContain('Clear Filters');
    });
  });

  describe('Sort Controls', () => {
    it('should show sort controls by default', () => {
      providerStore.providers = mockProviders;

      wrapper = mount(ProviderList);

      expect(wrapper.find('.sort-controls').exists()).toBe(true);
      expect(wrapper.find('.sort-select').exists()).toBe(true);
    });

    it('should hide sort controls when showSortControls is false', () => {
      providerStore.providers = mockProviders;

      wrapper = mount(ProviderList, {
        props: {
          showSortControls: false
        }
      });

      expect(wrapper.find('.sort-controls').exists()).toBe(false);
    });

    it('should not show sort controls when loading', () => {
      providerStore.providers = mockProviders;
      providerStore.loading = true;

      wrapper = mount(ProviderList);

      expect(wrapper.find('.sort-controls').exists()).toBe(false);
    });

    it('should not show sort controls when no providers', () => {
      providerStore.providers = [];

      wrapper = mount(ProviderList);

      expect(wrapper.find('.sort-controls').exists()).toBe(false);
    });

    it('should have sort options', () => {
      providerStore.providers = mockProviders;

      wrapper = mount(ProviderList);

      const options = wrapper.find('.sort-select').findAll('option');
      expect(options.length).toBe(3);
      expect(options[0].text()).toBe('Distance');
      expect(options[1].text()).toBe('Name');
      expect(options[2].text()).toBe('Type');
    });
  });

  describe('Sorting', () => {
    it('should sort by distance by default', async () => {
      // Provider 1 is closer to user location
      providerStore.providers = mockProviders;
      mapStore.userLocation = { lat: 34.0522, lng: -118.2437 };

      wrapper = mount(ProviderList);

      await nextTick();

      const cards = wrapper.findAllComponents(ProviderCard);
      expect(cards[0].props('provider').id).toBe(1); // Closer provider first
    });

    it('should sort by name when selected', async () => {
      providerStore.providers = mockProviders;

      wrapper = mount(ProviderList);

      const select = wrapper.find('.sort-select');
      await select.setValue('name');
      await nextTick();

      const cards = wrapper.findAllComponents(ProviderCard);
      expect(cards[0].props('provider').name).toBe('ABC Therapy');
      expect(cards[1].props('provider').name).toBe('XYZ Clinic');
    });

    it('should sort by type when selected', async () => {
      providerStore.providers = mockProviders;

      wrapper = mount(ProviderList);

      const select = wrapper.find('.sort-select');
      await select.setValue('type');
      await nextTick();

      const cards = wrapper.findAllComponents(ProviderCard);
      // Medical Clinic comes before Therapy Center
      expect(cards[0].props('provider').type).toBe('Medical Clinic');
      expect(cards[1].props('provider').type).toBe('Therapy Center');
    });

    it('should put providers without distance at end when sorting by distance', async () => {
      const providersWithMissing = [
        ...mockProviders,
        { ...mockProviders[0], id: 3, latitude: null, longitude: null }
      ];

      providerStore.providers = providersWithMissing;

      wrapper = mount(ProviderList);

      await nextTick();

      const cards = wrapper.findAllComponents(ProviderCard);
      expect(cards[2].props('provider').id).toBe(3); // No coords provider last
    });
  });

  describe('Provider Selection', () => {
    it('should pass selected state to provider cards', () => {
      providerStore.providers = mockProviders;
      providerStore.selectedProvider = mockProviders[0];

      wrapper = mount(ProviderList);

      const cards = wrapper.findAllComponents(ProviderCard);
      expect(cards[0].props('selected')).toBe(true);
      expect(cards[1].props('selected')).toBe(false);
    });

    it('should use selectedProviderId prop over store', () => {
      providerStore.providers = mockProviders;
      providerStore.selectedProvider = mockProviders[0];

      wrapper = mount(ProviderList, {
        props: {
          selectedProviderId: 2
        }
      });

      const cards = wrapper.findAllComponents(ProviderCard);
      expect(cards[0].props('selected')).toBe(false);
      expect(cards[1].props('selected')).toBe(true);
    });

    it('should emit provider-select when card is selected', async () => {
      providerStore.providers = mockProviders;

      wrapper = mount(ProviderList);

      const card = wrapper.findComponent(ProviderCard);
      await card.vm.$emit('select', 1);
      await nextTick();

      expect(wrapper.emitted('provider-select')).toBeTruthy();
      expect(wrapper.emitted('provider-select')[0][0]).toBe(1);
    });

    it('should update provider store when card is selected', async () => {
      providerStore.providers = mockProviders;

      const selectSpy = vi.spyOn(providerStore, 'selectProvider');

      wrapper = mount(ProviderList);

      const card = wrapper.findComponent(ProviderCard);
      await card.vm.$emit('select', 1);
      await nextTick();

      expect(selectSpy).toHaveBeenCalledWith(1);
    });

    it('should update map store when card is selected', async () => {
      providerStore.providers = mockProviders;

      const selectSpy = vi.spyOn(mapStore, 'selectProvider');

      wrapper = mount(ProviderList);

      const card = wrapper.findComponent(ProviderCard);
      await card.vm.$emit('select', 1);
      await nextTick();

      expect(selectSpy).toHaveBeenCalledWith(1);
    });
  });

  describe('Provider Click', () => {
    it('should emit provider-click when card is clicked', async () => {
      providerStore.providers = mockProviders;

      wrapper = mount(ProviderList);

      const card = wrapper.findComponent(ProviderCard);
      await card.vm.$emit('click', mockProviders[0]);
      await nextTick();

      expect(wrapper.emitted('provider-click')).toBeTruthy();
      expect(wrapper.emitted('provider-click')[0][0]).toEqual(mockProviders[0]);
    });
  });

  describe('Distance Calculation', () => {
    it('should calculate distance for providers with coordinates', () => {
      providerStore.providers = mockProviders;
      mapStore.userLocation = { lat: 34.0522, lng: -118.2437 };

      wrapper = mount(ProviderList);

      const card = wrapper.findComponent(ProviderCard);
      expect(card.props('distance')).not.toBeNull();
      expect(typeof card.props('distance')).toBe('number');
    });

    it('should return null distance when no user location', () => {
      providerStore.providers = mockProviders;
      mapStore.userLocation = null;

      wrapper = mount(ProviderList);

      const card = wrapper.findComponent(ProviderCard);
      expect(card.props('distance')).toBeNull();
    });

    it('should return null distance when provider has no coordinates', () => {
      const providerWithoutCoords = {
        ...mockProviders[0],
        latitude: null,
        longitude: null
      };

      providerStore.providers = [providerWithoutCoords];
      mapStore.userLocation = { lat: 34.0522, lng: -118.2437 };

      wrapper = mount(ProviderList);

      const card = wrapper.findComponent(ProviderCard);
      expect(card.props('distance')).toBeNull();
    });
  });

  describe('Props Passing to Cards', () => {
    it('should pass showInsurance prop to cards', () => {
      providerStore.providers = mockProviders;

      wrapper = mount(ProviderList, {
        props: {
          showInsurance: false
        }
      });

      const card = wrapper.findComponent(ProviderCard);
      expect(card.props('showInsurance')).toBe(false);
    });

    it('should pass showTherapies prop to cards', () => {
      providerStore.providers = mockProviders;

      wrapper = mount(ProviderList, {
        props: {
          showTherapies: false
        }
      });

      const card = wrapper.findComponent(ProviderCard);
      expect(card.props('showTherapies')).toBe(false);
    });

    it('should pass showAgeGroups prop to cards', () => {
      providerStore.providers = mockProviders;

      wrapper = mount(ProviderList, {
        props: {
          showAgeGroups: true
        }
      });

      const card = wrapper.findComponent(ProviderCard);
      expect(card.props('showAgeGroups')).toBe(true);
    });

    it('should pass maxTherapiesToShow prop to cards', () => {
      providerStore.providers = mockProviders;

      wrapper = mount(ProviderList, {
        props: {
          maxTherapiesToShow: 5
        }
      });

      const card = wrapper.findComponent(ProviderCard);
      expect(card.props('maxTherapiesToShow')).toBe(5);
    });
  });

  describe('Load More', () => {
    it('should show load more button when enabled and hasMore is true', () => {
      providerStore.providers = mockProviders;

      wrapper = mount(ProviderList, {
        props: {
          showLoadMore: true,
          hasMore: true
        }
      });

      expect(wrapper.find('.load-more-btn').exists()).toBe(true);
    });

    it('should not show load more when hasMore is false', () => {
      providerStore.providers = mockProviders;

      wrapper = mount(ProviderList, {
        props: {
          showLoadMore: true,
          hasMore: false
        }
      });

      expect(wrapper.find('.load-more-btn').exists()).toBe(false);
    });

    it('should not show load more when showLoadMore is false', () => {
      providerStore.providers = mockProviders;

      wrapper = mount(ProviderList, {
        props: {
          showLoadMore: false,
          hasMore: true
        }
      });

      expect(wrapper.find('.load-more-btn').exists()).toBe(false);
    });

    it('should emit load-more when button is clicked', async () => {
      providerStore.providers = mockProviders;

      wrapper = mount(ProviderList, {
        props: {
          showLoadMore: true,
          hasMore: true
        }
      });

      await wrapper.find('.load-more-btn').trigger('click');

      expect(wrapper.emitted('load-more')).toBeTruthy();
    });

    it('should not show load more when loading', () => {
      providerStore.providers = mockProviders;
      providerStore.loading = true;

      wrapper = mount(ProviderList, {
        props: {
          showLoadMore: true,
          hasMore: true
        }
      });

      expect(wrapper.find('.load-more-btn').exists()).toBe(false);
    });
  });

  describe('Scroll Functionality', () => {
    it('should render list container', () => {
      providerStore.providers = mockProviders;

      wrapper = mount(ProviderList);

      expect(wrapper.find('.list-container').exists()).toBe(true);
    });

    it('should have scroll to top button initially hidden', () => {
      providerStore.providers = mockProviders;

      wrapper = mount(ProviderList);

      expect(wrapper.find('.scroll-top-btn').exists()).toBe(false);
    });
  });

  describe('Default Props', () => {
    it('should use default prop values', () => {
      wrapper = mount(ProviderList);

      expect(wrapper.vm.showInsurance).toBe(true);
      expect(wrapper.vm.showTherapies).toBe(true);
      expect(wrapper.vm.showAgeGroups).toBe(false);
      expect(wrapper.vm.maxTherapiesToShow).toBe(3);
      expect(wrapper.vm.showSortControls).toBe(true);
      expect(wrapper.vm.emptyTitle).toBe('No providers found');
      expect(wrapper.vm.showLoadMore).toBe(false);
      expect(wrapper.vm.autoScrollToSelected).toBe(true);
    });
  });
});
