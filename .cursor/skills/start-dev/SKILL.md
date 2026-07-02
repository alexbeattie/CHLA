---
name: start-dev
description: Start the full KiNDD local dev environment - Postgres (Docker), Django backend, Vite frontend, and the iOS project in Xcode. Use when the user asks to start the dev environment, spin up the backend/frontend, run the app locally, or open the iOS project.
---

# Start KiNDD Dev Environment

Bring up all local services from the repo root (`/Users/alexbeattie/Developer/CHLA`). Skip any service the user excluded, and skip anything already running.

## Step 0: Check what is already running

Check the terminals folder and ports before starting anything:

```bash
lsof -nP -iTCP:5433 -iTCP:8000 -iTCP:5173 -sTCP:LISTEN
```

- 5433: Postgres (Docker)
- 8000: Django backend
- 5173: Vite frontend

Do not start duplicates of services that are already listening.

## Step 1: Database (Docker Postgres)

```bash
docker-compose -f docker-compose.local.yml up -d
```

If Docker isn't running (`docker info` fails), run `open -a Docker`, then poll `docker info` until it succeeds (up to ~60s) before retrying.

Verify: `docker exec chla-postgres-local pg_isready -U chla_dev -d chla_local` returns "accepting connections".

## Step 2: Backend (Django)

Run in the background (long-running dev server, do not block):

```bash
cd maplocation && source ../venv/bin/activate && python3 manage.py runserver
```

Verify: `curl -s -o /dev/null -w "%{http_code}" http://localhost:8000/api/` returns a non-5xx status. If it fails to start, run `python3 manage.py check` and report the error instead of retrying blindly.

## Step 3: Frontend (Vite)

Run in the background (long-running dev server, do not block):

```bash
cd map-frontend && ./switch-env.sh dev && npm run dev
```

Verify: dev server output shows `http://localhost:5173` or `curl -s -o /dev/null -w "%{http_code}" http://localhost:5173` returns 200.

## Step 4: iOS (Xcode)

```bash
open chla-ios/CHLA-iOS.xcodeproj
```

This opens Xcode; the user builds/runs from there. Do not run `xcodebuild`.

## Handoff

Report status of each service:

- Database: running on localhost:5433
- Backend API: http://localhost:8000/api/ (admin at /admin/)
- Frontend: http://localhost:5173
- Xcode: opened CHLA-iOS.xcodeproj

If any service failed, include the relevant error output and the most likely fix (see QUICK_START.md "Quick Debugging").
