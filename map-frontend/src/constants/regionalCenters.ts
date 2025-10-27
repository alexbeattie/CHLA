/**
 * Regional Center Constants for LA County
 * Consolidated from multiple locations in MapView.vue
 */

export interface RegionalCenterInfo {
  name: string;
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
    color: "#f1c40f",
    center: { lat: 34.2523, lng: -118.4085 },
  },
  "South Central Los Angeles Regional Center": {
    name: "South Central Los Angeles Regional Center",
    color: "#3498db",
    center: { lat: 33.9416, lng: -118.2085 },
  },
  "Eastern Los Angeles Regional Center": {
    name: "Eastern Los Angeles Regional Center",
    color: "#e74c3c",
    center: { lat: 34.0658, lng: -118.0967 },
  },
  "Westside Regional Center": {
    name: "Westside Regional Center",
    color: "#9b59b6",
    center: { lat: 34.0195, lng: -118.4912 },
  },
  "Harbor Regional Center": {
    name: "Harbor Regional Center",
    color: "#1abc9c",
    center: { lat: 33.8303, lng: -118.2923 },
  },
  "San Gabriel/Pomona Regional Center": {
    name: "San Gabriel/Pomona Regional Center",
    color: "#e67e22",
    center: { lat: 34.0555, lng: -117.9001 },
  },
  "Frank D. Lanterman Regional Center": {
    name: "Frank D. Lanterman Regional Center",
    color: "#2ecc71",
    center: { lat: 34.1478, lng: -118.1445 },
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
