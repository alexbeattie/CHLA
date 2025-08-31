# 🚀 CHLA Quick Reference Card

## 🔥 Most Common Tasks

### Start Local Development
```bash
# Terminal 1 - Backend
cd maplocation
source ../.venv/bin/activate
python3 manage.py runserver

# Terminal 2 - Frontend  
cd map-frontend
./switch-env.sh dev
npm run dev
```

### Deploy Everything
```bash
# 1. Deploy Backend (2-3 minutes)
cd maplocation
eb deploy --profile personal --region us-west-2

# 2. Deploy Frontend (1-2 minutes + 5-15 min cache clear)
cd map-frontend
./switch-env.sh prod
npm run build
aws s3 sync dist/ s3://kinddhelp-frontend-1755148345 --delete --profile personal --region us-west-2
aws cloudfront create-invalidation --distribution-id E2W6EECHUV4LMM --paths "/*" --profile personal
```

## 🔍 Quick Debugging

### Check if Backend is Working
```bash
# Test production
curl -I https://api.kinddhelp.com/api/regional-centers/

# Check health
eb health --profile personal --region us-west-2
```

### Frontend Not Updating?
1. Wait 15 minutes for CloudFront
2. Hard refresh: Cmd+Shift+R
3. Check S3 directly: http://kinddhelp-frontend-1755148345.s3-website-us-west-2.amazonaws.com

## 📝 Key URLs
- **Live Site**: https://kinddhelp.com
- **API**: https://api.kinddhelp.com
- **AWS Console**: https://console.aws.amazon.com/elasticbeanstalk/home?region=us-west-2

## ⚡ One-Liners

```bash
# View backend logs
eb logs --profile personal --region us-west-2

# Check what environment frontend is using
cat map-frontend/.env

# Switch to production
cd map-frontend && ./switch-env.sh prod

# Switch to development  
cd map-frontend && ./switch-env.sh dev
```

## 🚨 If Something's Broken

1. **502 Errors**: Backend is down → `cd maplocation && eb deploy --profile personal --region us-west-2`
2. **No Markers**: Check browser console → API calls failing?
3. **CORS Errors**: Backend config issue → Check EB environment variables
4. **Old Content**: CloudFront cache → Wait or use S3 direct URL

## 💰 Cost Saving Tips
- Use local development for testing
- Only deploy when ready
- S3/CloudFront costs are minimal
- EB t3.micro is free tier eligible
- RDS is the main cost (~$15/month)
