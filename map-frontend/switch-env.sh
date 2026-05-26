#!/bin/bash

# Script to switch between development and production environments
# Usage: ./switch-env.sh [dev|prod]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MAPBOX_TOKEN="${VITE_MAPBOX_TOKEN:-${MAPBOX_TOKEN:-}}"

case "${1:-dev}" in
  "dev"|"development")
    echo "Switching to DEVELOPMENT environment..."
    echo "VITE_API_BASE_URL=http://127.0.0.1:8000" > "$SCRIPT_DIR/.env"
    echo "VITE_MAPBOX_TOKEN=$MAPBOX_TOKEN" >> "$SCRIPT_DIR/.env"
    echo "Switched to DEVELOPMENT (localhost:8000)"
    echo " Run: npm run dev"
    ;;
  "prod"|"production")
    echo "Switching to PRODUCTION environment..."
    echo "VITE_API_BASE_URL=https://api.kinddhelp.com" > "$SCRIPT_DIR/.env"
    echo "VITE_MAPBOX_TOKEN=$MAPBOX_TOKEN" >> "$SCRIPT_DIR/.env"
    echo "Switched to PRODUCTION (api.kinddhelp.com)"
    echo " Run: npm run build"
    ;;
  *)
    echo "Usage: $0 [dev|prod]"
    echo " dev - Switch to development (localhost:8000)"
    echo " prod - Switch to production (api.kinddhelp.com)"
    exit 1
    ;;
esac
