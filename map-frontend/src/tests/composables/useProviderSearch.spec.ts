/**
 * Tests for useProviderSearch composable
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import { useProviderSearch } from '@/composables/useProviderSearch';
import axios from 'axios';

// Mock axios
vi.mock('axios');
const mockedAxios = vi.mocked(axios);

describe('useProviderSearch', () => {
  const apiUrl = 'http://localhost:8000';
  let composable: ReturnType<typeof useProviderSearch>;

  beforeEach(() => {
    setActivePinia(createPinia());
    // Reset mocks before each test
    vi.clearAllMocks();

    // Create fresh instance
    composable = useProviderSearch(apiUrl);
  });

  afterEach(() => {
    vi.resetAllMocks();
  });

  describe('Initialization', () => {
    it('should initialize with empty state', () => {
      expect(composable.providers.value).toEqual([]);
      expect(composable.loading.value).toBe(false);
      expect(composable.error.value).toBe(null);
      expect(composable.searchLocation.value).toBe('');
      expect(composable.searchCoordinates.value).toBe(null);
      expect(composable.regionalCenterInfo.value).toBe(null);
    });

    it('should initialize computed properties correctly', () => {
      expect(composable.providerCount.value).toBe(0);
      expect(composable.providersWithCoordinates.value).toEqual([]);
      expect(composable.hasProviders.value).toBe(false);
    });
  });

  describe('searchByZipCode', () => {
    it('should search providers by ZIP code successfully', async () => {
      const mockResponse = {
        data: {
          results: [
            {
              id: 1,
              name: 'Test Provider 1',
              address: '123 Main St, Pomona, CA 91769',
              latitude: 34.0552,
              longitude: -117.7500,
              phone: '(555) 123-4567',
              website: 'https://example.com',
              email: 'test@example.com',
              insurance_accepted: 'Regional Center, Insurance',
              therapy_types: ['ABA'],
              age_groups: ['3-5'],
              diagnoses_treated: ['Autism'],
              description: 'Test provider',
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
              therapy_types: ['Speech Therapy'],
              age_groups: ['6-12'],
              diagnoses_treated: ['Speech Delay'],
              description: null,
              type: null
            }
          ],
          count: 2,
          regional_center: {
            id: 20,
            name: 'San Gabriel/Pomona Regional Center',
            zip_codes: ['91766', '91767', '91768', '91769']
          }
        }
      };

      mockedAxios.get.mockResolvedValue(mockResponse);

      const result = await composable.searchByZipCode('91769');

      // Verify API call
      expect(mockedAxios.get).toHaveBeenCalledTimes(1);
      expect(mockedAxios.get).toHaveBeenCalledWith(
        expect.stringContaining('/api/providers-v2/by_regional_center/?zip_code=91769')
      );

      // Verify result
      expect(result).toBeDefined();
      expect(result?.providers.length).toBe(2);
      expect(result?.count).toBe(2);
      expect(result?.regional_center?.name).toBe('San Gabriel/Pomona Regional Center');

      // Verify state updates
      expect(composable.providers.value.length).toBe(2);
      expect(composable.loading.value).toBe(false);
      expect(composable.error.value).toBe(null);
      expect(composable.regionalCenterInfo.value?.name).toBe('San Gabriel/Pomona Regional Center');
    });

    it('should reject invalid ZIP codes', async () => {
      const result = await composable.searchByZipCode('123'); // Invalid ZIP

      expect(result).toBe(null);
      expect(composable.error.value).toBe('Invalid ZIP code format');
      expect(mockedAxios.get).not.toHaveBeenCalled();
    });

    it('should handle ZIP code search errors', async () => {
      mockedAxios.get.mockRejectedValue(new Error('Network error'));

      const result = await composable.searchByZipCode('91769');

      expect(result).toBe(null);
      expect(composable.error.value).toBe('Network error');
      expect(composable.providers.value).toEqual([]);
      expect(composable.loading.value).toBe(false);
    });

    it('should apply filters when searching by ZIP code', async () => {
      const mockResponse = {
        data: {
          results: [],
          count: 0,
          regional_center: {
            id: 20,
            name: 'San Gabriel/Pomona Regional Center',
            zip_codes: ['91769']
          }
        }
      };

      mockedAxios.get.mockResolvedValue(mockResponse);

      await composable.searchByZipCode('91769', {
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

      const result = await composable.searchByLocation(34.0522, -118.2437, 25);

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
      expect(composable.searchCoordinates.value).toEqual({
        lat: 34.0522,
        lng: -118.2437
      });
    });

    it('should use default radius if not provided', async () => {
      const mockResponse = { data: [] };
      mockedAxios.get.mockResolvedValue(mockResponse);

      await composable.searchByLocation(34.0522, -118.2437);

      expect(mockedAxios.get).toHaveBeenCalledWith(
        expect.stringMatching(/radius=25/)
      );
    });
  });

  describe('Computed properties', () => {
    it('should calculate providerCount correctly', async () => {
      const mockResponse = {
        data: {
          results: [
            { id: 1, name: 'Provider 1' },
            { id: 2, name: 'Provider 2' }
          ],
          count: 2
        }
      };

      mockedAxios.get.mockResolvedValue(mockResponse);
      await composable.searchByZipCode('91769');

      expect(composable.providerCount.value).toBe(2);
    });

    it('should filter providersWithCoordinates correctly', async () => {
      const mockResponse = {
        data: {
          results: [
            { id: 1, name: 'Provider 1', latitude: 34.0, longitude: -118.0 },
            { id: 2, name: 'Provider 2', latitude: null, longitude: null },
            { id: 3, name: 'Provider 3', latitude: 34.1, longitude: -118.1 }
          ],
          count: 3
        }
      };

      mockedAxios.get.mockResolvedValue(mockResponse);
      await composable.searchByZipCode('91769');

      expect(composable.providersWithCoordinates.value.length).toBe(2);
      expect(composable.providersWithCoordinates.value[0].id).toBe(1);
      expect(composable.providersWithCoordinates.value[1].id).toBe(3);
    });

    it('should update hasProviders correctly', async () => {
      expect(composable.hasProviders.value).toBe(false);

      const mockResponse = {
        data: {
          results: [{ id: 1, name: 'Provider 1' }],
          count: 1
        }
      };

      mockedAxios.get.mockResolvedValue(mockResponse);
      await composable.searchByZipCode('91769');

      expect(composable.hasProviders.value).toBe(true);
    });
  });

  describe('Utility methods', () => {
    it('should clear search state', async () => {
      // First, populate some data
      const mockResponse = {
        data: {
          results: [{ id: 1, name: 'Provider 1' }],
          count: 1,
          regional_center: { id: 1, name: 'Test RC', zip_codes: [] }
        }
      };

      mockedAxios.get.mockResolvedValue(mockResponse);
      await composable.searchByZipCode('91769');

      expect(composable.providers.value.length).toBe(1);
      expect(composable.regionalCenterInfo.value).not.toBe(null);

      // Clear
      composable.clearSearch();

      expect(composable.providers.value).toEqual([]);
      expect(composable.searchLocation.value).toBe('');
      expect(composable.searchCoordinates.value).toBe(null);
      expect(composable.regionalCenterInfo.value).toBe(null);
      expect(composable.error.value).toBe(null);
    });

    it('should get provider by ID', async () => {
      const mockResponse = {
        data: {
          results: [
            { id: 1, name: 'Provider 1' },
            { id: 2, name: 'Provider 2' },
            { id: 3, name: 'Provider 3' }
          ],
          count: 3
        }
      };

      mockedAxios.get.mockResolvedValue(mockResponse);
      await composable.searchByZipCode('91769');

      const provider = composable.getProviderById(2);
      expect(provider).toBeDefined();
      expect(provider?.name).toBe('Provider 2');

      const notFound = composable.getProviderById(999);
      expect(notFound).toBeUndefined();
    });

    it('should filter providers by predicate', async () => {
      const mockResponse = {
        data: {
          results: [
            { id: 1, name: 'ABA Provider', therapy_types: ['ABA'] },
            { id: 2, name: 'Speech Provider', therapy_types: ['Speech Therapy'] },
            { id: 3, name: 'Multi Provider', therapy_types: ['ABA', 'OT'] }
          ],
          count: 3
        }
      };

      mockedAxios.get.mockResolvedValue(mockResponse);
      await composable.searchByZipCode('91769');

      const abaProviders = composable.filterProviders(
        (p) => p.therapy_types?.includes('ABA') || false
      );

      expect(abaProviders.length).toBe(2);
      expect(abaProviders[0].id).toBe(1);
      expect(abaProviders[1].id).toBe(3);
    });
  });

  describe('Loading states', () => {
    it('should set loading state during request', async () => {
      let resolvePromise: any;
      const promise = new Promise((resolve) => {
        resolvePromise = resolve;
      });

      mockedAxios.get.mockReturnValue(promise);

      const searchPromise = composable.searchByZipCode('91769');

      // Should be loading immediately
      expect(composable.loading.value).toBe(true);

      // Resolve the promise
      resolvePromise({
        data: {
          results: [],
          count: 0
        }
      });

      await searchPromise;

      // Should not be loading after completion
      expect(composable.loading.value).toBe(false);
    });

    it('should clear loading state on error', async () => {
      mockedAxios.get.mockRejectedValue(new Error('Test error'));

      await composable.searchByZipCode('91769');

      expect(composable.loading.value).toBe(false);
      expect(composable.error.value).toBe('Test error');
    });
  });
});
