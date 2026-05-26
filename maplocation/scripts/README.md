# `maplocation/scripts/`

One-off operational scripts. **Not** part of the production deploy path.

Production migrations and data loads run through Django management commands
under `locations/management/commands/`, invoked from `docker-entrypoint.sh`.
The scripts here are diagnostic/repair tools that were used during specific
incidents or bring-up; most are already-run history.

## Layout

| Folder | Purpose |
|---|---|
| `migrations/` | One-shot migration repair (fake-apply, fix, verify). Already-run; kept for reference. |
| `checks/` | Read-only inspection scripts (check_*, verify_*, debug_*, list_*). Safe to re-run. |
| `sync/` | RDS ↔ local sync utilities and import helpers. **See security note below.** |
| `cleanup/` | Schema/relationship repair (rebuild_*, cleanup_*, restore_*). **See security note.** |
| `data/` | Bulk data population (ZIP codes, client users). |
| `migrate_to_postgis.sql` | One-time PostGIS migration SQL. |
| `setup_rds_postgis.sh` | One-time PostGIS setup against RDS. |

## Security note

Several scripts in `sync/` and `cleanup/` contain hardcoded RDS credentials
from earlier incident response. Treat the credential as compromised: rotate
it in AWS, update Elastic Beanstalk env vars, and scrub git history with
`git-filter-repo --replace-text` before sharing the repo more broadly. Do
not run these scripts as-is against new environments.

## When to add a new script here vs. as a management command

- **Management command** (`locations/management/commands/`): anything you
  want production to run, anything that needs Django settings, anything
  reusable. Discoverable via `python manage.py help`.
- **Script in `scripts/`**: throwaway diagnostics, one-time data fixes,
  shell-orchestration that doesn't fit a Django command.
