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
# --preload: Load app before forking workers (shares code, establishes DB connections early)
# --workers 2: Use 2 workers (t3.small has 2 vCPUs)
# --threads 2: Use 2 threads per worker for better concurrency
# --keep-alive 5: Keep connections alive for 5 seconds between requests
exec gunicorn --bind 0.0.0.0:8000 --workers 2 --threads 2 --timeout 120 --keep-alive 5 --preload maplocation.wsgi:application
