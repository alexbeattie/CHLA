#!/bin/bash

# Sync Local Database to AWS RDS
# This script exports data from local PostgreSQL and imports to RDS
# Use this to push local changes to production

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && cd .. && pwd)"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="$SCRIPT_DIR/maplocation/backups"
EXPORT_FILE="$BACKUP_DIR/local_export_$TIMESTAMP.json"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}╔══════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║   Database Sync: Local → AWS RDS                    ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════════════════════╝${NC}"
echo ""

# Create backup directory
mkdir -p "$BACKUP_DIR"

# Step 1: Load local environment
echo -e "${YELLOW}1. Loading local environment...${NC}"
if [ ! -f "$SCRIPT_DIR/.env.local" ]; then
    echo -e "${RED}Error: .env.local not found${NC}"
    exit 1
fi
set -a
source "$SCRIPT_DIR/.env.local"
set +a
LOCAL_DB_HOST=$DB_HOST
LOCAL_DB_PORT=$DB_PORT
LOCAL_DB_NAME=$DB_NAME
LOCAL_DB_USER=$DB_USER
echo -e "${GREEN}✓ Local environment loaded${NC}"
echo ""

# Step 2: Load production environment
echo -e "${YELLOW}2. Loading production environment...${NC}"
if [ ! -f "$SCRIPT_DIR/.env.production" ]; then
    echo -e "${RED}Error: .env.production not found${NC}"
    echo "Copy .env.production.example to .env.production and fill in your credentials"
    exit 1
fi
set -a
source "$SCRIPT_DIR/.env.production"
set +a
RDS_DB_HOST=$DB_HOST
RDS_DB_PORT=$DB_PORT
RDS_DB_NAME=$DB_NAME
RDS_DB_USER=$DB_USER
RDS_DB_PASSWORD=$DB_PASSWORD
echo -e "${GREEN}✓ Production environment loaded${NC}"
echo ""

# Step 3: Backup RDS (safety first!)
echo -e "${YELLOW}3. Creating RDS backup...${NC}"
read -p "Create RDS backup before sync? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    aws rds create-db-snapshot \
        --db-instance-identifier chla-postgres-db \
        --db-snapshot-identifier "manual-backup-$TIMESTAMP" \
        --region us-west-2 || echo "Backup failed (non-critical)"
    echo -e "${GREEN}✓ Backup initiated${NC}"
fi
echo ""

# Step 4: Export from local
echo -e "${YELLOW}4. Exporting data from local database...${NC}"
cd "$SCRIPT_DIR/maplocation"
python3 manage.py dumpdata \
    --natural-foreign \
    --natural-primary \
    --exclude contenttypes \
    --exclude auth.permission \
    --exclude admin.logentry \
    --exclude sessions.session \
    --indent 2 \
    locations.ProviderV2 \
    locations.RegionalCenter \
    locations.ProviderRegionalCenter \
    > "$EXPORT_FILE"

echo -e "${GREEN}✓ Exported to: $EXPORT_FILE${NC}"
echo ""

# Step 5: Test RDS connection
echo -e "${YELLOW}5. Testing RDS connection...${NC}"
export DB_HOST=$RDS_DB_HOST
export DB_PORT=$RDS_DB_PORT
export DB_NAME=$RDS_DB_NAME
export DB_USER=$RDS_DB_USER
export DB_PASSWORD=$RDS_DB_PASSWORD
export DB_SSL_REQUIRE=true

python3 manage.py check --database default > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ RDS connection successful${NC}"
else
    echo -e "${RED}✗ Cannot connect to RDS${NC}"
    exit 1
fi
echo ""

# Step 6: Apply migrations to RDS
echo -e "${YELLOW}6. Applying migrations to RDS...${NC}"
python3 manage.py migrate --database default
echo -e "${GREEN}✓ Migrations applied${NC}"
echo ""

# Step 7: Import data to RDS
echo -e "${YELLOW}7. Importing data to RDS...${NC}"
echo "This will overwrite existing data in RDS"
read -p "Continue? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Sync cancelled"
    exit 0
fi

python3 manage.py loaddata "$EXPORT_FILE"
echo -e "${GREEN}✓ Data imported to RDS${NC}"
echo ""

# Step 8: Verify sync
echo -e "${YELLOW}8. Verifying sync...${NC}"
PROVIDER_COUNT=$(python3 -c "
import os
import django
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'maplocation.settings')
django.setup()
from locations.models import ProviderV2
print(ProviderV2.objects.count())
")

RC_COUNT=$(python3 -c "
import os
import django
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'maplocation.settings')
django.setup()
from locations.models import RegionalCenter
print(RegionalCenter.objects.count())
")

echo -e "${GREEN}✓ RDS now has:${NC}"
echo "  - Providers: $PROVIDER_COUNT"
echo "  - Regional Centers: $RC_COUNT"
echo ""

echo -e "${GREEN}╔══════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║   Sync Complete!                                     ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════════════════════╝${NC}"
echo ""
echo "Backup file saved: $EXPORT_FILE"

