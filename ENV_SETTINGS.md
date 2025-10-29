# CHLA Provider Map - Environment Settings

**⚠️ SECURITY WARNING: Never commit files with real credentials to Git!**

## Quick Reference

### Local Development
```bash
# Copy template
cp .env.example .env.local

# Edit with your local settings
# Use Docker PostgreSQL: localhost:5433
# Or local PostgreSQL: localhost:5432
```

### Production (AWS)
```bash
# Copy template
cp .env.example .env.production

# Fill in production values (see below)
```

---

## Current Production Configuration

### 🗄️ Database (AWS RDS)
```bash
DB_HOST=chla-postgres-db.cpkvcu4f59w6.us-west-2.rds.amazonaws.com
DB_NAME=postgres
DB_USER=chla_admin
DB_PASSWORD=CHLASecure2024
DB_PORT=5432
DB_SSL_REQUIRE=true
```

**RDS Instance Info:**
- Instance ID: `chla-postgres-db`
- Engine: PostgreSQL with PostGIS
- Region: us-west-2
- Status: Available
- Addresses: 35.166.111.237, 52.26.226.218

### 🌐 API Backend (Elastic Beanstalk)
```bash
EB_ENVIRONMENT=chla-api-env
EB_APPLICATION=maplocation
AWS_REGION=us-west-2
BACKEND_URL=https://api.kinddhelp.com
```

**EB Environment Info:**
- Environment: `chla-api-env` (older) or `chla-api-prod` (newer)
- CNAME: chla-api-prod.eba-9aiqcppx.us-west-2.elasticbeanstalk.com
- Platform: Python 3.12 on Amazon Linux 2023

### 🎨 Frontend (S3 + CloudFront)
```bash
S3_BUCKET=kinddhelp-frontend-1755148345
CLOUDFRONT_DISTRIBUTION_ID=E2W6EECHUV4LMM
FRONTEND_URL=https://kinddhelp.com
```

### 🔐 Django Configuration
```bash
DJANGO_SECRET_KEY=k^v1yy9u1z+ztuj9wg))si(8q5s8%7k3#aorgm78jqwy@k@kg#
DJANGO_DEBUG=false
DJANGO_SETTINGS_MODULE=maplocation.settings
```

### 🛡️ CORS & Security
```bash
ALLOWED_HOSTS=api.kinddhelp.com,*.elasticbeanstalk.com,*
CORS_ALLOWED_ORIGINS=https://kinddhelp.com,https://www.kinddhelp.com
CSRF_TRUSTED_ORIGINS=https://api.kinddhelp.com,https://kinddhelp.com,https://www.kinddhelp.com
```

### 🔑 Basic Authentication (Admin Portal Gatekeeper)
```bash
BASIC_AUTH_USERNAME=clientaccess
BASIC_AUTH_PASSWORD=changeme123!
```

---

## Django Admin Users

### Superuser (Full Access)
```
Username: admin
Password: admin123
URL: http://localhost:8000/admin/ (local) or https://api.kinddhelp.com/admin/ (prod)
```

### Client User (Limited Access)
```
Username: client1
Password: client-password-123
URL: http://localhost:8000/client-portal/ (local) or https://api.kinddhelp.com/client-portal/ (prod)
```

**⚠️ SECURITY NOTE:**
1. Basic auth (`clientaccess`) is a gatekeeper - required BEFORE Django login
2. Then use Django credentials (admin or client1) to actually log in
3. **Change these passwords immediately in production!**

---

## Common Commands

### Local Development
```bash
# Start local database (Docker)
docker-compose -f docker-compose.local.yml up -d

# Activate venv and run server
cd maplocation
source ../venv/bin/activate
python3 manage.py runserver
# Or bind to all interfaces:
python3 manage.py runserver 0.0.0.0:8000

# Run frontend
cd map-frontend
./switch-env.sh dev
npm run dev
```

### Data Import (Local)
```bash
cd maplocation
source ../venv/bin/activate

# Import Pasadena providers
python3 manage.py import_regional_center_providers \
  --file "../data/Pasadena Provider List.xlsx" \
  --regional-center "Pasadena" \
  --geocode

# Import San Gabriel providers
python3 manage.py import_regional_center_providers \
  --file "../data/San Gabriel Pomona Provider List.xlsx" \
  --regional-center "San Gabriel" \
  --geocode
```

### Deployment
```bash
# Frontend deployment
cd map-frontend
npm run build
aws s3 sync dist/ s3://kinddhelp-frontend-1755148345 \
  --delete \
  --profile personal \
  --region us-west-2

# Invalidate CloudFront cache
aws cloudfront create-invalidation \
  --distribution-id E2W6EECHUV4LMM \
  --paths "/*" \
  --profile personal

# Backend deployment
cd maplocation
eb deploy chla-api-env --region us-west-2 --profile personal
```

---

## Local URLs

### Backend
- API Root: http://localhost:8000/api/
- Admin Panel: http://localhost:8000/admin/
- Client Portal: http://localhost:8000/client-portal/
- Health Check: http://localhost:8000/api/health/
- Regional Centers: http://localhost:8000/api/regional-centers/
- Providers: http://localhost:8000/api/providers-v2/

### Frontend (Development)
- Main App: http://localhost:5173 (Vite) or http://localhost:3000 (alternate)
- Login: http://localhost:5173/login
- Providers: http://localhost:5173/providers

---

## Production URLs

### Backend
- API Root: https://api.kinddhelp.com/api/
- Admin Panel: https://api.kinddhelp.com/admin/
- Client Portal: https://api.kinddhelp.com/client-portal/
- Health Check: https://api.kinddhelp.com/api/health/

### Frontend
- Main App: https://kinddhelp.com
- Alternate: https://www.kinddhelp.com

---

## DNS Configuration (Route53)

**Hosted Zone:** Z0467239OKDU4Z74D3ZB

### Records
- `kinddhelp.com` → CloudFront (Alias)
- `www.kinddhelp.com` → CloudFront (Alias)
- `api.kinddhelp.com` → chla-api-prod.eba-9aiqcppx.us-west-2.elasticbeanstalk.com (CNAME)

---

## SSL Certificates (ACM)

### Backend Certificate
- ARN: `arn:aws:acm:us-west-2:453324135535:certificate/38d0e387-737d-4c48-9760-71de6f9cf9d6`
- Domain: `api.kinddhelp.com`
- Validation: DNS (automatic via Route53)

### Frontend Certificate
- Managed automatically by CloudFront
- Domains: `kinddhelp.com`, `www.kinddhelp.com`

---

## Troubleshooting

### Check Database Connection
```bash
# Local
psql -h localhost -p 5433 -U chla_dev -d chla_local

# RDS
psql -h chla-postgres-db.cpkvcu4f59w6.us-west-2.rds.amazonaws.com \
     -U chla_admin \
     -d postgres \
     --set=sslmode=require
```

### Check EB Environment
```bash
# Status
eb status --profile personal --region us-west-2

# Logs
eb logs --profile personal --region us-west-2

# Health
eb health --profile personal --region us-west-2
```

### Check RDS
```bash
aws rds describe-db-instances \
  --profile personal \
  --region us-west-2 \
  --db-instance-identifier chla-postgres-db
```

### DNS Lookup
```bash
nslookup api.kinddhelp.com
# Should resolve to: chla-api-prod.eba-9aiqcppx.us-west-2.elasticbeanstalk.com
```

---

## Security Best Practices

1. ✅ Never commit `.env.*` files with real credentials
2. ✅ Use `.env.example` as template only
3. ✅ Rotate passwords regularly (every 90 days)
4. ✅ Use different credentials for local vs production
5. ✅ Store production credentials in secure password manager
6. ✅ Use GitHub Secrets for CI/CD
7. ❌ Never use `DJANGO_DEBUG=true` in production
8. ❌ Never use `ALLOWED_HOSTS=*` in production (only for testing)

---

## Generating New Secrets

### Django Secret Key
```bash
python3 -c 'from django.core.management.utils import get_random_secret_key; print(get_random_secret_key())'
```

### Strong Password
```bash
openssl rand -base64 32
```

---

## Related Documentation

- [Quick Start Guide](QUICK_START.md)
- [Deployment Guide](DEPLOY.md)
- [CI/CD Guide](.github/CICD_GUIDE.md)
- [GitHub Secrets](.github/SECRETS.md)

