# GitHub Secrets Configuration

This document lists all GitHub secrets required for the CI/CD pipeline to function properly.

## Required Secrets

Configure these secrets in your GitHub repository settings:
**Settings → Secrets and variables → Actions → Repository secrets**

### AWS Configuration

| Secret Name | Description | Example Value |
|------------|-------------|---------------|
| `AWS_ACCESS_KEY_ID` | AWS access key for deployments | `AKIAIOSFODNN7EXAMPLE` |
| `AWS_SECRET_ACCESS_KEY` | AWS secret access key | `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY` |

### Database Configuration (RDS)

| Secret Name | Description | Example Value |
|------------|-------------|---------------|
| `RDS_DB_NAME` | RDS database name | `production_db` |
| `RDS_DB_USER` | RDS database user | `admin` |
| `RDS_DB_PASSWORD` | RDS database password | `SecurePassword123!` |
| `RDS_DB_HOST` | RDS database host | `mydb.123456.us-west-2.rds.amazonaws.com` |
| `RDS_DB_PORT` | RDS database port | `5432` |
| `RDS_INSTANCE_ID` | RDS instance identifier | `chla-prod-db` |

### Django Configuration

| Secret Name | Description | Example Value |
|------------|-------------|---------------|
| `DJANGO_SECRET_KEY` | Django secret key (generate new) | `django-insecure-...` |

### Deployment URLs

| Secret Name | Description | Example Value |
|------------|-------------|---------------|
| `BACKEND_URL` | Production backend URL | `https://api.kinddhelp.com` |
| `FRONTEND_URL` | Production frontend URL | `https://kinddhelp.com` |
| `S3_BUCKET` | S3 bucket name for frontend | `kinddhelp-frontend-1755148345` |
| `CLOUDFRONT_DISTRIBUTION_ID` | CloudFront distribution ID | `E2W6EECHUV4LMM` |

## How to Set Secrets

### Via GitHub UI

1. Go to your repository on GitHub
2. Click **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Enter the name and value
5. Click **Add secret**

### Via GitHub CLI

```bash
# Set AWS credentials
gh secret set AWS_ACCESS_KEY_ID --body "your-access-key"
gh secret set AWS_SECRET_ACCESS_KEY --body "your-secret-key"

# Set RDS credentials
gh secret set RDS_DB_NAME --body "your-db-name"
gh secret set RDS_DB_USER --body "your-db-user"
gh secret set RDS_DB_PASSWORD --body "your-db-password"
gh secret set RDS_DB_HOST --body "your-db-host"
gh secret set RDS_DB_PORT --body "5432"
gh secret set RDS_INSTANCE_ID --body "your-rds-instance-id"

# Set Django secret key (generate with: python -c 'from django.core.management.utils import get_random_secret_key; print(get_random_secret_key())')
gh secret set DJANGO_SECRET_KEY --body "your-django-secret-key"

# Set deployment URLs
gh secret set BACKEND_URL --body "https://api.kinddhelp.com"
gh secret set FRONTEND_URL --body "https://kinddhelp.com"
gh secret set S3_BUCKET --body "your-s3-bucket-name"
gh secret set CLOUDFRONT_DISTRIBUTION_ID --body "your-cloudfront-id"
```

## Generating Secrets

### Django Secret Key

```bash
python -c 'from django.core.management.utils import get_random_secret_key; print(get_random_secret_key())'
```

### AWS Credentials

1. Log into AWS Console
2. Go to **IAM** → **Users** → **Your User**
3. Click **Security credentials**
4. Click **Create access key**
5. Choose **Command Line Interface (CLI)**
6. Download and save the credentials

### RDS Credentials

Get these from your RDS instance:
1. AWS Console → **RDS** → **Databases**
2. Click your database instance
3. **Connectivity & security** tab shows the endpoint (host)
4. Username and password are set when creating the instance

## Security Best Practices

- ✅ Never commit secrets to the repository
- ✅ Rotate secrets regularly (every 90 days recommended)
- ✅ Use different secrets for production and staging
- ✅ Limit AWS IAM permissions to minimum required
- ✅ Enable AWS CloudTrail for audit logging
- ❌ Never use production secrets in development
- ❌ Never share secrets via email or chat

## IAM Permissions Required

The AWS access key needs the following permissions:

### Elastic Beanstalk
- `elasticbeanstalk:*`

### S3
- `s3:ListBucket`
- `s3:GetObject`
- `s3:PutObject`
- `s3:DeleteObject`
- `s3:PutObjectAcl`

### CloudFront
- `cloudfront:CreateInvalidation`
- `cloudfront:GetInvalidation`

### RDS
- `rds:CreateDBSnapshot`
- `rds:DescribeDBSnapshots`
- `rds:DescribeDBInstances`

## Troubleshooting

### Deployment fails with "Access Denied"
- Check that AWS credentials are correct
- Verify IAM permissions are configured
- Ensure region matches (us-west-2)

### Database connection fails
- Verify RDS credentials
- Check RDS security group allows connections from EB
- Ensure SSL is configured if `DB_SSL_REQUIRE=true`

### Frontend not updating
- Verify S3 bucket name is correct
- Check CloudFront distribution ID
- Ensure CloudFront invalidation completed

### Health check fails
- Verify `BACKEND_URL` is accessible
- Check Django settings for `ALLOWED_HOSTS`
- Ensure database migrations completed

## Contact

For questions about secrets configuration, contact the DevOps team or repository maintainer.
