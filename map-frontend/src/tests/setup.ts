/**
 * Vitest setup file
 * Global test configuration and mocks
 */

import { config } from '@vue/test-utils';
import { vi } from 'vitest';

// Mock environment variables
process.env.VUE_APP_API_URL = 'http://localhost:8000';
process.env.VUE_APP_MAPBOX_TOKEN = 'test-mapbox-token';

// Mock window.fetch for Mapbox API calls
global.fetch = vi.fn();

// Mock console methods to reduce noise in tests
global.console = {
  ...console,
  log: vi.fn(),
  debug: vi.fn(),
  info: vi.fn(),
  warn: vi.fn(),
  error: vi.fn(),
};

const testTranslations: Record<string, string> = {
  'provider.providers': 'Providers',
  'sidebar.result': 'result',
  'sidebar.results': 'results',
  'providerCard.away': 'away',
  'providerCard.directions': 'Directions',
  'providerCard.getDirectionsTo': 'Get directions to {name}',
  'providerCard.directionsTitle': 'Directions',
  'providerCard.website': 'Website',
  'providerCard.accepts': 'Accepts',
  'providerCard.services': 'Services',
  'providerCard.more': 'more',
  'providerCard.ages': 'Ages',
  'providerCard.locationNotAvailable': 'Location not available',
};

config.global.mocks = {
  ...config.global.mocks,
  $t: (key: string, params?: Record<string, string | number>) => {
    let value = testTranslations[key] ?? key;

    if (params) {
      Object.entries(params).forEach(([paramKey, paramValue]) => {
        value = value.replace(`{${paramKey}}`, String(paramValue));
      });
    }

    return value;
  },
};
