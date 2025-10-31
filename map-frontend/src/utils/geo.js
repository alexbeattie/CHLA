/**
 * Geographic Utility Functions
 * Pure functions for geographic calculations
 */

/**
 * Get the approximate bounds for LA County
 * @returns {Array} [[minLng, minLat], [maxLng, maxLat]]
 */
export function getLACountyBounds() {
  // Approximate LA County bounds
  return [
    [-119.1, 33.3],
    [-117.5, 34.9],
  ];
}

/**
 * Check if a point is within given bounds
 * @param {number} lng - Longitude
 * @param {number} lat - Latitude
 * @param {Array} bounds - [[minLng, minLat], [maxLng, maxLat]]
 * @returns {boolean}
 */
export function isPointInBounds(lng, lat, bounds) {
  if (!Array.isArray(bounds) || bounds.length !== 2) return false;
  const [[minLng, minLat], [maxLng, maxLat]] = bounds;
  return lng >= minLng && lng <= maxLng && lat >= minLat && lat <= maxLat;
}

