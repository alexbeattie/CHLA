# CI/CD Pipeline Setup

This document explains the GitHub Actions CI/CD pipeline for the CHLA Map Application.

## Workflows Overview

### 1. `quick-ci.yml` - Fast Validation
**Triggers:** Every push to any branch, pull requests to main/develop

**What it does:**
- Runs fast syntax checks (2-3 minutes)
- Python syntax validation
- Django configuration check
- Frontend lint check
- Checks for debug statements (console.log, print)
- Triggers full deployment on push to `main` branch

**Duration:** ~3-5 minutes

### 2. `deploy-production.yml` - Production Deployment
**Triggers:**
- Automatically on push to `main` branch (after quick-ci passes)
- Manually via workflow_dispatch

**What it does:**
1. **Backend Deployment:**
   - Deploys Django app to Elastic Beanstalk
   - Verifies API health
   - Tests regional centers endpoint

2. **Frontend Deployment:**
   - Builds Vue.js application
   - Deploys to S3
   - Invalidates CloudFront cache
   - Verifies frontend accessibility

3. **Summary:**
   - Shows deployment status
   - Links to production URLs

**Duration:** ~15-20 minutes

### 3. `ci-cd.yml` - Legacy (Updated)
**Status:** Updated to use GitHub Secrets, but superseded by `deploy-production.yml`

**Note:** This workflow is kept for compatibility but the new `deploy-production.yml` is recommended.

## Manual Deployment Options

### Deploy Everything via GitHub Actions:
1. Go to **Actions** tab in GitHub
2. Select **Deploy to Production**
3. Click **Run workflow**
4. Select branch (usually `main`)
5. Choose what to deploy:
   - Backend: `true`
   - Frontend: `true`
6. Click **Run workflow**

### Deploy Backend Only:
1. Same as above, but set:
   - Backend: `true`
   - Frontend: `false`

### Deploy Frontend Only:
1. Same as above, but set:
   - Backend: `false`
   - Frontend: `true`

## Required GitHub Secrets

Configure these in **Settings → Secrets and variables → Actions**:

1. **AWS_ACCESS_KEY_ID** - AWS access key for deployments
2. **AWS_SECRET_ACCESS_KEY** - AWS secret key for deployments
3. **MAPBOX_TOKEN** - Mapbox API token for maps

See `GITHUB_SECRETS_SETUP.md` for detailed setup instructions.

## Workflow Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  Push to any branch                                         │
└─────────────────┬───────────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────────────┐
│  quick-ci.yml                                               │
│  - Python syntax check                                      │
│  - Django configuration check                               │
│  - Frontend lint                                            │
│  - Debug statement scan                                     │
└─────────────────┬───────────────────────────────────────────┘
                  │
                  │ (only on main branch push)
                  ▼
┌─────────────────────────────────────────────────────────────┐
│  deploy-production.yml                                      │
│                                                             │
│  ┌────────────────┐       ┌────────────────┐              │
│  │ Backend Deploy │       │ Frontend Deploy│              │
│  │ - EB deploy    │  ───► │ - npm build    │              │
│  │ - Health check │       │ - S3 sync      │              │
│  │                │       │ - CF invalidate│              │
│  └────────────────┘       └────────────────┘              │
│                                    │                        │
│                                    ▼                        │
│                          ┌────────────────┐                │
│                          │ Summary        │                │
│                          │ - Status report│                │
│                          │ - URLs         │                │
│                          └────────────────┘                │
└─────────────────────────────────────────────────────────────┘
```

## Environment Variables

Configured in workflow files:

```yaml
AWS_REGION: us-west-2
EB_ENVIRONMENT: chla-api-env
S3_BUCKET: kinddhelp-frontend-1755148345
CLOUDFRONT_DIST_ID: E2W6EECHUV4LMM
```

## Deployment Process

### On Push to Main:

1. **Code pushed to main branch**
2. **Quick CI runs** (~3 min)
   - ✅ Syntax checks pass
   - ✅ No debug statements
3. **Backend deployment starts** (~10 min)
   - 📦 Deploy to Elastic Beanstalk
   - ✅ Verify API health
4. **Frontend deployment starts** (~5 min)
   - 🏗️  Build Vue.js app
   - 📦 Upload to S3
   - 🔄 Invalidate CloudFront
   - ✅ Verify accessibility
5. **Summary generated**
   - 📊 Status report
   - 🌐 Production URLs

**Total time:** ~18-20 minutes

## Monitoring Deployments

### Via GitHub Actions:
1. Go to **Actions** tab
2. Click on the running workflow
3. Watch real-time logs
4. See deployment status

### Via AWS Console:
1. **Backend:** Elastic Beanstalk → Environments → chla-api-env
2. **Frontend:** S3 → kinddhelp-frontend-1755148345
3. **CDN:** CloudFront → Distribution E2W6EECHUV4LMM

### Via CLI:
```bash
# Backend status
eb status chla-api-env --region us-west-2

# Frontend/API health
curl -I https://kinddhelp.com
curl -I https://api.kinddhelp.com/api/regional-centers/
```

## Troubleshooting

### Deployment Fails at Backend:

**Check:**
1. EB environment health in AWS Console
2. GitHub Actions logs for error messages
3. EB logs: `eb logs chla-api-env --region us-west-2`

**Common issues:**
- Database connection (check RDS security groups)
- Environment variables (check .ebextensions)
- Docker build errors (check Dockerfile)

### Deployment Fails at Frontend:

**Check:**
1. Build output in GitHub Actions logs
2. S3 bucket permissions
3. CloudFront distribution status

**Common issues:**
- npm build errors (missing dependencies)
- S3 access denied (check IAM permissions)
- Environment variables not set

### Deployment Succeeds but Site Not Working:

**Check:**
1. CloudFront cache (may take 1-2 minutes)
2. Browser console for errors
3. API endpoints directly: `curl https://api.kinddhelp.com/api/regional-centers/`

**Common issues:**
- Mixed content errors (check HTTPS everywhere)
- CORS issues (check backend CORS settings)
- API URL mismatch (check .env.production)

## Local Development vs CI/CD

| Aspect | Local Development | CI/CD Pipeline |
|--------|------------------|----------------|
| AWS Credentials | `~/.aws/credentials` with profile | GitHub Secrets |
| Deploy Command | `./deploy-all.sh` | Automatic on push to main |
| Build Process | `npm run build` manually | Automated in workflow |
| Testing | Manual | Automated health checks |
| Rollback | Manual EB deploy | Manual workflow rerun with old commit |

## Best Practices

1. **Always test locally first:** Use `./deploy-all.sh` before pushing
2. **Use feature branches:** Don't push directly to main
3. **Watch the deployment:** Monitor GitHub Actions logs
4. **Verify after deployment:** Test the site manually
5. **Check health endpoints:** Ensure API is responding
6. **Monitor costs:** Review AWS billing regularly

## Rollback Procedure

### Via GitHub Actions:
1. Find the last successful deployment commit
2. Revert to that commit
3. Push to main (triggers new deployment)

### Manual Rollback:
```bash
# Backend
cd maplocation
eb deploy chla-api-env --version <previous-version>

# Frontend
cd map-frontend
git checkout <previous-commit>
./deploy.sh
```

## Performance Metrics

- **Quick CI:** ~3-5 minutes
- **Backend Deploy:** ~10-12 minutes
- **Frontend Deploy:** ~3-5 minutes
- **Total Deployment:** ~18-20 minutes
- **CloudFront Propagation:** 1-2 minutes additional

## Security Considerations

1. **Secrets are encrypted** in GitHub
2. **Never log secrets** in workflow output
3. **Rotate AWS keys** every 90 days
4. **Review IAM permissions** regularly
5. **Enable MFA** on AWS account
6. **Monitor CloudTrail** for unauthorized access

## Future Improvements

- [ ] Add automated testing before deployment
- [ ] Implement blue-green deployments
- [ ] Add Slack/email notifications
- [ ] Create staging environment
- [ ] Add performance monitoring
- [ ] Implement automatic rollback on failure
