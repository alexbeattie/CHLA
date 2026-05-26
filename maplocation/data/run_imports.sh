#!/bin/bash
# Helper script to import regional center provider data

cd /Users/alexbeattie/Developer/CHLA/maplocation

# Activate virtual environment
source venv/bin/activate

# Set Mapbox token (for geocoding)
export MAPBOX_ACCESS_TOKEN="YOUR_MAPBOX_TOKEN"

echo "========================================"
echo "Importing Pasadena Provider List"
echo "========================================"
python manage.py import_regional_center_providers \
  --file ../data/Pasadena_Provider_List.xlsx \
  --regional-center "Pasadena" \
  --geocode

echo ""
echo "========================================"
echo "Importing San Gabriel Pomona Provider List"
echo "========================================"
python manage.py import_regional_center_providers \
  --file ../data/San_Gabriel_Pomona_Provider_List.xlsx \
  --regional-center "San Gabriel" \
  --geocode

echo ""
echo "========================================"
echo "Import Complete!"
echo "========================================"
echo "Verify at: http://127.0.0.1:8000/client-portal/"
echo "API: http://127.0.0.1:8000/api/providers/"
