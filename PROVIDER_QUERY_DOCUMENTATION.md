# Provider Query Documentation

This document explains how to query provider data from the Django backend in the map application.

## Backend API Endpoints for Providers

The Django backend exposes several endpoints for querying provider data:

### Main Provider Endpoints

1. **Standard Provider Listing**: `/api/providers/`
   - Returns a basic list of all providers
   - Supports standard filtering via query parameters

2. **Provider By Area**: `/api/providers/by_area/`
   - Finds providers serving a specific area
   - Required parameter: `area` - The area name to search for

3. **Nearby Providers**: `/api/providers/nearby/`
   - Finds providers near a specific location
   - Required parameters: 
     - `lat`: Latitude
     - `lng`: Longitude
   - Optional parameters:
     - `radius`: Search radius in miles (default: 25)
     - `age`: Age of client
     - `diagnosis`: Diagnosis type
     - `funding`: Funding source name
     - `insurance`: Insurance carrier name
     - `service_model`: Service delivery model name

4. **Comprehensive Search**: `/api/providers/comprehensive_search/`
   - The most feature-rich search endpoint
   - Parameters:
     - `q`: Text search query
     - `lat`/`lng`: User's location
     - `radius`: Search radius in miles
     - `zip`: ZIP code to search near
     - `age`: Client age
     - `diagnosis`: Diagnosis (e.g., 'autism')
     - `funding_source`: ID or name of funding source
     - `format`: Response format (e.g., 'geojson')

### Other Specialized Provider Queries

- By Funding: `/api/providers/by_funding/`
- By Diagnosis: `/api/providers/by_diagnosis/`
- By Age Group: `/api/providers/by_age_group/`
- Available Filters: `/api/providers/filters/`

## Frontend Implementation

In the Vue frontend:

1. **Base URL Configuration**:
   ```javascript
   const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || "http://localhost:8000/api";
   ```

2. **Best Approach for Querying Providers**:
   - Start with the comprehensive search endpoint
   - Fall back to nearby search if comprehensive search fails
   - Fall back to standard provider listing as a last resort
   
   ```javascript
   try {
     // First try with the comprehensive search endpoint
     url = `${apiBaseUrl}/providers/comprehensive_search/?${queryParams.toString()}`;
     response = await axios.get(url);
   } catch (apiError) {
     // Fall back to a regular nearby search
     url = `${apiBaseUrl}/providers/nearby/?${queryParams.toString()}`;
     response = await axios.get(url);
   } catch (fallbackError) {
     // Final fallback to just list all providers
     url = `${apiBaseUrl}/providers/`;
     response = await axios.get(url);
   }
   ```

3. **Common Query Parameters**:
   - Text search: `q`
   - Location-based: `lat`, `lng`, `radius`
   - User profile: `age`, `diagnosis`
   - Funding filters: `insurance`, `funding_source` 
   - Format: `format=geojson` (for map display)

4. **Response Handling**:
   - GeoJSON responses need to be converted to provider objects
   - Regular JSON arrays can be used directly
   - Paginated responses need to extract the `.results` array

## Implementation Tips

1. Always include error handling and fallbacks when querying providers
2. Consider providing sample data for a better user experience when API calls fail
3. When displaying on a map, request GeoJSON format for better integration
4. Use the comprehensive search endpoint when you need to filter by multiple criteria
5. For simpler queries or specialized needs, use the more specific endpoints

## Example Query Parameters

```javascript
// Build query parameters
let queryParams = new URLSearchParams();

// Add search text if available
if (searchText && searchText.trim() !== "") {
  queryParams.append("q", searchText.trim());
}

// Add user location if available
if (userLocation.latitude && userLocation.longitude) {
  queryParams.append("lat", userLocation.latitude);
  queryParams.append("lng", userLocation.longitude);
  queryParams.append("radius", radius);
}

// Add user profile data if available
if (userData.age) {
  queryParams.append("age", userData.age);
}

if (userData.diagnosis) {
  queryParams.append("diagnosis", userData.diagnosis);
}

// Add filter options
if (acceptsInsurance) {
  queryParams.append("insurance", "any");
}

if (acceptsRegionalCenter) {
  queryParams.append("funding_source", "Regional Center");
}

// Request GeoJSON format if we're displaying on map
queryParams.append("format", "geojson");
```

## Current Implementation Status

The implementation in MapView.vue currently has a fallback system in place that tries:

1. First the comprehensive search endpoint
2. Then the nearby endpoint
3. Finally falls back to the standard endpoint

The approach ensures the application continues to work even if specific endpoints are unavailable or return errors.

## Troubleshooting MapView.vue

If you're experiencing issues with the MapView.vue file:

1. Ensure all methods are properly closed with appropriate curly braces
2. Make sure the script tag has a closing `</script>` tag
3. Add the style section with proper closing `</style>` tag
4. Complete any truncated methods like `selectLocation` that might be missing parts

Example of completing a properly formatted file:

```javascript
// Select a location
selectLocation(location) {
  this.selectedLocation = location;

  // Center map on selected location
  if (location.latitude && location.longitude) {
    this.map.flyTo({
      center: [location.longitude, location.latitude],
      zoom: 14,
    });
  }
},

// Close location details
closeLocationDetails() {
  this.selectedLocation = null;
}
```
