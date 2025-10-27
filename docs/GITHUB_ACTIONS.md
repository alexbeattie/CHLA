# GitHub Actions CI/CD Guide

Complete guide for GitHub Actions automatic deployment and notifications.

## Quick Start

```bash
# Push to main branch triggers automatic deployment
git add .
git commit -m "Your changes"
git push origin main

# Monitor at: https://github.com/YOUR_USERNAME/CHLAProj/actions
```

---

## Table of Contents
- [Overview](#overview)
- [Required Secrets](#required-secrets)
- [How It Works](#how-it-works)
- [Deployment Options](#deployment-options)
- [Monitoring](#monitoring)
- [Notifications](#notifications)
- [Troubleshooting](#troubleshooting)

---

## Overview

GitHub Actions automatically deploys the application when you push to the `main` branch:

1. **Runs Tests** - Validates code quality
2. **Deploys Backend** - Django app to AWS Elastic Beanstalk
3. **Deploys Frontend** - Vue app to S3/CloudFront
4. **Sends Notifications** - Email/Slack/GitHub comments

**Timeline:**
- Tests: ~2 minutes
- Backend Deploy: ~3-5 minutes
- Frontend Deploy: ~2 minutes
- CloudFront Invalidation: ~5-15 minutes
- **Total: ~15-25 minutes**

---

## Required Secrets

Navigate to: **Repository Settings → Secrets and Variables → Actions**

### AWS Credentials (Required)

| Secret Name | Value | Description |
|------------|-------|-------------|
| `AWS_ACCESS_KEY_ID` | Your AWS access key | IAM user with EB, S3, CloudFront access |
| `AWS_SECRET_ACCESS_KEY` | Your AWS secret key | Corresponding secret key |

### Backend Secrets (Required)

| Secret Name | Value | Description |
|------------|-------|-------------|
| `DJANGO_SECRET_KEY` | Your Django secret key | Generate with `python -c 'from django.core.management.utils import get_random_secret_key; print(get_random_secret_key())'` |
| `DB_PASSWORD` | RDS database password | PostgreSQL password |
| `DB_HOST` | `chla-postgres-db.cpkvcu4f59w6.us-west-2.rds.amazonaws.com` | RDS endpoint |

### Frontend Secrets (Required)

| Secret Name | Value | Description |
|------------|-------|-------------|
| `S3_BUCKET_NAME` | `kinddhelp-frontend-1755148345` | S3 bucket for frontend |
| `CLOUDFRONT_DISTRIBUTION_ID` | `E2W6EECHUV4LMM` | CloudFront distribution |

### Notification Secrets (Optional)

| Secret Name | Value | Description |
|------------|-------|-------------|
| `EMAIL` | `your-email@gmail.com` | Email for deployment notifications |
| `EMAIL_USERNAME` | `your-email@gmail.com` | Gmail username (same as EMAIL) |
| `EMAIL_PASSWORD` | Gmail App Password | 16-character app password (see [Email Setup](#email-notifications)) |
| `SLACK_WEBHOOK` | Slack webhook URL | For Slack notifications (see [Slack Setup](#slack-notifications)) |

---

## How It Works

### Workflow File

Located at: `.github/workflows/deploy.yml`

### Trigger Conditions

- **Automatic**: Push to `main` branch
- **Manual**: Via GitHub Actions tab

### Deployment Steps

1. **Checkout Code**: Clone repository
2. **Setup Python**: Configure Python 3.12
3. **Setup Node.js**: Configure Node.js 18
4. **Install Dependencies**: Backend and frontend packages
5. **Run Tests**: Validate code (if tests configured)
6. **Deploy Backend**:
   - Zip application files
   - Upload to Elastic Beanstalk
   - Auto-run migrations via `.ebextensions`
   - Wait for environment to be ready
   - Health check with retry logic
7. **Deploy Frontend**:
   - Build Vue application
   - Sync to S3 bucket
   - Invalidate CloudFront cache
   - Verify frontend accessible
8. **Send Notifications**: Email, Slack, or GitHub comments

### Health Check Logic

The workflow includes intelligent health checking:

```yaml
# Backend health check (retries 3 times with 20-second waits)
- Check valid endpoint: /api/regional-centers/
- Retry if backend is warming up
- Fail deployment if all retries exhausted

# Frontend health check
- Verify homepage loads
- Non-fatal (continues even if CloudFront still updating)
```

---

## Deployment Options

### Automatic Deployment (Recommended)

```bash
# Make changes
git add .
git commit -m "Description of changes"

# Push to main (triggers deployment)
git push origin main
```

### Manual Deployment via GitHub UI

1. Go to repository on GitHub
2. Click **Actions** tab
3. Click **Deploy CHLA Provider Map** workflow
4. Click **Run workflow** button
5. Select `main` branch
6. Click **Run workflow**

### Skip Deployment

Add `[skip ci]` to commit message:

```bash
git commit -m "Update docs [skip ci]"
git push origin main  # Won't trigger deployment
```

---

## Monitoring

### GitHub Actions Dashboard

1. Go to **Actions** tab in repository
2. Click on running workflow
3. Expand steps to see detailed logs
4. Green checkmarks = success
5. Red X = failure (click for error details)

### Check Backend Deployment

```bash
# Via EB CLI
eb status --region us-west-2

# Check health
eb health --region us-west-2

# View logs
eb logs --region us-west-2
```

### Check Frontend Deployment

```bash
# List S3 files
aws s3 ls s3://kinddhelp-frontend-1755148345/ --profile personal

# Check CloudFront invalidation status
aws cloudfront list-invalidations --distribution-id E2W6EECHUV4LMM --profile personal
```

### Verify Live Sites

- **Backend API**: https://api.kinddhelp.com/api/
- **Frontend**: https://kinddhelp.com
- **Admin Portal**: https://api.kinddhelp.com/client-portal/

---

## Notifications

### GitHub Issue Comments (Always Active)

Automatically comments on the latest commit with deployment status. No setup required.

**Example:**
```
Deployment Status: ✅ SUCCESS
Backend: https://api.kinddhelp.com/api/
Frontend: https://kinddhelp.com
```

### Email Notifications

**Setup Gmail App Password:**

1. Go to https://myaccount.google.com/security
2. Enable 2-factor authentication (if not enabled)
3. Search for "App passwords"
4. Select app: **Mail**
5. Generate password
6. Copy 16-character password (no spaces)

**Add GitHub Secrets:**

```
EMAIL = your-email@gmail.com
EMAIL_USERNAME = your-email@gmail.com
EMAIL_PASSWORD = xxxx xxxx xxxx xxxx
```

**Note**: Use app password, NOT your regular Gmail password.

### Slack Notifications

**Setup Slack Webhook:**

1. Go to https://api.slack.com/apps
2. Create new app or select existing
3. Add **Incoming Webhooks** feature
4. Activate incoming webhooks
5. Add webhook to workspace
6. Select channel
7. Copy webhook URL

**Add GitHub Secret:**

```
SLACK_WEBHOOK = https://hooks.slack.com/services/YOUR/WEBHOOK/URL
```

### Testing Notifications

```bash
# Make a small change
echo "# Test" >> README.md
git add README.md
git commit -m "Test notifications"
git push origin main

# Check email/Slack/GitHub for notification
```

---

## Troubleshooting

### Deployment Fails

**Check GitHub Actions Logs:**
1. Go to Actions tab
2. Click failed workflow
3. Expand failed step
4. Read error message

**Common Issues:**

| Issue | Cause | Solution |
|-------|-------|----------|
| Authentication failure | Missing/wrong AWS credentials | Verify `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` secrets |
| Backend 502 error | Backend not ready or down | Wait for warm-up, check EB health: `eb health` |
| S3 sync failure | Wrong bucket name or permissions | Verify `S3_BUCKET_NAME` secret and IAM permissions |
| CloudFront invalidation fails | Wrong distribution ID | Verify `CLOUDFRONT_DISTRIBUTION_ID` secret |
| Migration errors | Database schema issues | Check EB logs, manually run migrations if needed |

### Backend Health Check Fails

**Problem**: Health check returns 502 after retries

**Diagnosis:**
```bash
# Check EB environment health
eb health --region us-west-2

# Check recent logs
eb logs --region us-west-2

# SSH to instance
eb ssh chla-api-prod --region us-west-2
```

**Common Causes:**
- Environment still warming up (wait 1-2 minutes)
- Migration errors (check logs for `python manage.py migrate` output)
- Environment variables not set (verify in EB console)
- Application errors (check Django logs)

**Solution:**
```bash
# If environment is degraded, redeploy manually
eb deploy chla-api-prod --region us-west-2

# Or restart environment
eb restart chla-api-prod --region us-west-2
```

### Frontend Not Updating

**Problem**: Changes not visible on https://kinddhelp.com

**Diagnosis:**
1. Check if S3 files updated:
   ```bash
   aws s3 ls s3://kinddhelp-frontend-1755148345/ --profile personal
   ```
2. Check CloudFront invalidation:
   ```bash
   aws cloudfront list-invalidations --distribution-id E2W6EECHUV4LMM --profile personal
   ```
3. Hard refresh browser: `Cmd+Shift+R` (Mac) or `Ctrl+Shift+R` (Windows)

**Solution:**
```bash
# Manually invalidate CloudFront
aws cloudfront create-invalidation \
  --distribution-id E2W6EECHUV4LMM \
  --paths "/*" \
  --profile personal

# Wait 5-15 minutes for invalidation to complete
```

### Notifications Not Working

**Email not received:**
- Check spam folder
- Verify Gmail app password (NOT regular password)
- Ensure 2FA enabled on Gmail
- Check secret names match exactly: `EMAIL`, `EMAIL_USERNAME`, `EMAIL_PASSWORD`

**Slack not working:**
- Verify webhook URL is correct
- Check webhook is active in Slack app settings
- Ensure secret name is exactly: `SLACK_WEBHOOK`

**GitHub comments not appearing:**
- Check workflow has `GITHUB_TOKEN` permissions
- Verify workflow completed successfully
- Look for comment on latest commit

### Missing Secrets

**Problem**: Deployment fails with "secret not found"

**Solution:**
1. Go to Repository Settings → Secrets and Variables → Actions
2. Verify all required secrets are present
3. Secret names are case-sensitive (use exact names from table above)
4. Re-add any missing secrets
5. Trigger new deployment

### Permission Errors

**Problem**: AWS permission denied errors

**Solution:**

Ensure IAM user has these permissions:
- ElasticBeanstalk: Full access or deploy permissions
- S3: PutObject, DeleteObject, ListBucket for frontend bucket
- CloudFront: CreateInvalidation for distribution
- RDS: Connect permission (if accessing database)

**Minimum IAM Policy:**
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "elasticbeanstalk:*",
        "s3:PutObject",
        "s3:DeleteObject",
        "s3:ListBucket",
        "cloudfront:CreateInvalidation"
      ],
      "Resource": "*"
    }
  ]
}
```

---

## Best Practices

1. **Test Locally First**: Always run `python manage.py check` and `npm run build` before pushing
2. **Small Commits**: Deploy one feature at a time for easier debugging
3. **Monitor Deployments**: Watch GitHub Actions logs during deployment
4. **Backup Before Major Changes**: Create database backup before large migrations
5. **Use Feature Branches**: Develop in feature branches, merge to main when ready
6. **Clear Commit Messages**: Makes tracking deployments easier
7. **Check Health After Deploy**: Verify both frontend and backend work after deployment

---

## Workflow Improvements

### Fixed Issues (Already Implemented)

1. **Health Check URL**: Changed from `/api/` to `/api/regional-centers/` (valid endpoint)
2. **Retry Logic**: Retries health check 3 times with 20-second waits (handles warm-up)
3. **S3 Bucket**: Uses Elastic Beanstalk default bucket (no separate secret needed)
4. **Frontend Check**: Made non-fatal (continues even if CloudFront slow to update)

### Future Enhancements

- Add automated tests to workflow
- Implement staging environment
- Add database backup step before migrations
- Create rollback mechanism
- Add Slack/email digest of recent deployments

---

## Additional Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [AWS Elastic Beanstalk CLI](https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/eb-cli3.html)
- [CloudFront Invalidation](https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/Invalidation.html)
- [Deployment Guide](./DEPLOYMENT.md)
- [Database Sync Guide](./DATABASE_SYNC.md)
- [Troubleshooting Guide](./TROUBLESHOOTING.md)
