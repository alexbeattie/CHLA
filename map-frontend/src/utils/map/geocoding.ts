/**
 * Geocoding utilities for address and ZIP code lookups
 * Extracted from MapView.vue as part of Week 1 refactoring
 */

import type { Coordinates } from './coordinates';

export interface GeocodeResult extends Coordinates {
  formatted_address?: string;
  zip_code?: string;
  city?: string;
  state?: string;
}

/**
 * Check if a string is a valid US ZIP code (5 digits)
 * @param value String to check
 * @returns true if valid ZIP code format
 */
export function isValidZipCode(value: string): boolean {
  return /^\d{5}$/.test(value.trim());
}

/**
 * Check if a string is a valid ZIP+4 code (5 digits + dash + 4 digits)
 * @param value String to check
 * @returns true if valid ZIP+4 format
 */
export function isValidZipPlus4(value: string): boolean {
  return /^\d{5}-\d{4}$/.test(value.trim());
}

/**
 * Extract ZIP code from various formats
 * Handles "12345", "12345-6789", "City, CA 12345", etc.
 * @param value String potentially containing a ZIP code
 * @returns ZIP code or null if not found
 */
export function extractZipCode(value: string): string | null {
  // Try to find 5-digit ZIP code
  const match = value.match(/\b\d{5}\b/);
  return match ? match[0] : null;
}

/**
 * Geocode an address using Mapbox Geocoding API
 * @param address Address or place name to geocode
 * @param mapboxToken Mapbox access token
 * @param options Additional options
 * @returns Geocode result or null if failed
 */
export async function geocodeAddress(
  address: string,
  mapboxToken: string,
  options: {
    proximity?: Coordinates; // Bias results near this location
    bbox?: [number, number, number, number]; // Bounding box [minLng, minLat, maxLng, maxLat]
    types?: string[]; // Filter by feature types (e.g., ['postcode', 'place'])
  } = {}
): Promise<GeocodeResult | null> {
  try {
    const encodedAddress = encodeURIComponent(address);
    let url = `https://api.mapbox.com/geocoding/v5/mapbox.places/${encodedAddress}.json?access_token=${mapboxToken}`;

    // Add proximity bias if provided
    if (options.proximity) {
      url += `&proximity=${options.proximity.lng},${options.proximity.lat}`;
    }

    // Add bounding box if provided
    if (options.bbox) {
      url += `&bbox=${options.bbox.join(',')}`;
    }

    // Add type filters if provided
    if (options.types && options.types.length > 0) {
      url += `&types=${options.types.join(',')}`;
    }

    const response = await fetch(url);
    const data = await response.json();

    if (!data.features || data.features.length === 0) {
      return null;
    }

    const feature = data.features[0];
    const [lng, lat] = feature.center;

    const result: GeocodeResult = {
      lat,
      lng,
      formatted_address: feature.place_name
    };

    // Extract additional context information
    if (feature.context) {
      for (const ctx of feature.context) {
        if (ctx.id.startsWith('postcode')) {
          result.zip_code = ctx.text;
        } else if (ctx.id.startsWith('place')) {
          result.city = ctx.text;
        } else if (ctx.id.startsWith('region')) {
          result.state = ctx.short_code?.replace('US-', '');
        }
      }
    }

    return result;
  } catch (error) {
    console.error('Geocoding error:', error);
    return null;
  }
}

/**
 * Geocode a ZIP code specifically using Mapbox
 * @param zipCode 5-digit ZIP code
 * @param mapboxToken Mapbox access token
 * @returns Geocode result or null if failed
 */
export async function geocodeZipCode(
  zipCode: string,
  mapboxToken: string
): Promise<GeocodeResult | null> {
  if (!isValidZipCode(zipCode)) {
    console.warn('Invalid ZIP code format:', zipCode);
    return null;
  }

  return geocodeAddress(zipCode, mapboxToken, {
    types: ['postcode'],
    // Bias to California
    proximity: { lat: 34.0522, lng: -118.2437 }
  });
}

/**
 * Geocode using Nominatim (OpenStreetMap) as fallback
 * Free but rate-limited - use as backup when Mapbox fails
 * @param query Address or ZIP code
 * @returns Geocode result or null if failed
 */
export async function geocodeWithNominatim(query: string): Promise<GeocodeResult | null> {
  try {
    const encodedQuery = encodeURIComponent(query);
    const url = `https://nominatim.openstreetmap.org/search?format=json&q=${encodedQuery}&limit=1`;

    const response = await fetch(url, {
      headers: {
        'User-Agent': 'KINDD-ABA-Provider-Map/1.0' // Required by Nominatim
      }
    });

    const data = await response.json();

    if (!data || data.length === 0) {
      return null;
    }

    const result = data[0];

    return {
      lat: parseFloat(result.lat),
      lng: parseFloat(result.lon),
      formatted_address: result.display_name
    };
  } catch (error) {
    console.error('Nominatim geocoding error:', error);
    return null;
  }
}

/**
 * Reverse geocode coordinates to an address
 * @param lat Latitude
 * @param lng Longitude
 * @param mapboxToken Mapbox access token
 * @returns Geocode result with address or null if failed
 */
export async function reverseGeocode(
  lat: number,
  lng: number,
  mapboxToken: string
): Promise<GeocodeResult | null> {
  try {
    const url = `https://api.mapbox.com/geocoding/v5/mapbox.places/${lng},${lat}.json?access_token=${mapboxToken}`;

    const response = await fetch(url);
    const data = await response.json();

    if (!data.features || data.features.length === 0) {
      return null;
    }

    const feature = data.features[0];

    const result: GeocodeResult = {
      lat,
      lng,
      formatted_address: feature.place_name
    };

    // Extract ZIP code and other details
    if (feature.context) {
      for (const ctx of feature.context) {
        if (ctx.id.startsWith('postcode')) {
          result.zip_code = ctx.text;
        } else if (ctx.id.startsWith('place')) {
          result.city = ctx.text;
        } else if (ctx.id.startsWith('region')) {
          result.state = ctx.short_code?.replace('US-', '');
        }
      }
    }

    return result;
  } catch (error) {
    console.error('Reverse geocoding error:', error);
    return null;
  }
}

/**
 * Geocode with automatic fallback strategy
 * Tries Mapbox first, falls back to Nominatim if that fails
 * @param query Address or ZIP code
 * @param mapboxToken Mapbox access token (optional)
 * @returns Geocode result or null if all methods fail
 */
export async function geocodeWithFallback(
  query: string,
  mapboxToken?: string
): Promise<GeocodeResult | null> {
  // Try Mapbox if token provided
  if (mapboxToken) {
    const isZip = isValidZipCode(query);
    const mapboxResult = isZip
      ? await geocodeZipCode(query, mapboxToken)
      : await geocodeAddress(query, mapboxToken);

    if (mapboxResult) {
      return mapboxResult;
    }
  }

  // Fallback to Nominatim
  console.log('Mapbox geocoding failed, trying Nominatim...');
  return geocodeWithNominatim(query);
}
