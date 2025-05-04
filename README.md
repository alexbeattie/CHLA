# CHLA Map Application

A comprehensive mapping application for Children's Hospital Los Angeles (CHLA) that helps users find resources based on location and category.

## Repository Structure

This is a monorepo containing both frontend and backend code:

- **map-frontend/** - Vue.js frontend application with Mapbox GL integration
- **maplocation/** - Django backend application with REST API and GraphQL support

## Frontend Features

- Interactive map centered on Los Angeles
- Custom styled category-based markers
- Location filtering by category and search text
- Distance-based filtering with radius adjustment
- User information panel with address geocoding
- Responsive design for mobile and desktop

## Backend Features

- Django REST framework API
- GraphQL API using Graphene
- PostgreSQL database support
- User profile management
- Location and category data models

## Getting Started

### Running the Backend

```bash
cd maplocation
python manage.py runserver
```

### Running the Frontend

```bash
cd map-frontend
npm install
npm run dev
```

## Development Notes

- Sample data is included for demonstration purposes
- The application has both local and API data modes
- Map is centered on Los Angeles by default
