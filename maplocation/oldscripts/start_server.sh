#!/bin/bash

echo "Starting Map Location Finder..."
echo "---------------------------------------------"

# Activate virtual environment
source venv/bin/activate

# Start the development server
echo "Starting development server..."
echo "---------------------------------------------"
echo "Available Routes:"
echo "Main app: http://127.0.0.1:8000/ (Vue.js + Mapbox GL JS)"
echo "Vue Mapbox version: http://127.0.0.1:8000/vue/"
echo "Simple Mapbox version: http://127.0.0.1:8000/mapbox/"
echo "Leaflet version: http://127.0.0.1:8000/leaflet/"
echo "Basic version: http://127.0.0.1:8000/basic/"
echo "Public token version: http://127.0.0.1:8000/public/"
echo ""
echo "Using your Mapbox public token: pk.eyJ1IjoiYWxleGJlYXR0aWUiLCJhIjoiOVVEYU52WSJ9.S_uekMjvfZC5_s0dVVJgQg"
echo "---------------------------------------------"
echo "Admin interface: http://127.0.0.1:8000/admin/"
echo "Login with username: admin, password: admin123"
echo "---------------------------------------------"
echo "Press Ctrl+C to stop the server"
echo "---------------------------------------------"

python manage.py runserver