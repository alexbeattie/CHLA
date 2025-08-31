# Project Overview

CHLA Provider Map is a full-stack web application that helps families find autism and developmental disability services in California.

- Frontend: Vue 3 + Vite, deployed to S3 with CloudFront at `https://kinddhelp.com`.
- Backend: Django 5 + Django REST Framework, deployed on AWS Elastic Beanstalk (Python 3.12) with environment `chla-api-env-lb` at `https://api.kinddhelp.com`.
- Database: PostgreSQL (AWS RDS) with PostGIS. Local DB name defaults to `shafali`.

## Folder Structure

- `/map-frontend`: Vue 3 app (Vite)
  - Entry: `src/main.js`
  - Local dev: `npm run dev`
  - Build: `npm run build`
- `/maplocation`: Django backend project root
  - `maplocation/locations`: Main Django app (models, serializers, views, migrations)
  - `.ebextensions/01_auto_migrate.config`: Runs `migrate` and `collectstatic` on each EB deploy
  - `maplocation/settings.py`: Reads DB config from env; local defaults to `shafali`
  - `sync_data_to_rds.py`: Pushes local Postgres data to RDS (reads EB env, enforces SSL)
- `/.github/workflows/deploy.yml`: CI/CD workflow (push to `main` → build frontend, deploy backend)
- `/DEPLOYMENT_GUIDE.md`: Canonical guide for deployment and database sync

## Libraries and Frameworks

- Frontend: Vue 3, Vite, Bootstrap 5
- Backend: Django 5.x, Django REST Framework, django-cors-headers, whitenoise, gunicorn
- Geo/DB: PostgreSQL, PostGIS
- Infra/CI: AWS Elastic Beanstalk, RDS, S3, CloudFront, Route 53, GitHub Actions

## Coding Standards

- Python (Django)
  - Prefer explicit, descriptive names; avoid 1–2 char identifiers
  - Use guard clauses and early returns; handle edge cases first
  - Add concise docstrings for complex logic; avoid redundant inline comments
  - Do not hardcode secrets or DB credentials; use environment variables
- JavaScript (Vue)
  - Use single-file components; keep components small and composable
  - Prefer Composition API where sensible; avoid deep prop drilling
  - Keep API base URL in env (`VITE_API_BASE_URL`) rather than literals
- Formatting
  - Match existing formatting; do not reformat unrelated code
  - Wrap long lines; avoid dense one-liners

## UI Guidelines

- Mobile-first; sidebar collapses on small screens; map remains usable full-width
- Keep design clean and accessible; ensure sufficient contrast
- All external API base URLs must be configurable via env

## Build & Run (Quick Reference)

- Backend (local)
  - `cd maplocation && python3 -m venv .venv && source .venv/bin/activate && pip install -r requirements.txt`
  - `python3 manage.py migrate && python3 manage.py runserver`
- Frontend (local)
  - `cd map-frontend && npm install && npm run dev`
- Data sync to RDS (when local data must be pushed to prod) for  change in models.py, serializers.py, or views.py
  - `cd maplocation && export DB_PASSWORD='<current RDS password>' && python3 sync_data_to_rds.py`

## Deployment Flow

- Push to `main` triggers GitHub Actions:
  - Frontend: build and upload to S3, then CloudFront invalidation
  - Backend: deploy to EB `chla-api-env-lb`; `.ebextensions/01_auto_migrate.config` runs migrations and collectstatic

## CI Required Checks (suggested)

- `Deploy CHLA Provider Map / test` — backend unit tests
- Optional: add a frontend build check if it runs on PRs

## Additional Notes for Copilot

- Use `/api/...` paths with the `VITE_API_BASE_URL` variable in frontend code
- For Django DB access, rely on `settings.DATABASES["default"]` and env vars; do not hardcode
- Prefer `python3` for Django commands on macOS
- Do not introduce new deployment methods; use the existing GitHub Actions + Elastic Beanstalk setup
