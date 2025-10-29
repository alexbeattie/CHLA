#!/bin/bash
# Set GitHub secrets from AWS credentials

set -e

echo "Setting GitHub secrets from AWS personal profile..."

# Extract credentials
ACCESS_KEY=$(grep -A 2 "\[personal\]" ~/.aws/credentials | grep aws_access_key_id | cut -d '=' -f 2 | xargs)
SECRET_KEY=$(grep -A 2 "\[personal\]" ~/.aws/credentials | grep aws_secret_access_key | cut -d '=' -f 2 | xargs)

if [ -z "$ACCESS_KEY" ] || [ -z "$SECRET_KEY" ]; then
    echo "Error: Could not extract AWS credentials from personal profile"
    exit 1
fi

echo "Access key found: ${ACCESS_KEY:0:10}..."
echo "Secret key found: ${SECRET_KEY:0:10}..."

# Set secrets
echo "$ACCESS_KEY" | gh secret set AWS_ACCESS_KEY_ID
echo "$SECRET_KEY" | gh secret set AWS_SECRET_ACCESS_KEY

echo ""
echo "Secrets set successfully!"
echo ""
echo "Verifying:"
gh secret list

echo ""
echo "Done! You can now re-run the GitHub Action."

