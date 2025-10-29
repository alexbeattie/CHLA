# GitHub Secrets Setup Guide

This guide explains how to configure GitHub Secrets for the CI/CD pipeline.

## Required Secrets

The following secrets must be configured in your GitHub repository:

### 1. AWS_ACCESS_KEY_ID
Your AWS access key ID for deployments.

**Where to find it:**
- AWS Console → IAM → Users → Your User → Security credentials → Access keys
- Or from `~/.aws/credentials` file

**Value:**
```
AKIA...
```

### 2. AWS_SECRET_ACCESS_KEY
Your AWS secret access key for deployments.

**Where to find it:**
- This is only shown when creating the access key
- If lost, you must create a new access key pair

**Value:**
```
Your secret key (keep this secure!)
```

### 3. MAPBOX_TOKEN
Mapbox API token for map rendering.

**Current token:**
```
pk.eyJ1IjoiYWxleGJlYXR0aWUiLCJhIjoiOVVEYU52WSJ9.S_uekMjvfZC5_s0dVVJgQg
```

**Where to find it:**
- Mapbox Dashboard → Account → Access tokens

## How to Add Secrets to GitHub

### Via GitHub Web Interface:

1. Go to your repository on GitHub
2. Click **Settings** (top menu)
3. Click **Secrets and variables** → **Actions** (left sidebar)
4. Click **New repository secret**
5. Enter the name (e.g., `AWS_ACCESS_KEY_ID`)
6. Paste the value
7. Click **Add secret**
8. Repeat for all required secrets

### Via GitHub CLI:

```bash
# AWS credentials
gh secret set AWS_ACCESS_KEY_ID --body "AKIA..."
gh secret set AWS_SECRET_ACCESS_KEY --body "your-secret-key"

# Mapbox token
gh secret set MAPBOX_TOKEN --body "pk.eyJ1IjoiYWxleGJlYXR0aWUiLCJhIjoiOVVEYU52WSJ9.S_uekMjvfZC5_s0dVVJgQg"
```

## Verifying Secrets

After adding secrets, verify them:

1. Go to **Settings** → **Secrets and variables** → **Actions**
2. You should see all three secrets listed
3. Note: You cannot view secret values after they're added (security feature)

## IAM Permissions Required

The AWS user associated with the access keys needs the following permissions:

### For Backend Deployment:
- `elasticbeanstalk:*` (Elastic Beanstalk full access)
- `ec2:Describe*` (EC2 describe permissions)
- `s3:*` (S3 full access for EB artifacts)
- `cloudformation:*` (CloudFormation for EB)
- `autoscaling:*` (Auto Scaling for EB)
- `elasticloadbalancing:*` (Load Balancer for EB)

### For Frontend Deployment:
- `s3:ListBucket` on `kinddhelp-frontend-1755148345`
- `s3:PutObject`, `s3:DeleteObject` on `kinddhelp-frontend-1755148345/*`
- `cloudfront:CreateInvalidation` on distribution `E2W6EECHUV4LMM`
- `cloudfront:GetInvalidation` on distribution `E2W6EECHUV4LMM`

### Example IAM Policy:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "elasticbeanstalk:*",
        "s3:*",
        "ec2:*",
        "cloudformation:*",
        "autoscaling:*",
        "elasticloadbalancing:*"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "cloudfront:CreateInvalidation",
        "cloudfront:GetInvalidation"
      ],
      "Resource": "arn:aws:cloudfront::*:distribution/E2W6EECHUV4LMM"
    }
  ]
}
```

## Security Best Practices

1. **Rotate Keys Regularly**: Change AWS access keys every 90 days
2. **Minimum Permissions**: Only grant permissions needed for deployment
3. **Separate Users**: Consider separate IAM users for CI/CD vs personal use
4. **Enable MFA**: Enable MFA on your AWS root account
5. **Monitor Usage**: Check CloudTrail logs for unauthorized access
6. **Delete Old Keys**: Remove unused access keys

## Troubleshooting

### "Invalid AWS credentials" error:
1. Verify secrets are correctly named (exact match, case-sensitive)
2. Check that access keys haven't been rotated/deleted in AWS
3. Verify IAM user has required permissions
4. Try creating new access keys

### "Access Denied" errors:
1. Check IAM policy has required permissions
2. Verify resource ARNs are correct
3. Check if AWS account ID is correct

### Secrets not updating:
1. GitHub caches secrets, wait a few minutes
2. Re-run the workflow after updating secrets
3. Check workflow file references correct secret names

## Local Development

For local development, use AWS profiles instead of secrets:

```bash
# ~/.aws/credentials
[personal]
aws_access_key_id = AKIA...
aws_secret_access_key = your-secret-key

# Use with deploy scripts
AWS_PROFILE=personal ./deploy-all.sh
```

## Support

If you have issues:
1. Check GitHub Actions logs for specific error messages
2. Verify secrets are correctly configured
3. Test AWS credentials locally: `aws sts get-caller-identity`
4. Review IAM policies in AWS Console
