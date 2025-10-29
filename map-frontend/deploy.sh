#!/bin/bash

# Frontend Deployment Script for CHLA Map Application
# Deploys to S3 and invalidates CloudFront cache

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
S3_BUCKET="kinddhelp-frontend-1755148345"
CLOUDFRONT_DIST_ID="E2W6EECHUV4LMM"
AWS_PROFILE="personal"

echo "🚀 Starting Frontend Deployment to Production..."
echo ""

# Step 1: Ensure we're using production environment
echo "1️⃣  Setting production environment..."
cat > "$SCRIPT_DIR/.env.production" << 'EOF'
VITE_API_BASE_URL=https://api.kinddhelp.com
VITE_MAPBOX_TOKEN=pk.eyJ1IjoiYWxleGJlYXR0aWUiLCJhIjoiOVVEYU52WSJ9.S_uekMjvfZC5_s0dVVJgQg
EOF
echo "   ✅ Production environment configured"
echo ""

# Step 2: Build the frontend
echo "2️⃣  Building frontend..."
npm run build
if [ $? -ne 0 ]; then
    echo "   ❌ Build failed!"
    exit 1
fi
echo "   ✅ Build completed successfully"
echo ""

# Step 3: Deploy to S3
echo "3️⃣  Deploying to S3 bucket: $S3_BUCKET..."
AWS_PROFILE=$AWS_PROFILE aws s3 sync dist/ s3://$S3_BUCKET --delete
if [ $? -ne 0 ]; then
    echo "   ❌ S3 deployment failed!"
    exit 1
fi
echo "   ✅ Deployed to S3 successfully"
echo ""

# Step 4: Invalidate CloudFront cache
echo "4️⃣  Invalidating CloudFront cache..."
INVALIDATION_ID=$(AWS_PROFILE=$AWS_PROFILE aws cloudfront create-invalidation \
    --distribution-id $CLOUDFRONT_DIST_ID \
    --paths "/*" \
    --query 'Invalidation.Id' \
    --output text)

if [ $? -ne 0 ]; then
    echo "   ❌ CloudFront invalidation failed!"
    exit 1
fi
echo "   ✅ CloudFront invalidation created: $INVALIDATION_ID"
echo ""

# Step 5: Verify deployment
echo "5️⃣  Verifying deployment..."
sleep 5
echo "   Testing frontend: https://kinddhelp.com"
STATUS_CODE=$(curl -s -o /dev/null -w "%{http_code}" https://kinddhelp.com)
if [ "$STATUS_CODE" == "200" ]; then
    echo "   ✅ Frontend is accessible (HTTP $STATUS_CODE)"
else
    echo "   ⚠️  Frontend returned HTTP $STATUS_CODE"
fi
echo ""

echo "✅ Deployment Complete!"
echo ""
echo "🌐 Frontend URL: https://kinddhelp.com"
echo "📊 CloudFront Distribution: $CLOUDFRONT_DIST_ID"
echo "🪣 S3 Bucket: $S3_BUCKET"
echo ""
echo "Note: CloudFront cache invalidation may take 1-2 minutes to propagate."
