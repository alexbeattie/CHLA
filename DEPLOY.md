# CHLA Map Application - Deployment Guide

## Quick Start

### Deploy Everything
```bash
./deploy-all.sh
```

### Deploy Backend Only
```bash
cd maplocation
./deploy.sh
```

### Deploy Frontend Only
```bash
cd map-frontend
./deploy.sh
```

## Architecture

### Frontend
- **Framework**: Vue.js 3 with Vite
- **Hosting**: AWS S3 + CloudFront
- **URL**: https://kinddhelp.com
- **Distribution ID**: E2W6EECHUV4LMM
- **S3 Bucket**: kinddhelp-frontend-1755148345
- **AWS Profile**: personal

### Backend
- **Framework**: Django with GeoDjango/PostGIS
- **Hosting**: AWS Elastic Beanstalk (Docker)
- **URL**: https://api.kinddhelp.com
- **Environment**: chla-api-env
- **Region**: us-west-2
- **SSL Certificate**: Managed via ACM

### Database
- **Service**: AWS RDS PostgreSQL with PostGIS
- **Host**: chla-postgres-db.cpkvcu4f59w6.us-west-2.rds.amazonaws.com
- **Extensions**: PostGIS for geographic queries

## Prerequisites

### AWS CLI
Ensure AWS CLI is installed and configured with the `personal` profile:
```bash
aws configure --profile personal
```

### EB CLI
Install Elastic Beanstalk CLI:
```bash
pip install awsebcli
```

### Node.js
Install Node.js dependencies:
```bash
cd map-frontend
npm install
```

## Environment Variables

### Frontend (.env.production)
```bash
VITE_API_BASE_URL=https://api.kinddhelp.com
VITE_MAPBOX_TOKEN=pk.eyJ1IjoiYWxleGJlYXR0aWUiLCJhIjoiOVVEYU52WSJ9.S_uekMjvfZC5_s0dVVJgQg
```

### Backend (.ebextensions/03_env_vars.config)
```yaml
option_settings:
  aws:elasticbeanstalk:application:environment:
    DJANGO_DEBUG: "false"
    DJANGO_SETTINGS_MODULE: "maplocation.settings"
    DB_HOST: "chla-postgres-db.cpkvcu4f59w6.us-west-2.rds.amazonaws.com"
    DB_NAME: "postgres"
    DB_USER: "chla_admin"
    DB_PORT: "5432"
    DB_SSL_REQUIRE: "true"
    ALLOWED_HOSTS: "*"
    CSRF_TRUSTED_ORIGINS: "https://api.kinddhelp.com,https://kinddhelp.com,https://www.kinddhelp.com"
```

## Deployment Process

### Frontend Deployment Steps
1. Update `.env.production` with production API URL
2. Build the application: `npm run build`
3. Sync to S3: `aws s3 sync dist/ s3://kinddhelp-frontend-1755148345 --delete`
4. Invalidate CloudFront: `aws cloudfront create-invalidation --distribution-id E2W6EECHUV4LMM --paths "/*"`
5. Wait 1-2 minutes for cache invalidation

### Backend Deployment Steps
1. Commit all changes to git
2. Deploy to EB: `eb deploy chla-api-env --region us-west-2`
3. Monitor deployment: `eb health chla-api-env`
4. Check logs if needed: `eb logs chla-api-env`

## DNS Configuration

### Route53 Records (hosted zone: Z0467239OKDU4Z74D3ZB)
- **kinddhelp.com** → CloudFront distribution
- **www.kinddhelp.com** → CloudFront distribution
- **api.kinddhelp.com** → chla-api-env.eba-ehmtxp3g.us-west-2.elasticbeanstalk.com (CNAME)

## SSL/TLS Certificates

### Frontend Certificate
- Managed by CloudFront (automatic)
- Covers: kinddhelp.com, www.kinddhelp.com

### Backend Certificate
- **ARN**: arn:aws:acm:us-west-2:453324135535:certificate/38d0e387-737d-4c48-9760-71de6f9cf9d6
- **Domain**: api.kinddhelp.com
- **Validation**: DNS (automatic via Route53)

## Troubleshooting

### Frontend Issues

**Cache not updating:**
```bash
# Invalidate CloudFront cache
AWS_PROFILE=personal aws cloudfront create-invalidation \
    --distribution-id E2W6EECHUV4LMM \
    --paths "/*"
```

**Mixed content errors:**
- Ensure `.env.production` uses HTTPS for API URL
- Rebuild and redeploy frontend

### Backend Issues

**Deployment failed:**
```bash
# Check environment health
eb health chla-api-env --region us-west-2

# View recent logs
eb logs chla-api-env --region us-west-2
```

**HTTPS not working:**
```bash
# Check load balancer listeners
aws elbv2 describe-listeners \
    --load-balancer-arn $(aws elasticbeanstalk describe-environment-resources \
    --environment-name chla-api-env --region us-west-2 \
    --query 'EnvironmentResources.LoadBalancers[0].Name' --output text)

# Verify certificate
aws acm describe-certificate \
    --certificate-arn arn:aws:acm:us-west-2:453324135535:certificate/38d0e387-737d-4c48-9760-71de6f9cf9d6 \
    --region us-west-2
```

### Database Issues

**Connection problems:**
```bash
# Check security group allows EB instance
aws ec2 describe-security-groups \
    --group-ids $(aws rds describe-db-instances \
    --db-instance-identifier chla-postgres-db \
    --query 'DBInstances[0].VpcSecurityGroups[0].VpcSecurityGroupId' \
    --output text)
```

## Monitoring

### Check Frontend
```bash
curl -I https://kinddhelp.com
```

### Check Backend API
```bash
# Regional centers endpoint
curl https://api.kinddhelp.com/api/regional-centers/ | jq '.count'

# Provider search
curl "https://api.kinddhelp.com/api/providers-v2/by_regional_center/?zip_code=91403" | jq '.count'
```

### Check EB Environment
```bash
eb status chla-api-env --region us-west-2
eb health chla-api-env --region us-west-2
```

## Rollback

### Frontend Rollback
1. Find previous version in S3
2. Deploy old version:
   ```bash
   aws s3 sync s3://kinddhelp-frontend-1755148345-backup/ s3://kinddhelp-frontend-1755148345/
   ```

### Backend Rollback
```bash
# List application versions
eb appversion -a maplocation

# Deploy previous version
eb deploy chla-api-env --version <version-label>
```

## Cost Optimization

- CloudFront: ~$0.085/GB for first 10TB
- S3: ~$0.023/GB for storage
- EB: t3.small instance (~$15/month)
- RDS: db.t3.micro (~$15/month)
- ACM Certificates: Free

**Estimated monthly cost**: ~$35-50 USD

## Support

For issues or questions:
1. Check EB logs: `eb logs chla-api-env`
2. Check browser console for frontend errors
3. Review this deployment guide
4. Check AWS CloudWatch for detailed metrics
