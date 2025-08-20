#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}Map Location PostgreSQL Setup Script${NC}"
echo "This script will help set up the PostgreSQL database for the Map Location application."
echo ""

# Check if PostgreSQL is installed
if command -v psql &> /dev/null; then
    echo -e "${GREEN}PostgreSQL is installed.${NC}"
else
    echo -e "${RED}PostgreSQL is not installed. Please install it first.${NC}"
    echo "You can download it from: https://www.postgresql.org/download/"
    exit 1
fi

# Prompt for database credentials
echo ""
read -p "Enter database name [maplocation]: " DB_NAME
DB_NAME=${DB_NAME:-maplocation}

read -p "Enter database user [postgres]: " DB_USER
DB_USER=${DB_USER:-postgres}

read -p "Enter database password: " DB_PASSWORD

read -p "Enter database host [localhost]: " DB_HOST
DB_HOST=${DB_HOST:-localhost}

read -p "Enter database port [5432]: " DB_PORT
DB_PORT=${DB_PORT:-5432}

# Check if the user can connect to PostgreSQL
echo ""
echo "Checking PostgreSQL connection..."
if PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -c '\conninfo' &> /dev/null; then
    echo -e "${GREEN}Successfully connected to PostgreSQL.${NC}"
else
    echo -e "${RED}Failed to connect to PostgreSQL. Please check your credentials.${NC}"
    exit 1
fi

# Check if the database exists
echo ""
echo "Checking if database '$DB_NAME' exists..."
if PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -lqt | cut -d \| -f 1 | grep -qw $DB_NAME; then
    echo -e "${GREEN}Database '$DB_NAME' already exists.${NC}"
else
    echo "Creating database '$DB_NAME'..."
    if PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -c "CREATE DATABASE $DB_NAME;" &> /dev/null; then
        echo -e "${GREEN}Database '$DB_NAME' created successfully.${NC}"
    else
        echo -e "${RED}Failed to create database. Please check if you have sufficient privileges.${NC}"
        exit 1
    fi
fi

# Update Django settings
echo ""
echo "Updating Django settings.py to use PostgreSQL..."
SETTINGS_FILE="maplocation/settings.py"

# Backup the original settings file
cp $SETTINGS_FILE ${SETTINGS_FILE}.bak

# Update the settings file
sed -i.tmp "s/^DATABASES = {.*$/DATABASES = {/" $SETTINGS_FILE
sed -i.tmp "s/^    'default': {.*$/    'default': {/" $SETTINGS_FILE
sed -i.tmp "s/^        'ENGINE': 'django.db.backends.sqlite3',.*$/        'ENGINE': 'django.db.backends.postgresql',/" $SETTINGS_FILE
sed -i.tmp "s/^        'NAME': BASE_DIR \/ 'db.sqlite3',.*$/        'NAME': '$DB_NAME',/" $SETTINGS_FILE

# Add new lines
sed -i.tmp "/^        'NAME': '$DB_NAME',/a\\
        'USER': '$DB_USER',\\
        'PASSWORD': '$DB_PASSWORD',\\
        'HOST': '$DB_HOST',\\
        'PORT': '$DB_PORT',\\
" $SETTINGS_FILE

# Remove temporary file
rm ${SETTINGS_FILE}.tmp

echo -e "${GREEN}Django settings updated to use PostgreSQL.${NC}"

# Install psycopg2
echo ""
echo "Installing psycopg2-binary..."
pip install psycopg2-binary
echo -e "${GREEN}psycopg2-binary installed.${NC}"

# Run migrations
echo ""
echo "Running migrations..."
python manage.py migrate
echo -e "${GREEN}Migrations applied successfully.${NC}"

# Final instructions
echo ""
echo -e "${BLUE}PostgreSQL setup completed successfully!${NC}"
echo ""
echo "You can now start the Django server with:"
echo "  python manage.py runserver"
echo ""
echo "To access the application, visit:"
echo "  http://localhost:8000/"
echo ""
echo "To access the GraphQL interface, visit:"
echo "  http://localhost:8000/graphql/"
echo ""

# Make the script executable
chmod +x setup_postgres.sh