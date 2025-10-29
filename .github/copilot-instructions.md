# Project Overview

CHLA Provider Map is a full-stack web application that helps families find autism and developmental disability services in California.

- Frontend: Vue 3 + Vite + TypeScript, deployed to S3 with CloudFront at `https://kinddhelp.com`
- Backend: Django 5 + Django REST Framework, deployed on AWS Elastic Beanstalk (Python 3.12) with environment `chla-api-prod` at `https://api.kinddhelp.com`
- Database: PostgreSQL (AWS RDS) with PostGIS. Local DB name defaults to `shafali`
- Testing: pytest (backend), Vitest (frontend)
- State Management: Pinia stores (providerStore, mapStore, filterStore, regionalCenterStore, uiStore)

## Folder Structure

- `/map-frontend`: Vue 3 + TypeScript app (Vite)
  - Entry: `src/main.js`
  - Components: `src/components/` (MapCanvas.vue, ProviderCard.vue, etc.)
  - Composables: `src/composables/` (usePopups.ts, useMapLayers.ts, useGeolocation.ts)
  - Stores: `src/stores/` (Pinia stores for state management)
  - Views: `src/views/` (MapView.vue - main view)
  - Local dev: `npm run dev`
  - Build: `npm run build`
  - Tests: `npm test`
  
- `/maplocation`: Django backend project root
  - `maplocation/locations`: Main Django app (models, serializers, views, migrations)
  - `locations/tests/`: pytest test suite (test_models.py, test_api.py, test_health_check.py)
  - `.ebextensions/01_auto_migrate.config`: Runs `migrate` and `collectstatic` on each EB deploy
  - `maplocation/settings.py`: Reads DB config from env; local defaults to `shafali`
  - `sync_data_to_rds.py`: Pushes local Postgres data to RDS (reads EB env, enforces SSL)
  - `sync_rds_to_local.py`: Pulls RDS data to local database
  - `sync_providers_rds_to_local.py`: Syncs only provider data from RDS
  - `pytest.ini`: pytest configuration with coverage settings
  - `conftest.py`: pytest fixtures and test configuration
  
- `/.github/workflows/`: GitHub Actions CI/CD
  - `ci-cd.yml`: Main CI/CD pipeline (tests + automatic deployment)
  - `db-sync.yml`: Manual database sync workflow
  - `rollback.yml`: Manual rollback workflow
  - `deploy.yml`: Legacy deployment workflow (deprecated)
  
- `/.github/`: Documentation
  - `CICD_GUIDE.md`: Comprehensive CI/CD documentation
  - `SECRETS.md`: GitHub Secrets configuration guide
  - `copilot-instructions.md`: This file

## Libraries and Frameworks

- Frontend: Vue 3 (Composition API), TypeScript, Vite, Pinia (state management), MapLibre GL JS, Bootstrap 5
- Backend: Django 5.x, Django REST Framework, django-cors-headers, whitenoise, gunicorn
- Testing: pytest, pytest-django, pytest-cov, Vitest
- Code Quality: flake8, black, isort (Python); ESLint (JavaScript)
- Geo/DB: PostgreSQL, PostGIS
- Infra/CI: AWS Elastic Beanstalk, RDS, S3, CloudFront, Route 53, GitHub Actions

## Coding Standards

- Python (Django)
  - Prefer explicit, descriptive names; avoid 1–2 char identifiers
  - Use guard clauses and early returns; handle edge cases first
  - Add concise docstrings for complex logic; avoid redundant inline comments
  - Do not hardcode secrets or DB credentials; use environment variables
- JavaScript/TypeScript (Vue)
  - Use single-file components; keep components small and composable
  - **ALWAYS use Composition API** (not Options API) for new components; existing MapView.vue is being gradually refactored
  - Extract reusable logic to composables (e.g., `usePopups.ts`, `useMapLayers.ts`)
  - Use Pinia stores for shared state management
  - Keep API base URL in env (`VITE_API_BASE_URL`) rather than literals
  - TypeScript is preferred for composables and stores
- Formatting
  - Match existing formatting; do not reformat unrelated code
  - Wrap long lines; avoid dense one-liners

## UI Guidelines

- Mobile-first; sidebar collapses on small screens; map remains usable full-width
- Keep design clean and accessible; ensure sufficient contrast
- All external API base URLs must be configurable via env

## Build & Run (Quick Reference)

- Backend (local)
  - `cd maplocation && source ../.venv/bin/activate && pip install -r requirements.txt`
  - `python3 manage.py migrate && python3 manage.py runserver`
  - Run tests: `pytest` or `python -m pytest`
  
- Frontend (local)
  - `cd map-frontend && npm install && npm run dev`
  - Switch to dev env: `./switch-env.sh dev`
  - Switch to prod env: `./switch-env.sh prod`
  - Run tests: `npm test`
  - Build: `npm run build`
  
- Data sync workflows:
  - **Local → RDS (push)**: `cd maplocation && python3 sync_data_to_rds.py`
  - **RDS → Local (pull)**: `cd maplocation && python3 sync_rds_to_local.py`
  - **RDS → Local (providers only)**: `cd maplocation && python3 sync_providers_rds_to_local.py`
  - **Via GitHub Actions**: Use `db-sync.yml` workflow (manual trigger)

## Deployment Flow

### Automatic Deployment (via CI/CD)
- Push to `main` or `develop` triggers `ci-cd.yml` workflow:
  1. **Backend Tests**: pytest, flake8, black, isort
  2. **Frontend Tests**: Vitest, build verification
  3. **Deploy Backend**: Deploy to EB `chla-api-prod`; auto-runs migrations and collectstatic
  4. **Health Check**: Verifies backend is healthy (auto-rollback on failure)
  5. **Deploy Frontend**: Build and upload to S3, invalidate CloudFront cache
  6. **Post-Deployment**: End-to-end health checks

### Manual Workflows (GitHub Actions)
- **Database Sync**: `db-sync.yml` - Sync local DB to RDS with safety checks
- **Rollback**: `rollback.yml` - Rollback deployments (backend, frontend, or both)

### Manual Deployment (emergency)
```bash
# Backend
cd maplocation && eb deploy chla-api-prod --region us-west-2

# Frontend
cd map-frontend && ./switch-env.sh prod && npm run build
aws s3 sync dist/ s3://kinddhelp-frontend-1755148345 --delete --profile personal --region us-west-2
aws cloudfront create-invalidation --distribution-id E2W6EECHUV4LMM --paths "/*" --profile personal
```

## CI Required Checks

- ✅ Backend Tests & Linting (pytest, flake8, black, isort)
- ✅ Frontend Tests & Build (Vitest, Vite build)
- ✅ Deployment Health Checks (backend health endpoint)
- ✅ End-to-End Verification (post-deployment)

## Database Models (Backend)

### Provider Model
- **Current Model**: `ProviderV2` (table: `providers_v2`)
- **Old Model**: `Provider` - **REMOVED** (was migrated/archived in October 2024)
- Related models: `ProviderFundingSource`, `ProviderInsuranceCarrier`, `ProviderServiceModel`
- Management commands:
  - `geocode_providers` - Geocode providers using Mapbox API
  - `import_regional_center_providers` - Import provider data from Excel files
  - `populate_san_gabriel_zips` / `populate_pasadena_zips` - Populate ZIP code data

### API Endpoints
- `/api/providers/` - ProviderV2ViewSet (main endpoint)
- `/api/providers-v2/` - Alias for ProviderV2ViewSet
- `/api/providers-v2/comprehensive_search/` - Search with filters
- `/api/regional-centers/` - Regional center data
- `/api/regional-centers/service_area_boundaries/` - Service area polygons
- `/api/health/` - Health check endpoint

## Testing

### Backend Tests
```bash
cd maplocation
pytest                    # Run all tests
pytest -v                 # Verbose
pytest --cov              # With coverage
pytest -m unit            # Only unit tests
pytest -m integration     # Only integration tests
```

Test files:
- `locations/tests/test_models.py` - Model tests
- `locations/tests/test_api.py` - API endpoint tests
- `locations/tests/test_health_check.py` - Health check tests

### Frontend Tests
```bash
cd map-frontend
npm test                  # Run Vitest tests
npm run lint              # Run ESLint
```

## Recent Refactoring (October 2024)

### Frontend (MapView.vue)
- **Reduced from 6,690 → 4,734 lines** (29.3% reduction)
- Extracted composables:
  - `usePopups.ts` - Popup generation logic (723 lines)
  - `useMapLayers.ts` - Map layer management
  - `useGeolocation.ts` - Geolocation utilities
- Created new Pinia stores:
  - `regionalCenterStore` - Regional center state
  - `uiStore` - UI state (modals, sidebars)
- `fetchProviders()` refactored: 547 lines → 22 lines (delegated to providerStore)

### Backend (Provider Cleanup)
- Migrated from old `Provider` model to `ProviderV2`
- Removed 5 deprecated management commands
- Archived old scripts to `archive/old-provider-scripts/`
- Fixed model relationship conflicts
- Created migration to drop old `providers` table

## Additional Notes for Copilot

- **Always use `python3`** (not `python`) for Django commands on macOS
- Use `/api/...` paths with the `VITE_API_BASE_URL` variable in frontend code
- For Django DB access, rely on `settings.DATABASES["default"]` and env vars; do not hardcode
- Do not introduce new deployment methods; use the existing GitHub Actions + Elastic Beanstalk setup
- When creating new Vue components, **always use Composition API** with `<script setup>` syntax
- Extract reusable logic to composables, not inline in components
- Use Pinia stores for state that needs to be shared across components
- Run tests before committing: `pytest` (backend) and `npm test` (frontend)
- Check `.github/CICD_GUIDE.md` for comprehensive deployment documentation

## Environment Variables

### Backend (.env or EB environment)
- `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_HOST`, `DB_PORT` - Database credentials
- `DB_SSL_REQUIRE` - Set to `true` for RDS connections
- `DJANGO_SECRET_KEY` - Django secret key
- `DJANGO_DEBUG` - Set to `false` in production
- `MAPBOX_ACCESS_TOKEN` - For geocoding (optional, has default)

### Frontend (.env.development / .env.production)
- `VITE_API_BASE_URL` - Backend API URL
  - Development: `http://127.0.0.1:8000`
  - Production: `https://api.kinddhelp.com`
