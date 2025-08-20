#!/bin/bash

echo "Starting Map Location Finder (Basic Version)..."
echo "---------------------------------------------"

# Activate virtual environment
source venv/bin/activate

# Start the development server
echo "Starting development server..."
echo "---------------------------------------------"
echo "Try the BASIC version at: http://127.0.0.1:8000/basic/"
echo "Admin interface available at: http://127.0.0.1:8000/admin/"
echo "Login with username: admin, password: admin123"
echo "---------------------------------------------"
echo "Press Ctrl+C to stop the server"
echo "---------------------------------------------"

python manage.py runserver