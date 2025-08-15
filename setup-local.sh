#!/bin/bash

# CHLA Provider Map - Local Development Setup Script

set -e  # Exit on any error

echo "🚀 Setting up CHLA Provider Map local development environment..."
echo ""

# Check prerequisites
echo "📋 Checking prerequisites..."

# Check Python
if ! command -v python3 &> /dev/null; then
    echo "❌ Python 3 is required but not installed."
    exit 1
fi

PYTHON_VERSION=$(python3 -c 'import sys; print(".".join(map(str, sys.version_info[:2])))')
echo "✅ Python $PYTHON_VERSION found"

# Check Node.js
if ! command -v node &> /dev/null; then
    echo "❌ Node.js is required but not installed."
    echo "Please install Node.js 18+ from https://nodejs.org/"
    exit 1
fi

NODE_VERSION=$(node --version)
echo "✅ Node.js $NODE_VERSION found"

# Check PostgreSQL
if ! command -v psql &> /dev/null; then
    echo "⚠️  PostgreSQL not found. You may need to install it for local database."
    echo "For macOS: brew install postgresql"
    echo "For Ubuntu: sudo apt install postgresql postgresql-contrib"
fi

echo ""

# Setup backend
echo "🐍 Setting up Django backend..."
cd maplocation

# Create virtual environment if it doesn't exist
if [ ! -d ".venv" ]; then
    echo "Creating Python virtual environment..."
    python3 -m venv .venv
fi

# Activate virtual environment
echo "Activating virtual environment..."
source .venv/bin/activate

# Install dependencies
echo "Installing Python dependencies..."
pip install --upgrade pip
pip install -r requirements.txt

# Clear any production environment variables
echo "Clearing production environment variables..."
unset DJANGO_SECRET_KEY DJANGO_DEBUG ALLOWED_HOSTS CORS_ALLOWED_ORIGINS CSRF_TRUSTED_ORIGINS 2>/dev/null || true
unset DB_NAME DB_USER DB_PASSWORD DB_HOST DB_PORT 2>/dev/null || true

# Check database connection
echo "Checking Django configuration..."
python manage.py check

echo "✅ Backend setup complete!"
echo ""

# Setup frontend
echo "🌐 Setting up Vue.js frontend..."
cd ../map-frontend

# Install dependencies
echo "Installing Node.js dependencies..."
npm install

# Create local environment file
echo "Creating local environment configuration..."
cat > .env.local << EOF
# Local development environment
VITE_API_BASE_URL=http://127.0.0.1:8000/api
EOF

# Build frontend to test
echo "Testing frontend build..."
npm run build

echo "✅ Frontend setup complete!"
echo ""

# Create startup scripts
echo "📝 Creating startup scripts..."
cd ..

# Backend startup script
cat > start-backend.sh << 'EOF'
#!/bin/bash
cd maplocation
source .venv/bin/activate

# Clear production environment variables
unset DJANGO_SECRET_KEY DJANGO_DEBUG ALLOWED_HOSTS CORS_ALLOWED_ORIGINS CSRF_TRUSTED_ORIGINS 2>/dev/null || true
unset DB_NAME DB_USER DB_PASSWORD DB_HOST DB_PORT 2>/dev/null || true

echo "🚀 Starting Django development server..."
python manage.py runserver
EOF

# Frontend startup script
cat > start-frontend.sh << 'EOF'
#!/bin/bash
cd map-frontend
echo "🚀 Starting Vue.js development server..."
npm run dev
EOF

# Make scripts executable
chmod +x start-backend.sh start-frontend.sh

echo "✅ Startup scripts created!"
echo ""

# Summary
echo "🎉 Local development environment setup complete!"
echo ""
echo "📋 Next steps:"
echo ""
echo "1. Start the backend (Django):"
echo "   ./start-backend.sh"
echo "   Backend will be available at: http://127.0.0.1:8000"
echo ""
echo "2. Start the frontend (Vue.js) in a new terminal:"
echo "   ./start-frontend.sh"
echo "   Frontend will be available at: http://localhost:3000"
echo ""
echo "3. Test the full application:"
echo "   - Open http://localhost:3000 in your browser"
echo "   - Check that the mobile sidebar toggle works"
echo "   - Verify API calls are working"
echo ""
echo "📚 For deployment and AWS setup, see: DEPLOYMENT_GUIDE.md"
echo ""
echo "🐛 If you encounter issues:"
echo "   - Check that PostgreSQL is running (if using local DB)"
echo "   - Verify all dependencies are installed"
echo "   - See troubleshooting section in DEPLOYMENT_GUIDE.md"
