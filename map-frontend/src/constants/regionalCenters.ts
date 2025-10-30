/**
 * Regional Center Constants for LA County
 * Consolidated from multiple locations in MapView.vue
 */

export interface RegionalCenterInfo {
  name: string;
  abbreviation: string;
  color: string;
  center: {
    lat: number;
    lng: number;
  };
}

/**
 * LA Regional Centers with colors and coordinates
 * Used for: map visualization, ZIP code coloring, distance calculations
 */
export const LA_REGIONAL_CENTERS: Record<string, RegionalCenterInfo> = {
  "North Los Angeles County Regional Center": {
    name: "North Los Angeles County Regional Center",
    abbreviation: "NLACRC",
    color: "#f1c40f", // Yellow
    center: { lat: 34.2523, lng: -118.4085 }, // Van Nuys
  },
  "San Gabriel/Pomona Regional Center": {
    name: "San Gabriel/Pomona Regional Center",
    abbreviation: "SGPRC",
    color: "#4caf50", // Green
    center: { lat: 34.0522, lng: -117.7499 }, // Pomona
  },
  "Eastern Los Angeles Regional Center": {
    name: "Eastern Los Angeles Regional Center",
    abbreviation: "ELARC",
    color: "#ff9800", // Orange
    center: { lat: 33.9425, lng: -118.0353 }, // Whittier
  },
  "Westside Regional Center": {
    name: "Westside Regional Center",
    abbreviation: "WRC",
    color: "#e91e63", // Pink
    center: { lat: 34.0239, lng: -118.3897 }, // Culver City
  },
  "Frank D. Lanterman Regional Center": {
    name: "Frank D. Lanterman Regional Center",
    abbreviation: "FDLRC",
    color: "#9c27b0", // Purple
    center: { lat: 34.0689, lng: -118.1228 }, // Alhambra
  },
  "South Central Los Angeles Regional Center": {
    name: "South Central Los Angeles Regional Center",
    abbreviation: "SCLARC",
    color: "#f44336", // Red
    center: { lat: 34.0522, lng: -118.2437 }, // Los Angeles
  },
  "Harbor Regional Center": {
    name: "Harbor Regional Center",
    abbreviation: "HRC",
    color: "#2196f3", // Blue
    center: { lat: 33.7905, lng: -118.2923 }, // Torrance
  },
};

/**
 * Get color for a regional center by name
 */
export function getRegionalCenterColor(name: string): string {
  return LA_REGIONAL_CENTERS[name]?.color || "#95a5a6";
}

/**
 * Get coordinates for a regional center by name
 */
export function getRegionalCenterCoordinates(name: string): { lat: number; lng: number } | null {
  return LA_REGIONAL_CENTERS[name]?.center || null;
}

/**
 * Get all regional center names
 */
export function getRegionalCenterNames(): string[] {
  return Object.keys(LA_REGIONAL_CENTERS);
}

/**
 * Get regional centers as an array
 * Returns array of { name, abbreviation, color } objects
 */
export function getRegionalCentersList(): Array<{name: string; abbreviation: string; color: string}> {
  return Object.values(LA_REGIONAL_CENTERS).map(rc => ({
    name: rc.name,
    abbreviation: rc.abbreviation,
    color: rc.color
  }));
}

/**
 * Get coordinates in [lng, lat] format for Mapbox
 */
export function getRegionalCenterMapboxCoords(name: string): [number, number] | null {
  const center = LA_REGIONAL_CENTERS[name]?.center;
  return center ? [center.lng, center.lat] : null;
}

/**
 * LA County geographic bounds [west, south, east, north]
 */
export const LA_COUNTY_BOUNDS: [[number, number], [number, number]] = [
  [-118.9448, 32.7941], // Southwest corner
  [-117.6462, 34.8233], // Northeast corner
];

/**
 * LA County center point
 */
export const LA_COUNTY_CENTER = {
  lat: 34.0522,
  lng: -118.2437,
};

/**
 * Default map viewport for LA County
 */
export const DEFAULT_VIEWPORT = {
  latitude: LA_COUNTY_CENTER.lat,
  longitude: LA_COUNTY_CENTER.lng,
  zoom: 9,
};
