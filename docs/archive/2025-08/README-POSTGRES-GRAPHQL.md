# Map Location Finder - PostgreSQL & GraphQL Setup

This guide explains how to set up PostgreSQL and GraphQL for the Map Location Finder application.

## PostgreSQL Setup

### 1. Install PostgreSQL

If you haven't already installed PostgreSQL, you can download it from:
https://www.postgresql.org/download/

### 2. Create a Database

```bash
# Connect to PostgreSQL
psql -U postgres

# Create a database
CREATE DATABASE maplocation;

# Create a user (optional)
CREATE USER maplocation_user WITH PASSWORD 'your_password';

# Grant privileges
GRANT ALL PRIVILEGES ON DATABASE maplocation TO maplocation_user;
```

### 3. Configure Django for PostgreSQL

Update the `DATABASES` setting in `settings.py` by uncommenting the PostgreSQL configuration and setting your credentials:

```python
DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.postgresql',
        'NAME': 'maplocation',
        'USER': 'maplocation_user',  # or your PostgreSQL username
        'PASSWORD': 'your_password',
        'HOST': 'localhost',
        'PORT': '5432',
    }
}
```

### 4. Install Required Packages

```bash
pip install psycopg2-binary
```

### 5. Run Migrations

```bash
python manage.py migrate
```

## GraphQL Setup

### 1. Access the GraphQL Interface

After starting the server, navigate to:
```
http://127.0.0.1:8000/graphql/
```

This will open the GraphiQL interface, which allows you to test queries and mutations.

### 2. Query Examples

#### Get Current User Profile
```graphql
query {
  userProfile {
    id
    age
    address
    diagnosis
    diagnosisDisplay
  }
}
```

#### Get All Locations
```graphql
query {
  allLocations {
    id
    name
    address
    city
    categoryName
    rating
    priceLevel
  }
}
```

#### Get Nearby Locations
```graphql
query {
  nearbyLocations(lat: 37.7749, lng: -122.4194, radius: 5) {
    id
    name
    address
    distance
    latitude
    longitude
  }
}
```

### 3. Mutation Examples

#### Update User Profile
```graphql
mutation {
  updateUserProfile(
    age: 30, 
    address: "123 Main St, San Francisco, CA", 
    diagnosis: "autism"
  ) {
    userProfile {
      id
      age
      address
      diagnosis
      diagnosisDisplay
    }
  }
}
```

## User Profiles

The application now supports user profiles with the following features:

1. **Anonymous Users**: Users can create and update profiles without logging in, using session-based identification.
2. **Profile Data Storage**: The system stores age, address, diagnosis, and location coordinates.
3. **Address Geocoding**: When a user saves an address, it is geocoded to latitude/longitude coordinates using Mapbox.
4. **Data Persistence**: 
   - Data is saved to the database for logged-in users
   - Data is saved to localStorage as a fallback for anonymous users
   - Sessions are used to map anonymous users to database profiles

### REST API Endpoints for User Profiles

- **GET /api/users/profiles/me/**: Get the current user's profile
- **POST /api/users/profiles/update_me/**: Update the current user's profile with new information

### GraphQL Queries and Mutations

The system supports both REST API and GraphQL:

```graphql
# Get profile
query {
  userProfile {
    id
    age
    address
    city
    state
    zipCode
    latitude
    longitude
    diagnosis
    diagnosisDisplay
  }
}

# Update profile
mutation {
  updateUserProfile(
    age: 30,
    address: "123 Main St, San Francisco, CA",
    diagnosis: "autism"
  ) {
    userProfile {
      id
      age
      address
      diagnosis
    }
  }
}
```

## Vue.js Frontend Integration

The Vue.js frontend now fully integrates with the backend user profile system:

1. User information is collected through the user panel
2. Data is sent to the backend API and saved in the database
3. Addresses are geocoded to coordinates for map-based search
4. User profile data persists between sessions

### Key Components:

- **User Information Panel**: Collects age, address, and diagnosis information
- **User Data Persistence**: Saves data to both backend and localStorage for redundancy
- **Geocoding Integration**: Converts user addresses to coordinates for location-based searching
- **API Fallback Mechanism**: Gracefully handles offline scenarios

## Environment Variables

For security, consider using environment variables for database credentials:

```bash
export DB_NAME=maplocation
export DB_USER=maplocation_user
export DB_PASSWORD=your_password
export DB_HOST=localhost
export DB_PORT=5432
```

## Schema Evolution

As your data model evolves:

1. Update the Django models
2. Run migrations: `python manage.py makemigrations && python manage.py migrate`
3. Update the corresponding GraphQL schema
4. Update your Vue.js frontend to work with the new fields

## Future Enhancements

Consider these enhancements for the future:

1. **User Authentication**: Add proper login/registration system
2. **Extended User Profiles**: Add additional fields like preferences, favorites
3. **Geocoding Optimization**: Implement address autocomplete for better user experience 
4. **Data Analytics**: Track user searches and preferences for better recommendations