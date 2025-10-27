# CHLA Provider Map - Deployment Guide

## Local Development
```bash
# Backend
cd maplocation
source ../.venv/bin/activate
python3 manage.py runserver

# Frontend (new terminal)
cd map-frontend
npm run dev
```

## Production Deployment

### Backend (Django → Elastic Beanstalk)
```bash
cd maplocation
eb deploy --profile personal --region us-west-2
```

### Frontend (Vue → S3/CloudFront)
```bash
cd map-frontend
npm run build
aws s3 sync dist/ s3://kinddhelp-frontend-1755148345 --delete --profile personal --region us-west-2
aws cloudfront create-invalidation --distribution-id E2W6EECHUV4LMM --paths "/*" --profile personal
```

## URLs
- **Local**: http://localhost:3000 → http://127.0.0.1:8000
- **Production**: https://kinddhelp.com → https://api.kinddhelp.com

## Environment Files

### map-frontend/.env.development
```
VITE_API_BASE_URL=http://127.0.0.1:8000
VITE_PORT=3000
VITE_MAPBOX_TOKEN=pk.eyJ1IjoiYmVhdHR5LWFkbWluIiwiYSI6ImNsejFjNGt0YzFqMGMyanF3YW5hdWFmc3UifQ.sn7Uj_gDzzKL6PQq7vO7fw
```

### map-frontend/.env.production
```
VITE_API_BASE_URL=https://api.kinddhelp.com
VITE_MAPBOX_TOKEN=pk.eyJ1IjoiYmVhdHR5LWFkbWluIiwiYSI6ImNsejFjNGt0YzFqMGMyanF3YW5hdWFmc3UifQ.sn7Uj_gDzzKL6PQq7vO7fw
```

## AWS Resources
- **Backend**: chla-api-prod (Elastic Beanstalk)
- **Database**: chla-postgres-db (RDS PostgreSQL)
- **Frontend**: kinddhelp-frontend-1755148345 (S3)
- **CDN**: E2W6EECHUV4LMM (CloudFront)
