# KiNDD Data Ingestion Scripts

Scripts that collect provider and resource data from official public sources and normalize it to the KiNDD provider schema. Owned by the Data Scripting Intern role; the full assignment, rules, and output contract live in [docs/DATA_SCRIPTING_INTERN_BRIEF.md](../../docs/DATA_SCRIPTING_INTERN_BRIEF.md). Read that first.

## Setup

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env   # fill in your contact email
```

## Layout

- `fetch_npi_providers.py` - starter script: pulls providers from the NPPES NPI Registry API.
- `cache/` - raw API/HTTP responses, gitignored. Re-runs read from here instead of re-hitting sources.
- `output/` - working output, gitignored.
- `deliverables/` - final reviewed CSVs, committed in PRs.

## Example run

```bash
python fetch_npi_providers.py \
  --taxonomy "Behavior Analyst" \
  --city "Los Angeles" --city "Long Beach" --city "Pasadena" \
  --out output/npi_behavior_analysts.csv
```

The script rate-limits to 1 request/second, caches every page under `cache/`, and warns if a query segment hits the API's 1,200-row pagination cap (narrow with more `--city` values when it does).

## Rules that always apply

- Official APIs and open data before scraping; scraping only for targets approved in a task.
- 1 request/second per host, honest User-Agent with contact email, respect robots.txt and site terms.
- Provider business data only. Never personal or patient data.
- No credentials anywhere in this directory. The repo is public.
