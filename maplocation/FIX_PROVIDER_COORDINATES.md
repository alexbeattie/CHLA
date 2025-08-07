# Fixing Provider Coordinates

The issue with markers not appearing is likely because the providers in the database don't have latitude/longitude coordinates set.

## Quick Fix Steps:

1. **Check current status** (from the maplocation directory):
   ```bash
   python3 check_providers.py
   ```
   This will show you how many providers have coordinates and list San Diego providers.

2. **Geocode missing coordinates**:
   ```bash
   python3 geocode_providers_script.py
   ```
   This will automatically geocode any providers that have addresses but no coordinates.

3. **Alternative: Manual fix via Django admin**:
   - Go to http://localhost:8000/admin
   - Navigate to Providers
   - Edit San Diego providers and add coordinates manually
   - Example San Diego coordinates: Latitude: 32.7157, Longitude: -117.1611

## Frontend Improvements Added:

1. The Vue app now automatically detects providers missing coordinates
2. It will try to geocode them on the frontend using Mapbox
3. Better error messages when providers lack location data
4. Support for both direct lat/lng fields and PostGIS location objects

## Test the Fix:

After running the geocoding script, refresh your browser and search for "san diego" again. You should now see markers for all providers that have valid addresses.

## Database Note:

The Provider model automatically creates a PostGIS Point in the `location` field when you save latitude/longitude values. This happens in the model's save() method.