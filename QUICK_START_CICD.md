# Quick Start: CI/CD Pipeline

GitHub CLI is now installed and ready to use! Here's how to get started with your new CI/CD pipeline.

## ✅ You're All Set!

GitHub CLI version: **2.82.1**
Authenticated as: **alexbeattie**
Token scopes: `gist`, `read:org`, `repo`, `workflow` ✅

## 🚀 Quick Commands

### Deploy to Production

```bash
# Automatic: Just push to main
git push origin main

# Monitor the deployment
gh run watch

# View recent deployments
gh run list --workflow=ci-cd.yml
```

### Database Sync (Local to RDS)

```bash
# Sync all data to RDS
gh workflow run db-sync.yml \
  -f sync_type=full-sync \
  -f dry_run=false \
  -f backup_first=true

# Watch the sync progress
gh run watch
```

### Emergency Rollback

```bash
# Rollback both frontend and backend
gh workflow run rollback.yml \
  -f target=both \
  -f reason="Critical bug in latest release"

# Rollback just backend
gh workflow run rollback.yml \
  -f target=backend-only \
  -f reason="Backend API issue"
```

### Test Locally First

```bash
# Run all pre-deployment tests
./scripts/deploy-test.sh

# This checks:
# - Backend linting (flake8, black)
# - Backend tests (pytest)
# - Frontend tests (vitest)
# - Frontend build
# - Health check endpoint
```

## 📋 Next Steps

### 1. Configure GitHub Secrets (Required)

```bash
# Interactive setup
./scripts/setup-ci.sh
```

Or set manually in GitHub:
- Go to **Settings → Secrets → Actions**
- Add these secrets:
  - `AWS_ACCESS_KEY_ID`
  - `AWS_SECRET_ACCESS_KEY`
  - `RDS_DB_*` (host, name, user, password, instance ID)
  - `DJANGO_SECRET_KEY`
  - `BACKEND_URL`
  - `FRONTEND_URL`
  - `S3_BUCKET`
  - `CLOUDFRONT_DISTRIBUTION_ID`

See [.github/SECRETS.md](.github/SECRETS.md) for details.

### 2. Test the Pipeline

```bash
# Make a small change
echo "# Test" >> README.md

# Commit and push
git add README.md
git commit -m "Test CI/CD pipeline"
git push origin main

# Watch it deploy
gh run watch
```

### 3. Monitor Deployments

```bash
# View deployment logs
gh run view --log

# Check specific workflow
gh run list --workflow=ci-cd.yml --limit 5

# View workflow status
gh workflow view ci-cd.yml
```

## 🎯 Common Workflows

### New Feature Deployment

```bash
# 1. Create feature branch
git checkout -b feature/new-feature

# 2. Make changes and test locally
./scripts/deploy-test.sh

# 3. Push and create PR
git push origin feature/new-feature
gh pr create --title "Add new feature" --body "Description"

# 4. CI runs automatically on PR
# 5. After approval, merge to main
gh pr merge --merge

# 6. Automatic deployment to production
gh run watch
```

### Database Migration

```bash
# 1. Create migration locally
cd maplocation
python manage.py makemigrations

# 2. Test migration locally
python manage.py migrate

# 3. Push changes (migrations included)
git add maplocation/locations/migrations/
git commit -m "Add database migration for XYZ"
git push origin main

# 4. CI/CD automatically runs migrations on RDS
gh run watch
```

### Sync Provider Data

```bash
# Full sync (schema + data)
gh workflow run db-sync.yml \
  -f sync_type=full-sync \
  -f dry_run=false \
  -f backup_first=true

# Just providers
gh workflow run db-sync.yml \
  -f sync_type=providers-only \
  -f dry_run=false \
  -f backup_first=true

# Dry run first (preview)
gh workflow run db-sync.yml \
  -f sync_type=full-sync \
  -f dry_run=true \
  -f backup_first=false
```

## 🔍 Troubleshooting

### Deployment Failed

```bash
# View the logs
gh run view --log

# Check health endpoint
curl https://api.kinddhelp.com/api/health/

# Rollback if needed
gh workflow run rollback.yml \
  -f target=both \
  -f reason="Deployment failed"
```

### Tests Failing

```bash
# Run tests locally
cd maplocation && pytest
cd map-frontend && npm test

# View CI test logs
gh run view --log
```

### Database Issues

```bash
# Check RDS connection
python check_database.py

# Verify sync status
gh run list --workflow=db-sync.yml
```

## 📚 Documentation

- **[CI/CD Guide](.github/CICD_GUIDE.md)** - Comprehensive guide
- **[Secrets Setup](.github/SECRETS.md)** - Configure GitHub secrets
- **[Database Sync Report](DATABASE_SYNC_REPORT.md)** - Latest sync status

## 💡 Pro Tips

1. **Always test locally first**: `./scripts/deploy-test.sh`
2. **Use dry-run for database syncs**: Add `-f dry_run=true`
3. **Monitor deployments**: `gh run watch` shows real-time progress
4. **Check health after deploy**: `curl https://api.kinddhelp.com/api/health/`
5. **Create backups before big changes**: Database sync does this automatically

## 🆘 Get Help

```bash
# GitHub CLI help
gh help

# Workflow help
gh workflow view ci-cd.yml

# Run help
gh run view --help
```

## 🎉 You're Ready!

Your CI/CD pipeline is fully configured and ready to use. Just push to main and watch it deploy automatically!

```bash
# Current database status
# ✅ 179 unique providers
# ✅ 99.4% geocoded
# ✅ Updated ZIP codes preserved
# ✅ Zero duplicates
# ✅ Production ready!
```

Happy deploying! 🚀
