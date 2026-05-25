# AGENTS.md

## Cursor Cloud specific instructions

### Services Overview

| Service | Command | Port |
|---------|---------|------|
| PostGIS (Docker) | `docker compose -f docker-compose.local.yml up -d` | 5433 |
| Django Backend | `cd maplocation && source ../venv/bin/activate && python3 manage.py runserver 0.0.0.0:8000` | 8000 |
| Vue Frontend | `cd map-frontend && ./switch-env.sh dev && npm run dev -- --host 0.0.0.0` | 3000 |

### Environment Variables for Django

Before running Django commands, export these:

```
export DB_NAME=chla_local DB_USER=chla_dev DB_PASSWORD=dev_password DB_HOST=localhost DB_PORT=5433 DJANGO_DEBUG=true
```

### Key Gotchas

- **Docker daemon**: Must start `dockerd` manually on each VM boot (`sudo dockerd &>/tmp/dockerd.log &`) and fix socket permissions (`sudo chmod 666 /var/run/docker.sock`).
- **pgvector extension**: The PostGIS Docker image does not include pgvector by default. After the container starts, install it: `docker exec chla-postgres-local bash -c "apt-get update && apt-get install -y postgresql-16-pgvector"` then `docker exec chla-postgres-local psql -U chla_dev -d chla_local -c "CREATE EXTENSION IF NOT EXISTS vector;"`.
- **Migration 0026**: On a fresh database, migration `0026_delete_provider` fails because the `providers` table was already dropped in migration 0022. Workaround: `python3 manage.py migrate locations 0026_delete_provider --fake` then `python3 manage.py migrate`.
- **Backend tests**: Tests require `pytest`, `pytest-django`, and `pytest-cov`. Some tests have pre-existing failures due to outdated fixtures (e.g., referencing removed model fields like `accepts_insurance`).
- **Frontend tests**: Some vitest tests have pre-existing failures unrelated to environment setup.
- **Basic Auth**: The admin/client portal is behind Basic Auth (username: `clientaccess`, password: `changeme123!`) before Django login.

### Standard Commands

- **Lint/check (backend)**: `python3 manage.py check` and `python3 -m compileall -q .`
- **Lint/check (frontend)**: See `package.json` — no dedicated lint script beyond build validation.
- **Tests (backend)**: `cd maplocation && python3 -m pytest locations/tests/ -v`
- **Tests (frontend)**: `cd map-frontend && npx vitest run`
- **Build (frontend)**: `cd map-frontend && npm run build`
- **Quick pre-commit check**: `./scripts/quick-test.sh`
