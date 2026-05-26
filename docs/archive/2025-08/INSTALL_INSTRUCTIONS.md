# Provider Location Search Implementation

This implementation enhances the provider database with spatial capabilities, funding sources, insurance carriers, and service delivery models. It enables powerful location-based searches and filtering by various criteria.

## Installation Instructions

Follow these steps to apply the changes to your project:

### 1. Install Required Packages

First, install the new dependencies:

```bash
pip install -r requirements.txt
```

### 2. Install PostGIS

If you haven't already, you need to install PostGIS on your PostgreSQL database server:

#### For macOS with Homebrew:

```bash
brew install postgis
```

#### For Ubuntu/Debian:

```bash
sudo apt-get install postgis postgresql-[version]-postgis-[version]
```

### 3. Apply Database Migrations

Run the migration to create the new tables and add the necessary fields:

```bash
cd maplocation
python manage.py migrate
```

## Using the New Features

### API Endpoints

The implementation adds several new endpoints:

1. **Reference Data Endpoints**:
   - `/api/funding-sources/` - List all funding sources
   - `/api/insurance-carriers/` - List all insurance carriers
   - `/api/service-models/` - List all service delivery models

2. **Enhanced Provider Endpoints**:
   - `/api/providers/filters/` - Get available filter options for providers
   - `/api/providers/nearby/?lat=34.05&lng=-118.24&radius=10` - Find providers near a point
   - `/api/providers/by_area/?area=LOS+ANGELES` - Find providers by coverage area
   - `/api/providers/by_funding/?source=insurance&insurance=Anthem` - Find by funding source
   - `/api/providers/by_diagnosis/?diagnosis=autism` - Find by diagnosis
   - `/api/providers/by_age_group/?age=5` - Find by age group
   - `/api/providers/comprehensive_search/?q=autism&lat=34.05&lng=-118.24&radius=10&age=5` - Advanced search

### Sample API Requests

#### Find providers near a location that accept a specific insurance:

```
GET /api/providers/nearby/?lat=34.0522&lng=-118.2437&radius=10&insurance=Anthem+Blue+Cross
```

#### Find providers that accept Regional Center funding for autism:

```
GET /api/providers/comprehensive_search/?funding_source=Regional+Center&diagnosis=autism
```

#### Get providers that support center-based services for children ages 3-5:

```
GET /api/providers/comprehensive_search/?service_model=Center-Based&age=4
```

## Admin Interface

To manage the new data models, you can use the Django admin interface.

### Data Seeding

To populate the reference data tables (funding sources, insurance carriers, service delivery models), the migration includes initial data. However, you may want to populate more data through the admin interface.

## Frontend Integration

For MapBox integration with the new spatial data:

1. Use the `/api/providers/nearby/` endpoint with `format=geojson` parameter to get GeoJSON directly:

```
GET /api/providers/nearby/?lat=34.0522&lng=-118.2437&radius=10&format=geojson
```

2. This can be directly loaded into a MapBox map layer:

```javascript
map.addSource('providers', {
  type: 'geojson',
  data: 'http://localhost:8000/api/providers/nearby/?lat=34.0522&lng=-118.2437&radius=10&format=geojson'
});

map.addLayer({
  id: 'providers',
  type: 'circle',
  source: 'providers',
  paint: {
    'circle-radius': 6,
    'circle-color': '#FF5733'
  }
});
```

## Testing

Test the implementation with:

```bash
python manage.py test locations
```

## Common Issues

- If you encounter errors about PostGIS not being installed, verify the extension is properly installed in your PostgreSQL database.
- For "location field not found" errors, ensure you've migrated the database correctly.
- If GeoDjango features don't work, check that `django.contrib.gis` is in INSTALLED_APPS and the database ENGINE is set to 'django.contrib.gis.db.backends.postgis'.
