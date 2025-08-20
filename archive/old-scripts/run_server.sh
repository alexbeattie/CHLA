#!/bin/bash

# Activate virtual environment
source venv/bin/activate

# Run migrations if needed
echo "Checking and applying any pending migrations..."
python manage.py makemigrations
python manage.py migrate

# Set PYTHONPATH to include the project root
export PYTHONPATH=$(pwd):$PYTHONPATH

# Print helpful information
echo "========================================"
echo "Map Location Finder Server"
echo "========================================"
echo "Starting Django development server..."
echo ""
echo "Access the application at:"
echo "  http://127.0.0.1:8000/"
echo ""
echo "API endpoints:"
echo "  http://127.0.0.1:8000/api/locations/       (All locations)"
echo "  http://127.0.0.1:8000/api/locations/nearby/ (Nearby locations)"
echo "  http://127.0.0.1:8000/api/users/profiles/me/ (User profile)"
echo ""
echo "GraphQL interface:"
echo "  http://127.0.0.1:8000/graphql/"
echo ""
echo "Press Ctrl+C to stop the server"
echo "========================================"

# Start the Django development server
python manage.py runserver