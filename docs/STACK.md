# KiNDD - NDD Resource Navigator - Complete Stack Documentation

## Known Data Issues

### Regional Center ZIP Code Coverage

- **Issue**: ZIP 91403 (Sherman Oaks) is not mapped to any Regional Center in the database
- **Impact**: Users searching for this ZIP code will see "Regional Center (Not Found)"
- **Expected RC**: North Los Angeles County Regional Center
- **Status**: Awaiting data update from user
- **API Endpoint**: `/api/regional-centers/service_area_boundaries/` returns all RC polygons with their ZIP codes
- **Verification**: `curl "http://127.0.0.1:8000/api/regional-centers/lookup_by_zip/?zip_code=91403"` returns 404

## Architecture Overview

### Frontend

- **Framework**: Vue 3 with Vite
- **UI Library**: Bootstrap 5 + Custom CSS
- **Map**: Mapbox GL JS
- **Hosting**: AWS S3 + CloudFront
- **URL**: <https://kinddhelp.com>
- **S3 Bucket**: kinddhelp-frontend-1755148345
- **CloudFront ID**: E2W6EECHUV4LMM

### Backend

- **Framework**: Django 5.2 with Django REST Framework
- **Language**: Python 3.12
- **Hosting**: AWS Elastic Beanstalk
- **Environment**: chla-api-docker2
- **URL**: <https://api.kinddhelp.com>
- **SSL Certificate**: arn:aws:acm:us-west-2:795519544722:certificate/77514a62-6636-4fdb-8360-863aa711859e

### Database

- **Type**: PostgreSQL (AWS RDS)
- **Host**: chla-postgres-db.cpkvcu4f59w6.us-west-2.rds.amazonaws.com
- **Database Name**: postgres
- **Username**: chla_admin

## Project Structure

```
/CHLAProj/
├── map-frontend/           # Vue.js frontend application
│   ├── src/
│   │   ├── views/         # Vue components
│   │   ├── services/      # API services
│   │   └── assets/        # Static assets
│   ├── dist/              # Built files for deployment
│   ├── package.json       # Node dependencies
│   ├── vite.config.js     # Vite configuration
│   └── switch-env.sh      # Environment switcher script
├── maplocation/           # Django backend application
│   ├── locations/         # Main app with models/views
│   ├── users/            # User authentication app
│   ├── maplocation/      # Django settings
│   ├── manage.py         # Django management script
│   ├── requirements.txt  # Python dependencies
│   ├── Procfile          # EB deployment config
│   └── .ebextensions/    # EB configuration files
└── .venv/                # Python virtual environment
```

## Local Development

### Backend Setup

```bash
cd maplocation
source venv/bin/activate  # Activate Python virtual environment
pip install -r requirements.txt
python3 manage.py runserver 127.0.0.1:8000
```

### Frontend Setup

```bash
cd map-frontend
npm install
./switch-env.sh dev  # Switch to development environment
npm run dev          # Starts on http://localhost:3000
```

### Environment Switching

The `switch-env.sh` script manages environment variables:

- `./switch-env.sh dev` - Points to local backend (<http://127.0.0.1:8000>)
- `./switch-env.sh prod` - Points to production backend (<https://api.kinddhelp.com>)

## Deployment Process

### Deploy Backend (Django → Elastic Beanstalk)

```bash
cd maplocation
eb deploy --profile personal --region us-west-2
```

### Deploy Frontend (Vue → S3/CloudFront)

```bash
cd map-frontend
./switch-env.sh prod     # Switch to production URLs
npm run build           # Build for production
aws s3 sync dist/ s3://kinddhelp-frontend-1755148345 --delete --profile personal --region us-west-2
aws cloudfront create-invalidation --distribution-id E2W6EECHUV4LMM --paths "/*" --profile personal
```

## Environment Variables

### Backend (Elastic Beanstalk)

- `DJANGO_SECRET_KEY`: Django secret key
- `DJANGO_DEBUG`: false (in production)
- `ALLOWED_HOSTS`: api.kinddhelp.com,.elasticbeanstalk.com,localhost
- `CORS_ALLOWED_ORIGINS`: <https://kinddhelp.com,https://www.kinddhelp.com>
- `DB_HOST`: RDS endpoint
- `DB_NAME`: postgres
- `DB_USER`: chla_admin
- `DB_PASSWORD`: [stored in EB]
- `DB_SSL_REQUIRE`: true

### Frontend (Vite)

- `VITE_API_BASE_URL`: API endpoint URL
- `VITE_MAPBOX_TOKEN`: Mapbox access token

## API Endpoints

### Main Endpoints

- `/api/providers-v2/` - Provider listings
- `/api/providers-v2/comprehensive_search/` - Provider search with filters
- `/api/regional-centers/` - Regional center listings
- `/api/regional-centers/service_area_boundaries/` - Service area GeoJSON
- `/api/regional-centers/by_zip_code/` - Find regional center by ZIP
- `/api/users/auth/login/` - User authentication

## Key Features

1. **Provider Map**: Shows healthcare providers with filtering
2. **Regional Centers**: Display service areas and boundaries
3. **Search**: Location-based search with radius
4. **Filtering**: Insurance, languages, specializations
5. **User Authentication**: Client portal access

## Common Issues & Solutions

### 502 Bad Gateway Errors

- **Cause**: Usually unhealthy EB environment or SSL issues
- **Fix**: Check EB health, ensure ALLOWED_HOSTS includes load balancer IPs

### CORS Errors

- **Cause**: Frontend/backend domain mismatch
- **Fix**: Update CORS_ALLOWED_ORIGINS in EB environment variables

### Health Check Failures

- **Cause**: Django not responding to ELB health checks
- **Fix**: Ensure ALLOWED_HOSTS includes internal IPs (172.31.x.x)

### Mixed Content Warnings

- **Cause**: HTTP/HTTPS mismatch
- **Fix**: Ensure all URLs use HTTPS in production

## Database Management

### Local to RDS Sync

```bash
cd maplocation
python3 manage.py makemigrations
python3 manage.py migrate
git add .
git commit -m "Add migrations"
git push origin main  # Auto-deploys with migrations
```

### Connect to RDS

```bash
psql -h chla-postgres-db.cpkvcu4f59w6.us-west-2.rds.amazonaws.com -U chla_admin -d postgres
```

## Debugging Commands

### Check Backend Status

```bash
eb status --profile personal --region us-west-2
eb health --profile personal --region us-west-2
eb logs --profile personal --region us-west-2
```

### Test API Endpoints

```bash
# Test HTTPS
curl -I https://api.kinddhelp.com/api/regional-centers/

# Test with auth
curl -H "Authorization: Token YOUR_TOKEN" https://api.kinddhelp.com/api/providers-v2/
```

### View CloudFront Logs

```bash
aws cloudfront get-distribution --id E2W6EECHUV4LMM --profile personal
```

## Quick Reference

### URLs

- **Production Frontend**: <https://kinddhelp.com>
- **Production API**: <https://api.kinddhelp.com>
- **S3 Direct**: <http://kinddhelp-frontend-1755148345.s3-website-us-west-2.amazonaws.com>
- **EB Direct**: <https://chla-api-docker2.eba-9aiqcppx.us-west-2.elasticbeanstalk.com>

### AWS Resources

- **Region**: us-west-2
- **AWS Profile**: personal
- **EB Application**: chla-api
- **EB Environment**: chla-api-docker2
- **RDS Instance**: chla-postgres-db

## Pro Tips

1. **Always use `python3`** (not `python`) for macOS compatibility [[memory:6681121]]
2. **Use `./switch-env.sh`** to manage environment switching
3. **Hard refresh** (Cmd+Shift+R) after frontend deployments
4. **Check EB health** before assuming API issues
5. **CloudFront invalidation** takes 5-15 minutes
6. **S3 direct URL** is useful for testing without CloudFront

## Emergency Fixes

### Backend Down

1. Check EB health: `eb health`
2. Redeploy: `eb deploy`
3. Check logs: `eb logs`

### Frontend Not Updating

1. Clear CloudFront cache
2. Check S3 sync completed
3. Use S3 direct URL to bypass cache

### Database Connection Issues

1. Check RDS security groups
2. Verify DB_SSL_REQUIRE=true
3. Check VPC settings
