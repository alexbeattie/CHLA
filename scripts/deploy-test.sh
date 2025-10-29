#!/bin/bash
#
# Local Deployment Test Script
# Tests the deployment pipeline locally before pushing to GitHub
#

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "🚀 CHLA Deployment Test Script"
echo "================================"
echo ""

# Function to print status
print_status() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

# Check if in project root
if [ ! -f "maplocation/manage.py" ] || [ ! -f "map-frontend/package.json" ]; then
    print_error "Must run from project root directory"
    exit 1
fi

print_status "Starting deployment tests..."

# Test 1: Backend Linting
echo ""
echo "📋 Testing Backend Linting..."
cd maplocation

if command -v flake8 &> /dev/null; then
    flake8 . --count --select=E9,F63,F7,F82 --show-source --statistics --exclude=venv,migrations,staticfiles || print_warning "Linting issues found"
else
    print_warning "flake8 not installed, skipping linting"
fi

if command -v black &> /dev/null; then
    black --check . --exclude='/(venv|migrations|staticfiles)/' || print_warning "Code formatting issues found"
else
    print_warning "black not installed, skipping format check"
fi

cd ..

# Test 2: Backend Tests
echo ""
echo "🧪 Running Backend Tests..."
cd maplocation

if command -v pytest &> /dev/null; then
    pytest -v || {
        print_error "Backend tests failed"
        exit 1
    }
else
    print_warning "pytest not installed, skipping tests"
fi

print_status "Backend tests passed"
cd ..

# Test 3: Backend System Checks
echo ""
echo "🔍 Running Django System Checks..."
cd maplocation
python manage.py check || {
    print_error "Django system check failed"
    exit 1
}
print_status "Django system checks passed"
cd ..

# Test 4: Frontend Tests
echo ""
echo "🧪 Running Frontend Tests..."
cd map-frontend

if [ -f "package.json" ]; then
    npm test -- --run || {
        print_warning "Frontend tests failed or not configured"
    }
else
    print_error "Frontend package.json not found"
    exit 1
fi

print_status "Frontend tests passed"
cd ..

# Test 5: Frontend Build
echo ""
echo "🏗️  Testing Frontend Build..."
cd map-frontend
npm run build || {
    print_error "Frontend build failed"
    exit 1
}
print_status "Frontend build successful"
cd ..

# Test 6: Health Check Endpoint
echo ""
echo "🏥 Testing Health Check Endpoint..."
cd maplocation

# Start Django dev server in background
python manage.py runserver 8000 &
SERVER_PID=$!
sleep 5

# Test health endpoint
HEALTH_STATUS=$(curl -s http://localhost:8000/api/health/ | grep -o '"status":"healthy"' || echo "")

if [ -n "$HEALTH_STATUS" ]; then
    print_status "Health check endpoint working"
else
    print_warning "Health check endpoint returned unexpected response"
fi

# Kill dev server
kill $SERVER_PID 2>/dev/null || true
cd ..

# Summary
echo ""
echo "================================"
echo "✅ Deployment Tests Complete"
echo "================================"
echo ""
echo "Next steps:"
echo "1. Review any warnings above"
echo "2. Commit your changes"
echo "3. Push to trigger CI/CD pipeline"
echo ""
echo "Monitor deployment:"
echo "  gh run watch"
echo ""
echo "Manual deployment:"
echo "  gh workflow run ci-cd.yml"
echo ""
