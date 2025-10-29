#!/bin/bash

# Sync AWS RDS to Local Database
# This script exports data from RDS and imports to local PostgreSQL
# Use this to pull production data to local for development

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && cd .. && pwd)"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="$SCRIPT_DIR/maplocation/backups"
EXPORT_FILE="$BACKUP_DIR/rds_export_$TIMESTAMP.json"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}╔══════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║   Database Sync: AWS RDS → Local                     ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════════════════════╝${NC}"
echo ""

mkdir -p "$BACKUP_DIR"

# Step 1: Load production environment
echo -e "${YELLOW}1. Loading production environment...${NC}"
if [ ! -f "$SCRIPT_DIR/.env.production" ]; then
    echo -e "${RED}Error: .env.production not found${NC}"
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

# Step 2: Export from RDS
echo -e "${YELLOW}2. Exporting data from RDS...${NC}"
cd "$SCRIPT_DIR/maplocation"
export DB_HOST=$RDS_DB_HOST
export DB_PORT=$RDS_DB_PORT
export DB_NAME=$RDS_DB_NAME
export DB_USER=$RDS_DB_USER
export DB_PASSWORD=$RDS_DB_PASSWORD
export DB_SSL_REQUIRE=true

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

echo -e "${GREEN}✓ Exported from RDS: $EXPORT_FILE${NC}"
echo ""

# Step 3: Load local environment
echo -e "${YELLOW}3. Loading local environment...${NC}"
if [ ! -f "$SCRIPT_DIR/.env.local" ]; then
    echo -e "${RED}Error: .env.local not found${NC}"
    exit 1
fi
set -a
source "$SCRIPT_DIR/.env.local"
set +a
echo -e "${GREEN}✓ Local environment loaded${NC}"
echo ""

# Step 4: Apply migrations locally
echo -e "${YELLOW}4. Applying migrations to local database...${NC}"
python3 manage.py migrate --database default
echo -e "${GREEN}✓ Migrations applied${NC}"
echo ""

# Step 5: Import to local
echo -e "${YELLOW}5. Importing data to local database...${NC}"
echo "This will overwrite existing local data"
read -p "Continue? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Sync cancelled"
    exit 0
fi

python3 manage.py loaddata "$EXPORT_FILE"
echo -e "${GREEN}✓ Data imported to local database${NC}"
echo ""

# Step 6: Verify
echo -e "${YELLOW}6. Verifying sync...${NC}"
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

echo -e "${GREEN}✓ Local database now has:${NC}"
echo "  - Providers: $PROVIDER_COUNT"
echo "  - Regional Centers: $RC_COUNT"
echo ""

echo -e "${GREEN}╔══════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║   Sync Complete!                                     ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════════════════════╝${NC}"
echo ""
echo "Export saved: $EXPORT_FILE"

