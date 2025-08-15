#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

source "$SCRIPT_DIR/aws_env.sh"

cd "$REPO_ROOT/maplocation"

# Optional: ensure EB CLI is installed
if ! command -v eb >/dev/null 2>&1; then
  echo "Elastic Beanstalk CLI not found. Install with: brew install awsebcli" >&2
  exit 1
fi

APP_NAME=${APP_NAME:-chla-api}
ENV_NAME=${ENV_NAME:-chla-api-env}

echo "Deploying $APP_NAME to environment $ENV_NAME using AWS profile $AWS_PROFILE"

eb use "$ENV_NAME" || true
eb deploy "$ENV_NAME"

echo "Deployed. To run migrations: eb ssh $ENV_NAME --command 'python manage.py migrate && python manage.py collectstatic --noinput'"

