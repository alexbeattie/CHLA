# KiNDD - NDD Resource Navigator Cursor Workflow

Use this file with `AGENTS.md`.

## Start Every Session Here

1. Read `AGENTS.md`.
2. Read the nearest source-of-truth doc for the area you are touching.
3. Inspect the target files before proposing a rewrite.

## Good Default Loop

1. Understand the slice of the app you are changing.
2. Make one focused change at a time.
3. Prefer extraction over expansion, especially around `map-frontend/src/views/MapView.vue`.
4. Run the smallest relevant verification step before wrapping up.

## Cursor-Specific Guidance

- Keep plans short and concrete.
- Patch existing files before inventing new structure.
- Avoid repeating long repo docs in chat; link back to the real file instead.
- If a command, credential, URL, or environment detail is uncertain, verify it from the repo docs instead of guessing.

## What "Done" Looks Like

- The code change is scoped.
- Relevant tests or smoke checks were run, or the gap is called out clearly.
- Any workflow or setup changes are reflected in the appropriate docs.

## Quick References

- Shared workflow: `AGENTS.md`
- Project conventions: `.github/copilot-instructions.md`
- Setup and daily commands: `QUICK_START.md`
- Backend details: `maplocation/README.md`
- Frontend env switching: `map-frontend/ENVIRONMENT_SWITCHING.md`
