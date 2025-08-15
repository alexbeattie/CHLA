# GitHub CI/CD Setup Guide

## Quick Setup Steps

### 1. Create GitHub Repository

1. Go to [GitHub](https://github.com) and create a new repository
2. Name it something like `chla-provider-map`
3. Make it private (recommended for now)
4. Don't initialize with README (we have one)

### 2. Push Your Code to GitHub

```bash
cd /Users/alexbeattie/Documents/Cline/CHLAProj

# Initialize git if not already done
git init

# Add all files
git add .

# Commit
git commit -m "Initial commit - CHLA Provider Map with mobile sidebar"

# Add remote (replace YOUR_USERNAME with your GitHub username)
git remote add origin https://github.com/YOUR_USERNAME/chla-provider-map.git

# Push to GitHub
git push -u origin main
```

### 3. Set Up GitHub Secrets

Go to your repository → Settings → Secrets and variables → Actions

Click "New repository secret" and add each of these:

#### AWS Credentials
```
Name: AWS_ACCESS_KEY_ID
Value: [Your AWS access key]

Name: AWS_SECRET_ACCESS_KEY  
Value: [Your AWS secret key]
```

#### AWS Configuration
```
Name: EB_APPLICATION_NAME
Value: chla-api

Name: EB_ENVIRONMENT_NAME
Value: chla-api-env-v2

Name: S3_BUCKET_NAME
Value: kinddhelp-frontend-1755148345

Name: CLOUDFRONT_DISTRIBUTION_ID
Value: E2W6EECHUV4LMM
```

#### Database & Security
```
Name: DJANGO_SECRET_KEY
Value: [Generate a new one with: python -c "from django.core.management.utils import get_random_secret_key; print(get_random_secret_key())"]

Name: DB_PASSWORD
Value: CHLASecure2024

Name: DB_HOST
Value: chla-postgres-db.cpkvcu4f59w6.us-west-2.rds.amazonaws.com
```

### 4. Configure EB CLI for GitHub Actions

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
  profile: null
  repository: null
  sc: git
  workspace_type: Application
```

### 5. Test the Deployment

1. Push a small change to trigger deployment:
   ```bash
   echo "# CHLA Provider Map" > README.md
   git add README.md
   git commit -m "Add README to test deployment"
   git push
   ```

2. Go to your repository → Actions tab
3. Watch the deployment process
4. Check that your site updates at https://kinddhelp.com

### 6. Workflow Triggers

The GitHub Actions workflow will trigger on:
- **Push to main branch**: Full deployment (backend + frontend)
- **Pull request**: Testing only (no deployment)

### 7. Manual Deployment (if needed)

You can also trigger deployments manually:
1. Go to repository → Actions
2. Select "Deploy CHLA Provider Map" workflow
3. Click "Run workflow"
4. Choose the branch and click "Run workflow"

## Workflow Overview

The GitHub Actions workflow does:

1. **Test Job**:
   - Checks out code
   - Sets up Python 3.12 and Node.js 18
   - Installs dependencies
   - Runs backend checks
   - Builds frontend
   - Uploads build artifacts

2. **Deploy Backend Job** (main branch only):
   - Deploys Django app to Elastic Beanstalk
   - Updates environment variables
   - Verifies deployment

3. **Deploy Frontend Job** (main branch only):
   - Builds Vue.js app with production settings
   - Deploys to S3
   - Invalidates CloudFront cache
   - Verifies deployment

4. **Notify Job**:
   - Reports deployment status

## Benefits of GitHub CI/CD

✅ **Automated deployments** - No more manual commands
✅ **Consistent builds** - Same environment every time
✅ **Testing** - Catches issues before deployment
✅ **Rollback capability** - Easy to revert changes
✅ **Audit trail** - See exactly what was deployed when
✅ **Parallel deployments** - Backend and frontend deploy simultaneously

## Monitoring Deployments

### GitHub Actions UI
- Repository → Actions tab
- See real-time deployment progress
- View logs for debugging

### AWS Services
- **Elastic Beanstalk**: Monitor backend health
- **CloudFront**: Check cache invalidation status
- **S3**: Verify frontend files updated

### Quick Health Checks
```bash
# Check backend
curl -I https://api.kinddhelp.com/api/

# Check frontend
curl -I https://kinddhelp.com/

# Check mobile sidebar code is deployed
curl -s https://kinddhelp.com/ | grep -i "mobile-toggle"
```

## Troubleshooting GitHub Actions

### Common Issues

1. **AWS Credentials Error**
   - Verify AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY are correct
   - Check IAM permissions

2. **EB Deploy Failed**
   - Check EB_APPLICATION_NAME and EB_ENVIRONMENT_NAME
   - Verify EB CLI configuration

3. **S3 Upload Failed**
   - Check S3_BUCKET_NAME exists
   - Verify bucket permissions

4. **CloudFront Invalidation Failed**
   - Check CLOUDFRONT_DISTRIBUTION_ID is correct
   - Verify CloudFront permissions

### Debugging Steps

1. **Check Action Logs**:
   - Go to Actions tab → Failed workflow → Click job → Expand failing step

2. **Test Locally**:
   - Run the same commands locally to isolate issues

3. **Check AWS Console**:
   - Verify resources exist and have correct names
   - Check IAM permissions

## Advanced Configuration

### Environment-Specific Deployments

You can set up staging environments:

1. Create staging AWS resources
2. Add staging secrets (with `_STAGING` suffix)
3. Modify workflow to deploy staging on feature branches

### Custom Deployment Conditions

Modify `.github/workflows/deploy.yml` to:
- Deploy only on specific file changes
- Require manual approval for production
- Deploy different branches to different environments

### Notifications

Add Slack/Discord notifications:
```yaml
- name: Notify Slack
  if: always()
  uses: 8398a7/action-slack@v3
  with:
    status: ${{ job.status }}
    webhook_url: ${{ secrets.SLACK_WEBHOOK }}
```

## Security Best Practices

1. **Use least-privilege IAM roles** for GitHub Actions
2. **Rotate AWS keys** regularly
3. **Use environment-specific secrets** for staging/production
4. **Enable branch protection rules** requiring PR reviews
5. **Use dependabot** for automatic security updates

---

🎉 **You're all set!** Every push to main will now automatically deploy your application.
