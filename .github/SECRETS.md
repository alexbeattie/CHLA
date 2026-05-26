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

### Database & Django Secrets — AWS Secrets Manager

Production DB credentials and the Django `SECRET_KEY` are **not** stored as
GitHub secrets or in `.ebextensions`. They live in AWS Secrets Manager and
are fetched at container startup by `maplocation/docker-entrypoint.sh` using
the EB instance role (`aws-elasticbeanstalk-ec2-role`).

| Secret ID | Shape | Consumer |
|---|---|---|
| `kindd/prod/rds` | JSON: `{host, port, dbname, username, password, sslmode}` | `docker-entrypoint.sh` (prod) and `scripts/_rds_env.py` (dev laptop) |
| `kindd/prod/django-secret-key` | Plain string | `docker-entrypoint.sh` |

**Required IAM**: the EB instance role needs `secretsmanager:GetSecretValue`
on `arn:aws:secretsmanager:us-west-2:<account>:secret:kindd/prod/*` (note the
trailing `-*` for the version-suffix wildcard). Developer IAM identities
need the same permission to run the ops scripts under `maplocation/scripts/`.

**Rotating**: update the secret value via `aws secretsmanager update-secret`,
then redeploy or restart the EB environment so the entrypoint refetches.

### Django Configuration (other)

| Secret Name | Description | Example Value |
|------------|-------------|---------------|

(Most Django config flows through Secrets Manager now; only non-secret
configuration belongs in `.ebextensions/03_env_vars.config`.)

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

# RDS credentials and DJANGO_SECRET_KEY are NOT GitHub secrets — they live
# in AWS Secrets Manager (kindd/prod/rds and kindd/prod/django-secret-key).
# See the "Database & Django Secrets" section above.

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

- Never commit secrets to the repository
- Rotate secrets regularly (every 90 days recommended)
- Use different secrets for production and staging
- Limit AWS IAM permissions to minimum required
- Enable AWS CloudTrail for audit logging
- Never use production secrets in development
- Never share secrets via email or chat

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
