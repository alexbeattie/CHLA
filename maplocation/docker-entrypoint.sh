#!/bin/bash
set -e

echo "Fixing migration history if needed..."
python manage.py fix_migrations || echo "Migration fix skipped or failed"

echo "Running database migrations..."
python manage.py migrate --noinput

echo "Collecting static files..."
python manage.py collectstatic --noinput --clear

echo "Populating regional center ZIP codes..."
python manage.py populate_san_gabriel_zips || echo "San Gabriel ZIP population failed or skipped"
python manage.py populate_pasadena_zips || echo "Pasadena ZIP population failed or skipped"

echo "Importing provider data..."
python manage.py import_regional_center_providers --file "data/Pasadena Provider List.xlsx" --area "Pasadena" || echo "Pasadena import failed or skipped"
python manage.py import_regional_center_providers --file "data/San Gabriel Pomona Provider List.xlsx" --regional-center "San Gabriel" || echo "San Gabriel import failed or skipped"

echo "Starting Gunicorn..."
exec gunicorn --bind 0.0.0.0:8000 --workers 3 --timeout 120 maplocation.wsgi:application
