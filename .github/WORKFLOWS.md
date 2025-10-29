# GitHub Actions Workflows

## Quick Reference

### 1. `quick-ci.yml` - Fast Validation (30 seconds)
**Runs on:** Every push to any branch

**Purpose:** Fail fast - catch syntax/import errors in 30 seconds

**Steps:**
1. Python syntax check (~5s)
2. Debug statements check (~2s)
3. Django imports check (~20s)

**On main/develop:** Runs additional full validation (~10 min)

### 2. `deploy.yml` - Production Deployment
**Runs on:** 
- Manual trigger (workflow_dispatch)
- Push to `main` branch

**Steps:**
1. Deploy backend to Elastic Beanstalk (~15 min)
2. Deploy frontend to S3/CloudFront (~5 min)
3. Health checks + invalidation

**Manual Options:**
- Skip backend deployment
- Skip frontend deployment

## Workflow Strategy

```
Push to feature branch → Fast validation (30s)
                         ↓
                      Fails? → Fix immediately
                         ↓
                      Passes → Continue development

Push to main → Fast validation (30s)
               ↓
            Full validation (10 min)
               ↓
            Auto-deploy to production (20 min)
```

## Required Secrets

Configure in GitHub Settings > Secrets:

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`

## Commands

### Trigger manual deployment:
```bash
# Go to Actions tab → Deploy to Production → Run workflow
# Choose options: skip backend/frontend if needed
```

### View workflow status:
```bash
gh run list --workflow=quick-ci.yml
gh run list --workflow=deploy.yml
```

## Safety Measures

1. **Fast fail:** Syntax errors caught in <1 minute
2. **Manual deploy:** Deployments require explicit trigger
3. **Health checks:** Auto-rollback if health check fails
4. **Caching:** Dependencies cached for speed
5. **Timeouts:** All jobs have max timeouts

