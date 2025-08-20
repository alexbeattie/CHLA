#!/bin/bash

echo "Restarting Map Location Finder (SIMPLE VERSION)..."
echo "---------------------------------------------"

# Kill any existing Django processes
pkill -f "python manage.py runserver" || true

# Activate virtual environment  
source venv/bin/activate

# Collect static files
echo "Collecting static files..."
python manage.py collectstatic --noinput --clear

# Start the development server
echo "Starting development server..."
echo "---------------------------------------------"
echo "The application is now running at: http://127.0.0.1:8000/"
echo "This version uses a simplified pure JavaScript implementation that should be more reliable."
echo "---------------------------------------------"
echo "Available URLs:"
echo "- Main (Simple JS): http://127.0.0.1:8000/"
echo "- Vue.js version: http://127.0.0.1:8000/vue/"
echo "- Admin: http://127.0.0.1:8000/admin/"
echo "---------------------------------------------"
echo "Using Mapbox public token: pk.eyJ1IjoiYWxleGJlYXR0aWUiLCJhIjoiOVVEYU52WSJ9.S_uekMjvfZC5_s0dVVJgQg"
echo "---------------------------------------------"
echo "Press Ctrl+C to stop the server"
echo "---------------------------------------------"

python manage.py runserver