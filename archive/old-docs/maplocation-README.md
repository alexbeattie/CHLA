# Map Location Finder

A Django application with Vue.js frontend that allows users to search for nearby locations on a map, with filtering capabilities based on various criteria and user information collection.

## Features

- Interactive map-based search
- User geolocation detection
- Location filtering by:
  - Category
  - Price level
  - Amenities (parking, accessibility)
  - Distance
  - Search text
- Detailed location information
- User information collection (age, address, diagnosis)
- Address geocoding to coordinates
- Mobile-responsive design
- REST API and GraphQL interfaces

## Technology Stack

- **Backend**: Django with Django REST Framework
- **Frontend**: Vue.js 3 (CDN version)
- **Map API**: Mapbox GL JS
- **Geocoding**: Mapbox Geocoding API
- **CSS Framework**: Bootstrap 5
- **Database**: SQLite (default) or PostgreSQL
- **API**: REST and GraphQL

## Setup Instructions

1. Clone this repository:
```bash
git clone <repository-url>
cd maplocation
```

2. Create and activate a virtual environment:
```bash
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
```

3. Install dependencies:
```bash
pip install -r requirements.txt
```

4. Choose your database:

   **Option A: SQLite (default, no additional setup):**
   ```bash
   python manage.py migrate
   ```

   **Option B: PostgreSQL:**
   ```bash
   # Run the setup script and follow the prompts
   ./setup_postgres.sh
   ```

5. Create a superuser (admin):
```bash
python manage.py createsuperuser
```

6. Run the development server:
```bash
./run_server.sh
```

7. Access the application at http://127.0.0.1:8000/ and the admin interface at http://127.0.0.1:8000/admin/

8. For more information on PostgreSQL and GraphQL, see [README-POSTGRES-GRAPHQL.md](README-POSTGRES-GRAPHQL.md)

## API Endpoints

### Location API
- `GET /api/categories/` - List of location categories
- `GET /api/locations/` - List of all locations
- `GET /api/locations/{id}/` - Details for specific location
- `GET /api/locations/nearby/?lat={lat}&lng={lng}&radius={radius}` - Locations near a specific point
- `GET /api/locations/filters/` - Available filter options
- `GET /api/images/` - Location images
- `GET /api/reviews/` - Location reviews
- `POST /api/reviews/` - Submit a new review

### User Profile API
- `GET /api/users/profiles/me/` - Get current user's profile
- `POST /api/users/profiles/update_me/` - Update user's profile
  - Supported fields: age, address, city, state, zip_code, diagnosis, other_diagnosis, latitude, longitude

### GraphQL API
- `POST /graphql/` - GraphQL endpoint (also accessible via browser at `/graphql/`)
  - Supports both queries and mutations
  - See [README-POSTGRES-GRAPHQL.md](README-POSTGRES-GRAPHQL.md) for examples

## Project Structure

- `maplocation/` - Main Django project settings
  - `settings.py` - Project settings including database configuration
  - `urls.py` - URL patterns
  - `schema.py` - GraphQL schema
- `locations/` - Django app for location data
  - `models.py` - Location-related database models
  - `views.py` - API views and endpoints
  - `serializers.py` - JSON serializers
  - `admin.py` - Admin interface configuration
  - `schema.py` - GraphQL types for locations
- `users/` - Django app for user profiles
  - `models.py` - User profile model
  - `views.py` - User API views
  - `serializers.py` - User profile serializers
  - `schema.py` - GraphQL types for user profiles
- `templates/vue_app/` - Vue.js frontend templates
  - `vue_mapbox_raw.html` - Main Vue.js app with Mapbox integration
  - Additional templates for different map implementations
- `static/css/` - CSS stylesheets
- `setup_postgres.sh` - Script for PostgreSQL setup
- `run_server.sh` - Script to run the development server
- `README-POSTGRES-GRAPHQL.md` - Documentation for PostgreSQL and GraphQL

## Development

### Adding New Locations

1. Log in to the Django admin interface at http://127.0.0.1:8000/admin/
2. Navigate to "Locations" and click "Add Location"
3. Fill in the required fields and save

### Adding User Profiles

User profiles are automatically created when users enter their information in the frontend. You can also:

1. Log in to the Django admin interface at http://127.0.0.1:8000/admin/
2. Navigate to "User profiles" and click "Add User profile"
3. Fill in the required fields and save

### Customizing the Frontend

- The main Vue.js application is in `templates/vue_app/vue_mapbox_raw.html`
- CSS styles are embedded in the template file
- Additional Vue components can be added as needed

### Working with PostgreSQL and GraphQL always use the following file:

See [README-POSTGRES-GRAPHQL.md](README-POSTGRES-GRAPHQL.md) for detailed instructions on:

- Setting up PostgreSQL
- Using the GraphQL interface
- Example queries and mutations
- Working with user profile data

## Additional Features

The application includes several additional features that can be extended:

1. **Address Geocoding**: The app can convert addresses to coordinates using Mapbox's Geocoding API
2. **Anonymous User Profiles**: Session-based user identification for profile persistence without login
3. **Multi-version Support**: Multiple implementation variations available (Vue.js, basic HTML, Mapbox, Leaflet)
4. **Mobile Responsiveness**: The interface adapts to different screen sizes
5. **Fallback Mechanisms**: localStorage backup for offline or API failure scenarios

## License

[Specify license information here]

## Credits

Created for CHLA with MapBox integration.