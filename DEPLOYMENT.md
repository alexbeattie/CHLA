# CHLA Provider Map - Deployment Guide

## Overview

This project uses a scalable, cache-busting deployment system that:
- ✅ Builds frontend with version identifiers
- ✅ Deploys to S3 with proper cache headers
- ✅ Invalidates CloudFront cache
- ✅ Runs database migrations
- ✅ Updates Elastic Beanstalk
- ✅ Performs health checks
- ✅ Supports rollback
- ❌ **NO SSH or SSM required!**

## Quick Start

```bash
# Deploy to dev environment
./deploy.sh dev

# Deploy to staging
./deploy.sh staging

# Deploy to production
./deploy.sh production

# Rollback
./deploy.sh production rollback
```

## Prerequisites

### 1. Install AWS CLI
```bash
# macOS
brew install awscli

# Linux
pip install awscli

# Configure
aws configure
```

### 2. Set up AWS Resources

#### S3 Buckets for Frontend
```bash
aws s3 mb s3://chla-provider-map-frontend-dev
aws s3 mb s3://chla-provider-map-frontend-staging
aws s3 mb s3://chla-provider-map-frontend-prod

# Enable static website hosting
aws s3 website s3://chla-provider-map-frontend-prod \
  --index-document index.html \
  --error-document index.html
```

#### CloudFront Distribution (Production)
```bash
# Create CloudFront distribution pointing to S3 bucket
# Update CLOUDFRONT_ID in deploy.sh with your distribution ID
```

#### Elastic Beanstalk Application
```bash
# Create EB application
aws elasticbeanstalk create-application \
  --application-name chla-provider-map-prod \
  --description "CHLA Provider Map Production"

# Create environment
aws elasticbeanstalk create-environment \
  --application-name chla-provider-map-prod \
  --environment-name chla-provider-map-prod \
  --solution-stack-name "64bit Amazon Linux 2023 v4.0.0 running Python 3.11" \
  --option-settings file://ebconfig.json
```

## How It Works

### Frontend Deployment

1. **Build with Cache Busting**
   - Vite automatically adds hashes to JS/CSS files
   - Build ID and commit hash added as env variables
   - Example: `main.a1b2c3d4.js`

2. **S3 Upload with Smart Caching**
   ```
   HTML files:        no-cache (always check for updates)
   JS/CSS (hashed):   1 year cache (immutable)
   Images/Fonts:      1 month cache
   Other files:       1 hour cache
   ```

3. **CloudFront Invalidation**
   - Invalidates `/*` paths
   - Waits for completion
   - Users get fresh content immediately

### Backend Deployment

1. **Database Migrations**
   ```bash
   python manage.py migrate --no-input
   ```

2. **Static Files**
   ```bash
   python manage.py collectstatic --no-input --clear
   ```

3. **Elastic Beanstalk**
   - Creates versioned application bundle
   - Uploads to S3
   - Updates environment
   - Zero-downtime deployment

## Cache Strategy

### Why Cache Busting Matters

Without proper cache busting:
- Users see old JavaScript after deployment
- "Hard refresh" (Cmd+Shift+R) required
- Support nightmare!

With cache busting:
- JS/CSS files have unique names each build
- HTML is never cached
- Users always get latest code

### Cache Headers Explained

| File Type | Cache-Control | Why |
|-----------|---------------|-----|
| `index.html` | `no-cache` | Entry point, must check for updates |
| `main.[hash].js` | `max-age=31536000, immutable` | Hash changes = new file |
| `logo.png` | `max-age=2592000` | Images rarely change |
| `manifest.json` | `max-age=3600` | Config files, short cache |

## Troubleshooting

### Issue: Users seeing old code

**Solution:** Check CloudFront invalidation
```bash
aws cloudfront list-invalidations \
  --distribution-id YOUR_DISTRIBUTION_ID
```

### Issue: Deployment fails at migration

**Solution:** Check database connectivity
```bash
aws elasticbeanstalk describe-environment-health \
  --environment-name chla-provider-map-prod \
  --attribute-names All
```

### Issue: Health check fails

**Solution:** Check application logs
```bash
aws elasticbeanstalk describe-events \
  --environment-name chla-provider-map-prod \
  --max-records 50
```

## Rollback

If deployment fails:

```bash
# Interactive rollback
./deploy.sh production rollback

# Or manually
aws elasticbeanstalk update-environment \
  --application-name chla-provider-map-prod \
  --environment-name chla-provider-map-prod \
  --version-label v-20240115-abc123
```

## GitHub Actions (Coming Soon)

For automated CI/CD, GitHub Actions workflow will:
1. Run on push to `main`, `staging`, `develop`
2. Run tests
3. Build frontend & backend
4. Deploy to appropriate environment
5. Run health checks
6. Notify on Slack/email

## Environment Variables

### Frontend (.env files)
```bash
# map-frontend/.env.production
VITE_API_BASE_URL=https://api.chla-provider-map.com
VITE_MAPBOX_TOKEN=your_mapbox_token
```

### Backend (Elastic Beanstalk Environment Properties)
```bash
# Set via AWS Console or CLI
DJANGO_SETTINGS_MODULE=maplocation.settings
SECRET_KEY=your_secret_key
DATABASE_URL=postgres://...
AWS_STORAGE_BUCKET_NAME=chla-static-files
```

## Performance Monitoring

After deployment, monitor:

1. **CloudFront Cache Hit Ratio**
   - Should be > 90% for static assets
   - Check in CloudFront console

2. **S3 Request Metrics**
   - Low request count = good caching
   - Check in S3 console

3. **Application Performance**
   - Use New Relic/DataDog/CloudWatch
   - Monitor response times

## Cost Optimization

- S3: ~$0.023/GB/month
- CloudFront: ~$0.085/GB transfer
- With proper caching: ~$50/month for 10K users

## Security

- S3 bucket: **Block public access**, use CloudFront only
- CloudFront: **HTTPS only**, custom domain
- Elastic Beanstalk: **VPC**, security groups
- Secrets: **AWS Secrets Manager**, not environment variables

## Next Steps

1. [ ] Configure CloudFront distribution
2. [ ] Set up custom domain with Route 53
3. [ ] Add SSL certificate (AWS Certificate Manager)
4. [ ] Configure GitHub Actions
5. [ ] Set up monitoring (CloudWatch Alarms)
6. [ ] Add Slack notifications

## Support

For issues, check:
- Deployment logs: `deploy_YYYYMMDD_HHMMSS.log`
- AWS CloudWatch logs
- Elastic Beanstalk event logs

## References

- [Vite Build Configuration](https://vitejs.dev/guide/build.html)
- [S3 Static Website Hosting](https://docs.aws.amazon.com/AmazonS3/latest/userguide/WebsiteHosting.html)
- [CloudFront Caching](https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/Expiration.html)
- [Elastic Beanstalk Deployment](https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/using-features.deploy-existing-version.html)
