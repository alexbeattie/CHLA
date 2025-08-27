#!/usr/bin/env bash
set -euo pipefail

# EB AL2023 postdeploy hook: run Django migrations and collect static files
cd /var/app/current || exit 0

echo "Running Django migrations..."
python manage.py migrate --noinput

echo "Collecting static files..."
python manage.py collectstatic --noinput

echo "Postdeploy hook complete."


