#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

source "$SCRIPT_DIR/aws_env.sh"

cd "$REPO_ROOT/map-frontend"

API_BASE_URL=${VITE_API_BASE_URL:-}
if [[ -z "$API_BASE_URL" ]]; then
  echo "VITE_API_BASE_URL is required. Example: export VITE_API_BASE_URL=https://your-eb-env.elasticbeanstalk.com/api" >&2
  exit 1
fi

npm ci
VITE_API_BASE_URL="$API_BASE_URL" npm run build

BUCKET_NAME=${FRONTEND_BUCKET_NAME:-chla-frontend-$(date +%s)}
aws s3 mb "s3://$BUCKET_NAME" || true
aws s3 sync dist "s3://$BUCKET_NAME" --delete

echo "Frontend uploaded to s3://$BUCKET_NAME"
echo "Create/point CloudFront distribution to this bucket for production hosting."

