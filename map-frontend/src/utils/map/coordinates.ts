/**
 * Coordinate validation and bounds checking utilities
 * Extracted from MapView.vue as part of Week 1 refactoring
 */

export interface CoordinateBounds {
  minLat: number;
  maxLat: number;
  minLng: number;
  maxLng: number;
}

export interface Coordinates {
  lat: number;
  lng: number;
}

/**
 * California bounding box
 * Used to validate that providers are within CA
 */
export const CA_BOUNDS: CoordinateBounds = {
  minLat: 32,
  maxLat: 42,
  minLng: -125,
  maxLng: -114
};

/**
 * Los Angeles County approximate center
 */
export const LA_CENTER: Coordinates = {
  lat: 34.0522,
  lng: -118.2437
};

/**
 * Check if coordinates are within specified bounds
 * @param lat Latitude
 * @param lng Longitude
 * @param bounds Bounding box (defaults to CA_BOUNDS)
 * @returns true if within bounds
 */
export function isWithinBounds(
  lat: number,
  lng: number,
  bounds: CoordinateBounds = CA_BOUNDS
): boolean {
  return (
    lat >= bounds.minLat &&
    lat <= bounds.maxLat &&
    lng >= bounds.minLng &&
    lng <= bounds.maxLng
  );
}

/**
 * Validate and parse coordinate values
 * Handles various input types and validates ranges
 * @param lat Latitude value (any type)
 * @param lng Longitude value (any type)
 * @returns Parsed coordinates or null if invalid
 */
export function validateCoordinates(lat: any, lng: any): Coordinates | null {
  // Parse to numbers
  const parsedLat = typeof lat === 'number' ? lat : parseFloat(lat);
  const parsedLng = typeof lng === 'number' ? lng : parseFloat(lng);

  // Check for NaN
  if (isNaN(parsedLat) || isNaN(parsedLng)) {
    return null;
  }

  // Validate latitude range (-90 to 90)
  if (parsedLat < -90 || parsedLat > 90) {
    return null;
  }

  // Validate longitude range (-180 to 180)
  if (parsedLng < -180 || parsedLng > 180) {
    return null;
  }

  return {
    lat: parsedLat,
    lng: parsedLng
  };
}

/**
 * Check if coordinates are valid for California
 * Combines validation and bounds checking
 * @param lat Latitude
 * @param lng Longitude
 * @returns true if coordinates are valid and within CA
 */
export function isValidCACoordinate(lat: any, lng: any): boolean {
  const coords = validateCoordinates(lat, lng);
  if (!coords) return false;
  return isWithinBounds(coords.lat, coords.lng, CA_BOUNDS);
}

/**
 * Format coordinates for display
 * @param lat Latitude
 * @param lng Longitude
 * @param precision Number of decimal places (default: 4)
 * @returns Formatted string like "34.0522, -118.2437"
 */
export function formatCoordinates(lat: number, lng: number, precision: number = 4): string {
  return `${lat.toFixed(precision)}, ${lng.toFixed(precision)}`;
}

/**
 * Calculate distance between two points using Haversine formula
 * @param lat1 First point latitude
 * @param lng1 First point longitude
 * @param lat2 Second point latitude
 * @param lng2 Second point longitude
 * @returns Distance in miles
 */
export function calculateDistance(
  lat1: number,
  lng1: number,
  lat2: number,
  lng2: number
): number {
  const R = 3959; // Earth's radius in miles
  const dLat = toRadians(lat2 - lat1);
  const dLng = toRadians(lng2 - lng1);

  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRadians(lat1)) *
      Math.cos(toRadians(lat2)) *
      Math.sin(dLng / 2) *
      Math.sin(dLng / 2);

  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

/**
 * Convert degrees to radians
 */
function toRadians(degrees: number): number {
  return degrees * (Math.PI / 180);
}
