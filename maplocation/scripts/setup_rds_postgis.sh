#!/bin/bash
# Setup PostGIS on AWS RDS using AWS CLI
# This script enables PostGIS extension and runs the migration

set -e  # Exit on error

echo "🚀 PostGIS RDS Setup Script"
echo "================================"

# Check if required tools are installed
command -v aws >/dev/null 2>&1 || { echo "❌ AWS CLI not found. Install with: brew install awscli"; exit 1; }
command -v psql >/dev/null 2>&1 || { echo "❌ psql not found. Install with: brew install postgresql"; exit 1; }

# Get RDS instance identifier from environment or prompt
if [ -z "$RDS_INSTANCE_ID" ]; then
    echo ""
    echo "📋 Available RDS instances:"
    aws rds describe-db-instances --query 'DBInstances[*].[DBInstanceIdentifier,Engine,DBInstanceStatus]' --output table
    echo ""
    read -p "Enter RDS instance identifier: " RDS_INSTANCE_ID
fi

# Get RDS endpoint
echo ""
echo "🔍 Getting RDS endpoint..."
RDS_ENDPOINT=$(aws rds describe-db-instances \
    --db-instance-identifier "$RDS_INSTANCE_ID" \
    --query 'DBInstances[0].Endpoint.Address' \
    --output text)

if [ -z "$RDS_ENDPOINT" ] || [ "$RDS_ENDPOINT" = "None" ]; then
    echo "❌ Could not find RDS instance: $RDS_INSTANCE_ID"
    exit 1
fi

echo "✅ Found RDS endpoint: $RDS_ENDPOINT"

# Get database name
DB_NAME=$(aws rds describe-db-instances \
    --db-instance-identifier "$RDS_INSTANCE_ID" \
    --query 'DBInstances[0].DBName' \
    --output text)

if [ -z "$DB_NAME" ] || [ "$DB_NAME" = "None" ]; then
    read -p "Enter database name: " DB_NAME
fi

# Get master username
DB_USER=$(aws rds describe-db-instances \
    --db-instance-identifier "$RDS_INSTANCE_ID" \
    --query 'DBInstances[0].MasterUsername' \
    --output text)

echo ""
echo "📊 Connection Details:"
echo "  Endpoint: $RDS_ENDPOINT"
echo "  Database: $DB_NAME"
echo "  Username: $DB_USER"
echo ""

# Prompt for password
read -sp "Enter database password: " DB_PASSWORD
echo ""

# Test connection
echo ""
echo "🔌 Testing connection..."
PGPASSWORD="$DB_PASSWORD" psql -h "$RDS_ENDPOINT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT version();" > /dev/null 2>&1

if [ $? -eq 0 ]; then
    echo "✅ Connection successful!"
else
    echo "❌ Connection failed. Check your credentials and security groups."
    exit 1
fi

# Enable PostGIS extension
echo ""
echo "📦 Enabling PostGIS extension..."
PGPASSWORD="$DB_PASSWORD" psql -h "$RDS_ENDPOINT" -U "$DB_USER" -d "$DB_NAME" -c "CREATE EXTENSION IF NOT EXISTS postgis;" 2>&1

if [ $? -eq 0 ]; then
    echo "✅ PostGIS extension enabled!"
else
    echo "⚠️  PostGIS extension may already be enabled or you need rds_superuser role"
fi

# Verify PostGIS
echo ""
echo "🔍 Verifying PostGIS installation..."
POSTGIS_VERSION=$(PGPASSWORD="$DB_PASSWORD" psql -h "$RDS_ENDPOINT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT PostGIS_version();" 2>/dev/null | xargs)

if [ ! -z "$POSTGIS_VERSION" ]; then
    echo "✅ PostGIS installed: $POSTGIS_VERSION"
else
    echo "❌ PostGIS not available"
    exit 1
fi

# Run migration script
echo ""
read -p "Run PostGIS migration script? (y/n): " RUN_MIGRATION

if [ "$RUN_MIGRATION" = "y" ] || [ "$RUN_MIGRATION" = "Y" ]; then
    echo ""
    echo "🚀 Running migration script..."

    SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    MIGRATION_FILE="$SCRIPT_DIR/migrate_to_postgis.sql"

    if [ ! -f "$MIGRATION_FILE" ]; then
        echo "❌ Migration file not found: $MIGRATION_FILE"
        exit 1
    fi

    PGPASSWORD="$DB_PASSWORD" psql -h "$RDS_ENDPOINT" -U "$DB_USER" -d "$DB_NAME" -f "$MIGRATION_FILE"

    if [ $? -eq 0 ]; then
        echo "✅ Migration completed successfully!"
    else
        echo "❌ Migration failed"
        exit 1
    fi
fi

# Verify migration
echo ""
echo "📊 Verifying migration..."
PGPASSWORD="$DB_PASSWORD" psql -h "$RDS_ENDPOINT" -U "$DB_USER" -d "$DB_NAME" -c "
SELECT
    'providers_v2' as table_name,
    COUNT(*) as total_rows,
    COUNT(location) as with_location
FROM providers_v2
UNION ALL
SELECT
    'regional_centers' as table_name,
    COUNT(*) as total_rows,
    COUNT(location) as with_location
FROM regional_centers;
"

echo ""
echo "✅ PostGIS setup complete!"
echo ""
echo "Next steps:"
echo "  1. Update your .env with: DATABASE_URL=postgresql://$DB_USER:PASSWORD@$RDS_ENDPOINT:5432/$DB_NAME"
echo "  2. Deploy to Elastic Beanstalk: git push origin main (or eb deploy)"
echo "  3. Test spatial queries on production"
