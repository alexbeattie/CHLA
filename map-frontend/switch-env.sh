#!/bin/bash

# Script to switch between development and production environments
# Usage: ./switch-env.sh [dev|prod]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Resolve the Mapbox token without ever blanking a token we already have:
# shell environment first, then the current .env, then .env.production.
read_token_from_file() {
  [ -f "$1" ] && sed -n 's/^VITE_MAPBOX_TOKEN=//p' "$1" | head -n 1
}

MAPBOX_TOKEN="${VITE_MAPBOX_TOKEN:-${MAPBOX_TOKEN:-}}"
if [ -z "$MAPBOX_TOKEN" ]; then
  MAPBOX_TOKEN="$(read_token_from_file "$SCRIPT_DIR/.env")"
fi
if [ -z "$MAPBOX_TOKEN" ]; then
  MAPBOX_TOKEN="$(read_token_from_file "$SCRIPT_DIR/.env.production")"
fi
if [ -z "$MAPBOX_TOKEN" ]; then
  echo "WARNING: no Mapbox token found (shell env, .env, .env.production)."
  echo "The map will not render without VITE_MAPBOX_TOKEN."
fi

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
