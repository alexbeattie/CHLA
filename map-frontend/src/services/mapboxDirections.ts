/**
 * Mapbox Directions API Service
 * Handles driving directions, distance calculations, and route geometry
 */

const MAPBOX_TOKEN = import.meta.env.VITE_MAPBOX_TOKEN;
const DIRECTIONS_API_BASE = 'https://api.mapbox.com/directions/v5/mapbox/driving';

export interface DirectionsStep {
  distance: number; // meters
  duration: number; // seconds
  instruction: string;
  maneuver: {
    type: string;
    modifier?: string;
    bearing_before?: number;
    bearing_after?: number;
    location: [number, number]; // [lng, lat]
  };
  name?: string;
}

export interface DirectionsLeg {
  distance: number; // meters
  duration: number; // seconds
  steps: DirectionsStep[];
  summary: string;
}

export interface DirectionsRoute {
  distance: number; // meters
  duration: number; // seconds
  geometry: {
    coordinates: [number, number][]; // [lng, lat][]
    type: string;
  };
  legs: DirectionsLeg[];
}

export interface DirectionsResponse {
  routes: DirectionsRoute[];
  waypoints: Array<{
    name: string;
    location: [number, number];
  }>;
  code: string;
}

export interface DrivingDirections {
  distance: number; // miles
  duration: number; // minutes
  route: DirectionsRoute;
  steps: DirectionsStep[];
}

/**
 * Get driving directions between two points
 * @param origin [lng, lat] origin coordinates
 * @param destination [lng, lat] destination coordinates
 * @returns Driving directions with route and steps
 */
export async function getDrivingDirections(
  origin: [number, number],
  destination: [number, number]
): Promise<DrivingDirections> {
  const coordinates = `${origin[0]},${origin[1]};${destination[0]},${destination[1]}`;

  const params = new URLSearchParams({
    access_token: MAPBOX_TOKEN,
    geometries: 'geojson',
    steps: 'true',
    banner_instructions: 'true',
    voice_instructions: 'true',
    overview: 'full'
  });

  const url = `${DIRECTIONS_API_BASE}/${coordinates}?${params.toString()}`;

  try {
    const response = await fetch(url);

    if (!response.ok) {
      throw new Error(`Mapbox Directions API error: ${response.statusText}`);
    }

    const data: DirectionsResponse = await response.json();

    if (!data.routes || data.routes.length === 0) {
      throw new Error('No route found');
    }

    const route = data.routes[0];
    const steps = route.legs[0]?.steps || [];

    return {
      distance: metersToMiles(route.distance),
      duration: secondsToMinutes(route.duration),
      route,
      steps
    };
  } catch (error) {
    console.error('Error fetching driving directions:', error);
    throw error;
  }
}

/**
 * Get driving distance only (faster, no route geometry)
 * @param origin [lng, lat] origin coordinates
 * @param destination [lng, lat] destination coordinates
 * @returns Distance in miles
 */
export async function getDrivingDistance(
  origin: [number, number],
  destination: [number, number]
): Promise<number> {
  const coordinates = `${origin[0]},${origin[1]};${destination[0]},${destination[1]}`;

  const params = new URLSearchParams({
    access_token: MAPBOX_TOKEN,
    geometries: 'geojson',
    overview: 'false' // Don't need full geometry for distance only
  });

  const url = `${DIRECTIONS_API_BASE}/${coordinates}?${params.toString()}`;

  try {
    const response = await fetch(url);

    if (!response.ok) {
      throw new Error(`Mapbox Directions API error: ${response.statusText}`);
    }

    const data: DirectionsResponse = await response.json();

    if (!data.routes || data.routes.length === 0) {
      throw new Error('No route found');
    }

    return metersToMiles(data.routes[0].distance);
  } catch (error) {
    console.error('Error fetching driving distance:', error);
    throw error;
  }
}

/**
 * Format step instruction for display
 * @param step Direction step
 * @returns Formatted instruction string
 */
export function formatStepInstruction(step: DirectionsStep): string {
  const distance = metersToFeet(step.distance);
  const distanceStr = distance < 528 // 0.1 miles in feet
    ? `${Math.round(distance)} ft`
    : `${(distance / 5280).toFixed(1)} mi`;

  return `${step.instruction} (${distanceStr})`;
}

/**
 * Get icon class for maneuver type
 * @param maneuver Maneuver object
 * @returns Bootstrap icon class
 */
export function getManeuverIcon(maneuver: DirectionsStep['maneuver']): string {
  const type = maneuver.type;
  const modifier = maneuver.modifier;

  // Turn types
  if (type === 'turn') {
    if (modifier === 'left') return 'bi-arrow-left';
    if (modifier === 'right') return 'bi-arrow-right';
    if (modifier === 'slight left') return 'bi-arrow-up-left';
    if (modifier === 'slight right') return 'bi-arrow-up-right';
    if (modifier === 'sharp left') return 'bi-arrow-bar-left';
    if (modifier === 'sharp right') return 'bi-arrow-bar-right';
  }

  // Other maneuver types
  if (type === 'depart') return 'bi-record-circle';
  if (type === 'arrive') return 'bi-geo-alt-fill';
  if (type === 'merge') return 'bi-signpost-split';
  if (type === 'on ramp') return 'bi-arrow-up-right-circle';
  if (type === 'off ramp') return 'bi-arrow-down-right-circle';
  if (type === 'fork') return 'bi-signpost-split';
  if (type === 'roundabout') return 'bi-arrow-clockwise';
  if (type === 'rotary') return 'bi-arrow-counterclockwise';
  if (type === 'continue') return 'bi-arrow-up';

  return 'bi-arrow-up'; // default
}

/**
 * Convert meters to miles
 */
function metersToMiles(meters: number): number {
  return meters * 0.000621371;
}

/**
 * Convert meters to feet
 */
function metersToFeet(meters: number): number {
  return meters * 3.28084;
}

/**
 * Convert seconds to minutes
 */
function secondsToMinutes(seconds: number): number {
  return seconds / 60;
}

/**
 * Format duration for display
 * @param minutes Duration in minutes
 * @returns Formatted string like "15 min" or "1 hr 30 min"
 */
export function formatDuration(minutes: number): string {
  if (minutes < 60) {
    return `${Math.round(minutes)} min`;
  }

  const hours = Math.floor(minutes / 60);
  const mins = Math.round(minutes % 60);

  if (mins === 0) {
    return `${hours} hr`;
  }

  return `${hours} hr ${mins} min`;
}

/**
 * Format distance for display
 * @param miles Distance in miles
 * @returns Formatted string like "2.5 mi"
 */
export function formatDistance(miles: number): string {
  if (miles < 0.1) {
    return 'Less than 0.1 mi';
  }
  return `${miles.toFixed(1)} mi`;
}
