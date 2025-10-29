#!/bin/bash
# Geocode providers directly on RDS database

echo "🚀 Setting up RDS connection..."

# Set RDS environment variables
export DB_HOST="chla-postgres-db.cpkvcu4f59w6.us-west-2.rds.amazonaws.com"
export DB_NAME="postgres"
export DB_USER="chla_admin"
export DB_PASSWORD="CHLASecure2024"
export DB_SSL_REQUIRE="true"

echo "✅ RDS connection configured"
echo "   Host: $DB_HOST"
echo "   Database: $DB_NAME"
echo "   User: $DB_USER"
echo ""

# Activate virtual environment
source venv/bin/activate

echo "📍 Checking database connection..."
python3 check_database.py
echo ""

read -p "Continue with geocoding on RDS? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]
then
    echo "🔍 Checking geocoding status on RDS..."
    python3 check_geocoding_status.py
    echo ""
    
    read -p "Start geocoding all providers on RDS? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]
    then
        echo "🚀 Starting geocoding on RDS..."
        python3 manage.py geocode_providers --all
        echo ""
        echo "✅ Geocoding complete!"
        echo ""
        echo "🔍 Verifying results..."
        python3 verify_geocoded.py
    else
        echo "❌ Geocoding cancelled"
    fi
else
    echo "❌ Operation cancelled"
fi

# Clean up environment variables
unset DB_HOST DB_NAME DB_USER DB_PASSWORD DB_SSL_REQUIRE

