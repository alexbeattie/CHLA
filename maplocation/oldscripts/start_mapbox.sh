#!/bin/bash

echo "Starting Map Location Finder with Mapbox..."
echo "---------------------------------------------"

# Activate virtual environment
source venv/bin/activate

# Start the development server
echo "Starting development server..."
echo "---------------------------------------------"
echo "Open your browser to: http://127.0.0.1:8000/mapbox/"
echo "---------------------------------------------"
echo "IMPORTANT: Before running, edit the mapbox.html file to add your Mapbox access token"
echo "1. Open this file: templates/vue_app/mapbox.html"
echo "2. Find the line 'mapboxgl.accessToken = 'YOUR_MAPBOX_ACCESS_TOKEN';'"
echo "3. Replace 'YOUR_MAPBOX_ACCESS_TOKEN' with your actual Mapbox token"
echo "---------------------------------------------"
echo "Press Ctrl+C to stop the server"
echo "---------------------------------------------"

python manage.py runserver