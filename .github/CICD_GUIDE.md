# CI/CD Pipeline Guide

Comprehensive guide for the GitHub Actions CI/CD pipeline.

## Overview

The CI/CD pipeline automates:
- ✅ Backend testing (Django, pytest)
- ✅ Frontend testing (Vue, Vitest)
- ✅ Linting and code quality checks
- ✅ Automated deployment to AWS (Backend to EB, Frontend to S3/CloudFront)
- ✅ Database migrations (local to RDS)
- ✅ Health checks and deployment verification
- ✅ Automatic rollback on failure
- ✅ Manual rollback capability

## Workflows

### 1. Main CI/CD Pipeline (`ci-cd.yml`)

**Triggers:**
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop`
- Manual workflow dispatch

**Jobs:**
1. **Backend Tests** - Runs Django tests with PostgreSQL
2. **Frontend Tests** - Runs Vue/Vitest tests
3. **Deploy Backend** - Deploys to Elastic Beanstalk (main branch only)
4. **Deploy Frontend** - Deploys to S3/CloudFront (after backend succeeds)
5. **Post-Deployment** - Verifies deployment health

**Features:**
- Automatic rollback on health check failure
- Deployment summaries in GitHub Actions UI
- Cache for faster builds (pip, npm)
- Parallel test execution
- Code coverage reports

**Manual Deployment:**
```bash
# Via GitHub UI: Actions → CI/CD Pipeline → Run workflow
# Or via CLI:
gh workflow run ci-cd.yml
```

### 2. Database Sync (`db-sync.yml`)

**Purpose:** Sync data from local database to AWS RDS

**Triggers:**
- Manual workflow dispatch only

**Sync Types:**
- `schema-only` - Only apply migrations
- `data-only` - Only sync provider/ZIP data
- `full-sync` - Complete schema and data sync
- `providers-only` - Only sync provider data

**Features:**
- Automatic RDS snapshot before sync
- Dry run mode for testing
- Connection verification
- Data verification after sync

**Usage:**
```bash
# Via GitHub UI:
# Actions → Database Sync → Run workflow → Select sync type

# Via CLI:
gh workflow run db-sync.yml -f sync_type=full-sync -f dry_run=false -f backup_first=true
```

**Safety:**
- Always creates backup snapshot (unless disabled)
- Dry run mode shows what would happen without making changes
- Tests connection before making changes

### 3. Rollback Deployment (`rollback.yml`)

**Purpose:** Rollback failed or problematic deployments

**Triggers:**
- Manual workflow dispatch only

**Rollback Targets:**
- `backend-only` - Rollback Elastic Beanstalk deployment
- `frontend-only` - Restore frontend from S3 backup
- `both` - Rollback both backend and frontend

**Features:**
- Automatic version detection
- Manual version selection
- Health check verification
- Incident reporting

**Usage:**
```bash
# Via GitHub UI:
# Actions → Rollback Deployment → Run workflow
# Select target and provide reason

# Via CLI:
gh workflow run rollback.yml \
  -f target=backend-only \
  -f reason="Critical bug in v2.1.0"
```

## Deployment Flow

### Automatic Deployment (Push to Main)

```
Push to main
    ↓
Run Backend Tests (pytest, flake8, black)
    ↓
Run Frontend Tests (vitest)
    ↓
Deploy to Elastic Beanstalk
    ↓
Health Check (retry 10x)
    ↓
Deploy to S3/CloudFront
    ↓
Invalidate CloudFront Cache
    ↓
Verify Frontend
    ↓
End-to-End Health Check
    ↓
✅ Deployment Complete
```

### Rollback on Failure

```
Deploy to EB
    ↓
Health Check Fails
    ↓
Get Previous Version
    ↓
Redeploy Previous Version
    ↓
Verify Health
    ↓
⚠️ Rollback Complete
```

## Local Development

### Testing Locally Before Push

```bash
# Backend tests
cd maplocation
python -m pytest

# Frontend tests
cd map-frontend
npm test

# Linting
cd maplocation
flake8 .
black --check .
isort --check .

cd map-frontend
npm run lint
```

### Local to RDS Database Sync

Instead of using the GitHub Action, you can sync manually:

```bash
cd maplocation

# Set environment variables
export DB_NAME="your-rds-db"
export DB_USER="your-db-user"
export DB_PASSWORD="your-db-password"
export DB_HOST="your-rds-host.rds.amazonaws.com"
export DB_PORT="5432"
export DB_SSL_REQUIRE="true"
export DJANGO_SECRET_KEY="your-secret-key"

# Run migrations
python manage.py migrate

# Import provider data
python manage.py import_regional_center_providers \
  --file "data/Pasadena Provider List.xlsx" \
  --area "Pasadena"

python manage.py import_regional_center_providers \
  --file "data/San Gabriel Pomona Provider List.xlsx" \
  --regional-center "San Gabriel"

# Populate ZIP codes
python manage.py populate_san_gabriel_zips
python manage.py populate_pasadena_zips
```

## Environment Variables

### GitHub Actions Environment

Configured in workflows, using secrets:
- `AWS_REGION=us-west-2`
- `NODE_VERSION=18`
- `PYTHON_VERSION=3.12`

### Elastic Beanstalk Environment

Set via `.ebextensions/01_auto_migrate.config`:
- `DJANGO_SETTINGS_MODULE=maplocation.settings`
- Database credentials (from secrets)
- `DB_SSL_REQUIRE=true`

### Frontend Environment

Set via `switch-env.sh`:
- Production: `.env.production`
- Development: `.env.development`

## Monitoring Deployments

### View Deployment Status

```bash
# List recent workflow runs
gh run list --workflow=ci-cd.yml

# Watch a running workflow
gh run watch

# View logs
gh run view --log
```

### Check Deployment Health

```bash
# Backend health check
curl https://api.kinddhelp.com/api/health/

# Expected response:
# {
#   "status": "healthy",
#   "database": "connected",
#   "providers": 1234,
#   "regional_centers": 21,
#   "version": "2.0.0"
# }

# Frontend health check
curl -I https://kinddhelp.com
```

### Elastic Beanstalk Logs

```bash
cd maplocation
eb logs
```

### CloudFront Cache Status

```bash
# List recent invalidations
aws cloudfront list-invalidations \
  --distribution-id E2W6EECHUV4LMM

# Get invalidation status
aws cloudfront get-invalidation \
  --distribution-id E2W6EECHUV4LMM \
  --id INVALIDATION_ID
```

## Troubleshooting

### Tests Failing in CI but Pass Locally

**Possible causes:**
- Different Python/Node versions
- Missing environment variables
- Database state differences

**Solutions:**
```bash
# Match CI Python version
pyenv install 3.12
pyenv local 3.12

# Match CI Node version
nvm install 18
nvm use 18

# Use same database
createdb test_db
export DB_NAME=test_db
```

### Deployment Succeeds but Site is Down

**Check:**
1. Elastic Beanstalk environment health
2. Application logs
3. Security group rules
4. Database connection

```bash
cd maplocation
eb health
eb logs --all
```

### Frontend Not Updating

**Causes:**
- CloudFront cache not invalidated
- Build artifacts not uploaded
- Wrong S3 bucket

**Solutions:**
```bash
# Manual CloudFront invalidation
aws cloudfront create-invalidation \
  --distribution-id E2W6EECHUV4LMM \
  --paths "/*"

# Check S3 bucket
aws s3 ls s3://kinddhelp-frontend-1755148345/

# Manually sync
cd map-frontend
npm run build
aws s3 sync dist/ s3://kinddhelp-frontend-1755148345/ --delete
```

### Database Migration Fails

**Check:**
1. RDS connectivity
2. Migration conflicts
3. SSL requirements

```bash
# Test connection
psql -h your-host.rds.amazonaws.com \
     -U your-user \
     -d your-db \
     --set=sslmode=require

# Show migration status
python manage.py showmigrations

# Reset migrations (DANGEROUS - development only)
python manage.py migrate your_app zero
python manage.py migrate
```

### Rollback Not Working

**Manual rollback:**

```bash
# Backend
cd maplocation
eb deploy --version app-previous-version

# Frontend (restore from backup)
aws s3 sync \
  s3://kinddhelp-frontend-backup-TIMESTAMP/ \
  s3://kinddhelp-frontend-1755148345/ \
  --delete
```

## Best Practices

### Before Deploying

- ✅ Run tests locally
- ✅ Create feature branch for changes
- ✅ Open PR for code review
- ✅ Wait for CI to pass on PR
- ✅ Test on staging environment (if available)
- ✅ Merge to main during low-traffic hours

### During Deployment

- ✅ Monitor GitHub Actions logs
- ✅ Watch for errors or warnings
- ✅ Check health endpoints immediately
- ✅ Verify key functionality

### After Deployment

- ✅ Run smoke tests
- ✅ Check error tracking (if configured)
- ✅ Monitor application logs
- ✅ Be ready to rollback if needed

### Emergency Rollback Procedure

1. **Immediate Action:**
   ```bash
   # Trigger rollback via GitHub Actions
   gh workflow run rollback.yml -f target=both -f reason="Critical issue"
   ```

2. **Verify Rollback:**
   ```bash
   # Check health
   curl https://api.kinddhelp.com/api/health/
   ```

3. **Post-Incident:**
   - Document what went wrong
   - Create bug ticket
   - Fix issue in feature branch
   - Deploy fix after testing

## Advanced Usage

### Custom Deployment

Create a feature-specific deployment:

```bash
# Create a new workflow for feature testing
cp .github/workflows/ci-cd.yml .github/workflows/feature-deploy.yml

# Modify to deploy to staging environment
# Change branch triggers, environment URLs, etc.
```

### Scheduled Database Backups

Add to `.github/workflows/db-sync.yml`:

```yaml
on:
  schedule:
    - cron: '0 2 * * *'  # Daily at 2 AM UTC
```

### Deployment Notifications

Add Slack/Discord webhooks to workflows:

```yaml
- name: Notify Slack
  if: always()
  uses: 8398a7/action-slack@v3
  with:
    status: ${{ job.status }}
    webhook_url: ${{ secrets.SLACK_WEBHOOK }}
```

## Related Documentation

- [Secrets Configuration](.github/SECRETS.md)
- [Django Deployment](../maplocation/README.md)
- [Frontend Deployment](../map-frontend/README.md)
- [AWS Elastic Beanstalk Docs](https://docs.aws.amazon.com/elasticbeanstalk/)
- [GitHub Actions Docs](https://docs.github.com/en/actions)

## Support

For CI/CD issues:
1. Check GitHub Actions logs
2. Review this guide
3. Check AWS CloudWatch logs
4. Contact DevOps team
