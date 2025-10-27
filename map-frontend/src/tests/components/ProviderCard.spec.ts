/**
 * Tests for ProviderCard component
 * Week 4: Component Extraction
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { mount } from '@vue/test-utils';
import { nextTick } from 'vue';
import ProviderCard from '@/components/map/ProviderCard.vue';

describe('ProviderCard', () => {
  let wrapper: any;

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
    therapy_types: ['ABA Therapy', 'Speech Therapy', 'Occupational Therapy', 'Physical Therapy'],
    age_groups: ['Children', 'Adolescents'],
    diagnoses_treated: ['Autism', 'ADHD'],
    description: 'A comprehensive therapy center',
    type: 'Therapy Center'
  };

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
    }
  });

  describe('Component Rendering', () => {
    it('should render provider name', () => {
      wrapper = mount(ProviderCard, {
        props: { provider: mockProvider }
      });

      expect(wrapper.find('.provider-name').text()).toBe('ABC Therapy Center');
    });

    it('should render provider type', () => {
      wrapper = mount(ProviderCard, {
        props: { provider: mockProvider }
      });

      expect(wrapper.find('.provider-type').text()).toBe('Therapy Center');
    });

    it('should render provider address', () => {
      wrapper = mount(ProviderCard, {
        props: { provider: mockProvider }
      });

      expect(wrapper.find('.provider-address').text()).toContain('123 Main St, Los Angeles, CA 90001');
    });

    it('should not render type if not provided', () => {
      const providerWithoutType = { ...mockProvider, type: null };
      wrapper = mount(ProviderCard, {
        props: { provider: providerWithoutType }
      });

      expect(wrapper.find('.provider-type').exists()).toBe(false);
    });
  });

  describe('Selection State', () => {
    it('should show selected state when selected prop is true', () => {
      wrapper = mount(ProviderCard, {
        props: {
          provider: mockProvider,
          selected: true
        }
      });

      expect(wrapper.find('.provider-card').classes()).toContain('selected');
      expect(wrapper.find('.selected-indicator').exists()).toBe(true);
    });

    it('should not show selected indicator when not selected', () => {
      wrapper = mount(ProviderCard, {
        props: {
          provider: mockProvider,
          selected: false
        }
      });

      expect(wrapper.find('.provider-card').classes()).not.toContain('selected');
      expect(wrapper.find('.selected-indicator').exists()).toBe(false);
    });

    it('should have aria-pressed attribute matching selected state', () => {
      wrapper = mount(ProviderCard, {
        props: {
          provider: mockProvider,
          selected: true
        }
      });

      expect(wrapper.find('.provider-card').attributes('aria-pressed')).toBe('true');
    });
  });

  describe('Distance Display', () => {
    it('should display distance when provided', () => {
      wrapper = mount(ProviderCard, {
        props: {
          provider: mockProvider,
          distance: 5.2
        }
      });

      expect(wrapper.find('.provider-distance').exists()).toBe(true);
      expect(wrapper.find('.provider-distance').text()).toContain('5.2 mi');
    });

    it('should not display distance when null', () => {
      wrapper = mount(ProviderCard, {
        props: {
          provider: mockProvider,
          distance: null
        }
      });

      expect(wrapper.find('.provider-distance').exists()).toBe(false);
    });

    it('should format small distances correctly', () => {
      wrapper = mount(ProviderCard, {
        props: {
          provider: mockProvider,
          distance: 0.05
        }
      });

      expect(wrapper.find('.provider-distance').text()).toContain('Less than 0.1 mi');
    });

    it('should format distances under 1 mile with one decimal', () => {
      wrapper = mount(ProviderCard, {
        props: {
          provider: mockProvider,
          distance: 0.8
        }
      });

      expect(wrapper.find('.provider-distance').text()).toContain('0.8 mi');
    });

    it('should format distances over 1 mile with one decimal', () => {
      wrapper = mount(ProviderCard, {
        props: {
          provider: mockProvider,
          distance: 12.7
        }
      });

      expect(wrapper.find('.provider-distance').text()).toContain('12.7 mi');
    });
  });

  describe('Contact Information', () => {
    it('should display formatted phone number', () => {
      wrapper = mount(ProviderCard, {
        props: { provider: mockProvider }
      });

      expect(wrapper.text()).toContain('(310) 555-1234');
    });

    it('should render phone as clickable tel: link', () => {
      wrapper = mount(ProviderCard, {
        props: { provider: mockProvider }
      });

      const phoneLink = wrapper.find('a[href^="tel:"]');
      expect(phoneLink.exists()).toBe(true);
      expect(phoneLink.attributes('href')).toBe('tel:3105551234');
    });

    it('should display website link', () => {
      wrapper = mount(ProviderCard, {
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

      wrapper = mount(ProviderCard, {
        props: { provider: providerWithoutProtocol }
      });

      const websiteLink = wrapper.find('a[target="_blank"]');
      expect(websiteLink.attributes('href')).toBe('https://example.com');
    });

    it('should not display contact items if not provided', () => {
      const providerWithoutContact = {
        ...mockProvider,
        phone: null,
        website: null
      };

      wrapper = mount(ProviderCard, {
        props: { provider: providerWithoutContact }
      });

      expect(wrapper.find('.contact-item').exists()).toBe(false);
    });
  });

  describe('Insurance Badges', () => {
    it('should display insurance badges', () => {
      wrapper = mount(ProviderCard, {
        props: {
          provider: mockProvider,
          showInsurance: true
        }
      });

      expect(wrapper.find('.provider-insurance').exists()).toBe(true);
      expect(wrapper.findAll('.insurance-badge').length).toBe(3);
    });

    it('should parse comma-separated insurance types', () => {
      wrapper = mount(ProviderCard, {
        props: {
          provider: mockProvider,
          showInsurance: true
        }
      });

      const badges = wrapper.findAll('.insurance-badge');
      expect(badges[0].text()).toBe('Insurance');
      expect(badges[1].text()).toBe('Regional Center');
      expect(badges[2].text()).toBe('Private Pay');
    });

    it('should parse pipe-separated insurance types', () => {
      const providerWithPipes = {
        ...mockProvider,
        insurance_accepted: 'Insurance | Regional Center'
      };

      wrapper = mount(ProviderCard, {
        props: {
          provider: providerWithPipes,
          showInsurance: true
        }
      });

      const badges = wrapper.findAll('.insurance-badge');
      expect(badges.length).toBe(2);
      expect(badges[0].text()).toBe('Insurance');
      expect(badges[1].text()).toBe('Regional Center');
    });

    it('should not display insurance section when showInsurance is false', () => {
      wrapper = mount(ProviderCard, {
        props: {
          provider: mockProvider,
          showInsurance: false
        }
      });

      expect(wrapper.find('.provider-insurance').exists()).toBe(false);
    });

    it('should not display insurance section when insurance_accepted is empty', () => {
      const providerWithoutInsurance = {
        ...mockProvider,
        insurance_accepted: ''
      };

      wrapper = mount(ProviderCard, {
        props: {
          provider: providerWithoutInsurance,
          showInsurance: true
        }
      });

      expect(wrapper.find('.provider-insurance').exists()).toBe(false);
    });
  });

  describe('Therapy Types', () => {
    it('should display therapy types', () => {
      wrapper = mount(ProviderCard, {
        props: {
          provider: mockProvider,
          showTherapies: true,
          maxTherapiesToShow: 3
        }
      });

      expect(wrapper.find('.provider-therapies').exists()).toBe(true);
      const therapyChips = wrapper.findAll('.therapy-chip');
      expect(therapyChips.length).toBe(4); // 3 therapies + "more" chip
    });

    it('should limit displayed therapies to maxTherapiesToShow', () => {
      wrapper = mount(ProviderCard, {
        props: {
          provider: mockProvider,
          showTherapies: true,
          maxTherapiesToShow: 2
        }
      });

      const therapyChips = wrapper.findAll('.therapy-chip');
      // Should show 2 therapies + 1 "more" chip
      expect(therapyChips.length).toBe(3);
      expect(therapyChips[2].text()).toContain('+2 more');
    });

    it('should show all therapies if count is less than max', () => {
      const providerWithFewTherapies = {
        ...mockProvider,
        therapy_types: ['ABA Therapy', 'Speech Therapy']
      };

      wrapper = mount(ProviderCard, {
        props: {
          provider: providerWithFewTherapies,
          showTherapies: true,
          maxTherapiesToShow: 3
        }
      });

      const therapyChips = wrapper.findAll('.therapy-chip');
      expect(therapyChips.length).toBe(2); // No "more" chip
    });

    it('should not display therapies when showTherapies is false', () => {
      wrapper = mount(ProviderCard, {
        props: {
          provider: mockProvider,
          showTherapies: false
        }
      });

      expect(wrapper.find('.provider-therapies').exists()).toBe(false);
    });

    it('should not display therapies when therapy_types is empty', () => {
      const providerWithoutTherapies = {
        ...mockProvider,
        therapy_types: null
      };

      wrapper = mount(ProviderCard, {
        props: {
          provider: providerWithoutTherapies,
          showTherapies: true
        }
      });

      expect(wrapper.find('.provider-therapies').exists()).toBe(false);
    });
  });

  describe('Age Groups', () => {
    it('should display age groups when showAgeGroups is true', () => {
      wrapper = mount(ProviderCard, {
        props: {
          provider: mockProvider,
          showAgeGroups: true
        }
      });

      expect(wrapper.find('.provider-ages').exists()).toBe(true);
      expect(wrapper.find('.ages-list').text()).toBe('Children, Adolescents');
    });

    it('should not display age groups when showAgeGroups is false', () => {
      wrapper = mount(ProviderCard, {
        props: {
          provider: mockProvider,
          showAgeGroups: false
        }
      });

      expect(wrapper.find('.provider-ages').exists()).toBe(false);
    });

    it('should not display age groups when age_groups is empty', () => {
      const providerWithoutAges = {
        ...mockProvider,
        age_groups: null
      };

      wrapper = mount(ProviderCard, {
        props: {
          provider: providerWithoutAges,
          showAgeGroups: true
        }
      });

      expect(wrapper.find('.provider-ages').exists()).toBe(false);
    });
  });

  describe('Coordinates Warning', () => {
    it('should show warning when provider has no coordinates', () => {
      const providerWithoutCoords = {
        ...mockProvider,
        latitude: null,
        longitude: null
      };

      wrapper = mount(ProviderCard, {
        props: { provider: providerWithoutCoords }
      });

      expect(wrapper.find('.no-coordinates-warning').exists()).toBe(true);
      expect(wrapper.text()).toContain('Location not available on map');
    });

    it('should not show warning when provider has valid coordinates', () => {
      wrapper = mount(ProviderCard, {
        props: { provider: mockProvider }
      });

      expect(wrapper.find('.no-coordinates-warning').exists()).toBe(false);
    });

    it('should add has-coordinates class when coordinates are valid', () => {
      wrapper = mount(ProviderCard, {
        props: { provider: mockProvider }
      });

      expect(wrapper.find('.provider-card').classes()).toContain('has-coordinates');
    });

    it('should not add has-coordinates class when coordinates are invalid', () => {
      const providerWithoutCoords = {
        ...mockProvider,
        latitude: null,
        longitude: null
      };

      wrapper = mount(ProviderCard, {
        props: { provider: providerWithoutCoords }
      });

      expect(wrapper.find('.provider-card').classes()).not.toContain('has-coordinates');
    });
  });

  describe('Click Events', () => {
    it('should emit click event when card is clicked', async () => {
      wrapper = mount(ProviderCard, {
        props: { provider: mockProvider }
      });

      await wrapper.find('.provider-card').trigger('click');

      expect(wrapper.emitted('click')).toBeTruthy();
      expect(wrapper.emitted('click')[0][0]).toEqual(mockProvider);
    });

    it('should emit select event with provider id', async () => {
      wrapper = mount(ProviderCard, {
        props: { provider: mockProvider }
      });

      await wrapper.find('.provider-card').trigger('click');

      expect(wrapper.emitted('select')).toBeTruthy();
      expect(wrapper.emitted('select')[0][0]).toBe(1);
    });

    it('should handle Enter key press', async () => {
      wrapper = mount(ProviderCard, {
        props: { provider: mockProvider }
      });

      await wrapper.find('.provider-card').trigger('keyup.enter');

      expect(wrapper.emitted('click')).toBeTruthy();
    });

    it('should handle Space key press', async () => {
      wrapper = mount(ProviderCard, {
        props: { provider: mockProvider }
      });

      await wrapper.find('.provider-card').trigger('keyup.space');

      expect(wrapper.emitted('click')).toBeTruthy();
    });

    it('should not propagate click when phone link is clicked', async () => {
      wrapper = mount(ProviderCard, {
        props: { provider: mockProvider }
      });

      const phoneLink = wrapper.find('a[href^="tel:"]');
      await phoneLink.trigger('click');

      // Card click should not be emitted
      expect(wrapper.emitted('click')).toBeFalsy();
    });

    it('should not propagate click when website link is clicked', async () => {
      wrapper = mount(ProviderCard, {
        props: { provider: mockProvider }
      });

      const websiteLink = wrapper.find('a[target="_blank"]');
      await websiteLink.trigger('click');

      // Card click should not be emitted
      expect(wrapper.emitted('click')).toBeFalsy();
    });
  });

  describe('Accessibility', () => {
    it('should have role="button"', () => {
      wrapper = mount(ProviderCard, {
        props: { provider: mockProvider }
      });

      expect(wrapper.find('.provider-card').attributes('role')).toBe('button');
    });

    it('should have aria-label with provider name', () => {
      wrapper = mount(ProviderCard, {
        props: { provider: mockProvider }
      });

      expect(wrapper.find('.provider-card').attributes('aria-label')).toBe(
        'Provider: ABC Therapy Center'
      );
    });

    it('should be keyboard accessible with tabindex', () => {
      wrapper = mount(ProviderCard, {
        props: { provider: mockProvider }
      });

      expect(wrapper.find('.provider-card').attributes('tabindex')).toBe('0');
    });

    it('should open website links in new tab with security attributes', () => {
      wrapper = mount(ProviderCard, {
        props: { provider: mockProvider }
      });

      const websiteLink = wrapper.find('a[target="_blank"]');
      expect(websiteLink.attributes('rel')).toBe('noopener noreferrer');
    });
  });

  describe('Props Validation', () => {
    it('should use default prop values', () => {
      wrapper = mount(ProviderCard, {
        props: { provider: mockProvider }
      });

      expect(wrapper.vm.selected).toBe(false);
      expect(wrapper.vm.distance).toBe(null);
      expect(wrapper.vm.showInsurance).toBe(true);
      expect(wrapper.vm.showTherapies).toBe(true);
      expect(wrapper.vm.showAgeGroups).toBe(false);
      expect(wrapper.vm.maxTherapiesToShow).toBe(3);
    });

    it('should accept custom prop values', () => {
      wrapper = mount(ProviderCard, {
        props: {
          provider: mockProvider,
          selected: true,
          distance: 5.0,
          showInsurance: false,
          showTherapies: false,
          showAgeGroups: true,
          maxTherapiesToShow: 5
        }
      });

      expect(wrapper.vm.selected).toBe(true);
      expect(wrapper.vm.distance).toBe(5.0);
      expect(wrapper.vm.showInsurance).toBe(false);
      expect(wrapper.vm.showTherapies).toBe(false);
      expect(wrapper.vm.showAgeGroups).toBe(true);
      expect(wrapper.vm.maxTherapiesToShow).toBe(5);
    });
  });
});
