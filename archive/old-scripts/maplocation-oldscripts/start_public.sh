#!/bin/bash

echo "Starting Map Location Finder with Public Mapbox Token..."
echo "---------------------------------------------"

# Activate virtual environment
source venv/bin/activate

# Start the development server
echo "Starting development server..."
echo "---------------------------------------------"
echo "Open your browser to: http://127.0.0.1:8000/public/"
echo "This version uses a public Mapbox token that should work without errors."
echo "---------------------------------------------"
echo "Press Ctrl+C to stop the server"
echo "---------------------------------------------"

python manage.py runserver