/**
 * useGeolocation Composable
 * Handles user location detection, geocoding, and reverse geocoding
 * Extracted from MapView.vue to reduce complexity
 */

import { ref, computed } from 'vue';
import axios from 'axios';
import { Coordinates, validateCoordinates, extractZipCode } from '@/utils/map/coordinates';

export interface UserLocation {
  latitude: number | null;
  longitude: number | null;
  accuracy: number | null;
  detected: boolean;
  error: string | null;
  address?: string;
  zipCode?: string;
}

export function useGeolocation() {
  // State
  const userLocation = ref<UserLocation>({
    latitude: null,
    longitude: null,
    accuracy: null,
    detected: false,
    error: null,
  });

  const isDetecting = ref(false);

  // Computed
  const hasLocation = computed(() =>
    userLocation.value.latitude !== null && userLocation.value.longitude !== null
  );

  const coordinates = computed<Coordinates | null>(() => {
    if (!hasLocation.value) return null;
    return {
      lat: userLocation.value.latitude!,
      lng: userLocation.value.longitude!,
    };
  });

  /**
   * Get user's current position using browser geolocation API
   */
  async function detectUserLocation(): Promise<boolean> {
    isDetecting.value = true;
    userLocation.value.error = null;

    try {
      const position = await getCurrentPosition();

      userLocation.value = {
        latitude: position.coords.latitude,
        longitude: position.coords.longitude,
        accuracy: position.coords.accuracy,
        detected: true,
        error: null,
      };

      // Try to get address for the location
      try {
        const address = await reverseGeocode(position.coords.latitude, position.coords.longitude);
        userLocation.value.address = address;

        const zip = extractZipCode(address);
        if (zip) {
          userLocation.value.zipCode = zip;
        }
      } catch (geocodeError) {
        console.warn('Reverse geocoding failed:', geocodeError);
        // Not critical, continue without address
      }

      return true;
    } catch (error: any) {
      userLocation.value.error = error.message || 'Unable to detect location';
      userLocation.value.detected = false;
      console.error('Location detection failed:', error);
      return false;
    } finally {
      isDetecting.value = false;
    }
  }

  /**
   * Get current position from browser
   */
  function getCurrentPosition(): Promise<GeolocationPosition> {
    return new Promise((resolve, reject) => {
      if (!navigator.geolocation) {
        reject(new Error('Geolocation not supported'));
        return;
      }

      navigator.geolocation.getCurrentPosition(resolve, reject, {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 300000,
      });
    });
  }

  /**
   * Reverse geocode coordinates to address
   * Uses Nominatim (OpenStreetMap) as primary, with fallback
   */
  async function reverseGeocode(lat: number, lng: number): Promise<string> {
    try {
      // Validate coordinates first
      const coords = validateCoordinates(lat, lng);
      if (!coords) {
        throw new Error('Invalid coordinates');
      }

      // Use Nominatim for reverse geocoding
      const response = await axios.get('https://nominatim.openstreetmap.org/reverse', {
        params: {
          lat: coords.lat,
          lon: coords.lng,
          format: 'json',
        },
        headers: {
          'User-Agent': 'CHLA-Provider-Map/1.0',
        },
      });

      if (response.data && response.data.display_name) {
        return response.data.display_name;
      }

      // Fallback to simple coordinate display
      return `${coords.lat.toFixed(4)}, ${coords.lng.toFixed(4)}`;
    } catch (error) {
      console.error('Reverse geocoding failed:', error);
      return `${lat.toFixed(4)}, ${lng.toFixed(4)}`;
    }
  }

  /**
   * Geocode text address to coordinates
   * Supports: ZIP codes, addresses, city names
   */
  async function geocodeTextToCoords(searchText: string): Promise<Coordinates | null> {
    try {
      const trimmed = searchText.trim();
      if (!trimmed) return null;

      // Check if it's a ZIP code
      const zipCode = extractZipCode(trimmed);
      if (zipCode) {
        return await geocodeZipCode(zipCode);
      }

      // Try geocoding as address
      const response = await axios.get('https://nominatim.openstreetmap.org/search', {
        params: {
          q: trimmed,
          format: 'json',
          limit: 1,
          countrycodes: 'us',
        },
        headers: {
          'User-Agent': 'CHLA-Provider-Map/1.0',
        },
      });

      if (response.data && response.data.length > 0) {
        const result = response.data[0];
        const coords = validateCoordinates(result.lat, result.lon);
        return coords;
      }

      return null;
    } catch (error) {
      console.error('Geocoding failed:', error);
      return null;
    }
  }

  /**
   * Geocode ZIP code to coordinates
   * ZIP codes in US are well-defined
   */
  async function geocodeZipCode(zipCode: string): Promise<Coordinates | null> {
    try {
      const response = await axios.get('https://nominatim.openstreetmap.org/search', {
        params: {
          postalcode: zipCode,
          country: 'United States',
          format: 'json',
          limit: 1,
        },
        headers: {
          'User-Agent': 'CHLA-Provider-Map/1.0',
        },
      });

      if (response.data && response.data.length > 0) {
        const result = response.data[0];
        const coords = validateCoordinates(result.lat, result.lon);
        return coords;
      }

      return null;
    } catch (error) {
      console.error('ZIP code geocoding failed:', error);
      return null;
    }
  }

  /**
   * Set user location manually
   */
  function setUserLocation(lat: number, lng: number, address?: string) {
    const coords = validateCoordinates(lat, lng);
    if (!coords) {
      console.error('Invalid coordinates provided');
      return false;
    }

    userLocation.value = {
      latitude: coords.lat,
      longitude: coords.lng,
      accuracy: null,
      detected: false,
      error: null,
      address,
    };

    // Extract ZIP if address provided
    if (address) {
      const zip = extractZipCode(address);
      if (zip) {
        userLocation.value.zipCode = zip;
      }
    }

    return true;
  }

  /**
   * Clear user location
   */
  function clearLocation() {
    userLocation.value = {
      latitude: null,
      longitude: null,
      accuracy: null,
      detected: false,
      error: null,
    };
  }

  /**
   * Set fallback location (e.g., LA County center)
   */
  function setFallbackLocation(lat: number, lng: number, reason?: string) {
    console.log(`Setting fallback location: ${reason || 'No reason provided'}`);
    setUserLocation(lat, lng, 'Los Angeles, CA');
  }

  return {
    // State
    userLocation,
    isDetecting,

    // Computed
    hasLocation,
    coordinates,

    // Methods
    detectUserLocation,
    reverseGeocode,
    geocodeTextToCoords,
    geocodeZipCode,
    setUserLocation,
    clearLocation,
    setFallbackLocation,
  };
}
