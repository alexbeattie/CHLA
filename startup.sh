#!/bin/bash

# 🚀 CHLA Project Startup Script
# This script automatically starts both the Django backend and Vue.js frontend
# from the correct directories to prevent confusion!

echo "🚀 Starting CHLA Project..."
echo "================================"

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo "📍 Script location: $SCRIPT_DIR"

# Check if we're in the right place
if [ ! -f "$SCRIPT_DIR/maplocation/manage.py" ]; then
    echo "❌ ERROR: manage.py not found in maplocation/ directory!"
    echo "   Make sure you're running this from the CHLAProj root directory."
    exit 1
fi

echo "✅ Directory structure verified!"

# Function to start backend
start_backend() {
    echo ""
    echo "🐍 Starting Django Backend..."
    echo "   Directory: $SCRIPT_DIR/maplocation"
    
    cd "$SCRIPT_DIR/maplocation"
    
    # Check if virtual environment exists
    if [ ! -d ".venv" ]; then
        echo "❌ Virtual environment not found!"
        echo "   Please create it first: python -m venv .venv"
        exit 1
    fi
    
    # Activate virtual environment and start server
    source .venv/bin/activate
    echo "✅ Virtual environment activated"
    
    echo "🚀 Starting Django server on port 8000..."
    echo "   Backend will be available at: http://127.0.0.1:8000/"
    echo "   API endpoints: http://127.0.0.1:8000/api/"
    echo "   Admin panel: http://127.0.0.1:8000/admin/"
    echo ""
    echo "   Press Ctrl+C to stop the backend server"
    echo "================================================"
    
    python3 manage.py runserver 8000
}

# Function to start frontend
start_frontend() {
    echo ""
    echo "⚡ Starting Vue.js Frontend..."
    echo "   Directory: $SCRIPT_DIR/map-frontend"
    
    cd "$SCRIPT_DIR/map-frontend"
    
    # Check if node_modules exists
    if [ ! -d "node_modules" ]; then
        echo "📦 Installing dependencies..."
        npm install
    fi
    
    echo "🚀 Starting Vue.js development server..."
    echo "   Frontend will be available at: http://localhost:3000/"
    echo ""
    echo "   Press Ctrl+C to stop the frontend server"
    echo "================================================"
    
    npm run dev
}

# Function to start both services
start_both() {
    echo ""
    echo "🔄 Starting both services..."
    echo "   This will open two terminal windows"
    
    # Start backend in new terminal (macOS)
    osascript -e "
        tell application \"Terminal\"
            do script \"cd '$SCRIPT_DIR/maplocation' && source .venv/bin/activate && python3 manage.py runserver 8000\"
            set custom title of front window to \"CHLA Backend (Django)\"
        end tell
    "
    
    # Start frontend in new terminal (macOS)
    osascript -e "
        tell application \"Terminal\"
            do script \"cd '$SCRIPT_DIR/map-frontend' && npm run dev\"
            set custom title of front window to \"CHLA Frontend (Vue.js)\"
        end tell
    "
    
    echo "✅ Both services started in separate terminal windows!"
    echo "   Backend: Django server on port 8000"
    echo "   Frontend: Vue.js dev server on port 3000"
    echo ""
    echo "🌐 Open your browser to: http://localhost:3000/"
}

# Main menu
echo ""
echo "Choose an option:"
echo "1) Start Backend (Django) only"
echo "2) Start Frontend (Vue.js) only" 
echo "3) Start Both Services (recommended)"
echo "4) Exit"
echo ""

read -p "Enter your choice (1-4): " choice

case $choice in
    1)
        start_backend
        ;;
    2)
        start_frontend
        ;;
    3)
        start_both
        ;;
    4)
        echo "👋 Goodbye!"
        exit 0
        ;;
    *)
        echo "❌ Invalid choice. Please run the script again."
        exit 1
        ;;
esac
