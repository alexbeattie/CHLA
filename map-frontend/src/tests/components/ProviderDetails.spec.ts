/**
 * Tests for ProviderDetails component
 * Week 4: Component Extraction
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { mount } from '@vue/test-utils';
import { setActivePinia, createPinia } from 'pinia';
import { nextTick } from 'vue';
import ProviderDetails from '@/components/map/ProviderDetails.vue';
import { useMapStore } from '@/stores/mapStore';

describe('ProviderDetails', () => {
  let wrapper: any;
  let mapStore: any;

  const mockProvider = {
    id: 1,
    name: 'ABC Therapy Center',
    address: '123 Main St, Los Angeles, CA 90001',
    latitude: 34.0522,
    longitude: -118.2437,
    phone: '3105551234',
    website: 'https://example.com',
    email: 'info@example.com',
    insurance_accepted: 'Insurance, Regional Center, Private Pay',
    therapy_types: ['ABA Therapy', 'Speech Therapy', 'Occupational Therapy'],
    age_groups: ['Children', 'Adolescents'],
    diagnoses_treated: ['Autism', 'ADHD', 'Developmental Delays'],
    description: 'A comprehensive therapy center serving children and families.',
    type: 'Therapy Center'
  };

  beforeEach(() => {
    setActivePinia(createPinia());
    mapStore = useMapStore();

    vi.spyOn(mapStore, 'getDirectionsTo').mockResolvedValue({
      route: {},
      distance: 5.2,
      duration: 15
    });
  });

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
    }
    vi.clearAllMocks();
  });

  describe('Component Rendering', () => {
    it('should render details header', () => {
      wrapper = mount(ProviderDetails, {
        props: { provider: mockProvider }
      });

      expect(wrapper.find('.details-header').exists()).toBe(true);
      expect(wrapper.find('.header-title h2').text()).toBe('Provider Details');
    });

    it('should render provider name', () => {
      wrapper = mount(ProviderDetails, {
        props: { provider: mockProvider }
      });

      expect(wrapper.find('.provider-name').text()).toBe('ABC Therapy Center');
    });

    it('should render provider type badge', () => {
      wrapper = mount(ProviderDetails, {
        props: { provider: mockProvider }
      });

      expect(wrapper.find('.provider-type-badge').text()).toBe('Therapy Center');
    });

    it('should render close button', () => {
      wrapper = mount(ProviderDetails, {
        props: { provider: mockProvider }
      });

      expect(wrapper.find('.close-btn').exists()).toBe(true);
    });

    it('should show empty state when no provider', () => {
      wrapper = mount(ProviderDetails, {
        props: { provider: null }
      });

      expect(wrapper.find('.empty-details').exists()).toBe(true);
      expect(wrapper.text()).toContain('No provider selected');
    });
  });

  describe('Distance Display', () => {
    it('should display distance when provided', () => {
      wrapper = mount(ProviderDetails, {
        props: {
          provider: mockProvider,
          distance: 5.2
        }
      });

      expect(wrapper.find('.distance-section').exists()).toBe(true);
      expect(wrapper.find('.distance-text').text()).toContain('5.2 mi away');
    });

    it('should not display distance when null', () => {
      wrapper = mount(ProviderDetails, {
        props: {
          provider: mockProvider,
          distance: null
        }
      });

      expect(wrapper.find('.distance-section').exists()).toBe(false);
    });

    it('should format small distances correctly', () => {
      wrapper = mount(ProviderDetails, {
        props: {
          provider: mockProvider,
          distance: 0.05
        }
      });

      expect(wrapper.find('.distance-text').text()).toContain('Less than 0.1 mi away');
    });
  });

  describe('Address Section', () => {
    it('should display address', () => {
      wrapper = mount(ProviderDetails, {
        props: { provider: mockProvider }
      });

      expect(wrapper.find('.address-text').text()).toBe('123 Main St, Los Angeles, CA 90001');
    });

    it('should not display address section if no address', () => {
      const providerWithoutAddress = { ...mockProvider, address: null };

      wrapper = mount(ProviderDetails, {
        props: { provider: providerWithoutAddress }
      });

      const sections = wrapper.findAll('.detail-section');
      const addressSection = sections.find((s: any) => s.text().includes('Address'));
      expect(addressSection).toBeUndefined();
    });

    it('should show get directions button when showDirections is true', () => {
      wrapper = mount(ProviderDetails, {
        props: {
          provider: mockProvider,
          showDirections: true
        }
      });

      expect(wrapper.find('.btn-directions').exists()).toBe(true);
    });

    it('should hide get directions button when showDirections is false', () => {
      wrapper = mount(ProviderDetails, {
        props: {
          provider: mockProvider,
          showDirections: false
        }
      });

      expect(wrapper.find('.btn-directions').exists()).toBe(false);
    });

    it('should hide get directions button when no coordinates', () => {
      const providerWithoutCoords = {
        ...mockProvider,
        latitude: null,
        longitude: null
      };

      wrapper = mount(ProviderDetails, {
        props: {
          provider: providerWithoutCoords,
          showDirections: true
        }
      });

      expect(wrapper.find('.btn-directions').exists()).toBe(false);
    });
  });

  describe('Contact Information', () => {
    it('should display formatted phone number', () => {
      wrapper = mount(ProviderDetails, {
        props: { provider: mockProvider }
      });

      expect(wrapper.text()).toContain('(310) 555-1234');
    });

    it('should render phone as clickable tel: link', () => {
      wrapper = mount(ProviderDetails, {
        props: { provider: mockProvider }
      });

      const phoneLink = wrapper.find('a[href^="tel:"]');
      expect(phoneLink.exists()).toBe(true);
      expect(phoneLink.attributes('href')).toBe('tel:3105551234');
    });

    it('should display email', () => {
      wrapper = mount(ProviderDetails, {
        props: { provider: mockProvider }
      });

      const emailLink = wrapper.find('a[href^="mailto:"]');
      expect(emailLink.exists()).toBe(true);
      expect(emailLink.attributes('href')).toBe('mailto:info@example.com');
    });

    it('should display website link', () => {
      wrapper = mount(ProviderDetails, {
        props: { provider: mockProvider }
      });

      const websiteLink = wrapper.find('a[target="_blank"]');
      expect(websiteLink.exists()).toBe(true);
      expect(websiteLink.attributes('href')).toBe('https://example.com');
    });

    it('should add https:// to website if missing', () => {
      const providerWithoutProtocol = {
        ...mockProvider,
        website: 'example.com'
      };

      wrapper = mount(ProviderDetails, {
        props: { provider: providerWithoutProtocol }
      });

      const websiteLink = wrapper.find('a[target="_blank"]');
      expect(websiteLink.attributes('href')).toBe('https://example.com');
    });

    it('should not display contact items if not provided', () => {
      const providerWithoutContact = {
        ...mockProvider,
        phone: null,
        email: null,
        website: null
      };

      wrapper = mount(ProviderDetails, {
        props: { provider: providerWithoutContact }
      });

      expect(wrapper.findAll('.contact-item').length).toBe(0);
    });
  });

  describe('Insurance Section', () => {
    it('should display insurance badges', () => {
      wrapper = mount(ProviderDetails, {
        props: { provider: mockProvider }
      });

      const badges = wrapper.findAll('.insurance-badge');
      expect(badges.length).toBe(3);
      expect(badges[0].text()).toBe('Insurance');
      expect(badges[1].text()).toBe('Regional Center');
      expect(badges[2].text()).toBe('Private Pay');
    });

    it('should parse pipe-separated insurance types', () => {
      const providerWithPipes = {
        ...mockProvider,
        insurance_accepted: 'Insurance | Regional Center'
      };

      wrapper = mount(ProviderDetails, {
        props: { provider: providerWithPipes }
      });

      const badges = wrapper.findAll('.insurance-badge');
      expect(badges.length).toBe(2);
    });

    it('should not display insurance section if not provided', () => {
      const providerWithoutInsurance = {
        ...mockProvider,
        insurance_accepted: null
      };

      wrapper = mount(ProviderDetails, {
        props: { provider: providerWithoutInsurance }
      });

      const sections = wrapper.findAll('.detail-section');
      const insuranceSection = sections.find((s: any) => s.text().includes('Insurance Accepted'));
      expect(insuranceSection).toBeUndefined();
    });
  });

  describe('Therapy Types Section', () => {
    it('should display therapy types as list', () => {
      wrapper = mount(ProviderDetails, {
        props: { provider: mockProvider }
      });

      const therapyItems = wrapper.findAll('.therapy-list li');
      expect(therapyItems.length).toBe(3);
      expect(therapyItems[0].text()).toContain('ABA Therapy');
      expect(therapyItems[1].text()).toContain('Speech Therapy');
      expect(therapyItems[2].text()).toContain('Occupational Therapy');
    });

    it('should show check icons for therapies', () => {
      wrapper = mount(ProviderDetails, {
        props: { provider: mockProvider }
      });

      const checkIcons = wrapper.findAll('.therapy-list i');
      expect(checkIcons.length).toBe(3);
    });

    it('should not display therapy section if not provided', () => {
      const providerWithoutTherapies = {
        ...mockProvider,
        therapy_types: null
      };

      wrapper = mount(ProviderDetails, {
        props: { provider: providerWithoutTherapies }
      });

      expect(wrapper.find('.therapy-list').exists()).toBe(false);
    });

    it('should not display therapy section if empty array', () => {
      const providerWithEmptyTherapies = {
        ...mockProvider,
        therapy_types: []
      };

      wrapper = mount(ProviderDetails, {
        props: { provider: providerWithEmptyTherapies }
      });

      expect(wrapper.find('.therapy-list').exists()).toBe(false);
    });
  });

  describe('Age Groups Section', () => {
    it('should display age group chips', () => {
      wrapper = mount(ProviderDetails, {
        props: { provider: mockProvider }
      });

      const ageChips = wrapper.findAll('.age-chip');
      expect(ageChips.length).toBe(2);
      expect(ageChips[0].text()).toBe('Children');
      expect(ageChips[1].text()).toBe('Adolescents');
    });

    it('should not display age section if not provided', () => {
      const providerWithoutAges = {
        ...mockProvider,
        age_groups: null
      };

      wrapper = mount(ProviderDetails, {
        props: { provider: providerWithoutAges }
      });

      expect(wrapper.find('.age-chips').exists()).toBe(false);
    });
  });

  describe('Diagnoses Section', () => {
    it('should display diagnosis chips', () => {
      wrapper = mount(ProviderDetails, {
        props: { provider: mockProvider }
      });

      const diagnosisChips = wrapper.findAll('.diagnosis-chip');
      expect(diagnosisChips.length).toBe(3);
      expect(diagnosisChips[0].text()).toBe('Autism');
      expect(diagnosisChips[1].text()).toBe('ADHD');
      expect(diagnosisChips[2].text()).toBe('Developmental Delays');
    });

    it('should not display diagnoses section if not provided', () => {
      const providerWithoutDiagnoses = {
        ...mockProvider,
        diagnoses_treated: null
      };

      wrapper = mount(ProviderDetails, {
        props: { provider: providerWithoutDiagnoses }
      });

      expect(wrapper.find('.diagnoses-chips').exists()).toBe(false);
    });
  });

  describe('Description Section', () => {
    it('should display description', () => {
      wrapper = mount(ProviderDetails, {
        props: { provider: mockProvider }
      });

      expect(wrapper.find('.description-text').text()).toBe(
        'A comprehensive therapy center serving children and families.'
      );
    });

    it('should not display description section if not provided', () => {
      const providerWithoutDescription = {
        ...mockProvider,
        description: null
      };

      wrapper = mount(ProviderDetails, {
        props: { provider: providerWithoutDescription }
      });

      expect(wrapper.find('.description-text').exists()).toBe(false);
    });
  });

  describe('Coordinates Warning', () => {
    it('should show warning when provider has no coordinates', () => {
      const providerWithoutCoords = {
        ...mockProvider,
        latitude: null,
        longitude: null
      };

      wrapper = mount(ProviderDetails, {
        props: { provider: providerWithoutCoords }
      });

      expect(wrapper.find('.warning-section').exists()).toBe(true);
      expect(wrapper.text()).toContain('Location coordinates not available');
    });

    it('should not show warning when provider has valid coordinates', () => {
      wrapper = mount(ProviderDetails, {
        props: { provider: mockProvider }
      });

      expect(wrapper.find('.warning-section').exists()).toBe(false);
    });
  });

  describe('Close Event', () => {
    it('should emit close event when close button is clicked', async () => {
      wrapper = mount(ProviderDetails, {
        props: { provider: mockProvider }
      });

      await wrapper.find('.close-btn').trigger('click');

      expect(wrapper.emitted('close')).toBeTruthy();
    });
  });

  describe('Get Directions', () => {
    it('should call mapStore.getDirectionsTo when button is clicked', async () => {
      wrapper = mount(ProviderDetails, {
        props: {
          provider: mockProvider,
          showDirections: true
        }
      });

      await wrapper.find('.btn-directions').trigger('click');
      await nextTick();

      expect(mapStore.getDirectionsTo).toHaveBeenCalledWith({
        lat: 34.0522,
        lng: -118.2437
      });
    });

    it('should emit get-directions event with provider info', async () => {
      wrapper = mount(ProviderDetails, {
        props: {
          provider: mockProvider,
          showDirections: true
        }
      });

      await wrapper.find('.btn-directions').trigger('click');
      await nextTick();

      expect(wrapper.emitted('get-directions')).toBeTruthy();
      expect(wrapper.emitted('get-directions')[0][0]).toEqual({
        providerId: 1,
        providerName: 'ABC Therapy Center',
        coordinates: {
          lat: 34.0522,
          lng: -118.2437
        }
      });
    });

    it('should handle error when getting directions fails', async () => {
      const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
      mapStore.getDirectionsTo.mockRejectedValueOnce(new Error('Directions error'));

      wrapper = mount(ProviderDetails, {
        props: {
          provider: mockProvider,
          showDirections: true
        }
      });

      await wrapper.find('.btn-directions').trigger('click');
      await nextTick();

      expect(consoleErrorSpy).toHaveBeenCalled();

      consoleErrorSpy.mockRestore();
    });
  });

  describe('Visibility', () => {
    it('should add is-visible class when isVisible is true', () => {
      wrapper = mount(ProviderDetails, {
        props: {
          provider: mockProvider,
          isVisible: true
        }
      });

      expect(wrapper.find('.provider-details').classes()).toContain('is-visible');
    });

    it('should not add is-visible class when isVisible is false', () => {
      wrapper = mount(ProviderDetails, {
        props: {
          provider: mockProvider,
          isVisible: false
        }
      });

      expect(wrapper.find('.provider-details').classes()).not.toContain('is-visible');
    });
  });

  describe('Footer Slot', () => {
    it('should render footer slot content', () => {
      wrapper = mount(ProviderDetails, {
        props: { provider: mockProvider },
        slots: {
          footer: '<div class="custom-footer">Custom Footer Content</div>'
        }
      });

      expect(wrapper.find('.custom-footer').exists()).toBe(true);
      expect(wrapper.text()).toContain('Custom Footer Content');
    });

    it('should not render footer section if no slot content', () => {
      wrapper = mount(ProviderDetails, {
        props: { provider: mockProvider }
      });

      expect(wrapper.find('.details-footer').exists()).toBe(false);
    });
  });

  describe('Default Props', () => {
    it('should use default prop values', () => {
      wrapper = mount(ProviderDetails);

      expect(wrapper.vm.provider).toBe(null);
      expect(wrapper.vm.isVisible).toBe(true);
      expect(wrapper.vm.distance).toBe(null);
      expect(wrapper.vm.showDirections).toBe(true);
    });

    it('should accept custom prop values', () => {
      wrapper = mount(ProviderDetails, {
        props: {
          provider: mockProvider,
          isVisible: false,
          distance: 5.0,
          showDirections: false
        }
      });

      expect(wrapper.vm.provider).toEqual(mockProvider);
      expect(wrapper.vm.isVisible).toBe(false);
      expect(wrapper.vm.distance).toBe(5.0);
      expect(wrapper.vm.showDirections).toBe(false);
    });
  });

  describe('Accessibility', () => {
    it('should have aria-label on close button', () => {
      wrapper = mount(ProviderDetails, {
        props: { provider: mockProvider }
      });

      expect(wrapper.find('.close-btn').attributes('aria-label')).toBe('Close details');
    });

    it('should open website links in new tab with security attributes', () => {
      wrapper = mount(ProviderDetails, {
        props: { provider: mockProvider }
      });

      const websiteLink = wrapper.find('a[target="_blank"]');
      expect(websiteLink.attributes('rel')).toBe('noopener noreferrer');
    });
  });
});
