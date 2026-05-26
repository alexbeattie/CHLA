#!/bin/bash
# Sync data FROM RDS (production) TO local database
# RDS is the source of truth

set -e # Exit on error

echo "Sync FROM RDS TO Local Database"
echo "=================================="
echo ""
echo " WARNING: This will overwrite your LOCAL database with RDS data!"
echo ""

# RDS Configuration — fetched from AWS Secrets Manager (kindd/prod/rds JSON blob)
SECRET_JSON=$(aws secretsmanager get-secret-value \
    --secret-id kindd/prod/rds \
    --query SecretString --output text \
    --region "${AWS_REGION:-us-west-2}")
RDS_HOST=$(echo "$SECRET_JSON" | python3 -c "import sys,json;print(json.load(sys.stdin)['host'])")
RDS_DB=$(echo "$SECRET_JSON" | python3 -c "import sys,json;print(json.load(sys.stdin)['dbname'])")
RDS_USER=$(echo "$SECRET_JSON" | python3 -c "import sys,json;print(json.load(sys.stdin)['username'])")
RDS_PASSWORD=$(echo "$SECRET_JSON" | python3 -c "import sys,json;print(json.load(sys.stdin)['password'])")
unset SECRET_JSON

# Local Configuration
LOCAL_HOST="localhost"
LOCAL_DB="shafali"
LOCAL_USER="alexbeattie"
LOCAL_PASSWORD="" # Usually empty for local

# Temporary files
DUMP_FILE="rds_dump_$(date +%Y%m%d_%H%M%S).sql"
DATA_ONLY_FILE="rds_data_$(date +%Y%m%d_%H%M%S).sql"

echo "Source (RDS):"
echo " Host: $RDS_HOST"
echo " Database: $RDS_DB"
echo ""
echo "Target (Local):"
echo " Host: $LOCAL_HOST"
echo " Database: $LOCAL_DB"
echo ""

read -p "Continue? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]
then
    echo "Sync cancelled"
    exit 1
fi

echo ""
echo "Step 1: Dumping data from RDS..."
echo " (This may take a minute...)"

# Dump ONLY data (not schema) from RDS
PGPASSWORD="$RDS_PASSWORD" pg_dump \
    -h "$RDS_HOST" \
    -U "$RDS_USER" \
    -d "$RDS_DB" \
    --data-only \
    --no-owner \
    --no-privileges \
    -f "$DATA_ONLY_FILE"

if [ $? -eq 0 ]; then
    echo " RDS data dumped to: $DATA_ONLY_FILE"
    FILE_SIZE=$(du -h "$DATA_ONLY_FILE" | cut -f1)
    echo " File size: $FILE_SIZE"
else
    echo " Failed to dump RDS data"
    exit 1
fi

echo ""
echo "Step 2: Restoring data to local database..."
echo " This will replace existing data in: $LOCAL_DB"
echo ""

read -p "Continue with restore? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]
then
    echo "Restore cancelled (dump file saved: $DATA_ONLY_FILE)"
    exit 1
fi

# Restore to local database
if [ -z "$LOCAL_PASSWORD" ]; then
    psql -h "$LOCAL_HOST" -U "$LOCAL_USER" -d "$LOCAL_DB" -f "$DATA_ONLY_FILE"
else
    PGPASSWORD="$LOCAL_PASSWORD" psql -h "$LOCAL_HOST" -U "$LOCAL_USER" -d "$LOCAL_DB" -f "$DATA_ONLY_FILE"
fi

if [ $? -eq 0 ]; then
    echo ""
    echo "Data restored successfully!"
else
    echo ""
    echo "Failed to restore data"
    echo " Dump file saved: $DATA_ONLY_FILE"
    exit 1
fi

echo ""
echo "Cleanup..."
read -p "Delete dump file? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]
then
    rm "$DATA_ONLY_FILE"
    echo " Dump file deleted"
else
    echo " Dump file kept: $DATA_ONLY_FILE"
fi

echo ""
echo "=" * 60
echo "SYNC COMPLETE!"
echo "=" * 60
echo ""
echo "Your local database now matches RDS (production)"
echo ""
echo "To verify, run:"
echo " cd /Users/alexbeattie/Developer/CHLA/maplocation"
echo " source venv/bin/activate"
echo " python3 verify_geocoded.py"
echo ""

