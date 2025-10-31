/**
 * Validation Utility Functions
 * Pure functions for validating addresses, ZIP codes, and coordinates
 */

/**
 * Check if a ZIP code is in LA County (90xxx-93xxx range)
 * @param {string} zipCode - 5-digit ZIP code
 * @returns {boolean}
 */
export function isLACountyZip(zipCode) {
  if (!zipCode || typeof zipCode !== 'string') return false;
  const zip = zipCode.trim();
  // LA County ZIPs start with 9 (90xxx-93xxx)
  return /^\d{5}$/.test(zip) && zip.startsWith('9');
}

/**
 * Check if coordinates are within LA County bounds
 * LA County bounds: roughly 33.7-34.8 N, -118.9--117.6 W
 * @param {number} lat - Latitude
 * @param {number} lng - Longitude
 * @returns {boolean}
 */
export function isInLACounty(lat, lng) {
  if (typeof lat !== 'number' || typeof lng !== 'number') return false;
  if (isNaN(lat) || isNaN(lng)) return false;
  
  return lat >= 33.7 && lat <= 34.8 &&
         lng >= -118.9 && lng <= -117.6;
}

/**
 * Detect if a string is a standalone ZIP code (5 digits)
 * @param {string} text - Text to check
 * @returns {string|null} ZIP code if found, null otherwise
 */
export function extractZipCode(text) {
  if (!text || typeof text !== 'string') return null;
  const match = text.trim().match(/^\d{5}$/);
  return match ? match[0] : null;
}

/**
 * Extract ZIP code from an address string
 * Looks for ZIP after "CA" or at the end of address
 * @param {string} address - Address string
 * @returns {string|null} ZIP code if found, null otherwise
 */
export function extractZipFromAddress(address) {
  if (!address || typeof address !== 'string') return null;
  
  // Look for ZIP after "CA" or "California"
  const afterCAMatch = address.match(/\b(?:CA|California)\s+(\d{5})\b/i);
  if (afterCAMatch) return afterCAMatch[1];
  
  // Look for ZIP at end of address
  const endMatch = address.match(/,\s*(\d{5})$/);
  if (endMatch) return endMatch[1];
  
  return null;
}

/**
 * Check if text looks like an address (vs a ZIP or search term)
 * @param {string} text - Text to check
 * @returns {boolean}
 */
export function looksLikeAddress(text) {
  if (!text || typeof text !== 'string') return false;
  
  // Has comma, or starts with street number, or contains city/state
  return /,/.test(text) || 
         /^\d+\s+\w/.test(text) || 
         /\b(ca|california|los angeles|la)\b/i.test(text);
}

/**
 * Validate coordinate values are reasonable for California
 * @param {number} lat - Latitude
 * @param {number} lng - Longitude
 * @returns {boolean}
 */
export function isValidCaliforniaCoordinate(lat, lng) {
  if (typeof lat !== 'number' || typeof lng !== 'number') return false;
  if (isNaN(lat) || isNaN(lng)) return false;
  
  // California roughly: 32-42°N, -125--114°W
  return lat >= 32.0 && lat <= 42.0 &&
         lng >= -125.0 && lng <= -114.0;
}

