#!/bin/bash

echo "Restarting Map Location Finder (FIXED VERSION)..."
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
echo "This version uses Django's verbatim tag to avoid template conflicts."
echo "---------------------------------------------"
echo "Using your Mapbox public token: pk.eyJ1IjoiYWxleGJlYXR0aWUiLCJhIjoiOVVEYU52WSJ9.S_uekMjvfZC5_s0dVVJgQg"
echo "---------------------------------------------"
echo "Press Ctrl+C to stop the server"
echo "---------------------------------------------"

python manage.py runserver