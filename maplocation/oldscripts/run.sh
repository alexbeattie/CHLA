#!/bin/bash

echo "Starting Map Location Finder application..."
echo "---------------------------------------------"

# Activate virtual environment
source venv/bin/activate

# Collect static files
echo "Collecting static files..."
python manage.py collectstatic --noinput --clear

# Run migrations (if any are pending)
echo "Running migrations..."
python manage.py migrate

# Start the development server
echo "Starting development server..."
echo "---------------------------------------------"
echo "The application is now running at: http://127.0.0.1:8000/"
echo "Admin interface available at: http://127.0.0.1:8000/admin/"
echo "Login with username: admin, password: admin123"
echo "---------------------------------------------"
echo "Press Ctrl+C to stop the server"
echo "---------------------------------------------"

python manage.py runserver