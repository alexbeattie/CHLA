#!/bin/bash
set -e

echo "=== Verifying GitHub Secrets ==="
echo ""

# Check if secrets exist
echo "Current secrets in repo:"
gh secret list
echo ""

# Extract AWS credentials
echo "Extracting AWS credentials from ~/.aws/credentials..."
ACCESS_KEY=$(grep -A 2 "\[personal\]" ~/.aws/credentials | grep aws_access_key_id | cut -d '=' -f 2 | xargs)
SECRET_KEY=$(grep -A 2 "\[personal\]" ~/.aws/credentials | grep aws_secret_access_key | cut -d '=' -f 2 | xargs)

if [ -z "$ACCESS_KEY" ] || [ -z "$SECRET_KEY" ]; then
    echo "ERROR: Could not extract AWS credentials"
    exit 1
fi

echo "Found credentials:"
echo "  Access Key: ${ACCESS_KEY:0:10}...${ACCESS_KEY: -4}"
echo "  Secret Key: ${SECRET_KEY:0:10}...${SECRET_KEY: -4}"
echo ""

# Set secrets with explicit stdin
echo "Setting AWS_ACCESS_KEY_ID..."
echo -n "$ACCESS_KEY" | gh secret set AWS_ACCESS_KEY_ID

echo "Setting AWS_SECRET_ACCESS_KEY..."
echo -n "$SECRET_KEY" | gh secret set AWS_SECRET_ACCESS_KEY

echo ""
echo "=== Secrets set successfully! ==="
echo ""
echo "Verifying:"
gh secret list

