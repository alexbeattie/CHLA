/**
 * Vitest setup file
 * Global test configuration and mocks
 */

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
