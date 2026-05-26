# map-frontend

Vue 3 + Vite frontend for KiNDD - NDD Resource Navigator. Renders the provider map, search, and clinical tooling UI.

## Quick start

```bash
cd map-frontend
cp .env.example .env
# fill in VITE_MAPBOX_TOKEN and adjust VITE_API_BASE_URL if needed
npm install
npm run dev
```

Dev server runs at http://localhost:3000 (override with `VITE_PORT`).

## Commands

| Command | Purpose |
|---|---|
| `npm run dev` | Local dev server with HMR |
| `npm run build` | Production build to `dist/` |
| `npm run preview` | Preview the production build locally |
| `npm run test` | Run unit tests |
| `npm run lint` | Lint the source tree |

## Environment

See `.env.example` for the full variable list. The app reads:

- `VITE_API_BASE_URL` — backend base URL (canonical; some code paths fall back to `VITE_API_URL`).
- `VITE_MAPBOX_TOKEN` — Mapbox public token (`VITE_MAPBOX_ACCESS_TOKEN` and `VITE_MAPBOX_API_TOKEN` are legacy aliases still read by some modules).
- `VITE_GA_MEASUREMENT_ID` — optional Google Analytics ID.
- `VITE_PORT` — optional dev server port.

Per-environment files (`.env.development`, `.env.production`, `.env.test`) layer on top of `.env`. See `ENVIRONMENT_SWITCHING.md` for the workflow.

## Further reading

- `../docs/STACK.md` — full stack architecture
- `../docs/DEPLOYMENT.md` — deployment runbook
- `../docs/ENVIRONMENT.md` — backend environment reference
- `src/composables/README.md` — Vue composables overview
