/**
 * Tests for useRegionalCenter composable
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { useRegionalCenter } from '@/composables/useRegionalCenter';
import axios from 'axios';

// Mock axios
vi.mock('axios');
const mockedAxios = vi.mocked(axios);

describe('useRegionalCenter', () => {
  const apiUrl = 'http://localhost:8000';
  let composable: ReturnType<typeof useRegionalCenter>;

  beforeEach(() => {
    vi.clearAllMocks();
    composable = useRegionalCenter(apiUrl);
  });

  afterEach(() => {
    vi.resetAllMocks();
  });

  describe('Initialization', () => {
    it('should initialize with empty state', () => {
      expect(composable.regionalCenters.value).toEqual([]);
      expect(composable.currentRegionalCenter.value).toBe(null);
      expect(composable.regionalCenterBoundary.value).toBe(null);
      expect(composable.loading.value).toBe(false);
      expect(composable.error.value).toBe(null);
    });

    it('should initialize computed properties correctly', () => {
      expect(composable.hasRegionalCenter.value).toBe(false);
      expect(composable.regionalCenterName.value).toBe('Unknown');
      expect(composable.regionalCenterZipCodes.value).toEqual([]);
      expect(composable.hasBoundary.value).toBe(false);
    });
  });

  describe('fetchRegionalCenters', () => {
    it('should fetch all regional centers', async () => {
      const mockResponse = {
        data: [
          {
            id: 1,
            name: 'Harbor Regional Center',
            regional_center: 'Harbor Regional Center',
            zip_codes: ['90710', '90732', '90744']
          },
          {
            id: 20,
            name: 'San Gabriel/Pomona Regional Center',
            regional_center: 'San Gabriel/Pomona Regional Center',
            zip_codes: ['91766', '91767', '91768', '91769']
          }
        ]
      };

      mockedAxios.get.mockResolvedValue(mockResponse);

      const result = await composable.fetchRegionalCenters();

      expect(mockedAxios.get).toHaveBeenCalledWith(`${apiUrl}/api/regional-centers/`);
      expect(result).toEqual(mockResponse.data);
      expect(composable.regionalCenters.value).toEqual(mockResponse.data);
      expect(composable.loading.value).toBe(false);
      expect(composable.error.value).toBe(null);
    });

    it('should handle fetch errors', async () => {
      mockedAxios.get.mockRejectedValue(new Error('Network error'));

      const result = await composable.fetchRegionalCenters();

      expect(result).toEqual([]);
      expect(composable.regionalCenters.value).toEqual([]);
      expect(composable.error.value).toBe('Network error');
      expect(composable.loading.value).toBe(false);
    });
  });

  describe('findByZipCode', () => {
    it('should find regional center by ZIP code', async () => {
      const mockResponse = {
        data: {
          id: 20,
          name: 'San Gabriel/Pomona Regional Center',
          regional_center: 'San Gabriel/Pomona Regional Center',
          zip_codes: ['91766', '91767', '91768', '91769']
        }
      };

      mockedAxios.get.mockResolvedValue(mockResponse);

      const result = await composable.findByZipCode('91769');

      expect(mockedAxios.get).toHaveBeenCalledWith(
        `${apiUrl}/api/regional-centers/by_zip_code/`,
        { params: { zip_code: '91769' } }
      );
      expect(result).toEqual(mockResponse.data);
      expect(composable.currentRegionalCenter.value).toEqual(mockResponse.data);
      expect(composable.hasRegionalCenter.value).toBe(true);
      expect(composable.regionalCenterName.value).toBe('San Gabriel/Pomona Regional Center');
    });

    it('should reject invalid ZIP codes', async () => {
      const result = await composable.findByZipCode('123');

      expect(result).toBe(null);
      expect(composable.error.value).toBe('Invalid ZIP code format');
      expect(mockedAxios.get).not.toHaveBeenCalled();
    });

    it('should handle not found errors', async () => {
      mockedAxios.get.mockRejectedValue({
        response: {
          data: {
            error: 'No regional center found for ZIP code 99999'
          }
        }
      });

      const result = await composable.findByZipCode('99999');

      expect(result).toBe(null);
      expect(composable.error.value).toBe('No regional center found for ZIP code 99999');
    });

    it('should handle network errors', async () => {
      mockedAxios.get.mockRejectedValue(new Error('Network error'));

      const result = await composable.findByZipCode('91769');

      expect(result).toBe(null);
      expect(composable.error.value).toBe('Network error');
    });
  });

  describe('getById', () => {
    it('should fetch regional center by ID', async () => {
      const mockResponse = {
        data: {
          id: 20,
          name: 'San Gabriel/Pomona Regional Center',
          regional_center: 'San Gabriel/Pomona Regional Center',
          zip_codes: ['91766', '91767']
        }
      };

      mockedAxios.get.mockResolvedValue(mockResponse);

      const result = await composable.getById(20);

      expect(mockedAxios.get).toHaveBeenCalledWith(`${apiUrl}/api/regional-centers/20/`);
      expect(result).toEqual(mockResponse.data);
      expect(composable.currentRegionalCenter.value).toEqual(mockResponse.data);
    });

    it('should handle get by ID errors', async () => {
      mockedAxios.get.mockRejectedValue(new Error('Not found'));

      const result = await composable.getById(999);

      expect(result).toBe(null);
      expect(composable.error.value).toBe('Not found');
    });
  });

  describe('setRegionalCenter', () => {
    it('should set current regional center', () => {
      const rc = {
        id: 20,
        name: 'Test RC',
        regional_center: 'Test RC',
        zip_codes: ['91769']
      };

      composable.setRegionalCenter(rc);

      expect(composable.currentRegionalCenter.value).toEqual(rc);
      expect(composable.hasRegionalCenter.value).toBe(true);
    });

    it('should allow setting null', () => {
      const rc = {
        id: 20,
        name: 'Test RC',
        regional_center: 'Test RC',
        zip_codes: ['91769']
      };

      composable.setRegionalCenter(rc);
      composable.setRegionalCenter(null);

      expect(composable.currentRegionalCenter.value).toBe(null);
      expect(composable.hasRegionalCenter.value).toBe(false);
    });
  });

  describe('clearRegionalCenter', () => {
    it('should clear all regional center state', () => {
      composable.setRegionalCenter({
        id: 20,
        name: 'Test RC',
        regional_center: 'Test RC',
        zip_codes: ['91769']
      });
      composable.error.value = 'Some error';

      composable.clearRegionalCenter();

      expect(composable.currentRegionalCenter.value).toBe(null);
      expect(composable.regionalCenterBoundary.value).toBe(null);
      expect(composable.error.value).toBe(null);
    });
  });

  describe('isZipInRegionalCenter', () => {
    it('should return true for ZIP in regional center', () => {
      composable.setRegionalCenter({
        id: 20,
        name: 'Test RC',
        regional_center: 'Test RC',
        zip_codes: ['91766', '91767', '91768', '91769']
      });

      expect(composable.isZipInRegionalCenter('91769')).toBe(true);
      expect(composable.isZipInRegionalCenter('91766')).toBe(true);
    });

    it('should return false for ZIP not in regional center', () => {
      composable.setRegionalCenter({
        id: 20,
        name: 'Test RC',
        regional_center: 'Test RC',
        zip_codes: ['91766', '91767']
      });

      expect(composable.isZipInRegionalCenter('91769')).toBe(false);
      expect(composable.isZipInRegionalCenter('90001')).toBe(false);
    });

    it('should return false if no current regional center', () => {
      expect(composable.isZipInRegionalCenter('91769')).toBe(false);
    });

    it('should return false if regional center has no ZIP codes', () => {
      composable.setRegionalCenter({
        id: 20,
        name: 'Test RC',
        regional_center: 'Test RC',
        zip_codes: []
      });

      expect(composable.isZipInRegionalCenter('91769')).toBe(false);
    });
  });

  describe('generateApproximateBoundary', () => {
    it('should generate boundary from ZIP codes', async () => {
      const zipCodes = ['91769', '91766', '91767'];

      // Mock Mapbox geocoding responses
      global.fetch = vi.fn()
        .mockResolvedValueOnce({
          json: async () => ({
            features: [
              {
                center: [-117.7500, 34.0552]
              }
            ]
          })
        })
        .mockResolvedValueOnce({
          json: async () => ({
            features: [
              {
                center: [-117.7400, 34.0652]
              }
            ]
          })
        })
        .mockResolvedValueOnce({
          json: async () => ({
            features: [
              {
                center: [-117.7450, 34.0602]
              }
            ]
          })
        });

      const boundary = await composable.generateApproximateBoundary(zipCodes, 'test-token');

      expect(boundary).toBeDefined();
      expect(boundary?.type).toBe('Polygon');
      expect(boundary?.coordinates).toBeDefined();
      expect(boundary?.coordinates[0]).toHaveLength(3);
      expect(composable.regionalCenterBoundary.value).toEqual(boundary);
    });

    it('should return null for empty ZIP codes', async () => {
      const boundary = await composable.generateApproximateBoundary([], 'test-token');

      expect(boundary).toBe(null);
    });

    it('should return null if not enough ZIP codes', async () => {
      const zipCodes = ['91769', '91766'];

      global.fetch = vi.fn()
        .mockResolvedValueOnce({
          json: async () => ({
            features: [{ center: [-117.7500, 34.0552] }]
          })
        })
        .mockResolvedValueOnce({
          json: async () => ({
            features: [] // No result for second ZIP
          })
        });

      const boundary = await composable.generateApproximateBoundary(zipCodes, 'test-token');

      expect(boundary).toBe(null);
    });

    it('should handle geocoding errors', async () => {
      const zipCodes = ['91769'];

      global.fetch = vi.fn().mockRejectedValue(new Error('Geocoding error'));

      const boundary = await composable.generateApproximateBoundary(zipCodes, 'test-token');

      expect(boundary).toBe(null);
      expect(composable.error.value).toBe('Geocoding error');
    });
  });

  describe('getHighlightGeoJSON', () => {
    it('should return GeoJSON for boundary', () => {
      const mockBoundary = {
        type: 'Polygon' as const,
        coordinates: [
          [
            [-117.7500, 34.0552],
            [-117.7400, 34.0652]
          ]
        ]
      };

      composable.setRegionalCenter({
        id: 20,
        name: 'Test RC',
        regional_center: 'Test Regional Center',
        zip_codes: ['91769']
      });
      composable.regionalCenterBoundary.value = mockBoundary;

      const geoJSON = composable.getHighlightGeoJSON();

      expect(geoJSON).toBeDefined();
      expect(geoJSON.type).toBe('Feature');
      expect(geoJSON.properties.name).toBe('Test Regional Center');
      expect(geoJSON.geometry).toEqual(mockBoundary);
    });

    it('should return null if no boundary', () => {
      const geoJSON = composable.getHighlightGeoJSON();

      expect(geoJSON).toBe(null);
    });
  });

  describe('findNearestToCoordinates', () => {
    it('should find nearest regional center by reverse geocoding', async () => {
      // Mock Mapbox reverse geocoding
      global.fetch = vi.fn().mockResolvedValue({
        json: async () => ({
          features: [
            {
              text: '91769'
            }
          ]
        })
      });

      // Mock regional center lookup
      const mockRC = {
        id: 20,
        name: 'San Gabriel/Pomona Regional Center',
        regional_center: 'San Gabriel/Pomona Regional Center',
        zip_codes: ['91769']
      };

      mockedAxios.get.mockResolvedValue({ data: mockRC });

      const result = await composable.findNearestToCoordinates(
        34.0552,
        -117.7500,
        'test-token'
      );

      expect(result).toEqual(mockRC);

      // Verify reverse geocoding API call
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('api.mapbox.com/geocoding')
      );
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('-117.75,34.0552')
      );
    });

    it('should return null if no features returned', async () => {
      global.fetch = vi.fn().mockResolvedValue({
        json: async () => ({
          features: []
        })
      });

      const result = await composable.findNearestToCoordinates(
        34.0552,
        -117.7500,
        'test-token'
      );

      expect(result).toBe(null);
    });

    it('should handle errors', async () => {
      global.fetch = vi.fn().mockRejectedValue(new Error('Geocoding error'));

      const result = await composable.findNearestToCoordinates(
        34.0552,
        -117.7500,
        'test-token'
      );

      expect(result).toBe(null);
      expect(composable.error.value).toBe('Geocoding error');
    });
  });

  describe('Computed properties', () => {
    it('should calculate regionalCenterZipCodes', () => {
      expect(composable.regionalCenterZipCodes.value).toEqual([]);

      composable.setRegionalCenter({
        id: 20,
        name: 'Test RC',
        regional_center: 'Test RC',
        zip_codes: ['91766', '91767', '91769']
      });

      expect(composable.regionalCenterZipCodes.value).toEqual(['91766', '91767', '91769']);
    });

    it('should calculate hasBoundary', () => {
      expect(composable.hasBoundary.value).toBe(false);

      composable.regionalCenterBoundary.value = {
        type: 'Polygon',
        coordinates: [[[0, 0]]]
      };

      expect(composable.hasBoundary.value).toBe(true);
    });
  });

  describe('Loading states', () => {
    it('should set loading state during fetch', async () => {
      let resolvePromise: any;
      const promise = new Promise((resolve) => {
        resolvePromise = resolve;
      });

      mockedAxios.get.mockReturnValue(promise);

      const fetchPromise = composable.fetchRegionalCenters();

      expect(composable.loading.value).toBe(true);

      resolvePromise({ data: [] });
      await fetchPromise;

      expect(composable.loading.value).toBe(false);
    });
  });
});
