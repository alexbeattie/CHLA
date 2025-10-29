#!/bin/bash
#
# CI/CD Setup Script
# Configures GitHub secrets and verifies CI/CD pipeline setup
#

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🔧 CHLA CI/CD Setup Script${NC}"
echo "================================"
echo ""

# Check if gh CLI is installed
if ! command -v gh &> /dev/null; then
    echo -e "${RED}✗${NC} GitHub CLI (gh) is not installed"
    echo "Install it from: https://cli.github.com/"
    exit 1
fi

# Check if user is authenticated
if ! gh auth status &> /dev/null; then
    echo -e "${RED}✗${NC} Not authenticated with GitHub CLI"
    echo "Run: gh auth login"
    exit 1
fi

echo -e "${GREEN}✓${NC} GitHub CLI is installed and authenticated"
echo ""

# Function to set secret
set_secret() {
    local name=$1
    local description=$2
    local example=$3

    echo -e "${YELLOW}Setting${NC} $name"
    echo "Description: $description"
    [ -n "$example" ] && echo "Example: $example"

    read -sp "Enter value (or press Enter to skip): " value
    echo ""

    if [ -n "$value" ]; then
        gh secret set "$name" --body "$value"
        echo -e "${GREEN}✓${NC} Set $name"
    else
        echo -e "${YELLOW}⚠${NC} Skipped $name"
    fi
    echo ""
}

echo "This script will help you configure GitHub secrets for CI/CD."
echo "You can skip any secret and set it manually later."
echo ""
read -p "Press Enter to continue..."
echo ""

# AWS Secrets
echo -e "${BLUE}=== AWS Configuration ===${NC}"
set_secret "AWS_ACCESS_KEY_ID" "AWS Access Key ID" "AKIAIOSFODNN7EXAMPLE"
set_secret "AWS_SECRET_ACCESS_KEY" "AWS Secret Access Key" ""

# RDS Secrets
echo -e "${BLUE}=== RDS Database Configuration ===${NC}"
set_secret "RDS_DB_NAME" "RDS database name" "production_db"
set_secret "RDS_DB_USER" "RDS database user" "admin"
set_secret "RDS_DB_PASSWORD" "RDS database password" ""
set_secret "RDS_DB_HOST" "RDS database host" "mydb.xxx.us-west-2.rds.amazonaws.com"
set_secret "RDS_DB_PORT" "RDS database port" "5432"
set_secret "RDS_INSTANCE_ID" "RDS instance identifier" "chla-prod-db"

# Django Secrets
echo -e "${BLUE}=== Django Configuration ===${NC}"
echo "Generating Django secret key..."
DJANGO_KEY=$(python -c 'from django.core.management.utils import get_random_secret_key; print(get_random_secret_key())' 2>/dev/null || echo "")
if [ -n "$DJANGO_KEY" ]; then
    gh secret set "DJANGO_SECRET_KEY" --body "$DJANGO_KEY"
    echo -e "${GREEN}✓${NC} Generated and set DJANGO_SECRET_KEY"
else
    set_secret "DJANGO_SECRET_KEY" "Django secret key" ""
fi
echo ""

# Deployment URLs
echo -e "${BLUE}=== Deployment URLs ===${NC}"
set_secret "BACKEND_URL" "Production backend URL" "https://api.kinddhelp.com"
set_secret "FRONTEND_URL" "Production frontend URL" "https://kinddhelp.com"
set_secret "S3_BUCKET" "S3 bucket for frontend" "kinddhelp-frontend-1755148345"
set_secret "CLOUDFRONT_DISTRIBUTION_ID" "CloudFront distribution ID" "E2W6EECHUV4LMM"

# Verify setup
echo ""
echo -e "${BLUE}=== Verifying Setup ===${NC}"
echo ""

# Check workflows
if [ -f ".github/workflows/ci-cd.yml" ]; then
    echo -e "${GREEN}✓${NC} Main CI/CD workflow found"
else
    echo -e "${RED}✗${NC} Main CI/CD workflow missing"
fi

if [ -f ".github/workflows/db-sync.yml" ]; then
    echo -e "${GREEN}✓${NC} Database sync workflow found"
else
    echo -e "${RED}✗${NC} Database sync workflow missing"
fi

if [ -f ".github/workflows/rollback.yml" ]; then
    echo -e "${GREEN}✓${NC} Rollback workflow found"
else
    echo -e "${RED}✗${NC} Rollback workflow missing"
fi

# Check test setup
if [ -f "maplocation/pytest.ini" ]; then
    echo -e "${GREEN}✓${NC} Backend test configuration found"
else
    echo -e "${YELLOW}⚠${NC} Backend test configuration missing"
fi

if [ -f "map-frontend/package.json" ]; then
    echo -e "${GREEN}✓${NC} Frontend package.json found"
else
    echo -e "${RED}✗${NC} Frontend package.json missing"
fi

echo ""
echo "================================"
echo -e "${GREEN}✅ Setup Complete${NC}"
echo "================================"
echo ""
echo "Next steps:"
echo "1. Review secrets in GitHub Settings"
echo "2. Test the pipeline with a push to main"
echo "3. Monitor deployment: gh run watch"
echo ""
echo "Documentation:"
echo "  - CI/CD Guide: .github/CICD_GUIDE.md"
echo "  - Secrets: .github/SECRETS.md"
echo ""
