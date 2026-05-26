# Map Location Frontend

A Vue.js frontend for the Map Location application that shows locations on a Mapbox map and collects user information.

## Features

- Interactive map-based search using Mapbox GL JS
- User information collection (age, address, diagnosis)
- Address geocoding to coordinates
- Category filtering
- Distance-based filtering
- Mobile-responsive design

## Setup Instructions

1. Install dependencies:
```bash
npm install
```

2. Start the development server:
```bash
npm run dev
```

3. Build for production:
```bash
npm run build
```

## Project Structure

- `src/` - Application source code
  - `components/` - Reusable Vue components
    - `UserInfoPanel.vue` - User information form
    - `LocationList.vue` - List of locations
    - `LocationDetail.vue` - Detailed location view
  - `views/` - Page components
    - `MapView.vue` - Main map page
  - `router/` - Vue Router configuration
  - `assets/` - Static assets
  - `App.vue` - Root component
  - `main.js` - Application entry point

## Backend Integration

This frontend connects to a Django backend via:
- REST API (`/api/locations/`, `/api/users/profiles/me/`, etc.)
- API proxy configuration in `vite.config.js`

For the backend code, see the `maplocation` directory.

## Technology Stack

- Vue.js 3
- Vue Router for navigation
- Mapbox GL JS for maps
- Mapbox Geocoding API for address geocoding
- Axios for API requests
- Vite for build tooling
- Bootstrap 5 for styling

## Notes

- Mapbox token is configured in `MapView.vue`
- API urls are proxied to the backend in development