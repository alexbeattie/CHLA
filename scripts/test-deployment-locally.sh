#!/bin/bash

# Test Deployment Locally (Before pushing to GitHub)
# Catches common errors in seconds instead of waiting 10+ minutes for CI/CD to fail

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && cd .. && pwd)"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}╔══════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   Local Deployment Test (Fast Pre-flight Checks)    ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════════════════╝${NC}"
echo ""

ERRORS=0

# Test 1: Backend Python Syntax
echo -e "${YELLOW}1. Checking Python syntax...${NC}"
cd "$SCRIPT_DIR/maplocation"
if python3 -m py_compile locations/*.py 2>/dev/null; then
    echo -e "${GREEN}✓ Python syntax OK${NC}"
else
    echo -e "${RED}✗ Python syntax errors found${NC}"
    ERRORS=$((ERRORS + 1))
fi
echo ""

# Test 2: Django Configuration
echo -e "${YELLOW}2. Checking Django configuration...${NC}"
cd "$SCRIPT_DIR/maplocation"
export DB_NAME=test_db
export DB_USER=test
export DB_PASSWORD=test
export DB_HOST=localhost
export DJANGO_SECRET_KEY=test-key
export DJANGO_DEBUG=false

if python3 manage.py check --deploy 2>/dev/null; then
    echo -e "${GREEN}✓ Django configuration OK${NC}"
else
    echo -e "${RED}✗ Django configuration errors${NC}"
    python3 manage.py check --deploy
    ERRORS=$((ERRORS + 1))
fi
echo ""

# Test 3: Requirements.txt valid
echo -e "${YELLOW}3. Checking requirements.txt...${NC}"
if pip install -r requirements.txt --dry-run --no-deps 2>&1 | grep -q "ERROR"; then
    echo -e "${RED}✗ Invalid package in requirements.txt${NC}"
    ERRORS=$((ERRORS + 1))
else
    echo -e "${GREEN}✓ requirements.txt valid${NC}"
fi
echo ""

# Test 4: Frontend Build
echo -e "${YELLOW}4. Testing frontend build...${NC}"
cd "$SCRIPT_DIR/map-frontend"
if [ ! -d "node_modules" ]; then
    echo -e "${YELLOW}Installing npm packages...${NC}"
    npm install > /dev/null 2>&1
fi

# Quick syntax check (no full build)
if npm run build -- --mode production 2>&1 | grep -q "error"; then
    echo -e "${RED}✗ Frontend build has errors${NC}"
    ERRORS=$((ERRORS + 1))
else
    echo -e "${GREEN}✓ Frontend builds successfully${NC}"
fi
echo ""

# Test 5: Environment Variables
echo -e "${YELLOW}5. Checking environment files...${NC}"
if [ ! -f "$SCRIPT_DIR/.env.local" ]; then
    echo -e "${YELLOW}⚠ .env.local not found (optional)${NC}"
else
    echo -e "${GREEN}✓ .env.local exists${NC}"
fi

if [ ! -f "$SCRIPT_DIR/.env.production" ]; then
    echo -e "${RED}✗ .env.production not found (required for deployment)${NC}"
    ERRORS=$((ERRORS + 1))
else
    echo -e "${GREEN}✓ .env.production exists${NC}"
fi
echo ""

# Test 6: AWS Credentials
echo -e "${YELLOW}6. Checking AWS credentials...${NC}"
if aws sts get-caller-identity --profile personal > /dev/null 2>&1; then
    echo -e "${GREEN}✓ AWS credentials valid${NC}"
else
    echo -e "${RED}✗ AWS credentials not configured${NC}"
    ERRORS=$((ERRORS + 1))
fi
echo ""

# Test 7: EB CLI Configuration
echo -e "${YELLOW}7. Checking EB CLI configuration...${NC}"
cd "$SCRIPT_DIR/maplocation"
if [ -f ".elasticbeanstalk/config.yml" ]; then
    echo -e "${GREEN}✓ EB CLI configured${NC}"
else
    echo -e "${RED}✗ EB CLI not configured${NC}"
    ERRORS=$((ERRORS + 1))
fi
echo ""

# Test 8: Git Status
echo -e "${YELLOW}8. Checking git status...${NC}"
cd "$SCRIPT_DIR"
if [[ -n $(git status -s) ]]; then
    echo -e "${YELLOW}⚠ Uncommitted changes:${NC}"
    git status -s | head -5
    echo ""
else
    echo -e "${GREEN}✓ No uncommitted changes${NC}"
fi
echo ""

# Summary
echo -e "${BLUE}╔══════════════════════════════════════════════════════╗${NC}"
if [ $ERRORS -eq 0 ]; then
    echo -e "${GREEN}║   ✓ All checks passed! Ready to deploy              ║${NC}"
    echo -e "${BLUE}╚══════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo "Next steps:"
    echo "  1. Manual: ./maplocation/deploy.sh && ./map-frontend/deploy.sh"
    echo "  2. GitHub: git push origin main (triggers automatic deployment)"
    exit 0
else
    echo -e "${RED}║   ✗ $ERRORS error(s) found - Fix before deploying       ║${NC}"
    echo -e "${BLUE}╚══════════════════════════════════════════════════════╝${NC}"
    exit 1
fi

