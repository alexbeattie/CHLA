#!/bin/bash

###############################################################################
# CHLA Provider Map - Automated Deployment Script
#
# Features:
# - Frontend build with cache busting
# - Database migrations
# - Static file collection
# - Service restart
# - Health checks
# - Rollback capability
# - No SSH/SSM required - uses AWS services directly
#
# Usage:
#   ./deploy.sh [environment]
#
# Environments: dev, staging, production
###############################################################################

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
ENVIRONMENT=${1:-dev}
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
DEPLOY_LOG="deploy_${TIMESTAMP}.log"

# Load environment-specific configuration
case "$ENVIRONMENT" in
  production)
    APP_NAME="chla-provider-map-prod"
    EB_ENV="chla-provider-map-prod"
    S3_BUCKET="chla-provider-map-frontend-prod"
    CLOUDFRONT_ID="E1234567890ABC"  # TODO: Update with your CloudFront distribution ID
    ;;
  staging)
    APP_NAME="chla-provider-map-staging"
    EB_ENV="chla-provider-map-staging"
    S3_BUCKET="chla-provider-map-frontend-staging"
    CLOUDFRONT_ID="E0987654321XYZ"  # TODO: Update
    ;;
  dev|*)
    APP_NAME="chla-provider-map-dev"
    EB_ENV="chla-provider-map-dev"
    S3_BUCKET="chla-provider-map-frontend-dev"
    CLOUDFRONT_ID=""  # Dev might not have CloudFront
    ;;
esac

###############################################################################
# Helper Functions
###############################################################################

log_info() {
  echo -e "${BLUE}[INFO]${NC} $1" | tee -a "$DEPLOY_LOG"
}

log_success() {
  echo -e "${GREEN}[SUCCESS]${NC} $1" | tee -a "$DEPLOY_LOG"
}

log_warning() {
  echo -e "${YELLOW}[WARNING]${NC} $1" | tee -a "$DEPLOY_LOG"
}

log_error() {
  echo -e "${RED}[ERROR]${NC} $1" | tee -a "$DEPLOY_LOG"
}

check_command() {
  if ! command -v $1 &> /dev/null; then
    log_error "$1 is not installed. Please install it first."
    exit 1
  fi
}

###############################################################################
# Pre-deployment Checks
###############################################################################

pre_deployment_checks() {
  log_info "Running pre-deployment checks..."

  # Check required commands
  check_command git
  check_command node
  check_command npm
  check_command python3
  check_command aws

  # Check AWS credentials
  if ! aws sts get-caller-identity &> /dev/null; then
    log_error "AWS credentials not configured. Run 'aws configure' first."
    exit 1
  fi

  # Check git status
  if [[ -n $(git status --porcelain) ]]; then
    log_warning "You have uncommitted changes. Consider committing them first."
    read -p "Continue anyway? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
      exit 1
    fi
  fi

  # Get current commit
  COMMIT_HASH=$(git rev-parse --short HEAD)
  BRANCH=$(git branch --show-current)

  log_info "Deploying branch: $BRANCH"
  log_info "Commit: $COMMIT_HASH"
  log_success "Pre-deployment checks passed"
}

###############################################################################
# Build Frontend
###############################################################################

build_frontend() {
  log_info "Building frontend..."

  cd "$SCRIPT_DIR/map-frontend"

  # Install dependencies
  log_info "Installing frontend dependencies..."
  npm ci --prefer-offline

  # Generate cache-busting build
  log_info "Building production bundle with cache busting..."
  export VITE_BUILD_ID="$TIMESTAMP"
  export VITE_COMMIT_HASH="$COMMIT_HASH"
  npm run build

  # Verify build
  if [ ! -d "dist" ]; then
    log_error "Build failed - dist directory not found"
    exit 1
  fi

  log_success "Frontend build completed"
}

###############################################################################
# Deploy Frontend to S3
###############################################################################

deploy_frontend() {
  log_info "Deploying frontend to S3..."

  cd "$SCRIPT_DIR/map-frontend/dist"

  # Sync files to S3 with appropriate cache headers
  log_info "Uploading assets with cache headers..."

  # HTML files - no cache (always check for updates)
  aws s3 sync . "s3://$S3_BUCKET/" \
    --exclude "*" \
    --include "*.html" \
    --cache-control "no-cache, no-store, must-revalidate" \
    --metadata-directive REPLACE \
    --delete

  # JS/CSS with hash - long cache (1 year)
  aws s3 sync . "s3://$S3_BUCKET/" \
    --exclude "*" \
    --include "*.js" \
    --include "*.css" \
    --cache-control "public, max-age=31536000, immutable" \
    --metadata-directive REPLACE

  # Images and fonts - medium cache (1 month)
  aws s3 sync . "s3://$S3_BUCKET/" \
    --exclude "*" \
    --include "*.png" \
    --include "*.jpg" \
    --include "*.jpeg" \
    --include "*.svg" \
    --include "*.woff" \
    --include "*.woff2" \
    --include "*.ttf" \
    --cache-control "public, max-age=2592000" \
    --metadata-directive REPLACE

  # Everything else - short cache (1 hour)
  aws s3 sync . "s3://$S3_BUCKET/" \
    --cache-control "public, max-age=3600" \
    --metadata-directive REPLACE \
    --delete

  log_success "Frontend deployed to S3"
}

###############################################################################
# Invalidate CloudFront Cache
###############################################################################

invalidate_cloudfront() {
  if [ -z "$CLOUDFRONT_ID" ]; then
    log_info "No CloudFront distribution configured for $ENVIRONMENT"
    return
  fi

  log_info "Invalidating CloudFront cache..."

  INVALIDATION_ID=$(aws cloudfront create-invalidation \
    --distribution-id "$CLOUDFRONT_ID" \
    --paths "/*" \
    --query 'Invalidation.Id' \
    --output text)

  log_info "Invalidation created: $INVALIDATION_ID"
  log_info "Waiting for invalidation to complete (this may take a few minutes)..."

  aws cloudfront wait invalidation-completed \
    --distribution-id "$CLOUDFRONT_ID" \
    --id "$INVALIDATION_ID"

  log_success "CloudFront cache invalidated"
}

###############################################################################
# Deploy Backend
###############################################################################

deploy_backend() {
  log_info "Deploying backend..."

  cd "$SCRIPT_DIR/maplocation"

  # Install Python dependencies
  log_info "Installing Python dependencies..."
  pip install -r requirements.txt --quiet

  # Run database migrations
  log_info "Running database migrations..."
  python manage.py migrate --no-input

  # Collect static files
  log_info "Collecting static files..."
  python manage.py collectstatic --no-input --clear

  # Create application version
  log_info "Creating Elastic Beanstalk application version..."

  # Create source bundle
  git archive --format=zip HEAD -o "../app-${TIMESTAMP}.zip"

  # Upload to S3
  aws s3 cp "../app-${TIMESTAMP}.zip" "s3://elasticbeanstalk-${AWS_REGION:-us-west-2}-$(aws sts get-caller-identity --query Account --output text)/"

  # Create application version
  aws elasticbeanstalk create-application-version \
    --application-name "$APP_NAME" \
    --version-label "v-${TIMESTAMP}-${COMMIT_HASH}" \
    --source-bundle S3Bucket="elasticbeanstalk-${AWS_REGION:-us-west-2}-$(aws sts get-caller-identity --query Account --output text)",S3Key="app-${TIMESTAMP}.zip" \
    --description "Deploy from commit ${COMMIT_HASH} at ${TIMESTAMP}"

  # Update environment
  log_info "Updating Elastic Beanstalk environment..."
  aws elasticbeanstalk update-environment \
    --application-name "$APP_NAME" \
    --environment-name "$EB_ENV" \
    --version-label "v-${TIMESTAMP}-${COMMIT_HASH}"

  # Wait for environment to be ready
  log_info "Waiting for environment to be ready (this may take several minutes)..."
  aws elasticbeanstalk wait environment-updated \
    --application-name "$APP_NAME" \
    --environment-names "$EB_ENV"

  log_success "Backend deployed successfully"
}

###############################################################################
# Health Check
###############################################################################

health_check() {
  log_info "Running health checks..."

  # Get environment URL
  ENV_URL=$(aws elasticbeanstalk describe-environments \
    --application-name "$APP_NAME" \
    --environment-names "$EB_ENV" \
    --query 'Environments[0].CNAME' \
    --output text)

  # Check backend health
  log_info "Checking backend health at https://${ENV_URL}/health/"

  HEALTH_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "https://${ENV_URL}/health/" || echo "000")

  if [ "$HEALTH_STATUS" == "200" ]; then
    log_success "Backend health check passed"
  else
    log_error "Backend health check failed (HTTP $HEALTH_STATUS)"
    return 1
  fi

  # Check frontend
  if [ -n "$CLOUDFRONT_ID" ]; then
    FRONTEND_URL=$(aws cloudfront get-distribution \
      --id "$CLOUDFRONT_ID" \
      --query 'Distribution.DomainName' \
      --output text)

    log_info "Checking frontend at https://${FRONTEND_URL}"
    FRONTEND_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "https://${FRONTEND_URL}/" || echo "000")

    if [ "$FRONTEND_STATUS" == "200" ]; then
      log_success "Frontend health check passed"
    else
      log_warning "Frontend health check returned HTTP $FRONTEND_STATUS"
    fi
  fi

  log_success "All health checks passed"
}

###############################################################################
# Rollback
###############################################################################

rollback() {
  log_warning "Rolling back deployment..."

  # Get previous version
  PREVIOUS_VERSION=$(aws elasticbeanstalk describe-environments \
    --application-name "$APP_NAME" \
    --environment-names "$EB_ENV" \
    --query 'Environments[0].VersionLabel' \
    --output text)

  log_info "Current version: $PREVIOUS_VERSION"

  # List recent versions
  log_info "Available versions for rollback:"
  aws elasticbeanstalk describe-application-versions \
    --application-name "$APP_NAME" \
    --max-records 5 \
    --query 'ApplicationVersions[].[VersionLabel,DateCreated]' \
    --output table

  read -p "Enter version to rollback to: " ROLLBACK_VERSION

  if [ -z "$ROLLBACK_VERSION" ]; then
    log_error "No version specified, aborting rollback"
    exit 1
  fi

  aws elasticbeanstalk update-environment \
    --application-name "$APP_NAME" \
    --environment-name "$EB_ENV" \
    --version-label "$ROLLBACK_VERSION"

  log_success "Rollback initiated to version: $ROLLBACK_VERSION"
}

###############################################################################
# Main Deployment Flow
###############################################################################

main() {
  log_info "=========================================="
  log_info "CHLA Provider Map Deployment"
  log_info "Environment: $ENVIRONMENT"
  log_info "Timestamp: $TIMESTAMP"
  log_info "=========================================="

  # Check if rollback mode
  if [ "$2" == "rollback" ]; then
    rollback
    exit 0
  fi

  # Run deployment
  pre_deployment_checks

  log_info "Starting deployment process..."

  # Build and deploy frontend
  build_frontend
  deploy_frontend
  invalidate_cloudfront

  # Deploy backend
  deploy_backend

  # Run health checks
  if health_check; then
    log_success "=========================================="
    log_success "Deployment completed successfully!"
    log_success "Environment: $ENVIRONMENT"
    log_success "Version: v-${TIMESTAMP}-${COMMIT_HASH}"
    log_success "=========================================="
  else
    log_error "=========================================="
    log_error "Deployment completed with warnings"
    log_error "Please check the logs for details"
    log_error "=========================================="
    exit 1
  fi
}

# Run main function
main "$@"
