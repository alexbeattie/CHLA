/**
 * Map utilities index
 * Convenient single import point for all map-related utilities
 *
 * Usage:
 * import { isValidZipCode, isWithinBounds, CA_BOUNDS } from '@/utils/map';
 */

// Export all from coordinates (includes isValidZipCode, extractZipCode)
export * from './coordinates';

// Export from geocoding, excluding duplicates
export {
  geocodeAddress,
  geocodeZipCode,
  geocodeWithNominatim,
  reverseGeocode,
  geocodeWithFallback,
  isValidZipPlus4,
  type GeocodeResult
} from './geocoding';
