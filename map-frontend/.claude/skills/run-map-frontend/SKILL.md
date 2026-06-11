---
name: run-map-frontend
description: Run, build, drive, smoke-test, or screenshot the KiNDD map-frontend (Vue 3 + Vite + Mapbox web app). Use when asked to run the frontend, start the dev server, take a screenshot of the map app, verify a UI change, or check that MapView still renders.
---

# Run map-frontend (KiNDD - NDD Resource Navigator web app)

Vue 3 + Vite + Pinia + Mapbox GL single-page app. Driven headlessly via a
Puppeteer driver at `.claude/skills/run-map-frontend/driver.mjs` (Puppeteer is
already a devDependency - no extra installs). All paths below are relative to
`map-frontend/`.

## Prerequisites

- Node 20+ and npm (verified with Node v20.9.0 / npm 10.9.2).
- `npm install` if `node_modules/` is missing.
- A `.env` file with `VITE_API_BASE_URL` and `VITE_MAPBOX_TOKEN`. Copy
  `.env.development.example` if absent. The app launches and all static pages
  work even when the Mapbox token is empty and the backend is down - see
  Gotchas.

## Run (agent path)

Start the dev server in the background, then drive it with the Puppeteer
driver. The server listens on port 3000 (set in `vite.config.js`), NOT Vite's
default 5173.

```bash
npm run dev   # run in background; ready in ~2s, serves http://localhost:3000/
```

Wait for it, then smoke-test all key routes (screenshots land in
`/tmp/map-frontend-shots/`):

```bash
node .claude/skills/run-map-frontend/driver.mjs smoke
```

Expected: `OK` for `/`, `/about`, `/faq`, `/regional-centers`, `/clinicians`
and a final `SMOKE PASS`. The home route reports console errors when the
Mapbox token is empty or the backend is down - the page still mounts.

Other driver commands (all verified):

```bash
# Screenshot one route
node .claude/skills/run-map-frontend/driver.mjs shot /faq faq.png

# Evaluate JS in a page, print the result
node .claude/skills/run-map-frontend/driver.mjs eval /about "document.querySelectorAll('a').length + ' links'"

# Click a selector, screenshot the result. This dismisses the onboarding
# modal that covers the map UI on first load:
node .claude/skills/run-map-frontend/driver.mjs click / ".skip-link" home-after-skip.png
```

Override the port/URL with `PORT=4000` or `BASE_URL=http://localhost:4000`
env vars on the driver.

Always LOOK at the screenshots (Read the PNG) - a mounted-but-broken page
still reports `OK` if `#app` has content.

## Run (human path)

`npm run dev`, open <http://localhost:3000/> in a browser, Ctrl-C to stop.

## Test

```bash
npx vitest run
```

`npm test` runs `vitest` in WATCH mode and never exits - always use
`vitest run` for a one-shot pass. As of 2026-06-10 the suite has 151
pre-existing failures out of 453 tests (302 pass). Do not treat a red suite
as your regression - diff failures against a clean checkout.

## Gotchas

- **Port 3000, not 5173.** `vite.config.js` sets `server.port` to
  `VITE_PORT || 3000`.
- **Port 8000 collision.** The dev proxy forwards `/api` to
  `VITE_API_BASE_URL` (default `http://127.0.0.1:8000`). On this machine
  another Django project (Ourself Health, `backend.urls`) often occupies
  port 8000 - the frontend's API calls then 404 silently ("Regional Center
  (Not Found)" in the sidebar). Check who owns the port with
  `lsof -nP -iTCP:8000 -sTCP:LISTEN` and confirm
  `curl http://127.0.0.1:8000/api/regional-centers/` returns JSON, not a
  Django 404 page. The KiNDD backend lives in `../maplocation/`
  (`python manage.py runserver`) - not verified by this skill; it needs
  PostGIS.
- **Empty Mapbox token = blank dark canvas, app still works.** With
  `VITE_MAPBOX_TOKEN` unset you get `MapCanvas: Error creating map` in the
  console and a black map area, but the nav, sidebar, filters, search, and
  all static routes render fine. Good enough for most UI verification.
- **Onboarding modal covers the home route on first load.** Every fresh
  headless session shows the "Step 1 of 4" modal. Dismiss with the
  `.skip-link` button (driver `click / ".skip-link"`) before asserting on
  the map UI.
- **Known page error on home:** `this.searchProviders is not a function`
  fires after skipping onboarding (from MapView.vue). Pre-existing; do not
  chase it as a regression of your change.
- **Do not wait for network idle when driving the app.** With the backend
  down or the wrong server on :8000, pending `/api` requests keep the network
  busy forever and `networkidle2` navigation times out (seen on
  `/clinicians`). The driver uses `waitUntil: "load"` plus a 1.5s settle for
  this reason - keep it that way.

## Troubleshooting

- `curl http://localhost:5173/` fails → wrong port; the server is on 3000.
- `npm test` appears to hang → it's vitest watch mode; use `npx vitest run`.
- Driver exits 1 with `FAIL <route> ... timeout` → dev server not up yet;
  re-check the background task output for `VITE ... ready`.
