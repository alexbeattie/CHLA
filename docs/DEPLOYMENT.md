# Deployment Guide

Complete guide for deploying the CHLA Provider Map application to production.

## Quick Reference

**Auto-Deploy (Recommended)**
```bash
git add .
git commit -m "Your changes"
git push origin main  # Triggers automatic deployment
```

**Monitor Deployment**
- GitHub Actions: https://github.com/YOUR_USERNAME/CHLAProj/actions
- Total time: ~15-25 minutes

## Table of Contents
- [Prerequisites](#prerequisites)
- [Local Development](#local-development)
- [Production Deployment](#production-deployment)
- [Environment Configuration](#environment-configuration)
- [Database Synchronization](#database-synchronization)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Secrets in GitHub

Navigate to: **Repository Settings → Secrets and Variables → Actions**

Required secrets:
- `AWS_ACCESS_KEY_ID` - AWS IAM access key
- `AWS_SECRET_ACCESS_KEY` - AWS IAM secret key
- `DJANGO_SECRET_KEY` - Django secret key
- `DB_PASSWORD` - RDS database password
- `DB_HOST` - RDS endpoint
- `S3_BUCKET_NAME` - Frontend S3 bucket (kinddhelp-frontend-1755148345)
- `CLOUDFRONT_DISTRIBUTION_ID` - CloudFront distribution (E2W6EECHUV4LMM)

### Local Requirements
- Python 3.12+
- Node.js 18+
- AWS CLI configured with personal profile
- EB CLI installed (for manual deploys)

---

## Local Development

### Backend (Django)

```bash
cd maplocation
source ../.venv/bin/activate  # Activate virtual environment
python3 manage.py runserver
```

**Access at:** http://127.0.0.1:8000

### Frontend (Vue)

```bash
cd map-frontend
npm run dev
```

**Access at:** http://localhost:3000

### Local Environment Files

**map-frontend/.env.development**
```env
VITE_API_BASE_URL=http://127.0.0.1:8000
VITE_PORT=3000
VITE_MAPBOX_TOKEN=pk.eyJ1IjoiYmVhdHR5LWFkbWluIiwiYSI6ImNsejFjNGt0YzFqMGMyanF3YW5hdWFmc3UifQ.sn7Uj_gDzzKL6PQq7vO7fw
```

---

## Production Deployment

### Automatic Deployment (GitHub Actions)

The repository is configured for automatic deployment when pushing to `main`:

1. **Test Locally**
   ```bash
   # Backend checks
   cd maplocation && python3 manage.py check

   # Frontend build test
   cd map-frontend && npm run build
   ```

2. **Commit and Push**
   ```bash
   git add .
   git commit -m "Description of changes"
   git push origin main
   ```

3. **Monitor Progress**
   - Go to GitHub Actions tab
   - Watch the deployment workflow
   - Backend deploys to Elastic Beanstalk (~10-15 min)
   - Frontend deploys to S3/CloudFront (~5-10 min)

4. **Verify Deployment**
   - Backend API: https://api.kinddhelp.com/api/
   - Frontend: https://kinddhelp.com
   - Hard refresh if needed: `Cmd+Shift+R` (Mac) or `Ctrl+Shift+R` (Windows)

### Manual Deployment

**Backend to Elastic Beanstalk:**
```bash
cd maplocation
eb deploy chla-api-prod --profile personal --region us-west-2
```

**Frontend to S3/CloudFront:**
```bash
cd map-frontend
npm run build
aws s3 sync dist/ s3://kinddhelp-frontend-1755148345 --delete --profile personal --region us-west-2
aws cloudfront create-invalidation --distribution-id E2W6EECHUV4LMM --paths "/*" --profile personal
```

---

## Environment Configuration

### Production Environment (.env.production)

**Frontend (map-frontend/.env.production):**
```env
VITE_API_BASE_URL=https://api.kinddhelp.com
VITE_MAPBOX_TOKEN=pk.eyJ1IjoiYmVhdHR5LWFkbWluIiwiYSI6ImNsejFjNGt0YzFqMGMyanF3YW5hdWFmc3UifQ.sn7Uj_gDzzKL6PQq7vO7fw
```

### Elastic Beanstalk Environment Variables

Set via AWS Console or EB CLI:

```bash
eb setenv \
  DB_NAME=postgres \
  DB_USER=chla_admin \
  DB_PASSWORD=your_password \
  DB_HOST=chla-postgres-db.cpkvcu4f59w6.us-west-2.rds.amazonaws.com \
  DB_PORT=5432 \
  DB_SSL_REQUIRE=true \
  DJANGO_DEBUG=false \
  DJANGO_SECRET_KEY=your_production_secret_key \
  ALLOWED_HOSTS=api.kinddhelp.com \
  CORS_ALLOWED_ORIGINS=https://kinddhelp.com,https://www.kinddhelp.com
```

---

## Database Synchronization

### Automatic Migration (Recommended)

Migrations run automatically on deployment via `.ebextensions/01_auto_migrate.config`.

**To deploy new migrations:**
```bash
# 1. Create migrations locally
python manage.py makemigrations

# 2. Test migrations locally
python manage.py migrate

# 3. Commit and push (triggers auto-deploy with migrations)
git add .
git commit -m "Add new migrations"
git push origin main
```

### Check Migration Status

**Local:**
```bash
python manage.py showmigrations locations
```

**Remote (via EB SSH):**
```bash
eb ssh chla-api-prod
python manage.py showmigrations locations
```

### Manual Migration (Emergency)

If auto-migration fails:

```bash
# SSH to EB instance
eb ssh chla-api-prod

# Run migrations manually
python manage.py migrate --run-syncdb

# Check specific migration
python manage.py shell
>>> from django.db import connection
>>> with connection.cursor() as cursor:
...     cursor.execute("SELECT column_name FROM information_schema.columns WHERE table_name = 'regional_centers';")
...     print(cursor.fetchall())
```

### Local to RDS Data Sync

**Option 1: Dump and Restore (Full Sync)**

See: [docs/DATABASE_SYNC.md](./DATABASE_SYNC.md) for detailed instructions.

**Option 2: Migration-Based Sync (Recommended)**

Always use Django migrations to sync schema changes. Direct SQL dumps should only be used for data recovery or initial setup.

---

## AWS Resources

### Backend
- **Environment**: chla-api-prod
- **Platform**: Python 3.12 on Amazon Linux 2
- **Region**: us-west-2
- **URL**: https://api.kinddhelp.com
- **SSL Certificate**: arn:aws:acm:us-west-2:795519544722:certificate/77514a62-6636-4fdb-8360-863aa711859e

### Database
- **Type**: PostgreSQL (RDS)
- **Endpoint**: chla-postgres-db.cpkvcu4f59w6.us-west-2.rds.amazonaws.com
- **Database**: postgres
- **Username**: chla_admin

### Frontend
- **S3 Bucket**: kinddhelp-frontend-1755148345
- **CloudFront ID**: E2W6EECHUV4LMM
- **URL**: https://kinddhelp.com

---

## Troubleshooting

### Deployment Fails

**GitHub Actions failure:**
1. Check Actions tab for error logs
2. Verify all GitHub secrets are set correctly
3. Check AWS IAM permissions
4. Review EB environment health

**Migration errors during deployment:**
1. Check EB logs: `eb logs`
2. Verify migrations work locally first
3. Check `.ebextensions/01_auto_migrate.config` is present
4. Manually SSH and run migrations if needed

### Frontend Not Updating

**CloudFront caching:**
- Wait 5-15 minutes for invalidation to complete
- Force refresh: `Cmd+Shift+R` (Mac) or `Ctrl+Shift+R` (Windows)
- Check CloudFront invalidation status in AWS Console

**S3 sync issues:**
```bash
# Manually sync and invalidate
cd map-frontend
npm run build
aws s3 sync dist/ s3://kinddhelp-frontend-1755148345 --delete --profile personal
aws cloudfront create-invalidation --distribution-id E2W6EECHUV4LMM --paths "/*" --profile personal
```

### Database Connection Issues

**RDS connection timeout:**
1. Check RDS security groups allow EB access
2. Verify VPC configuration
3. Ensure DB_SSL_REQUIRE=true is set
4. Check RDS instance status

**Migration sync issues:**
1. Compare local vs remote migration status
2. Ensure `.ebextensions` config is working
3. Check EB logs for migration errors
4. Manually apply migrations if auto-migration fails

### Environment Variable Issues

**Variables not being picked up:**
1. Verify GitHub secrets are set correctly
2. Check EB environment variables in AWS Console
3. Restart EB environment after setting variables
4. Check GitHub Actions workflow uses correct secret names

---

## Pre-Deployment Checklist

- [ ] Code tested locally (backend and frontend)
- [ ] Migrations created and tested locally
- [ ] Environment variables configured
- [ ] GitHub secrets verified
- [ ] No sensitive data in code (check .env files not committed)
- [ ] Frontend builds without errors (`npm run build`)
- [ ] Backend passes checks (`python manage.py check`)
- [ ] Changes committed with clear message
- [ ] Ready to monitor deployment in GitHub Actions

---

## Post-Deployment Verification

1. **Backend API**
   - Visit: https://api.kinddhelp.com/api/
   - Check provider endpoints work
   - Verify admin portal accessible

2. **Frontend**
   - Visit: https://kinddhelp.com
   - Test map functionality
   - Verify filtering works
   - Check mobile responsiveness

3. **Database**
   - Verify new migrations applied
   - Check data integrity
   - Test CRUD operations

4. **Logs**
   - Check EB logs for errors: `eb logs`
   - Monitor CloudWatch (if configured)
   - Review GitHub Actions logs

---

## Additional Resources

- [Full Stack Documentation](../STACK_DOCUMENTATION.md)
- [Database Sync Guide](./DATABASE_SYNC.md)
- [Backend Getting Started](../maplocation/README.md)
- [GitHub Actions Setup](./GITHUB_ACTIONS.md)
- [Troubleshooting Guide](./TROUBLESHOOTING.md)
