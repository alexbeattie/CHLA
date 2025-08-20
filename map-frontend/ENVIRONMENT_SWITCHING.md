# Environment Switching Guide

This project supports both local development and production deployment with easy switching.

## Quick Commands

### Switch to Development (Local)
```bash
./switch-env.sh dev
npm run dev
```

### Switch to Production (Deploy)
```bash
./switch-env.sh prod
npm run build
aws s3 sync dist/ s3://kinddhelp-frontend-1755148345 --delete --profile personal --region us-west-2
aws cloudfront create-invalidation --distribution-id E2W6EECHUV4LMM --paths "/*" --profile personal
```

## How It Works

- **`.env`** - Main environment file (gets updated by switch script)
- **`.env.development`** - Development-specific overrides
- **`.env.production`** - Production-specific overrides

## Development Workflow

1. **Start local development:**
   ```bash
   ./switch-env.sh dev
   npm run dev
   ```
   - Frontend runs on http://localhost:3000
   - API calls go to http://127.0.0.1:8000 (your local Django server)

2. **Deploy to production:**
   ```bash
   ./switch-env.sh prod
   npm run build
   # Then deploy to S3 and invalidate CloudFront
   ```

## What Gets Changed

The `switch-env.sh` script updates the main `.env` file:
- **Development**: `VITE_API_BASE_URL=http://127.0.0.1:8000`
- **Production**: `VITE_API_BASE_URL=https://api.kinddhelp.com`

## Troubleshooting

- **Providers not showing**: Check that you're using the right environment
- **API errors**: Verify the API URL in the browser console
- **Build issues**: Ensure you're in the right mode (`npm run build` for production)

## Pro Tips

- Always run `./switch-env.sh dev` before starting local development
- Always run `./switch-env.sh prod` before building for production
- The script automatically handles the environment switching
- No more manual editing of `.env` files!
