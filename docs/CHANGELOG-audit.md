# Documentation Audit — 2026-05-25

Result of the docs-audit run. All moves used `git mv` to preserve history. No files were deleted.

## Canonical layout established

```
README.md                 # project overview + quickstart
QUICK_START.md            # top-level fast path
AGENTS.md, CLAUDE.md, CURSOR.md   # agent contracts
docs/                     # long-form documentation
  README.md               # index
  STACK.md                # architecture (was STACK_DOCUMENTATION.md)
  ENVIRONMENT.md          # backend env reference (was ENV_SETTINGS.md)
  DEPLOYMENT.md           # canonical deploy
  DATABASE_SYNC.md        # canonical sync concept
  DATABASE_SYNC_OPS.md    # operational runbook (was maplocation/DATABASE_SYNC_GUIDE.md)
  GITHUB_ACTIONS.md       # canonical CI
  QUICK_START_CICD.md     # CI quickstart (moved from root)
  ADMIN_DATA_ENTRY_GUIDE.md (moved from root)
  LLM_INTEGRATION_GAMEPLAN.md (moved from root)
  agent/                  # response-format + safety contracts (live)
  archive/
    2025-08/              # CHLA-era pre-rename material
    2025-10/              # MapView refactor + setup reports
<service>/README.md       # service entry point
<service>/...             # service-local docs
.github/                  # CI operator docs
.agents/                  # vendor SDK reference
```

## Moves and renames

### Promoted to `docs/`
- `STACK_DOCUMENTATION.md` → `docs/STACK.md`
- `ENV_SETTINGS.md` → `docs/ENVIRONMENT.md`
- `LLM_INTEGRATION_GAMEPLAN.md` → `docs/LLM_INTEGRATION_GAMEPLAN.md`
- `ADMIN_DATA_ENTRY_GUIDE.md` → `docs/ADMIN_DATA_ENTRY_GUIDE.md`
- `QUICK_START_CICD.md` → `docs/QUICK_START_CICD.md`
- `maplocation/DATABASE_SYNC_GUIDE.md` → `docs/DATABASE_SYNC_OPS.md`

### Archived to `docs/archive/2025-08/` (CHLA-era pre-rename)
- `archive/old-docs/CRITICAL_NOTES.md`
- `archive/old-docs/INSTALL_INSTRUCTIONS.md`
- `archive/old-docs/map-frontend-README.md`
- `archive/old-docs/maplocation-README.md`
- `archive/old-docs/PROVIDER_QUERY_DOCUMENTATION.md`
- `archive/old-docs/README-POSTGRES-GRAPHQL.md`
- `archive/old-docs/STARTUP_GUIDE.md`
- (`archive/old-docs/` directory removed once empty)

### Archived to `docs/archive/2025-10/` (frozen point-in-time reports)

Root-level frozen reports:
- `REFACTORING_PROGRESS.md` → `REFACTORING_PROGRESS-root.md`
- `REFACTORING_SUMMARY.md` → `REFACTORING_SUMMARY-root.md`
- `FINAL_REFACTORING_RESULTS.md`
- `SETUP_COMPLETE.md`
- `MIGRATION_FIX_SUMMARY.md`
- `DATABASE_SYNC_REPORT.md`
- `DEPLOY.md` → `DEPLOY-root.md`
- `DEPLOYMENT.md` → `DEPLOYMENT-root.md`

MapView refactor history (under `docs/`):
- `MAPVIEW_ANALYSIS.md`, `MAPVIEW_CLEANUP_PLAN.md`, `MAPVIEW_REFACTOR_PLAN.md`,
  `MAPVIEW_REFACTOR_SESSION_1.md`, `MAPVIEW_REFACTOR_SESSION_FINAL.md`
- `REFACTOR_PROGRESS.md`, `REFACTORING_PROGRESS.md`, `FINAL_REFACTORING_STATUS.md`
- `REORGANIZATION_SUMMARY.md`, `SESSION_SUMMARY.md`, `SESSION_SUMMARY_2025-10-27.md`
- `WEEK_2_COMPLETION.md`, `WEEK_3_COMPLETION.md`, `WEEK_3_KICKOFF.md`,
  `WEEK_4_COMPLETION.md`, `WEEK_4_KICKOFF.md`, `WEEK_5_COMPLETION.md`,
  `WEEK_5_KICKOFF.md`, `WEEK_5B_STATUS.md`
- `QUICK_REFERENCE.md` → `QUICK_REFERENCE-mapview-refactor.md` (was a refactor cheat sheet, not a general reference)

CI / secrets duplicates:
- `.github/CI_CD_SETUP.md` (superseded by `.github/CICD_GUIDE.md`)
- `.github/GITHUB_SECRETS_SETUP.md` → `GITHUB_SECRETS_SETUP-github.md` (superseded by `.github/SECRETS.md`)

`maplocation/` frozen reports:
- `PROVIDER_CLEANUP_PLAN.md` → `maplocation-PROVIDER_CLEANUP_PLAN.md`
- `PROVIDER_CLEANUP_COMPLETED.md` → `maplocation-PROVIDER_CLEANUP_COMPLETED.md`
- `CLEANUP_COMPLETE_READY_TO_USE.md` → `maplocation-CLEANUP_COMPLETE_READY_TO_USE.md`
- `locations/management/commands/REFACTORING_SUMMARY.md` → `maplocation-commands-REFACTORING_SUMMARY.md`

## Created

- `.env.example` (root) — placeholder template for Django/Postgres env vars; previously missing.
- `map-frontend/README.md` — service entry point with quickstart and env reference.
- `docs-audit/REPORT.md` — Phase 1 + Phase 2 audit report.
- `docs/CHANGELOG-audit.md` — this file.

## Updated

- `map-frontend/.env.example` — variable names now match what the app actually reads
  (`VITE_API_BASE_URL`, `VITE_MAPBOX_TOKEN`, plus optional `VITE_GA_MEASUREMENT_ID`,
  `VITE_API_URL` legacy fallback, `VITE_PORT`). Old example used a divergent
  `VITE_API_URL` and was missing variables the code actually reads.
- `README.md` — `STACK_DOCUMENTATION.md` references → `docs/STACK.md`.
- `docs/README.md` — index updated; `STACK.md` path corrected; new entries for
  `ENVIRONMENT.md`, `ADMIN_DATA_ENTRY_GUIDE.md`, `QUICK_START_CICD.md`,
  `LLM_INTEGRATION_GAMEPLAN.md`, `DATABASE_SYNC_OPS.md`.
- `docs/DEPLOYMENT.md` — `../STACK_DOCUMENTATION.md` → `./STACK.md`.
- `docs/ENVIRONMENT.md` — fixed relative paths after the move (`./DEPLOYMENT.md`,
  `../QUICK_START.md`, `../.github/CICD_GUIDE.md`, `../.github/SECRETS.md`).
- `docs/API_AND_DATA_ISSUES.md` — `STACK_DOCUMENTATION.md` → `docs/STACK.md`.
- `docs/CLINICAL_TEAM_HANDOFF.md` — `STACK_DOCUMENTATION.md` → `docs/STACK.md`.
- `docs/NONPROFIT_ONBOARDING_PACKET.md` — references to `STACK_DOCUMENTATION.md`,
  `ENV_SETTINGS.md`, `maplocation/DATABASE_SYNC_GUIDE.md`, `archive/old-docs/`
  retargeted to their new homes.
- `docs/COMPOSABLES_INTEGRATION_GUIDE.md` — `MAPVIEW_REFACTOR_PLAN.md` reference
  retargeted to archive.
- `map-frontend/src/composables/README.md` — `MAPVIEW_REFACTOR_PLAN.md` and
  `SESSION_SUMMARY.md` references retargeted to archive.
- `maplocation/README.md` — references to `../STACK_DOCUMENTATION.md`,
  `ADMIN_SECURITY_OPTIONS.md`, `QUICK_ADMIN_SECURITY.md`, `manual_sync_commands.md`
  retargeted to `../docs/STACK.md` and `../docs/archive/`.

## Decisions deferred (not executed)

These items from the audit report were not executed and remain pending your call:

- **Item #50** SEO completion docs (`docs/SEO_COMPLETION_SUMMARY.md`, `docs/SEO_DEPLOYMENT_STEPS.md`, `docs/SEO_IMPLEMENTATION_CHECKLIST.md`) — left in place; archive when convenient.
- **Item #56** Untrack `map-frontend/.env.{development,production,test}` from git per `.gitignore` policy — still tracked. Recommend `git rm --cached` and replace each with a `.env.<env>.example`.
- **Item #62** Restore `docs/TROUBLESHOOTING.md` — still missing. `docs/DEPLOYMENT.md`, `docs/DATABASE_SYNC.md`, and `docs/GITHUB_ACTIONS.md` link to it; either restore from `docs/archive/TROUBLESHOOTING.md` or remove the links.
- **Item #63** `docs/NONPROFIT_REGISTRATION_TASK_BRIEF.md` references non-existent `SYNC_NOTES.md`, `STATUS.md`, `RUNBOOK.md` — links left in place; create or remove.

## Items reconsidered during execution

- **Originally B9 / Item #53**: `chla-ios/APP_STORE_SUBMISSION.md` was archived,
  then **restored** because it serves a different purpose than
  `chla-ios/APP_STORE_SUBMISSION_GUIDE.md`: the former is the public-facing
  EN/ES copy referenced from `docs/CLINICAL_TEAM_HANDOFF.md`,
  `docs/PLATFORM_STRATEGY_BRIEF.md`, `docs/NONPROFIT_ONBOARDING_PACKET.md`, and
  `docs/SLACK_CHANNEL_SETUP_PACKET.md`; the latter is the developer submission
  process. Both are kept.
- **`docs/QUICK_REFERENCE.md`** was archived as
  `QUICK_REFERENCE-mapview-refactor.md` because its content was a
  MapView-refactor cheat sheet (mention of weekly milestones, "next: Week 3"),
  not a general project quick reference.

## Verified

- `git mv` used throughout (no `mv` or `rm`).
- No values from any `.env*` file were printed during the audit.
- No tracked `.env*` file was modified (`.env.example` files and the new root
  `.env.example` are placeholder-only).
- All cross-doc links in surviving documents now resolve, except for the four
  decisions deferred above.
