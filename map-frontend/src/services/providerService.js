/**
 * Provider API Service
 */

import { getApiRoot } from "@/utils/api.js";

/**
 * Build query parameters for provider search
 */
export function buildProviderQueryParams(options) {
  const {
    searchText,
    userLocation,
    radius,
    filterOptions,
    userData
  } = options;

  const queryParams = new URLSearchParams();

  // Add search text if available
  if (searchText && searchText.trim() !== "") {
    queryParams.append("q", searchText.trim());
  }

  // Add location/radius
  const searchLat = options.searchLat || userLocation?.latitude;
  const searchLng = options.searchLng || userLocation?.longitude;

  if (searchText && searchText.trim() !== "") {
    queryParams.append("location", searchText.trim());
  }

  if (searchLat && searchLng) {
    queryParams.append("lat", searchLat);
    queryParams.append("lng", searchLng);
    const searchRadius = radius || 25;
    queryParams.append("radius", searchRadius);
  }

  // Add user profile filters
  if (filterOptions.matchesAge && userData.age) {
    queryParams.append("age", userData.age);
  }

  if (filterOptions.matchesDiagnosis && userData.diagnosis) {
    queryParams.append("diagnosis", userData.diagnosis);
  }

  // Add enum-based filters
  (filterOptions.diagnoses || []).forEach((d) =>
    queryParams.append("diagnosis", d)
  );
  (filterOptions.therapies || []).forEach((t) =>
    queryParams.append("therapy", t)
  );

  // Add insurance filter options
  if (filterOptions.acceptsInsurance) {
    queryParams.append("insurance", "insurance");
  }

  if (filterOptions.acceptsRegionalCenter) {
    queryParams.append("insurance", "regional center");
  }

  // Add specialization filter
  if (filterOptions.matchesDiagnosis && userData.diagnosis) {
    queryParams.append("specialization", userData.diagnosis);
  }

  return queryParams;
}

/**
 * Build provider API URL based on search type
 */
export function buildProviderUrl(searchText, queryParams) {
  const isZipSearch = searchText && /^\d{5}$/.test(searchText.trim());
  
  if (isZipSearch) {
    return `${getApiRoot()}/api/providers-v2/by_regional_center/?zip_code=${searchText.trim()}&${queryParams.toString()}`;
  }
  
  return `${getApiRoot()}/api/providers-v2/comprehensive_search/?${queryParams.toString()}`;
}

/**
 * Check if specific filters are active
 */
export function hasActiveFilters(filterOptions) {
  return (
    filterOptions.acceptsInsurance ||
    filterOptions.acceptsRegionalCenter ||
    filterOptions.acceptsPrivatePay ||
    filterOptions.matchesDiagnosis ||
    filterOptions.matchesAge
  );
}

/**
 * Validate provider data
 */
export function validateProvider(provider) {
  if (!provider) return false;
  
  // Check for invalid coordinates (common bug)
  const lat = parseFloat(provider.latitude);
  const lng = parseFloat(provider.longitude);
  
  if (isNaN(lat) || isNaN(lng)) {
    console.warn(`⚠️ Invalid coordinates for provider: ${provider.name}`, {
      latitude: provider.latitude,
      longitude: provider.longitude
    });
    return false;
  }
  
  // Check for zero coordinates (indicates missing data)
  if (lat === 0 && lng === 0) {
    console.warn(`⚠️ Zero coordinates for provider: ${provider.name}`);
    return false;
  }
  
  return true;
}

/**
 * Filter and validate providers
 */
export function filterValidProviders(providers) {
  return providers.filter(validateProvider);
}

/**
 * Check if providers are in LA County
 */
export function filterProvidersInLACounty(providers) {
  const laCountyBounds = {
    west: -118.7,
    east: -118.0,
    south: 33.7,
    north: 34.4
  };

  return providers.filter(provider => {
    const lng = parseFloat(provider.longitude);
    const lat = parseFloat(provider.latitude);
    return lng >= laCountyBounds.west && lng <= laCountyBounds.east &&
           lat >= laCountyBounds.south && lat <= laCountyBounds.north;
  });
}

/**
 * LA County bounds for fallback
 */
export const LA_COUNTY_MAP_BOUNDS = [
  [-118.7, 33.7], // Southwest
  [-118.0, 34.4]  // Northeast
];

