#!/bin/bash
# Geocode providers directly on RDS database.
# Pulls the RDS connection blob from AWS Secrets Manager (kindd/prod/rds)
# and exports the values as DB_* env vars before invoking Django.
set -e

echo "Setting up RDS connection from AWS Secrets Manager..."

SECRET_JSON=$(aws secretsmanager get-secret-value \
    --secret-id kindd/prod/rds \
    --query SecretString --output text \
    --region "${AWS_REGION:-us-west-2}")

export DB_HOST=$(echo "$SECRET_JSON" | python3 -c "import sys,json;print(json.load(sys.stdin)['host'])")
export DB_PORT=$(echo "$SECRET_JSON" | python3 -c "import sys,json;print(json.load(sys.stdin)['port'])")
export DB_NAME=$(echo "$SECRET_JSON" | python3 -c "import sys,json;print(json.load(sys.stdin)['dbname'])")
export DB_USER=$(echo "$SECRET_JSON" | python3 -c "import sys,json;print(json.load(sys.stdin)['username'])")
export DB_PASSWORD=$(echo "$SECRET_JSON" | python3 -c "import sys,json;print(json.load(sys.stdin)['password'])")
export DB_SSL_REQUIRE="true"
unset SECRET_JSON

echo "RDS connection configured"
echo "  Host: $DB_HOST"
echo "  Database: $DB_NAME"
echo "  User: $DB_USER"
echo ""

# Activate virtual environment (assumes script is run from maplocation/)
source venv/bin/activate

echo "Checking database connection..."
python3 scripts/checks/check_database.py
echo ""

read -p "Continue with geocoding on RDS? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Checking geocoding status on RDS..."
    python3 scripts/checks/check_geocoding_status.py
    echo ""

    read -p "Start geocoding all providers on RDS? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "Starting geocoding on RDS..."
        python3 manage.py geocode_providers --all
        echo ""
        echo "Geocoding complete!"
        echo ""
        echo "Verifying results..."
        python3 scripts/checks/verify_geocoded.py
    else
        echo "Geocoding cancelled"
    fi
else
    echo "Operation cancelled"
fi

# Clean up environment variables
unset DB_HOST DB_PORT DB_NAME DB_USER DB_PASSWORD DB_SSL_REQUIRE
