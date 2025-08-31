# GitHub Actions CI/CD Setup

## ✅ Your Automatic Deployment is Already Set Up!

When you push to the `main` branch, GitHub Actions will automatically:
1. Run tests
2. Deploy backend to Elastic Beanstalk
3. Deploy frontend to S3/CloudFront

## 🔑 Required GitHub Secrets

Make sure these secrets are set in your GitHub repository settings:
(Settings → Secrets and variables → Actions)

### AWS Credentials
- `AWS_ACCESS_KEY_ID` - Your AWS access key
- `AWS_SECRET_ACCESS_KEY` - Your AWS secret key

### Backend Secrets
- `DJANGO_SECRET_KEY` - Your Django secret key
- `DB_PASSWORD` - Your RDS database password (CHLASecure2024)
- `DB_HOST` - chla-postgres-db.cpkvcu4f59w6.us-west-2.rds.amazonaws.com

### Frontend Secrets
- `S3_BUCKET_NAME` - kinddhelp-frontend-1755148345
- `CLOUDFRONT_DISTRIBUTION_ID` - E2W6EECHUV4LMM

## 📝 How to Add/Update Secrets

```bash
# Go to your GitHub repository
# Click Settings → Secrets and variables → Actions → New repository secret

# Add each secret with these exact names and values
```

## 🚀 How to Deploy

### Automatic Deployment
```bash
# Just push to main branch
git add .
git commit -m "Your changes"
git push origin main

# GitHub Actions will automatically deploy everything!
```

### Manual Deployment
1. Go to your GitHub repository
2. Click "Actions" tab
3. Click "Deploy CHLA Provider Map"
4. Click "Run workflow"
5. Select "main" branch
6. Click "Run workflow"

## 📊 Monitor Deployment

1. Go to GitHub Actions tab
2. Click on the running workflow
3. Watch the progress in real-time
4. Green checkmarks = success!

## ⏱️ Deployment Timeline

- **Tests**: ~2 minutes
- **Backend Deploy**: ~3-5 minutes
- **Frontend Deploy**: ~2 minutes
- **CloudFront Invalidation**: ~5-15 minutes
- **Total**: ~15-25 minutes for full deployment

## 🔍 If Deployment Fails

Check the GitHub Actions logs:
1. Click on the failed job
2. Expand the failed step
3. Read the error message

Common issues:
- Missing GitHub secrets
- AWS credentials expired
- Build errors in code
