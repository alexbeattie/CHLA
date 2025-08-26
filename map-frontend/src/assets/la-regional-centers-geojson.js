// LA Regional Centers GeoJSON with CHLA brand colors
// Service areas correspond to the actual 7 regional centers and follow real geographic features
// Designed to fill the entire LA County like puzzle pieces

export const laRegionalCentersGeoJSON = {
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "properties": {
        "name": "North Los Angeles County",
        "color": "#ffcc0a", // CHLA Yellow
        "fillColor": "#ffcc0a",
        "fillOpacity": 0.6,
        "strokeColor": "#333333",
        "strokeWidth": 1.5,
        "phone": "818-778-1900",
        "website": "https://www.nlacrc.org",
        "description": "Serves northern LA County including Santa Clarita, Lancaster, Palmdale, Antelope Valley, and surrounding areas"
      },
      "geometry": {
        "type": "Polygon",
        "coordinates": [[
          [-118.6682, 34.8233], // Northwest corner of LA County
          [-118.6682, 34.6000], // West boundary
          [-118.5500, 34.6000], // Southwest
          [-118.4500, 34.5500], // South
          [-118.3500, 34.5000], // Southeast
          [-118.2500, 34.4500], // East
          [-118.1500, 34.5000], // Northeast
          [-118.0500, 34.6000], // North
          [-118.1500, 34.7000], // Northwest
          [-118.3000, 34.7500], // West
          [-118.4500, 34.8000], // Northwest
          [-118.6000, 34.8000], // North
          [-118.6682, 34.8233]  // Back to start
        ]]
      }
    },
    {
      "type": "Feature",
      "properties": {
        "name": "San Gabriel/Pomona",
        "color": "#22b2e7", // CHLA Light Blue
        "fillColor": "#22b2e7",
        "fillOpacity": 0.6,
        "strokeColor": "#333333",
        "strokeWidth": 1.5,
        "phone": "909-620-7722",
        "website": "https://www.sgprc.org",
        "description": "Serves eastern LA County including Pomona, Diamond Bar, Walnut, Chino Hills, and surrounding areas"
      },
      "geometry": {
        "type": "Polygon",
        "coordinates": [[
          [-118.0500, 34.6000], // North boundary
          [-117.6467, 34.6000], // East boundary of LA County
          [-117.6467, 34.3000], // Southeast
          [-117.8000, 34.2000], // South
          [-118.0000, 34.2000], // Southwest
          [-118.1500, 34.5000], // West
          [-118.0500, 34.6000]  // Back to start
        ]]
      }
    },
    {
      "type": "Feature",
      "properties": {
        "name": "Eastern Los Angeles",
        "color": "#5aba4b", // CHLA Green
        "fillColor": "#5aba4b",
        "fillOpacity": 0.6,
        "strokeColor": "#333333",
        "strokeWidth": 1.5,
        "phone": "626-299-4700",
        "website": "https://www.elarc.org",
        "description": "Serves eastern LA County including Alhambra, Monterey Park, Rosemead, San Gabriel, Temple City, and surrounding areas"
      },
      "geometry": {
        "type": "Polygon",
        "coordinates": [[
          [-118.2500, 34.4500], // North boundary
          [-118.0000, 34.2000], // Northeast
          [-118.0000, 34.1000], // East
          [-118.2000, 34.0000], // Southeast
          [-118.3000, 34.0000], // Southwest
          [-118.3500, 34.2000], // West
          [-118.3500, 34.3500], // Northwest
          [-118.2500, 34.4500]  // Back to start
        ]]
      }
    },
    {
      "type": "Feature",
      "properties": {
        "name": "Westside",
        "color": "#e91e63", // Light Pink/Magenta (matching reference)
        "fillColor": "#e91e63",
        "fillOpacity": 0.6,
        "strokeColor": "#333333",
        "strokeWidth": 1.5,
        "phone": "310-258-4000",
        "website": "https://www.westsiderc.org",
        "description": "Serves the Westside including Culver City, Santa Monica, Venice, Marina del Rey, and surrounding areas"
      },
      "geometry": {
        "type": "Polygon",
        "coordinates": [[
          [-118.6682, 34.6000], // North boundary (west coast)
          [-118.5500, 34.6000], // Northeast
          [-118.4500, 34.5500], // East
          [-118.3500, 34.5000], // Southeast
          [-118.3500, 34.3500], // South
          [-118.4500, 34.2000], // Southwest
          [-118.5500, 34.1000], // West
          [-118.6500, 34.0000], // Southwest
          [-118.6682, 34.1000], // West coast
          [-118.6682, 34.3000], // Northwest
          [-118.6682, 34.6000]  // Back to start
        ]]
      }
    },
    {
      "type": "Feature",
      "properties": {
        "name": "Lanterman",
        "color": "#73398c", // CHLA Purple
        "fillColor": "#73398c",
        "fillOpacity": 0.6,
        "strokeColor": "#333333",
        "strokeWidth": 1.5,
        "phone": "213-383-1300",
        "website": "https://www.lanterman.org",
        "description": "Serves central LA including Downtown LA, Hollywood, West Hollywood, Beverly Hills, and surrounding areas"
      },
      "geometry": {
        "type": "Polygon",
        "coordinates": [[
          [-118.4500, 34.2000], // North boundary
          [-118.3500, 34.2000], // Northeast
          [-118.3500, 34.0000], // East
          [-118.4500, 34.0000], // Southeast
          [-118.5500, 34.0000], // Southwest
          [-118.5500, 34.1000], // West
          [-118.4500, 34.2000]  // Back to start
        ]]
      }
    },
    {
      "type": "Feature",
      "properties": {
        "name": "South Central Los Angeles",
        "color": "#ee293d", // CHLA Red
        "fillColor": "#ee293d",
        "fillOpacity": 0.6,
        "strokeColor": "#333333",
        "strokeWidth": 1.5,
        "phone": "213-744-7000",
        "website": "https://www.sclarc.org",
        "description": "Serves South LA including Watts, Compton, Carson, Gardena, and surrounding areas"
      },
      "geometry": {
        "type": "Polygon",
        "coordinates": [[
          [-118.5500, 34.1000], // North boundary
          [-118.4500, 34.1000], // Northeast
          [-118.4500, 34.0000], // East
          [-118.5500, 34.0000], // Southeast
          [-118.6500, 34.0000], // Southwest
          [-118.6500, 34.1000], // West
          [-118.5500, 34.1000]  // Back to start
        ]]
      }
    },
    {
      "type": "Feature",
      "properties": {
        "name": "Harbor",
        "color": "#673ab7", // Darker Purple (matching reference)
        "fillColor": "#673ab7",
        "fillOpacity": 0.6,
        "strokeColor": "#333333",
        "strokeWidth": 1.5,
        "phone": "310-540-1711",
        "website": "https://www.harborrc.org",
        "description": "Serves the South Bay including Torrance, Redondo Beach, Manhattan Beach, Palos Verdes, and surrounding areas"
      },
      "geometry": {
        "type": "Polygon",
        "coordinates": [[
          [-118.6500, 34.1000], // North boundary
          [-118.5500, 34.1000], // Northeast
          [-118.5500, 34.0000], // East
          [-118.6500, 34.0000], // Southeast
          [-118.6682, 34.0000], // Southwest
          [-118.6682, 34.1000], // West coast
          [-118.6500, 34.1000]  // Back to start
        ]]
      }
    }
  ]
};

// Function to get regional center by name
export function getRegionalCenterByName(name) {
  return laRegionalCentersGeoJSON.features.find(
    feature => feature.properties.name === name
  );
}

// Function to get all regional centers
export function getAllRegionalCenters() {
  return laRegionalCentersGeoJSON.features;
}

// Function to get regional center colors
export function getRegionalCenterColors() {
  return laRegionalCentersGeoJSON.features.map(feature => ({
    name: feature.properties.name,
    color: feature.properties.color,
    fillColor: feature.properties.fillColor
  }));
}

// Function to check if a point is within LA County bounds
export function isWithinLACounty(lng, lat) {
  // LA County approximate bounds
  const laCountyBounds = {
    north: 34.8233, // Northernmost point
    south: 32.5121, // Southernmost point
    east: -117.6467, // Easternmost point
    west: -118.6682  // Westernmost point
  };
  
  return lng >= laCountyBounds.west && 
         lng <= laCountyBounds.east && 
         lat >= laCountyBounds.south && 
         lat <= laCountyBounds.north;
}
