# CHLA Provider Map - Fixed Deployment Configuration

## ✅ SIMPLE WORKING SETUP

### Production URLs:
- **Frontend**: https://kinddhelp.com (via CloudFront → S3)
- **API**: https://api.kinddhelp.com (direct to Elastic Beanstalk)

### Local Development:
- **Frontend**: http://localhost:3000
- **Backend**: http://127.0.0.1:8000

## 🚀 DEPLOYMENT COMMANDS

### Deploy Backend (Django → Elastic Beanstalk)
```bash
cd maplocation
eb use chla-api-env-v2 --profile personal --region us-west-2
eb deploy --profile personal --region us-west-2
```

### Deploy Frontend (Vue.js → S3/CloudFront)
```bash
cd map-frontend

# Build with production API URL
echo 'VITE_API_BASE_URL=https://api.kinddhelp.com' > .env.production
echo 'VITE_MAPBOX_TOKEN=pk.eyJ1IjoiYmVhdHR5LWFkbWluIiwiYSI6ImNsejFjNGt0YzFqMGMyanF3YW5hdWFmc3UifQ.sn7Uj_gDzzKL6PQq7vO7fw' >> .env.production
npm run build

# Deploy to S3
aws s3 sync dist/ s3://kinddhelp-frontend-1755148345 --delete --profile personal --region us-west-2

# Clear CloudFront cache
aws cloudfront create-invalidation --distribution-id E2W6EECHUV4LMM --paths "/*" --profile personal
```

## 📁 ENVIRONMENT FILES

### Local Development (.env.development)
```
VITE_API_BASE_URL=http://127.0.0.1:8000
VITE_PORT=3000
VITE_MAPBOX_TOKEN=pk.eyJ1IjoiYmVhdHR5LWFkbWluIiwiYSI6ImNsejFjNGt0YzFqMGMyanF3YW5hdWFmc3UifQ.sn7Uj_gDzzKL6PQq7vO7fw
```

### Production (.env.production)
```
VITE_API_BASE_URL=https://api.kinddhelp.com
VITE_MAPBOX_TOKEN=pk.eyJ1IjoiYmVhdHR5LWFkbWluIiwiYSI6ImNsejFjNGt0YzFqMGMyanF3YW5hdWFmc3UifQ.sn7Uj_gDzzKL6PQq7vO7fw
```

## 🔧 REQUIRED SETUP

### 1. DNS Configuration (Route 53)
- `kinddhelp.com` → CloudFront (E2W6EECHUV4LMM)
- `www.kinddhelp.com` → CloudFront (E2W6EECHUV4LMM)
- `api.kinddhelp.com` → Elastic Beanstalk (chla-api-env-v2.eba-9aiqcppx.us-west-2.elasticbeanstalk.com)

### 2. Elastic Beanstalk Environment Variables
```bash
eb setenv \
  DJANGO_DEBUG=false \
  ALLOWED_HOSTS='api.kinddhelp.com,chla-api-env-v2.eba-9aiqcppx.us-west-2.elasticbeanstalk.com,.elasticbeanstalk.com' \
  CORS_ALLOWED_ORIGINS='https://kinddhelp.com,https://www.kinddhelp.com,http://localhost:3000' \
  CSRF_TRUSTED_ORIGINS='https://api.kinddhelp.com,https://kinddhelp.com' \
  DB_NAME=postgres \
  DB_USER=chla_admin \
  DB_PASSWORD=CHLASecure2024 \
  DB_HOST=chla-postgres-db.cpkvcu4f59w6.us-west-2.rds.amazonaws.com \
  DB_PORT=5432 \
  DB_SSL_REQUIRE=true \
  DJANGO_SECRET_KEY='k^v1yy9u1z+ztuj9wg))si(8q5s8%7k3#aorgm78jqwy@k@kg#' \
  --profile personal --region us-west-2
```

### 3. CloudFront Configuration
- Keep it SIMPLE: Only serve static frontend files
- NO backend routing through CloudFront
- Origin: S3 bucket `kinddhelp-frontend-1755148345`

## 🧹 CLEANUP NEEDED

### Files to Archive:
```bash
mkdir -p archive/deployment-attempts
mv cloudfront-update.json archive/deployment-attempts/
mv update_cloudfront.py archive/deployment-attempts/
mv check_cloudfront.sh archive/deployment-attempts/
mv current-config.json archive/deployment-attempts/
mv updated-config.json archive/deployment-attempts/
```

### S3 Buckets to Remove:
- `chla-frontend-1755700706` (created today)
- Keep only: `kinddhelp-frontend-1755148345` (original)

## ✅ FINAL CHECKLIST

1. [ ] Ensure `api.kinddhelp.com` DNS points to EB
2. [ ] Update frontend to use `https://api.kinddhelp.com`
3. [ ] Deploy frontend to original S3 bucket
4. [ ] Remove CloudFront backend routing
5. [ ] Test everything works
6. [ ] Archive unnecessary files

## 📝 NOTES

- KISS: Keep It Simple, Stupid
- Don't route API through CloudFront - use subdomain
- One S3 bucket for frontend
- Clear separation of concerns
- This is how it worked 5 days ago!
