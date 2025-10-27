# CHLA Provider Map - Getting Started Guide

A full-stack web application for mapping and managing healthcare providers, built with Django 5.2 (Python 3.12) REST API backend and Vue 3 frontend.

## Table of Contents

- [Quick Start](#quick-start)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [Project Structure](#project-structure)
- [Development Workflow](#development-workflow)
- [API Documentation](#api-documentation)
- [Deployment](#deployment)
- [Troubleshooting](#troubleshooting)

---

## Quick Start

```bash
# 1. Clone and navigate to the project
cd /path/to/maplocation

# 2. Create and activate virtual environment
python3.12 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# 3. Install dependencies
pip install -r requirements.txt

# 4. Set up PostgreSQL database (see Configuration section)

# 5. Run migrations
python manage.py migrate

# 6. Create superuser for admin access
python manage.py createsuperuser

# 7. Start development server
python manage.py runserver

# Server will be available at http://localhost:8000
```

---

## Prerequisites

### Required Software

- **Python 3.12** (3.13+ supported but uses different PostgreSQL driver)
- **PostgreSQL 12+** (local or cloud instance)
- **Git** for version control
- **pip** for package management

### Recommended Tools

- **virtualenv** or **venv** for isolated Python environments
- **PostgreSQL GUI** (pgAdmin, TablePlus, etc.) for database management
- **Postman** or **curl** for API testing

---

## Installation

### 1. Clone the Repository

```bash
git clone <repository-url>
cd maplocation
```

### 2. Create Virtual Environment

```bash
# Using venv (built-in)
python3.12 -m venv venv

# Activate the environment
source venv/bin/activate  # macOS/Linux
# OR
venv\Scripts\activate  # Windows
```

### 3. Install Python Dependencies

```bash
pip install -r requirements.txt
```

**Key dependencies installed:**
- Django 5.2 - Web framework
- Django REST Framework 3.15.2 - API framework
- psycopg2/psycopg - PostgreSQL database adapter
- django-cors-headers - CORS support
- graphene-django - GraphQL support
- gunicorn - Production WSGI server
- whitenoise - Static file serving

### 4. Set Up PostgreSQL Database

**Option A: Local PostgreSQL**

```bash
# Install PostgreSQL (macOS)
brew install postgresql@16
brew services start postgresql@16

# Create database
createdb chla_provider_map

# Create user (optional)
psql postgres
CREATE USER chla_admin WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE chla_provider_map TO chla_admin;
```

**Option B: Use existing AWS RDS instance** (see Configuration section)

---

## Configuration

### Environment Variables

The application uses environment variables for configuration. Set these in your shell or create a `.env` file (not tracked in Git).

#### Required Variables

```bash
# Database Configuration
export DB_NAME="chla_provider_map"           # Database name
export DB_USER="your_username"               # Database user
export DB_PASSWORD="your_password"           # Database password
export DB_HOST="localhost"                   # Database host
export DB_PORT="5432"                        # Database port

# Django Security
export DJANGO_SECRET_KEY="your-secret-key-here"  # Generate with: python -c 'from django.core.management.utils import get_random_secret_key; print(get_random_secret_key())'
export DJANGO_DEBUG="true"                       # Set to "false" in production
```

#### Optional Variables (with defaults)

```bash
# Admin Portal Basic Auth (default: clientaccess / changeme123!)
export BASIC_AUTH_USERNAME="admin"
export BASIC_AUTH_PASSWORD="secure_password"

# Allowed Hosts (default: * in dev)
export ALLOWED_HOSTS="localhost,127.0.0.1"

# CORS Configuration (defaults include localhost:3000-3003)
export CORS_ALLOWED_ORIGINS="http://localhost:3000,https://yourdomain.com"

# CSRF Configuration
export CSRF_TRUSTED_ORIGINS="https://yourdomain.com"

# AWS RDS SSL (for production)
export DB_SSL_REQUIRE="true"
```

#### Default Development Settings

If not set, the application defaults to:
- `DB_NAME`: "shafali"
- `DB_USER`: "alexbeattie"
- `DB_HOST`: "localhost"
- `DB_PORT`: "5432"
- `ALLOWED_HOSTS`: "*" (all hosts allowed)
- CORS Origins: localhost ports 3000-3003, kinddhelp.com

### Generate a Secret Key

```bash
python -c 'from django.core.management.utils import get_random_secret_key; print(get_random_secret_key())'
```

---

## Running the Application

### 1. Apply Database Migrations

```bash
python manage.py migrate
```

This creates all necessary database tables based on 15 migration files in `locations/migrations/`.

### 2. Create a Superuser (Admin Access)

```bash
python manage.py createsuperuser
```

Follow the prompts to create an admin account.

### 3. Collect Static Files (for production)

```bash
python manage.py collectstatic --noinput
```

### 4. Start Development Server

```bash
python manage.py runserver

# Or specify port
python manage.py runserver 8080
```

Access the application at:
- **API Root**: http://localhost:8000/api/
- **Admin Portal**: http://localhost:8000/client-portal/ (requires Basic Auth + superuser)
- **GraphQL**: http://localhost:8000/graphql/

---

## Project Structure

```
maplocation/
├── maplocation/              # Django project configuration
│   ├── settings.py          # Main settings file
│   ├── urls.py              # Root URL configuration
│   ├── wsgi.py              # WSGI entry point for production
│   ├── middleware.py        # Custom middleware (Basic Auth)
│   └── schema.py            # GraphQL schema
│
├── locations/               # Main app - Provider/Location management
│   ├── models.py           # Data models (Provider, RegionalCenter, etc.)
│   ├── views.py            # API viewsets and endpoints
│   ├── serializers.py      # REST API serializers
│   ├── admin.py            # Django admin configuration
│   ├── urls.py             # API routing
│   └── management/         # 19 custom management commands
│       └── commands/
│           ├── import_chla_data.py
│           ├── populate_la_regional_centers.py
│           ├── geocode_providers.py
│           └── ...
│
├── users/                   # User authentication and profiles
│   ├── models.py           # UserProfile model
│   ├── views.py            # Auth views
│   └── serializers.py      # User serializers
│
├── templates/               # HTML templates
├── static/                  # Static assets (CSS, JS, images)
├── staticfiles/            # Collected static files (generated)
│
├── .ebextensions/          # AWS Elastic Beanstalk deployment configs
│   ├── 01_auto_migrate.config
│   └── https.config
│
├── .elasticbeanstalk/      # EB CLI configuration
├── requirements.txt        # Python dependencies
├── manage.py              # Django management CLI
├── Procfile               # Deployment process configuration
└── db.sqlite3             # Local SQLite DB (for testing)
```

### Key Applications

- **locations**: Core business logic for providers, locations, regional centers, service areas
- **users**: User authentication, profiles, and diagnosis tracking
- **maplocation**: Project-wide settings, URL routing, middleware

---

## Development Workflow

### Running Management Commands

The project includes 19 custom management commands for data operations:

```bash
# Import provider data
python manage.py import_chla_data

# Geocode provider addresses
python manage.py geocode_providers

# Populate regional center data
python manage.py populate_la_regional_centers
python manage.py update_orange_county_zips
python manage.py populate_harbor_zips

# Generate service area boundaries
python manage.py generate_service_areas

# Emergency data population
python manage.py emergency_populate
```

List all available commands:
```bash
python manage.py help
```

### Creating New Migrations

After modifying models:

```bash
# Create migration files
python manage.py makemigrations

# Apply migrations
python manage.py migrate
```

### Running the Django Shell

```bash
python manage.py shell

# Example: Query providers
from locations.models import ProviderV2
providers = ProviderV2.objects.all()
print(providers.count())
```

### Database Management

```bash
# View current migrations
python manage.py showmigrations

# Roll back migration
python manage.py migrate locations 0014  # Migrate to specific version

# Dump data to JSON
python manage.py dumpdata locations.ProviderV2 > providers_backup.json

# Load data from JSON
python manage.py loaddata providers_backup.json
```

---

## API Documentation

### Base URL

- **Development**: http://localhost:8000/api/
- **Production**: https://api.kinddhelp.com/api/ (or your configured domain)

### Authentication

The API uses **Token Authentication**:

```bash
# Obtain token (create endpoint if needed)
curl -X POST http://localhost:8000/api/users/auth/login/ \
  -H "Content-Type: application/json" \
  -d '{"username": "user", "password": "pass"}'

# Use token in requests
curl http://localhost:8000/api/providers/ \
  -H "Authorization: Token YOUR_TOKEN_HERE"
```

### Main API Endpoints

#### Providers (V2)

```bash
# List all providers
GET /api/providers/

# Filter providers
GET /api/providers/?regional_center=1&service_delivery_model=In-Person

# Get provider by ID
GET /api/providers/{id}/

# Find nearby providers (custom action)
GET /api/providers/nearby/?lat=34.0522&lng=-118.2437&radius=10

# Create provider (requires authentication)
POST /api/providers/
Content-Type: application/json
{
  "name": "Provider Name",
  "address": "123 Main St",
  "city": "Los Angeles",
  "state": "CA",
  "zip_code": "90001",
  ...
}
```

#### Reference Data

```bash
# Location categories
GET /api/categories/

# Regional centers
GET /api/regional-centers/

# Funding sources
GET /api/funding-sources/

# Insurance carriers
GET /api/insurance-carriers/

# Service delivery models
GET /api/service-models/

# California counties (GeoJSON)
GET /api/california-counties/
```

#### Legacy Endpoints

```bash
# Legacy providers (V1)
GET /api/providers-legacy/

# Legacy locations
GET /api/locations/
```

### Query Parameters

The API supports filtering, pagination, and ordering:

```bash
# Filtering
?regional_center=1&accepts_insurance=true

# Pagination
?page=2&page_size=20

# Ordering
?ordering=name  # Ascending
?ordering=-created_at  # Descending

# Search
?search=autism
```

### GraphQL Endpoint

Access GraphQL at `/graphql/` for more flexible queries:

```graphql
query {
  allProviders {
    edges {
      node {
        id
        name
        address
        city
      }
    }
  }
}
```

---

## Deployment

### AWS Elastic Beanstalk (Production)

The application is configured for AWS EB deployment:

```bash
# Initialize EB CLI (if not done)
eb init

# Deploy to production
git push origin main  # Triggers auto-deployment

# Or deploy directly
eb deploy chla-api-prod

# Check status
eb status

# View logs
eb logs
```

### Environment Configuration

Set production environment variables in EB console or using EB CLI:

```bash
eb setenv DB_NAME=production_db \
  DB_USER=admin \
  DB_PASSWORD=secure_password \
  DB_HOST=your-rds-endpoint.amazonaws.com \
  DB_SSL_REQUIRE=true \
  DJANGO_DEBUG=false \
  DJANGO_SECRET_KEY=your-production-secret-key
```

### Auto-Deploy Features

The `.ebextensions/01_auto_migrate.config` automatically:
- Runs database migrations on deploy
- Collects static files
- Restarts the application

### Production Checklist

- [ ] Set `DJANGO_DEBUG=false`
- [ ] Configure strong `DJANGO_SECRET_KEY`
- [ ] Set specific `ALLOWED_HOSTS`
- [ ] Configure `CORS_ALLOWED_ORIGINS` for frontend domains
- [ ] Enable database SSL (`DB_SSL_REQUIRE=true`)
- [ ] Update `BASIC_AUTH_USERNAME` and `BASIC_AUTH_PASSWORD`
- [ ] Run `collectstatic` before deployment
- [ ] Test migrations in staging environment
- [ ] Set up CloudWatch logging
- [ ] Configure backup strategy for RDS

---

## Troubleshooting

### Database Connection Issues

**Problem**: `psycopg2.OperationalError: could not connect to server`

**Solutions**:
```bash
# Check PostgreSQL is running
brew services list  # macOS
sudo systemctl status postgresql  # Linux

# Verify environment variables
echo $DB_HOST
echo $DB_NAME

# Test connection manually
psql -h localhost -U your_user -d your_database
```

### GDAL/GIS Issues

**Note**: GeoDjango (`django.contrib.gis`) is disabled in this project due to GDAL installation complexity.

If you see GDAL-related errors, they are likely from commented-out dependencies. The project uses standard decimal coordinates instead.

### Migration Conflicts

**Problem**: Migration conflicts after pulling changes

```bash
# Show current state
python manage.py showmigrations

# If stuck, reset migrations (DEVELOPMENT ONLY)
python manage.py migrate locations zero
python manage.py migrate locations

# Or create merge migration
python manage.py makemigrations --merge
```

### Static Files Not Loading

```bash
# Collect static files
python manage.py collectstatic --clear --noinput

# Check STATIC_ROOT setting
python manage.py shell
>>> from django.conf import settings
>>> print(settings.STATIC_ROOT)
```

### Admin Portal Basic Auth Issues

**Problem**: Can't access `/client-portal/`

The admin portal requires **two authentication layers**:
1. HTTP Basic Auth (username/password set via env vars)
2. Django superuser login

```bash
# Check Basic Auth credentials
echo $BASIC_AUTH_USERNAME  # default: clientaccess
echo $BASIC_AUTH_PASSWORD  # default: changeme123!

# Create superuser if needed
python manage.py createsuperuser
```

### CORS Errors

**Problem**: Frontend can't access API

```bash
# Verify CORS settings
python manage.py shell
>>> from django.conf import settings
>>> print(settings.CORS_ALLOWED_ORIGINS)

# Add frontend URL to environment
export CORS_ALLOWED_ORIGINS="http://localhost:3000,http://localhost:3001"
```

### Token Authentication Issues

**Problem**: 401 Unauthorized

```bash
# Create token for user
python manage.py shell
>>> from django.contrib.auth.models import User
>>> from rest_framework.authtoken.models import Token
>>> user = User.objects.get(username='your_username')
>>> token = Token.objects.create(user=user)
>>> print(token.key)
```

---

## Additional Resources

- **Full Stack Documentation**: See `../STACK_DOCUMENTATION.md` for complete architecture
- **Admin Security**: See `ADMIN_SECURITY_OPTIONS.md` for security implementations
- **Quick Admin Setup**: See `QUICK_ADMIN_SECURITY.md`
- **Manual Sync Commands**: See `manual_sync_commands.md` for database sync procedures

### Useful Django Commands

```bash
# Check for issues
python manage.py check

# Run tests (when test suite exists)
python manage.py test

# Start Python shell with Django context
python manage.py shell

# Database shell
python manage.py dbshell

# View all URLs
python manage.py show_urls  # Requires django-extensions
```

### Development Tips

1. **Use virtual environment** - Always activate `venv` before running commands
2. **Environment variables** - Consider using `python-dotenv` for local `.env` file management
3. **Database backups** - Regularly backup local database during development
4. **Migration workflow** - Always create migrations before modifying models
5. **Code style** - Follow PEP 8 guidelines for Python code
6. **Git workflow** - Commit migration files with model changes
7. **Testing locally** - Test all changes locally before pushing to production

---

## Getting Help

For issues or questions:

1. Check existing documentation in the project
2. Review Django 5.2 documentation: https://docs.djangoproject.com/
3. Review DRF documentation: https://www.django-rest-framework.org/
4. Check project commit history for context on recent changes
5. Contact the development team

---

## License

[Add your license information here]

## Contributors

[Add contributor information here]
