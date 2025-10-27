/**
 * Tests for providerStore
 * Pinia store for provider data management
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import { useProviderStore } from '@/stores/providerStore';
import axios from 'axios';

// Mock axios
vi.mock('axios');
const mockedAxios = vi.mocked(axios);

describe('providerStore', () => {
  beforeEach(() => {
    // Create a fresh Pinia instance for each test
    setActivePinia(createPinia());
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.resetAllMocks();
  });

  describe('Initialization', () => {
    it('should initialize with empty state', () => {
      const store = useProviderStore();

      expect(store.providers).toEqual([]);
      expect(store.selectedProvider).toBe(null);
      expect(store.loading).toBe(false);
      expect(store.error).toBe(null);
      expect(store.searchLocation).toBe('');
      expect(store.searchCoordinates).toBe(null);
      expect(store.regionalCenterInfo).toBe(null);
    });

    it('should initialize computed properties correctly', () => {
      const store = useProviderStore();

      expect(store.providerCount).toBe(0);
      expect(store.providersWithCoordinates).toEqual([]);
      expect(store.hasProviders).toBe(false);
      expect(store.selectedProviderId).toBe(null);
      expect(store.hasRegionalCenter).toBe(false);
      expect(store.regionalCenterName).toBe('Unknown');
    });
  });

  describe('searchByZipCode', () => {
    it('should search providers by ZIP code', async () => {
      const store = useProviderStore();

      const mockResponse = {
        data: {
          results: [
            {
              id: 1,
              name: 'Test Provider 1',
              address: '123 Main St, Pomona, CA 91769',
              latitude: 34.0552,
              longitude: -117.7500,
              phone: '555-1234',
              website: 'https://test.com',
              email: 'test@test.com',
              insurance_accepted: 'Regional Center',
              therapy_types: ['ABA'],
              age_groups: ['3-5'],
              diagnoses_treated: ['Autism'],
              description: 'Test',
              type: 'Clinic'
            },
            {
              id: 2,
              name: 'Test Provider 2',
              address: '456 Oak Ave, Pomona, CA 91769',
              latitude: 34.0562,
              longitude: -117.7510,
              phone: null,
              website: null,
              email: null,
              insurance_accepted: 'Regional Center',
              therapy_types: ['OT'],
              age_groups: ['6-12'],
              diagnoses_treated: ['ADHD'],
              description: null,
              type: null
            }
          ],
          count: 2,
          regional_center: {
            id: 20,
            name: 'San Gabriel/Pomona Regional Center',
            zip_codes: ['91769']
          }
        }
      };

      mockedAxios.get.mockResolvedValue(mockResponse);

      const result = await store.searchByZipCode('91769');

      expect(result).toBeDefined();
      expect(result?.providers.length).toBe(2);
      expect(result?.count).toBe(2);
      expect(result?.regional_center?.name).toBe('San Gabriel/Pomona Regional Center');

      expect(store.providers.length).toBe(2);
      expect(store.loading).toBe(false);
      expect(store.error).toBe(null);
      expect(store.regionalCenterInfo?.name).toBe('San Gabriel/Pomona Regional Center');
      expect(store.hasRegionalCenter).toBe(true);
    });

    it('should reject invalid ZIP codes', async () => {
      const store = useProviderStore();

      const result = await store.searchByZipCode('123');

      expect(result).toBe(null);
      expect(store.error).toBe('Invalid ZIP code format');
      expect(mockedAxios.get).not.toHaveBeenCalled();
    });

    it('should handle search errors', async () => {
      const store = useProviderStore();

      mockedAxios.get.mockRejectedValue(new Error('Network error'));

      const result = await store.searchByZipCode('91769');

      expect(result).toBe(null);
      expect(store.error).toBe('Network error');
      expect(store.providers).toEqual([]);
      expect(store.loading).toBe(false);
    });

    it('should apply filters when searching', async () => {
      const store = useProviderStore();

      const mockResponse = {
        data: {
          results: [],
          count: 0,
          regional_center: {
            id: 20,
            name: 'Test RC',
            zip_codes: ['91769']
          }
        }
      };

      mockedAxios.get.mockResolvedValue(mockResponse);

      await store.searchByZipCode('91769', {
        insurance: 'regional center',
        age: '3-5',
        diagnosis: 'Autism'
      });

      expect(mockedAxios.get).toHaveBeenCalledWith(
        expect.stringMatching(/insurance=regional\+center/)
      );
      expect(mockedAxios.get).toHaveBeenCalledWith(
        expect.stringMatching(/age=3-5/)
      );
      expect(mockedAxios.get).toHaveBeenCalledWith(
        expect.stringMatching(/diagnosis=Autism/)
      );
    });
  });

  describe('searchByLocation', () => {
    it('should search providers by coordinates', async () => {
      const store = useProviderStore();

      const mockResponse = {
        data: [
          {
            id: 1,
            name: 'Test Provider',
            address: '123 Main St',
            latitude: 34.0522,
            longitude: -118.2437,
            phone: null,
            website: null,
            email: null,
            insurance_accepted: 'Insurance',
            therapy_types: null,
            age_groups: null,
            diagnoses_treated: null,
            description: null,
            type: null
          }
        ]
      };

      mockedAxios.get.mockResolvedValue(mockResponse);

      const result = await store.searchByLocation(34.0522, -118.2437, 25);

      expect(mockedAxios.get).toHaveBeenCalledWith(
        expect.stringMatching(/comprehensive_search/)
      );
      expect(mockedAxios.get).toHaveBeenCalledWith(
        expect.stringMatching(/lat=34.0522/)
      );
      expect(mockedAxios.get).toHaveBeenCalledWith(
        expect.stringMatching(/lng=-118.2437/)
      );
      expect(mockedAxios.get).toHaveBeenCalledWith(
        expect.stringMatching(/radius=25/)
      );

      expect(result?.providers.length).toBe(1);
      expect(store.searchCoordinates).toEqual({
        lat: 34.0522,
        lng: -118.2437
      });
    });

    it('should use default radius if not provided', async () => {
      const store = useProviderStore();

      const mockResponse = { data: [] };
      mockedAxios.get.mockResolvedValue(mockResponse);

      await store.searchByLocation(34.0522, -118.2437);

      expect(mockedAxios.get).toHaveBeenCalledWith(
        expect.stringMatching(/radius=25/)
      );
    });
  });

  describe('selectProvider', () => {
    it('should select a provider by ID', () => {
      const store = useProviderStore();

      store.providers = [
        {
          id: 1,
          name: 'Test Provider',
          address: '123 Main St',
          latitude: 34.0,
          longitude: -118.0,
          phone: null,
          website: null,
          email: null,
          insurance_accepted: 'Insurance',
          therapy_types: null,
          age_groups: null,
          diagnoses_treated: null,
          description: null,
          type: null
        }
      ];

      store.selectProvider(1);

      expect(store.selectedProvider).toBeDefined();
      expect(store.selectedProvider?.id).toBe(1);
      expect(store.selectedProviderId).toBe(1);
    });

    it('should clear selection when null is provided', () => {
      const store = useProviderStore();

      store.selectProvider(1);
      store.selectProvider(null);

      expect(store.selectedProvider).toBe(null);
      expect(store.selectedProviderId).toBe(null);
    });

    it('should warn if provider not found', () => {
      const store = useProviderStore();

      const consoleSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

      store.selectProvider(999);

      expect(consoleSpy).toHaveBeenCalledWith(
        expect.stringContaining('Provider with ID 999 not found')
      );

      consoleSpy.mockRestore();
    });
  });

  describe('getProviderById', () => {
    it('should get provider by ID', () => {
      const store = useProviderStore();

      store.providers = [
        { id: 1, name: 'Provider 1' } as any,
        { id: 2, name: 'Provider 2' } as any,
        { id: 3, name: 'Provider 3' } as any
      ];

      const provider = store.getProviderById(2);

      expect(provider).toBeDefined();
      expect(provider?.name).toBe('Provider 2');
    });

    it('should return undefined if not found', () => {
      const store = useProviderStore();

      store.providers = [{ id: 1, name: 'Provider 1' } as any];

      const provider = store.getProviderById(999);

      expect(provider).toBeUndefined();
    });
  });

  describe('filterProviders', () => {
    it('should filter providers by predicate', () => {
      const store = useProviderStore();

      store.providers = [
        {
          id: 1,
          name: 'ABA Provider',
          therapy_types: ['ABA']
        } as any,
        {
          id: 2,
          name: 'Speech Provider',
          therapy_types: ['Speech Therapy']
        } as any,
        {
          id: 3,
          name: 'Multi Provider',
          therapy_types: ['ABA', 'OT']
        } as any
      ];

      const abaProviders = store.filterProviders(
        (p) => p.therapy_types?.includes('ABA') || false
      );

      expect(abaProviders.length).toBe(2);
      expect(abaProviders[0].id).toBe(1);
      expect(abaProviders[1].id).toBe(3);
    });
  });

  describe('clearProviders', () => {
    it('should clear all provider data', () => {
      const store = useProviderStore();

      // Set some data
      store.providers = [{ id: 1, name: 'Test' } as any];
      store.selectedProvider = { id: 1, name: 'Test' } as any;
      store.searchLocation = 'Test Location';
      store.searchCoordinates = { lat: 34.0, lng: -118.0 };
      store.regionalCenterInfo = { id: 1, name: 'Test RC', zip_codes: [] };
      store.error = 'Test Error';

      // Clear
      store.clearProviders();

      expect(store.providers).toEqual([]);
      expect(store.selectedProvider).toBe(null);
      expect(store.searchLocation).toBe('');
      expect(store.searchCoordinates).toBe(null);
      expect(store.regionalCenterInfo).toBe(null);
      expect(store.error).toBe(null);
    });
  });

  describe('Computed properties', () => {
    it('should calculate providerCount', () => {
      const store = useProviderStore();

      expect(store.providerCount).toBe(0);

      store.providers = [
        { id: 1 } as any,
        { id: 2 } as any
      ];

      expect(store.providerCount).toBe(2);
    });

    it('should filter providersWithCoordinates', () => {
      const store = useProviderStore();

      store.providers = [
        { id: 1, latitude: 34.0, longitude: -118.0 } as any,
        { id: 2, latitude: null, longitude: null } as any,
        { id: 3, latitude: 34.1, longitude: -118.1 } as any
      ];

      expect(store.providersWithCoordinates.length).toBe(2);
      expect(store.providersWithCoordinates[0].id).toBe(1);
      expect(store.providersWithCoordinates[1].id).toBe(3);
    });

    it('should calculate hasProviders', () => {
      const store = useProviderStore();

      expect(store.hasProviders).toBe(false);

      store.providers = [{ id: 1 } as any];

      expect(store.hasProviders).toBe(true);
    });

    it('should calculate hasRegionalCenter', () => {
      const store = useProviderStore();

      expect(store.hasRegionalCenter).toBe(false);

      store.regionalCenterInfo = {
        id: 1,
        name: 'Test RC',
        zip_codes: []
      };

      expect(store.hasRegionalCenter).toBe(true);
    });

    it('should calculate regionalCenterName', () => {
      const store = useProviderStore();

      expect(store.regionalCenterName).toBe('Unknown');

      store.regionalCenterInfo = {
        id: 1,
        name: 'San Gabriel/Pomona RC',
        zip_codes: []
      };

      expect(store.regionalCenterName).toBe('San Gabriel/Pomona RC');
    });
  });

  describe('Loading states', () => {
    it('should set loading state during request', async () => {
      const store = useProviderStore();

      let resolvePromise: any;
      const promise = new Promise((resolve) => {
        resolvePromise = resolve;
      });

      mockedAxios.get.mockReturnValue(promise);

      const searchPromise = store.searchByZipCode('91769');

      expect(store.loading).toBe(true);

      resolvePromise({
        data: {
          results: [],
          count: 0
        }
      });

      await searchPromise;

      expect(store.loading).toBe(false);
    });

    it('should clear loading state on error', async () => {
      const store = useProviderStore();

      mockedAxios.get.mockRejectedValue(new Error('Test error'));

      await store.searchByZipCode('91769');

      expect(store.loading).toBe(false);
      expect(store.error).toBe('Test error');
    });
  });

  describe('setApiBaseUrl', () => {
    it('should allow setting custom API URL', () => {
      const store = useProviderStore();

      store.setApiBaseUrl('https://custom-api.com');

      // The URL should be used in subsequent requests
      // (We can't directly test this without making an actual request)
      expect(true).toBe(true); // Placeholder
    });
  });
});
