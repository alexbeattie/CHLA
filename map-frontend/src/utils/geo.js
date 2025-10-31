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

/**
 * Calculate bounds from provider locations
 * @param {Array} providers - Array of provider objects with latitude/longitude
 * @returns {Array|null} Bounds [[minLng, minLat], [maxLng, maxLat]] or null if no valid coords
 */
export function calculateProviderBounds(providers) {
  if (!providers || providers.length === 0) {
    return null;
  }

  const validCoords = providers
    .filter(provider => provider.latitude && provider.longitude)
    .map(provider => [parseFloat(provider.longitude), parseFloat(provider.latitude)]);

  if (validCoords.length === 0) {
    return null;
  }

  // Calculate bounding box
  const lngs = validCoords.map(coord => coord[0]);
  const lats = validCoords.map(coord => coord[1]);
  
  let bounds = [
    [Math.min(...lngs), Math.min(...lats)], // Southwest corner
    [Math.max(...lngs), Math.max(...lats)]  // Northeast corner
  ];

  // Always constrain to LA County area as the baseline
  // This ensures the map never zooms out beyond the relevant service area
  const laCountyBounds = [
    [-118.7, 33.7], // Southwest corner of LA County
    [-118.0, 34.4]  // Northeast corner of LA County
  ];

  // If calculated bounds are within LA County, use them
  // Otherwise, use LA County bounds as the maximum extent
  const lngSpan = bounds[1][0] - bounds[0][0];
  const latSpan = bounds[1][1] - bounds[0][1];
  
  if (lngSpan > 1.5 || latSpan > 1.5) {
    console.log("🔍 Using LA County bounds as baseline (calculated bounds too large)");
    bounds = laCountyBounds;
  } else {
    // Ensure bounds don't exceed LA County limits
    bounds[0][0] = Math.max(bounds[0][0], laCountyBounds[0][0]); // West limit
    bounds[0][1] = Math.max(bounds[0][1], laCountyBounds[0][1]); // South limit
    bounds[1][0] = Math.min(bounds[1][0], laCountyBounds[1][0]); // East limit
    bounds[1][1] = Math.min(bounds[1][1], laCountyBounds[1][1]); // North limit
    console.log("🔍 Constrained calculated bounds to LA County limits");
  }

  console.log(`🔍 Final bounds from ${validCoords.length} providers:`, bounds);
  return bounds;
}

