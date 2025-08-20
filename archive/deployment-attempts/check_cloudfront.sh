#!/bin/bash
echo "Checking CloudFront distribution status..."

while true; do
    STATUS=$(aws cloudfront get-distribution --id E2W6EECHUV4LMM --profile personal --query "Distribution.Status" --output text 2>/dev/null)
    
    if [ "$STATUS" = "Deployed" ]; then
        echo "✅ CloudFront distribution is now deployed!"
        echo "Testing API routing through kinddhelp.com..."
        
        # Test the API
        if curl -s 'https://kinddhelp.com/api/providers/' | jq '.results[0].name' 2>/dev/null; then
            echo "✅ API routing is working! You can now rebuild the frontend to use https://kinddhelp.com"
            echo ""
            echo "Run these commands to switch back:"
            echo "cd map-frontend"
            echo "echo 'VITE_API_BASE_URL=https://kinddhelp.com' > .env.production"
            echo "echo 'VITE_MAPBOX_TOKEN=pk.eyJ1IjoiYmVhdHR5LWFkbWluIiwiYSI6ImNsejFjNGt0YzFqMGMyanF3YW5hdWFmc3UifQ.sn7Uj_gDzzKL6PQq7vO7fw' >> .env.production"
            echo "npm run build"
            echo "aws s3 sync dist/ s3://chla-frontend-1755700706 --delete --profile personal --region us-west-2"
            break
        else
            echo "⚠️  Distribution deployed but API not responding yet. Waiting..."
        fi
    else
        echo "Status: $STATUS - waiting 30 seconds..."
    fi
    
    sleep 30
done
