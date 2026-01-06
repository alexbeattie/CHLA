#!/bin/bash

# Backend Deployment Script for CHLA Map Application
# Deploys Django backend to AWS Elastic Beanstalk

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EB_ENV="chla-api-docker2"
AWS_REGION="us-west-2"

echo "🚀 Starting Backend Deployment to Elastic Beanstalk..."
echo ""

# Step 1: Check if we're in a git repository
echo "1️⃣  Checking git status..."
if [[ ! -d .git ]]; then
    echo "   ❌ Not a git repository. Please initialize git first."
    exit 1
fi

# Check for uncommitted changes
if [[ -n $(git status -s) ]]; then
    echo "   ⚠️  Warning: You have uncommitted changes"
    read -p "   Continue anyway? (y/n) " -n 1 -r
    echo
    if [[ ! ${REPLY} =~ ^[Yy]$ ]]; then
        echo "   Deployment cancelled."
        exit 1
    fi
fi
echo "   ✅ Git check complete"
echo ""

# Step 2: Verify environment configuration
echo "2️⃣  Verifying Elastic Beanstalk environment..."
eb status "${EB_ENV"} --region "${AWS_REGION"} > /dev/null 2>&1
if [[ $? -ne 0 ]]; then
    echo "   ❌ EB environment '${EB_ENV}' not found or not accessible"
    exit 1
fi
echo "   ✅ EB environment verified"
echo ""

# Step 3: Deploy to Elastic Beanstalk
echo "3️⃣  Deploying to Elastic Beanstalk..."
echo "   Environment: ${EB_ENV}"
echo "   Region: ${AWS_REGION}"
echo ""
eb deploy "${EB_ENV}" --region "${AWS_REGION}" --timeout 10

if [[ $? -ne 0 ]]; then
    echo "   ❌ Deployment failed!"
    echo ""
    echo "   Checking environment health..."
    eb health "${EB_ENV}" --region "${AWS_REGION}"
    exit 1
fi
echo "   ✅ Deployment completed"
echo ""

# Step 4: Check environment health
echo "4️⃣  Checking environment health..."
eb health "${EB_ENV}" --region "${AWS_REGION}"
echo ""

# Step 5: Verify API is accessible
echo "5️⃣  Verifying API endpoints..."
echo "   Testing: https://api.kinddhelp.com/api/regional-centers/"
sleep 5

HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" https://api.kinddhelp.com/api/regional-centers/)
if [[ "${HTTP_STATUS}" == "200" ]]; then
    echo "   ✅ API is accessible (HTTP ${HTTP_STATUS})"

    # Test regional centers count
    COUNT=$(curl -s https://api.kinddhelp.com/api/regional-centers/ | python3 -c "import sys, json; print(json.load(sys.stdin)['count'])" 2>/dev/null || echo "?")
    echo "   ✅ Regional Centers: ${COUNT}"
else
    echo "   ⚠️  API returned HTTP ${HTTP_STATUS}"
fi
echo ""

echo "✅ Deployment Complete!"
echo ""
echo "🌐 API URL: https://api.kinddhelp.com"
echo "📦 EB Environment:${$EB_EN}V"
echo "🌍 Region:${$AWS_REGIO}N"
echo ""
echo "View logs: eb logs ${EB_ENV} --region ${AWS_REGION}"
echo "View status: eb status ${EB_ENV} --region ${AWS_REGION}"
