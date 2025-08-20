#!/bin/bash
set -euo pipefail

echo "🔧 Fixing CHLA deployment to simple, working configuration..."

# 1. Update Route 53 to point api.kinddhelp.com to current EB environment
echo "📍 Updating DNS for api.kinddhelp.com..."
cat > /tmp/route53-change.json << EOF
{
    "Changes": [{
        "Action": "UPSERT",
        "ResourceRecordSet": {
            "Name": "api.kinddhelp.com.",
            "Type": "CNAME",
            "TTL": 60,
            "ResourceRecords": [{
                "Value": "chla-api-env-v2.eba-9aiqcppx.us-west-2.elasticbeanstalk.com"
            }]
        }
    }]
}
EOF

aws route53 change-resource-record-sets \
    --hosted-zone-id Z0467239OKDU4Z74D3ZB \
    --change-batch file:///tmp/route53-change.json \
    --profile personal

echo "✅ DNS updated"

# 2. Update EB environment variables
echo "🔧 Updating Elastic Beanstalk environment variables..."
cd maplocation
eb use chla-api-env-v2 --profile personal --region us-west-2
eb setenv \
  DJANGO_DEBUG=false \
  ALLOWED_HOSTS='api.kinddhelp.com,chla-api-env-v2.eba-9aiqcppx.us-west-2.elasticbeanstalk.com,.elasticbeanstalk.com' \
  CORS_ALLOWED_ORIGINS='https://kinddhelp.com,https://www.kinddhelp.com,http://localhost:3000' \
  CSRF_TRUSTED_ORIGINS='https://api.kinddhelp.com,https://kinddhelp.com' \
  --profile personal --region us-west-2

echo "✅ Backend configured"

# 3. Build and deploy frontend with correct API URL
echo "🚀 Building and deploying frontend..."
cd ../map-frontend

# Create production env file
cat > .env.production << EOF
VITE_API_BASE_URL=https://api.kinddhelp.com
VITE_MAPBOX_TOKEN=pk.eyJ1IjoiYmVhdHR5LWFkbWluIiwiYSI6ImNsejFjNGt0YzFqMGMyanF3YW5hdWFmc3UifQ.sn7Uj_gDzzKL6PQq7vO7fw
EOF

# Build
npm run build

# Deploy to ORIGINAL S3 bucket
aws s3 sync dist/ s3://kinddhelp-frontend-1755148345 --delete --profile personal --region us-west-2

# Invalidate CloudFront
aws cloudfront create-invalidation --distribution-id E2W6EECHUV4LMM --paths "/*" --profile personal

echo "✅ Frontend deployed"

# 4. Clean up files
echo "🧹 Archiving temporary files..."
cd ..
mkdir -p archive/deployment-attempts
mv -f cloudfront-update.json archive/deployment-attempts/ 2>/dev/null || true
mv -f update_cloudfront.py archive/deployment-attempts/ 2>/dev/null || true
mv -f check_cloudfront.sh archive/deployment-attempts/ 2>/dev/null || true
mv -f map-frontend/current-config.json archive/deployment-attempts/ 2>/dev/null || true
mv -f updated-config.json archive/deployment-attempts/ 2>/dev/null || true

echo "✅ Cleanup complete"

echo ""
echo "🎉 DEPLOYMENT FIXED!"
echo ""
echo "Your app is now accessible at:"
echo "  Frontend: https://kinddhelp.com"
echo "  API: https://api.kinddhelp.com"
echo ""
echo "DNS propagation may take 1-2 minutes."
echo ""
echo "To test:"
echo "  curl https://api.kinddhelp.com/api/providers/"
echo ""
