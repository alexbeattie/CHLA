# CHLA Provider Map - Complete Deployment Guide

## Overview
This guide covers:

1. Local development environment setup
2. AWS infrastructure overview
3. Manual deployment process
4. GitHub Actions CI/CD setup (recommended)
5. Troubleshooting common issues

---

## 1. Local Development Environment Setup

### Prerequisites

- Python 3.12 (for Django backend)
- Node.js 18+ (for Vue.js frontend)
- PostgreSQL (local database)
- AWS CLI configured with your personal profile

### Backend Setup (Django)

1. **Navigate to backend directory:**

   ```bash
   cd /Users/alexbeattie/Documents/Cline/CHLAProj/maplocation
   ```

2. **Create and activate virtual environment:**

   ```bash
   python3 -m venv .venv
   source .venv/bin/activate
   ```

3. **Install dependencies:**

   ```bash
   pip install -r requirements.txt
   ```

4. **Set up local environment variables:**

   ```bash
   # Clear any production environment variables
   unset DJANGO_SECRET_KEY DJANGO_DEBUG ALLOWED_HOSTS CORS_ALLOWED_ORIGINS CSRF_TRUSTED_ORIGINS
   unset DB_NAME DB_USER DB_PASSWORD DB_HOST DB_PORT
   ```

5. **Run database migrations:**

   ```bash
   python manage.py migrate
   ```

6. **Start development server:**

   ```bash
   python manage.py runserver
   ```

   Backend will be available at: <http://127.0.0.1:8000>

### Frontend Setup (Vue.js)

1. **Navigate to frontend directory:**

   ```bash
   cd /Users/alexbeattie/Documents/Cline/CHLAProj/map-frontend
   ```

2. **Install dependencies:**

   ```bash
   npm install
   ```

3. **Create local environment file:**

   ```bash
   echo "VITE_API_BASE_URL=http://127.0.0.1:8000/api" > .env.local
   ```

4. **Start development server:**

   ```bash
   npm run dev
   ```

   Frontend will be available at: <http://localhost:3000>

---

## 2. AWS Infrastructure Overview

### Current AWS Services

#### 2.1 Elastic Beanstalk (Backend API)

- **Environment**: `chla-api-env-v2`
- **Platform**: Python 3.12
- **URL**: <https://api.kinddhelp.com>
- **Database**: RDS PostgreSQL
- **Key Files**: `Procfile`, `requirements.txt`, `.ebignore`

#### 2.2 RDS PostgreSQL Database

- **Instance**: `chla-postgres-db`
- **Engine**: PostgreSQL 16.3
- **Extensions**: PostGIS (for geographic data)
- **Connection**: Environment variables in EB

#### 2.3 S3 Buckets

- **Frontend Bucket**: `kinddhelp-frontend-1755148345` (currently active)
- **Alternative Bucket**: `chla-frontend-1755161523` (newer, not connected to CloudFront)
- **Purpose**: Static website hosting

#### 2.4 CloudFront Distribution

- **ID**: `E2W6EECHUV4LMM`
- **Domains**: `kinddhelp.com`, `www.kinddhelp.com`
- **Origin**: S3 bucket `kinddhelp-frontend-1755148345`
- **Purpose**: CDN for faster global delivery

#### 2.5 Route 53 DNS

- **Hosted Zone**: `kinddhelp.com`
- **Records**:
  - `kinddhelp.com` → CloudFront distribution
  - `www.kinddhelp.com` → CloudFront distribution
  - `api.kinddhelp.com` → Elastic Beanstalk ALB

#### 2.6 ACM SSL Certificates

- **CloudFront Certificate**: `kinddhelp.com`, `www.kinddhelp.com` (us-east-1)
- **ALB Certificate**: `api.kinddhelp.com` (us-west-2)

---

## 3. Manual Deployment Process

### 3.1 Backend Deployment (Django to Elastic Beanstalk)

1. **Set AWS profile:**

   ```bash
   export AWS_PROFILE=personal
   ```

2. **Navigate to backend directory:**

   ```bash
   cd /Users/alexbeattie/Documents/Cline/CHLAProj/maplocation
   ```

3. **Deploy to Elastic Beanstalk:**

   ```bash
   eb deploy --staged
   ```

4. **Monitor deployment:**

   ```bash
   eb logs
   eb health
   ```

### 3.2 Frontend Deployment (Vue.js to S3/CloudFront)

1. **Build production frontend:**

   ```bash
   cd /Users/alexbeattie/Documents/Cline/CHLAProj/map-frontend
   VITE_API_BASE_URL="https://api.kinddhelp.com/api" npm run build
   ```

2. **Deploy to S3:**

   ```bash
   aws s3 sync dist s3://kinddhelp-frontend-1755148345 --delete --profile personal
   ```

3. **Invalidate CloudFront cache:**

   ```bash
   aws cloudfront create-invalidation --distribution-id E2W6EECHUV4LMM --paths "/*" --profile personal
   ```

### 3.3 Environment Variables Management

#### Backend Environment Variables (Elastic Beanstalk)

```bash
eb setenv \
  DJANGO_SECRET_KEY="your-secret-key" \
  DJANGO_DEBUG="false" \
  ALLOWED_HOSTS="api.kinddhelp.com,localhost" \
  CORS_ALLOWED_ORIGINS="https://kinddhelp.com,https://www.kinddhelp.com" \
  CSRF_TRUSTED_ORIGINS="https://kinddhelp.com,https://www.kinddhelp.com" \
  DB_NAME="postgres" \
  DB_USER="chla_admin" \
  DB_PASSWORD="CHLASecure2024" \
  DB_HOST="chla-postgres-db.cpkvcu4f59w6.us-west-2.rds.amazonaws.com" \
  DB_PORT="5432"
```

---

## 4. GitHub Actions CI/CD Setup (Recommended)

### 4.1 Repository Setup

1. **Create GitHub repository**
2. **Push your code to GitHub**
3. **Set up GitHub Secrets**

### 4.2 GitHub Secrets Configuration

Go to GitHub repo → Settings → Secrets and variables → Actions

Add these secrets:

```
AWS_ACCESS_KEY_ID: your-aws-access-key
AWS_SECRET_ACCESS_KEY: your-aws-secret-key
AWS_REGION: us-west-2
EB_APPLICATION_NAME: chla-api
EB_ENVIRONMENT_NAME: chla-api-env-v2
S3_BUCKET_NAME: kinddhelp-frontend-1755148345
CLOUDFRONT_DISTRIBUTION_ID: E2W6EECHUV4LMM
DJANGO_SECRET_KEY: your-secret-key
DB_PASSWORD: CHLASecure2024
DB_HOST: chla-postgres-db.cpkvcu4f59w6.us-west-2.rds.amazonaws.com
```

### 4.3 GitHub Actions Workflow

Create `.github/workflows/deploy.yml`:

```yaml
name: Deploy CHLA Provider Map

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up Python
      uses: actions/setup-python@v4
      with:
        python-version: '3.12'
    
    - name: Set up Node.js
      uses: actions/setup-node@v4
      with:
        node-version: '18'
    
    - name: Install backend dependencies
      run: |
        cd maplocation
        pip install -r requirements.txt
    
    - name: Install frontend dependencies
      run: |
        cd map-frontend
        npm ci
    
    - name: Run backend tests
      run: |
        cd maplocation
        python manage.py test
    
    - name: Build frontend
      run: |
        cd map-frontend
        npm run build

  deploy-backend:
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up Python
      uses: actions/setup-python@v4
      with:
        python-version: '3.12'
    
    - name: Install EB CLI
      run: |
        pip install awsebcli
    
    - name: Configure AWS credentials
      uses: aws-actions/configure-aws-credentials@v2
      with:
        aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
        aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
        aws-region: ${{ secrets.AWS_REGION }}
    
    - name: Deploy to Elastic Beanstalk
      run: |
        cd maplocation
        eb deploy ${{ secrets.EB_ENVIRONMENT_NAME }} --staged
    
    - name: Update environment variables
      run: |
        cd maplocation
        eb setenv \
          DJANGO_SECRET_KEY="${{ secrets.DJANGO_SECRET_KEY }}" \
          DJANGO_DEBUG="false" \
          ALLOWED_HOSTS="api.kinddhelp.com,localhost" \
          CORS_ALLOWED_ORIGINS="https://kinddhelp.com,https://www.kinddhelp.com" \
          CSRF_TRUSTED_ORIGINS="https://kinddhelp.com,https://www.kinddhelp.com" \
          DB_NAME="postgres" \
          DB_USER="chla_admin" \
          DB_PASSWORD="${{ secrets.DB_PASSWORD }}" \
          DB_HOST="${{ secrets.DB_HOST }}" \
          DB_PORT="5432"

  deploy-frontend:
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up Node.js
      uses: actions/setup-node@v4
      with:
        node-version: '18'
    
    - name: Install dependencies
      run: |
        cd map-frontend
        npm ci
    
    - name: Build frontend
      run: |
        cd map-frontend
        VITE_API_BASE_URL="https://api.kinddhelp.com/api" npm run build
    
    - name: Configure AWS credentials
      uses: aws-actions/configure-aws-credentials@v2
      with:
        aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
        aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
        aws-region: ${{ secrets.AWS_REGION }}
    
    - name: Deploy to S3
      run: |
        cd map-frontend
        aws s3 sync dist s3://${{ secrets.S3_BUCKET_NAME }} --delete
    
    - name: Invalidate CloudFront
      run: |
        aws cloudfront create-invalidation \
          --distribution-id ${{ secrets.CLOUDFRONT_DISTRIBUTION_ID }} \
          --paths "/*"
```

### 4.4 EB CLI Configuration

Create `maplocation/.elasticbeanstalk/config.yml`:

```yaml
branch-defaults:
  main:
    environment: chla-api-env-v2
    group_suffix: null
global:
  application_name: chla-api
  branch: null
  default_ec2_keyname: null
  default_platform: Python 3.12
  default_region: us-west-2
  include_git_submodules: true
  instance_profile: null
  platform_name: null
  platform_version: null
  profile: personal
  repository: null
  sc: git
  workspace_type: Application
```

---

## 5. Key Configuration Files

### 5.1 Backend Files

#### `maplocation/requirements.txt`

```
Django==5.2
djangorestframework==3.15.2
django-cors-headers==4.7.0
django-filter==24.3
Pillow==11.2.1
graphene-django==3.1.5
djangorestframework-gis==1.1
gunicorn==22.0.0
whitenoise==6.7.0
psycopg[binary]>=3.1.8; python_version >= "3.13"
psycopg2-binary==2.9.9; python_version < "3.13"
```

#### `maplocation/Procfile`

```
web: gunicorn maplocation.wsgi:application
```

#### `maplocation/.ebignore`

```
venv/
.venv/
__pycache__/
*.pyc
*.pyo
*.pyd
*.log
.git/
.gitignore
*.swp
.DS_Store
node_modules/
dist/
build/
.eggs/
*.egg-info/
staticfiles/
```

### 5.2 Frontend Files

#### `map-frontend/package.json` (key scripts)

```json
{
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  }
}
```

#### `map-frontend/.env.local` (local development)

```
VITE_API_BASE_URL=http://127.0.0.1:8000/api
```

---

## 6. Deployment Checklist

### Before Each Deployment

- [ ] Test locally (both frontend and backend)
- [ ] Run tests
- [ ] Check environment variables
- [ ] Verify API endpoints work
- [ ] Test mobile responsiveness

### Backend Deployment Steps

- [ ] Set AWS_PROFILE=personal
- [ ] Navigate to maplocation directory
- [ ] Run `eb deploy --staged`
- [ ] Check `eb health` and `eb logs`
- [ ] Test API endpoints

### Frontend Deployment Steps

- [ ] Build with production API URL
- [ ] Deploy to correct S3 bucket
- [ ] Invalidate CloudFront cache
- [ ] Test website functionality

### Post-Deployment Verification

- [ ] Test <https://kinddhelp.com> loads correctly
- [ ] Test mobile sidebar functionality
- [ ] Verify API calls work from frontend
- [ ] Check provider data loads
- [ ] Test map functionality

---

## 7. Troubleshooting Common Issues

### Issue: Mobile sidebar not appearing
**Cause**: Browser cache or CloudFront cache
**Solution**: 

1. Invalidate CloudFront cache
2. Clear browser cache or use incognito mode

### Issue: API calls failing
**Cause**: CORS configuration or wrong API URL
**Solution**: 

1. Check CORS_ALLOWED_ORIGINS in EB environment
2. Verify VITE_API_BASE_URL in frontend build

### Issue: Database connection failed
**Cause**: Environment variables or security group
**Solution**: 

1. Check DB_* environment variables in EB
2. Verify RDS security group allows EB access

### Issue: 500 errors on backend
**Cause**: Missing environment variables or code errors
**Solution**: 

1. Check `eb logs` for detailed errors
2. Verify all required environment variables are set

### Issue: CloudFront serving old content
**Cause**: CloudFront cache not invalidated
**Solution**: 

1. Run CloudFront invalidation
2. Wait 5-10 minutes for propagation

---

## 8. Quick Reference Commands

### Local Development

```bash
# Start backend
cd maplocation && source .venv/bin/activate && python manage.py runserver

# Start frontend
cd map-frontend && npm run dev
```

### Manual Deployment

```bash
# Backend
export AWS_PROFILE=personal
cd maplocation && eb deploy --staged

# Frontend
cd map-frontend
VITE_API_BASE_URL="https://api.kinddhelp.com/api" npm run build
aws s3 sync dist s3://kinddhelp-frontend-1755148345 --delete --profile personal
aws cloudfront create-invalidation --distribution-id E2W6EECHUV4LMM --paths "/*" --profile personal
```

### Monitoring

```bash
# Backend health and logs
eb health
eb logs

# Check website
curl -I https://kinddhelp.com
curl -I https://api.kinddhelp.com/api/providers/
```

---

## 9. Next Steps / Improvements

1. **Set up GitHub Actions CI/CD** (recommended)
2. **Add automated testing**
3. **Set up staging environment**
4. **Implement database backups**
5. **Add monitoring and alerting**
6. **Set up SSL certificate auto-renewal**
7. **Optimize CloudFront caching rules**
8. **Add error tracking (e.g., Sentry)**

---

**Questions?** Check the troubleshooting section or refer to AWS documentation for specific services.
