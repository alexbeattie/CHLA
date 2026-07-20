# KiNDD Data Scripting Intern Brief

Assignment for the engineering intern responsible for collecting, cleaning, and delivering provider and resource data from public internet sources for KiNDD - NDD Resource Navigator.

This brief follows the same working model as the other role briefs in `docs/`. Product facts in the onboarding docs win over this brief; this brief wins on process and deliverables.

---

## 1. Role And Mission

- Role: Data Scripting Intern
- Reports to: KiNDD product owner
- Mission: Build small, reproducible Python scripts that pull provider and resource data from official public sources, normalize it to the KiNDD provider schema, and deliver reviewed CSV files through pull requests. You write scripts and produce data files; the product owner reviews everything and performs all imports into the live system.

You are not responsible for (and cannot affect) deployments, databases, or the live site. Nothing you write ships without the product owner's review and merge.

---

## 2. How You Work: Fork And Pull Request

1. Fork `https://github.com/alexbeattie/CHLA` to your own GitHub account. The repo is public; no invite is needed.
2. Clone your fork and create one branch per task, named `data/<short-topic>`, for example `data/npi-la-county`.
3. All of your code lives in `scripts/data-ingestion/`. Do not modify files outside that directory unless a task explicitly says to.
4. When a piece of work is ready, open a pull request from your fork's branch to `main` on the upstream repo. Fill in the checklist in section 7. Keep PRs small: one source or one script per PR beats one giant PR.
5. The product owner reviews every PR. Expect review comments; respond in the PR thread so the discussion is searchable.

Local setup:

```bash
cd scripts/data-ingestion
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env   # then fill in your contact email
```

Repo docs to read once before your first PR: `AGENTS.md` and `QUICK_START.md` in the repo root.

---

## 3. Access Boundaries

What you have:

- A fork of the public repo.
- This brief, the `scripts/data-ingestion/` starter directory, and the repo docs above.

What you never have, and never need:

- AWS credentials or console access.
- Database access of any kind (the live Postgres, RDS, connection strings).
- GitHub Actions secrets, deploy scripts, or the db-sync scripts.
- App Store, Play Store, Slack, or Drive credentials.
- The contents of anyone's `.env` file.

Hard rule: if a task ever seems to require a credential, an API key someone else owns, or access to a private system, stop and flag it to the product owner. Do not ask around for keys and do not accept credentials pasted into chat.

---

## 4. Data Source Priorities

Work sources in this order:

1. **Official APIs and open data.** The NPPES NPI Registry API (`https://npiregistry.cms.hhs.gov/api-page`) is the canonical free source for US healthcare provider data and needs no API key. California DDS publishes Regional Center and vendor information. Government and official sources beat scraping every time: they are legal, stable, and structured.
2. **Approved scraping targets.** Only sites the product owner has named in a task. Section 5 rules apply to every request.
3. **Never:** anything behind a login or paywall, any source whose terms of service forbid collection, and any personal data about patients or families.

If you find a promising new source, propose it in your weekly update before writing a scraper for it.

---

## 5. Collection Rules (Non-Negotiable)

- Check `robots.txt` and the site's terms before scraping a host. If collection is disallowed, skip the site and note it in your report.
- Rate limit to at most 1 request per second per host, with exponential backoff on errors. Never parallelize requests against one host.
- Send an honest `User-Agent` that identifies the project and includes your contact email (the starter script does this for you).
- Cache raw responses locally under `cache/` (gitignored) so re-runs and debugging do not re-hit the source.
- Collect provider business information only: names, addresses, phone numbers, websites, services. Never collect or store personal data about patients, families, or individuals who are not operating as providers.
- When in doubt about whether a source or field is acceptable, ask before collecting.

---

## 6. Output Contract

Each task produces a CSV in `scripts/data-ingestion/deliverables/`, committed in your PR. Columns, in order:

| Column | Fill with |
| --- | --- |
| `name` | Provider or organization name |
| `address` | Single line: `Street, City, CA ZIP` |
| `latitude` | Leave empty (owner geocodes with existing tooling) |
| `longitude` | Leave empty |
| `phone` | `XXX-XXX-XXXX`, no extensions |
| `website` | Full URL if the source has one, else empty |
| `therapy_types` | Semicolon-separated service names, else empty |
| `insurance_accepted` | Comma-separated, else empty |
| `diagnoses_treated` | Leave empty unless the source states it |
| `age_groups` | Leave empty unless the source states it |
| `regional_centers` | Leave empty (owner assigns) |
| `description` | One sentence from the source, else empty |
| `type` | `individual` or `organization` |
| `email` | Business email if published, else empty |
| `npi` | NPI number when the source is NPPES, else empty |
| `source_name` | Short source label, for example `NPPES` |
| `source_url` | URL or API query that produced the row |
| `fetched_at` | ISO 8601 UTC timestamp |

Empty means an empty cell, not `N/A` or `unknown`. The final three columns are provenance: every row must be traceable to where and when it came from.

Deduplicate before delivering:

- Within your file: by `npi` when present, otherwise by normalized name plus phone.
- Against the existing directory: check `providers_complete_export.csv` in the repo root and drop rows that already exist. Note the counts in your validation report.

Every deliverable PR includes a short validation report in the PR description: source and query parameters, date fetched, total rows, duplicates removed and the dedupe key used, null counts for the non-empty-by-design columns, and five sample rows.

---

## 7. Pull Request Checklist

Paste this into each PR description and complete it:

```
Source: <name and URL>
Query/scope: <parameters or pages covered>
Fetched: <date>
Rows delivered: <n> (removed <n> in-file dupes, <n> already in providers_complete_export.csv)
Null counts: <column: n, ...>
Sample rows: <paste 5>
Collection rules: [ ] robots.txt/ToS checked  [ ] rate-limited  [ ] raw responses cached
Scope: [ ] only scripts/data-ingestion/ touched
```

Commit message style: plain, present tense, no emojis. Example: `Add NPI registry pull for LA County behavior analysts`.

---

## 8. First Task: NPI Registry Pull For LA County

Goal: a deliverable CSV of LA County providers relevant to neurodevelopmental services, pulled from the NPPES NPI Registry API.

1. Read the API docs at `https://npiregistry.cms.hhs.gov/api-page`. Version 2.1, no key required.
2. Use the starter script `fetch_npi_providers.py` in `scripts/data-ingestion/`. It handles rate limiting, caching, pagination, and the output columns.
3. Pull these taxonomies to start, one run each: `Behavior Analyst`, `Speech-Language Pathologist`, `Occupational Therapist`, `Developmental Therapist`, `Psychologist`, `Developmental-Behavioral Pediatrics`.
4. Mind the API's pagination cap: `limit` maxes at 200 and `skip` at 1000, so one query returns at most 1,200 rows. Segment big taxonomies by city (the script's `--city` flag, repeatable). Start with the largest LA County cities: Los Angeles, Long Beach, Glendale, Santa Clarita, Pasadena, Torrance, Pomona, Palmdale, Lancaster, El Monte. The script warns you when a segment hits the cap.
5. Merge, dedupe, and deliver per section 6. One PR for the script changes and the first taxonomy is fine; follow-up taxonomies can come as small follow-up PRs.

Done means: the deliverable CSV is in the PR, the checklist is complete, and every row has `npi`, `source_name`, `source_url`, and `fetched_at` filled.

---

## 9. Reporting

Weekly update every Friday before 17:00, in Slack (`#kindd`) if you have workspace access, otherwise as a comment on your active PR or issue. Three lines:

- Done: what was delivered or merged this week.
- Next: what you plan to do next week.
- Blocked: anything you are waiting on, or `none`.

---

## 10. Shared Guardrails

Same rules as every KiNDD role:

- Product name is `KiNDD - NDD Resource Navigator`; short form `KiNDD`.
- No emojis in code, docs, commit messages, or PRs.
- No secrets, credentials, `.env` contents, or raw database dumps in the repository, PRs, or chat. The repo is public.
- No personal or patient data, ever.
- No public statements about the product; anything outward-facing goes through the product owner.
