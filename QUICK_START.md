# CHLA Provider Map - Quick Start Guide

**Get up and running in 5 minutes**

## 🚀 First Time Setup

### 1. Start Local Database (Docker)
```bash
docker-compose -f docker-compose.local.yml up -d
```

### 2. Set Up Backend
```bash
cd maplocation
source ../venv/bin/activate  # or create: python3 -m venv venv
pip install -r requirements.txt

# Copy environment file
cp ../.env.local .env

# Run migrations
python3 manage.py migrate

# Create superuser
python3 manage.py createsuperuser
```

### 3. Set Up Frontend
```bash
cd map-frontend
npm install
```

## 🏃 Daily Development

### Start Everything (One Command)
```bash
# Terminal 1: Start database
docker-compose -f docker-compose.local.yml up

# Terminal 2: Start backend
cd maplocation && source ../venv/bin/activate && python3 manage.py runserver

# Terminal 3: Start frontend
cd map-frontend && ./switch-env.sh dev && npm run dev
```

Access:
- Frontend: http://localhost:5173
- Backend API: http://localhost:8000/api/
- Admin: http://localhost:8000/admin/

## 📊 Database Management

### Pull Production Data to Local
```bash
./scripts/db-sync-rds-to-local.sh
```

### Push Local Data to Production
```bash
./scripts/db-sync-local-to-rds.sh
```

### Just Migrations (Safe)
```bash
# Apply to local
cd maplocation
python3 manage.py makemigrations
python3 manage.py migrate

# Apply to RDS
source .env.production
python3 manage.py migrate
```

## 🚢 Deployment

### Test Before Deploying (⚡ Fast - 10 seconds)
```bash
./scripts/quick-test.sh
```

### Full Pre-flight Check (3 minutes)
```bash
./scripts/test-deployment-locally.sh
```

### Deploy Manually (Backup Method)
```bash
# Backend only
cd maplocation && ./deploy.sh

# Frontend only
cd map-frontend && ./deploy.sh
```

### Deploy via GitHub Actions (Primary Method)
```bash
# Just push to main - automatic deployment
git push origin main

# Or trigger manually
gh workflow run ci-cd.yml
```

## 🐛 Quick Debugging

### Backend Not Working?
```bash
cd maplocation
python3 manage.py check
python3 manage.py showmigrations
eb logs  # for production
```

### Frontend Not Working?
```bash
cd map-frontend
npm run lint
npm run build  # test build
```

### Database Connection Issues?
```bash
# Test local connection
psql -h localhost -p 5433 -U chla_dev -d chla_local

# Test RDS connection
psql -h <RDS_HOST> -U chla_admin -d postgres --set=sslmode=require
```

## 🔧 Common Commands

```bash
# Backend
python3 manage.py runserver           # Start dev server
python3 manage.py shell               # Django shell
python3 manage.py makemigrations      # Create migrations
python3 manage.py migrate             # Apply migrations

# Frontend
npm run dev                           # Start dev server
npm run build                         # Production build
npm run lint                          # Check code quality
./switch-env.sh dev                   # Switch to dev API
./switch-env.sh prod                  # Switch to prod API

# Docker
docker-compose -f docker-compose.local.yml up      # Start
docker-compose -f docker-compose.local.yml down    # Stop
docker-compose -f docker-compose.local.yml ps      # Status

# AWS
eb status                             # Check EB status
eb logs                               # View logs
aws s3 ls s3://kinddhelp-frontend-1755148345/  # Check S3
```

## ⚡ Speed Tips

1. **Before every commit**: `./scripts/quick-test.sh` (10 seconds)
2. **Before deployment**: `./scripts/test-deployment-locally.sh` (3 min)
3. **Use Docker for DB**: Faster than managing PostgreSQL manually
4. **Keep envs in sync**: Run db sync scripts regularly

## 📚 Full Documentation

- [Complete Deployment Guide](DEPLOY.md)
- [CI/CD Documentation](.github/CICD_GUIDE.md)
- [Environment Setup](.github/SECRETS.md)
- [Repository Rules](.cursorrules) (for Cursor/Claude)

## 🆘 Help

**Something broken?**
1. Check you're using correct environment (`.env.local` vs `.env.production`)
2. Run `./scripts/quick-test.sh` to diagnose
3. Check GitHub Actions logs for deployment issues
4. Review error messages in terminal

**Still stuck?**
- Check recent git commits for breaking changes
- Try syncing database: `./scripts/db-sync-rds-to-local.sh`
- Restart Docker: `docker-compose down && docker-compose up`

