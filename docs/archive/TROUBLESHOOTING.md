# 🔧 CHLA Troubleshooting Guide

## Quick Diagnosis Flowchart

```
Website not loading?
├─ Check: https://api.kinddhelp.com/api/
│  ├─ Returns "OK" → Frontend issue
│  │  └─ Fix: Redeploy frontend
│  └─ Timeout/Error → Backend issue
│     └─ Fix: Check EB health & redeploy
│
Markers not showing?
├─ Open browser console (F12)
├─ Look for red errors
│  ├─ 404 errors → Wrong API URL
│  │  └─ Fix: Check map-frontend/.env
│  ├─ 502 errors → Backend down
│  │  └─ Fix: Redeploy backend
│  └─ CORS errors → Config issue
│     └─ Fix: Update EB CORS settings
```

## Common Issues & 2-Minute Fixes

### 1. "502 Bad Gateway" Errors
```bash
# Quick fix - redeploy backend
cd maplocation
eb deploy --profile personal --region us-west-2
```

### 2. Frontend Changes Not Showing
```bash
# Clear cache and redeploy
cd map-frontend
./switch-env.sh prod
npm run build
aws s3 sync dist/ s3://kinddhelp-frontend-1755148345 --delete --profile personal --region us-west-2
aws cloudfront create-invalidation --distribution-id E2W6EECHUV4LMM --paths "/*" --profile personal

# Then wait 10-15 minutes and hard refresh (Cmd+Shift+R)
```

### 3. API Working But No Data
```bash
# Check if database has data
cd maplocation
source ../.venv/bin/activate
python3 manage.py shell
>>> from locations.models import ProviderV2
>>> ProviderV2.objects.count()
>>> exit()
```

### 4. HTTPS Not Working
- Go to AWS Console → Elastic Beanstalk → Configuration
- Check Load Balancer has HTTPS listener on port 443
- Certificate should be: api.kinddhelp.com

### 5. Local Development Not Working
```bash
# Backend fix
cd maplocation
source ../.venv/bin/activate
pip install -r requirements.txt
python3 manage.py migrate
python3 manage.py runserver

# Frontend fix
cd map-frontend
rm -rf node_modules package-lock.json
npm install
./switch-env.sh dev
npm run dev
```

## Health Check Commands

```bash
# 1. Is backend alive?
curl -s -o /dev/null -w "%{http_code}" https://api.kinddhelp.com/api/

# 2. EB environment status
eb status --profile personal --region us-west-2

# 3. View recent logs
eb logs --profile personal --region us-west-2 | tail -50

# 4. Frontend environment check
cat map-frontend/.env

# 5. Test specific API endpoint
curl https://api.kinddhelp.com/api/regional-centers/ | python3 -m json.tool | head -20
```

## Nuclear Options (Last Resort)

### Restart Everything
```bash
# 1. Restart EB instances
eb restart --profile personal --region us-west-2

# 2. Full frontend rebuild
cd map-frontend
rm -rf dist node_modules package-lock.json
npm install
./switch-env.sh prod
npm run build
aws s3 rm s3://kinddhelp-frontend-1755148345 --recursive --profile personal
aws s3 sync dist/ s3://kinddhelp-frontend-1755148345 --profile personal --region us-west-2
aws cloudfront create-invalidation --distribution-id E2W6EECHUV4LMM --paths "/*" --profile personal
```

## Don't Panic Checklist

- [ ] Backend responding? `curl https://api.kinddhelp.com/api/`
- [ ] Frontend environment correct? `cat map-frontend/.env`
- [ ] CloudFront cache cleared? (wait 15 min)
- [ ] Browser cache cleared? (Cmd+Shift+R)
- [ ] Console errors checked? (F12)
- [ ] EB health green? `eb health`

## Contact Points

- **S3 Direct** (bypasses CloudFront): http://kinddhelp-frontend-1755148345.s3-website-us-west-2.amazonaws.com
- **EB Direct** (bypasses domain): https://chla-api-prod.eba-9aiqcppx.us-west-2.elasticbeanstalk.com
- **Local Backend**: http://127.0.0.1:8000
- **Local Frontend**: http://localhost:3000
