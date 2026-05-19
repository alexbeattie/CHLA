# Clinical Team Handoff

Onboarding and handoff package for the new clinical stakeholder team (MDs, physicians, clinicians). This single document contains:

1. Slack channel copy (welcome message + follow-ups)
2. Resource bundle (what to share, what to keep in engineering)
3. Postgres / PostGIS full-copy runbook (export, restore, verify)
4. RAG / Pinecone copy + rebuild runbook (current configuration + handoff paths)
5. Verification and launch checklist

> **Companion documents (operator-facing)**
>
> - [docs/SLACK_CHANNEL_SETUP_PACKET.md](./SLACK_CHANNEL_SETUP_PACKET.md) - exact Slack channel + Drive folder setup, ready-to-post copy, and document rename/upload map.
> - [docs/DATA_EXPORT_AND_DRIVE_UPLOAD_RUNBOOK.md](./DATA_EXPORT_AND_DRIVE_UPLOAD_RUNBOOK.md) - copy/paste Postgres and RAG snapshot commands, checksums, restore verification, and upload steps.
> - [docs/NONPROFIT_REGISTRATION_TASK_BRIEF.md](./NONPROFIT_REGISTRATION_TASK_BRIEF.md) - detailed US-generic task assignment for the hired nonprofit-registration operator.
> - Drive handoff folder for all uploads and links: <https://drive.google.com/drive/folders/1FMphfJJ10m4WVNLeTgr4P8EG_T1fbr1Q?usp=share_link>
>
> **Data handling note**
>
> Full internal copies of the operational database and RAG corpus are approved for this team. Even so: do not paste raw database dumps, `.env` files, RDS hostnames, passwords, Cohere/Pinecone/OpenAI/Firecrawl/NCBI keys, or full Slack-attached archives into Slack. Share **access links** to a controlled internal location (the Drive folder above) and follow the verification steps below before granting access.

---

## Table of Contents

- [1. Slack Channel Package](#1-slack-channel-package)
- [2. Clinical Documentation Bundle](#2-clinical-documentation-bundle)
- [3. Postgres / PostGIS Full Copy Runbook](#3-postgres--postgis-full-copy-runbook)
- [4. RAG / Pinecone Current Copy Runbook](#4-rag--pinecone-current-copy-runbook)
- [5. Verification & Launch Checklist](#5-verification--launch-checklist)

---

## 1. Slack Channel Package

### Suggested channel

- **Name (suggested):** `#kindd-clinical-stakeholders`
- **Visibility:** Private (clinical reviewers + a small core engineering/PM group)
- **Initial members:** New MD / physician / clinician stakeholders + product owner + 1 engineer on-call for questions
- **Channel description:**
  > Clinical review and feedback for KiNDD - NDD Resource Navigator + NDD Research RAG. Product context, data snapshots, and review workflows live here. Engineering deploys, infra, and credentials live elsewhere.

### A. Pinned welcome message

> **Welcome to the KiNDD clinical stakeholder channel.**
>
> This channel is where the new clinical team (MDs, physicians, and clinician stakeholders) will help us validate provider data, regional center logic, and the NDD research assistant we are building.
>
> **What KiNDD is**
> A geospatial care-navigation platform for neurodevelopmental disorders (NDD) in California (LA County focus). It has a web map, an iOS app ("NDD Resources / KiNDD"), and a research RAG assistant that answers questions over public NDD research with citations.
>
> **Start here:** [`docs/NONPROFIT_ONBOARDING_PACKET.md`](./NONPROFIT_ONBOARDING_PACKET.md) - the curated onboarding guide. It walks through the project in the right order for non-technical stakeholders and includes the full presentation flow.
>
> **Then read (in this order):**
>
> 1. Product overview - `README.md`
> 2. Platform strategy - `docs/PLATFORM_STRATEGY_BRIEF.md`
> 3. Regional Centers domain model - `docs/REGIONAL_CENTERS_CONCEPT.md`
> 4. iOS app overview - `chla-ios/README.md`
> 5. iOS home screen / clinical UX intent - `chla-ios/HOME_PAGE_REDESIGN_SPEC.md`
> 6. App-Store-facing copy - `chla-ios/APP_STORE_SUBMISSION.md`
> 7. Research assistant - `autism_rag/README.md`
> 8. Data entry workflow - `ADMIN_DATA_ENTRY_GUIDE.md`
> 9. Known data/API issues - `docs/API_AND_DATA_ISSUES.md`
>
> **Data snapshots (internal, access-controlled):**
>
> - Provider / Regional Center Postgres (PostGIS) database: see pinned thread "Postgres snapshot".
> - NDD Research RAG corpus state (Pinecone index + raw/processed artifacts): see pinned thread "RAG snapshot".
>
> Both snapshots are full internal copies. They are not for redistribution and should not be posted in this channel or attached to messages. Request access in the snapshot threads below.
>
> **How to give feedback**
> Use threads under each weekly review post. Prefix clinical issues with `Clinical:` and data-quality issues with `Data:`. For anything urgent or PHI-adjacent, DM the product owner instead of posting in-channel.

### B. Follow-up posts (one thread each)

#### Thread 1 - "Product overview"

> Quick read on what we have today and where it is going. Source docs:
>
> - `README.md` - what the product does, live URLs.
> - `docs/PLATFORM_STRATEGY_BRIEF.md` - three layers (data/GIS, web + iOS surfaces, AI on Bedrock + research RAG), what exists today.
> - `chla-ios/APP_STORE_SUBMISSION.md` - public-facing copy in EN/ES, useful as the "what families read" reference.
>
> Open question for the team: what does success look like for a clinician using KiNDD with a family in clinic? Reply in-thread.

#### Thread 2 - "Postgres snapshot" (database handoff)

> We have prepared a full internal copy of the operational Postgres / PostGIS database that backs the provider map and Regional Center logic.
>
> Contents (high level): providers, regional centers + ZIP coverage, insurance carriers, funding sources, service delivery models, provider relationship tables, location categories / images / reviews, and Django auth/admin tables.
>
> Access:
>
> - Snapshot lives in `<internal storage location>` as `chla_postgres_full_YYYYMMDD.dump` (Postgres custom format).
> - Restore runbook: `docs/CLINICAL_TEAM_HANDOFF.md` section 3.
> - To request access, reply in this thread with your internal account / email. Do not request a copy by DM and do not re-upload the dump anywhere.

#### Thread 3 - "RAG snapshot" (research assistant handoff)

> We have prepared a current copy of the NDD Research RAG state. (The underlying repo folder is named `autism_rag/` for historical reasons; scope is neurodevelopmental disorders, with autism as one of several conditions covered.)
>
> Contents (high level):
>
> - Pinecone index `autism-research-rag` with per-source namespaces (`public_literature`, `clinical_trials`, `nih_grants`, `sfari_gene`, `public_web`, `controlled_metadata`, `rare_ndd`, `approved_controlled_exports`).
> - Raw + processed source artifacts under `autism_rag/data/raw` and `autism_rag/data/processed`.
> - Source policy: public / permitted sources only. Controlled-access datasets (NDA, SFARI Base, SPARK, dbGaP, MSSNG) are metadata-only until DUA/IRB is executed.
>
> Two supported handoff paths (pick whichever your environment supports):
>
> 1. Re-create the Pinecone index from raw/processed artifacts using the existing ingestion scripts.
> 2. Use Pinecone-side export/replication into a clinical-team-owned index if you have Pinecone access.
>
> Runbook: `docs/CLINICAL_TEAM_HANDOFF.md` section 4.

#### Thread 4 - "How to review and give feedback"

> Weekly review cadence:
>
> - Monday: product post lists 3-5 things to look at this week (data slices, screens, RAG questions).
> - Throughout the week: clinicians reply in thread with findings.
> - Friday: engineering/PM summarizes what changed.
>
> Labels:
>
> - `Data:` data quality issue (wrong info, missing provider, bad mapping).
> - `Clinical:` clinical accuracy issue (incorrect clinical framing, unsafe phrasing).
> - `Product:` product / UX suggestion.
>
> Anything PHI-adjacent or that names a real patient/family: DM the product owner, do not post in-channel.

#### Thread 5 - "Who owns what"

> - Product / clinical liaison: `<name>`
> - Engineering on-call for this channel: `<name>`
> - Data / DB questions: `<name>`
> - Research RAG questions: `<name>`
> - Access to Postgres / RAG snapshots: reply in the snapshot threads.

---

## 2. Clinical Documentation Bundle

These are the docs to actively share with the clinical channel, in the order they should be read. **Start with [`docs/NONPROFIT_ONBOARDING_PACKET.md`](./NONPROFIT_ONBOARDING_PACKET.md)** - it is the curated, professional onboarding guide assembled from the full doc scan. The rest are supporting references.

| #   | Document                                                                        | Why it matters                                                                                                                                                               |
| --- | ------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | [`docs/NONPROFIT_ONBOARDING_PACKET.md`](./NONPROFIT_ONBOARDING_PACKET.md)       | Curated onboarding packet. Start here. Designed for non-technical stakeholders, includes the recommended presentation flow.                                                  |
| 2   | [`README.md`](../README.md)                                                     | Project overview, live URLs, capability summary.                                                                                                                             |
| 3   | [`docs/PLATFORM_STRATEGY_BRIEF.md`](./PLATFORM_STRATEGY_BRIEF.md)               | Platform vision, three-layer architecture, what exists today, growth directions.                                                                                             |
| 4   | [`docs/REGIONAL_CENTERS_CONCEPT.md`](./REGIONAL_CENTERS_CONCEPT.md)             | California Regional Centers domain model, ZIP -> RC mapping, "RC is not insurance" framing.                                                                                  |
| 5   | [`chla-ios/README.md`](../chla-ios/README.md)                                   | iOS app overview, feature list, what the mobile surface does.                                                                                                                |
| 6   | [`chla-ios/HOME_PAGE_REDESIGN_SPEC.md`](../chla-ios/HOME_PAGE_REDESIGN_SPEC.md) | Clinical UX intent for the iOS home screen ("Ask KiNDD", quick actions, RC map card).                                                                                        |
| 7   | [`chla-ios/APP_STORE_SUBMISSION.md`](../chla-ios/APP_STORE_SUBMISSION.md)       | Public-facing copy in English + Spanish - the language families actually read.                                                                                               |
| 8   | [`docs/CLINICAL_TEAM_HANDOFF.md`](./CLINICAL_TEAM_HANDOFF.md)                   | This document - Slack copy, Postgres + RAG snapshot runbooks, verification checklist.                                                                                        |
| 9   | [`autism_rag/README.md`](../autism_rag/README.md)                               | NDD research RAG overview + source policy (public sources only, controlled-access via DUA/IRB). Folder is named `autism_rag/` for historical reasons; clinical scope is NDD. |
| 10  | [`ADMIN_DATA_ENTRY_GUIDE.md`](../ADMIN_DATA_ENTRY_GUIDE.md)                     | How provider records are entered, edited, and validated. Useful for clinicians flagging data corrections.                                                                    |
| 11  | [`docs/API_AND_DATA_ISSUES.md`](./API_AND_DATA_ISSUES.md)                       | Known data-quality issues and gaps. Aligns stakeholders on what to look for during review.                                                                                   |

**Do not post in the clinical channel by default** (these mix infra/credentials/operational identifiers with low clinical value):

- `QUICK_START.md`, `QUICK_START_CICD.md`
- `STACK_DOCUMENTATION.md` (contains AWS hostnames, bucket names, RDS identifiers)
- `maplocation/README.md`, `map-frontend/ENVIRONMENT_SWITCHING.md`
- `docs/DEPLOYMENT.md`, `docs/GITHUB_ACTIONS.md`, `docs/DATABASE_SYNC.md`, `docs/EB_DEPLOYMENT_ISSUE.md`
- Refactor / week-by-week / SEO / session-summary docs

Sharing a redacted excerpt of `STACK_DOCUMENTATION.md` is fine if clinicians need to understand a specific user-impact issue; do not share the full file.

---

## 3. Postgres / PostGIS Full Copy Runbook

This is the supported way to produce a full internal copy of the operational database for the clinical handoff. It supersedes the partial-subset sync scripts in `maplocation/` (`sync_rds_to_local.py`, `sync_rds_to_local_complete.py`, `sync_from_rds_to_local.sh`) for the purposes of this handoff.

### 3.1 Source of truth

- Database config: [`maplocation/maplocation/settings.py`](../maplocation/maplocation/settings.py)
  - Engine: `django.contrib.gis.db.backends.postgis`
  - Env-driven: `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_HOST`, `DB_PORT`, `DB_SSL_REQUIRE`, `DB_CONN_MAX_AGE`.
- Reference patterns (subset, not full-copy): [`docs/DATABASE_SYNC.md`](./DATABASE_SYNC.md).
- Local target image / schema parity: [`docker-compose.local.yml`](../docker-compose.local.yml) (PostGIS 16, port 5433).

### 3.2 What is included

The dump captures the full Django schema for the `maplocation` project, including:

- `providers_v2` (core providers, including coordinates)
- `regional_centers` (LA County + statewide RC reference)
- `provider_regional_centers` (provider <-> RC relationships)
- Insurance carriers, funding sources, service delivery models, and their join tables
- Location categories, images, reviews
- Django `auth_*` / `django_*` admin tables, plus user app tables
- Any `pgvector` / embedding columns added by recent migrations (e.g. `locations/migrations/0032_add_embedding_field.py`)

Externally-managed schemas referenced by the codebase (e.g. the `hmgl.location` view at `locations/models.py` `managed = False`) are **not** part of the dump and must be re-pointed to the original source if needed.

### 3.3 Prerequisites on the export machine

- Network access to the source Postgres instance.
- A PG client matching the **server major version** (currently PostGIS 16 / Postgres 16). Mismatched `pg_dump` versions will fail.
- An environment with `DB_*` exported (use `.env`, never inline shell history):

  ```bash
  export DB_HOST=...        # source DB host
  export DB_PORT=5432
  export DB_USER=...
  export DB_PASSWORD=...    # do not commit, do not paste in Slack
  export DB_NAME=...
  ```

### 3.4 Produce a full custom-format dump

```bash
STAMP="$(date -u +%Y%m%d)"
OUTFILE="chla_postgres_full_${STAMP}.dump"

PGPASSWORD="$DB_PASSWORD" pg_dump \
  -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
  -Fc --no-owner --no-privileges \
  --verbose \
  -f "$OUTFILE"
```

Notes:

- `-Fc` (custom format) is required so the restore step can do parallel restore and selective table restore later.
- `--no-owner --no-privileges` keeps the dump portable across environments with different role names.
- Do not use `--data-only` for the handoff. The clinical team needs schema + extensions + data.
- If TLS is required, the connection string can use `?sslmode=require`, matching `DB_SSL_REQUIRE=true` in `settings.py`.

### 3.5 Optional: also produce a plain-SQL bootstrap

Useful if the receiving environment is more comfortable with `psql` than `pg_restore`:

```bash
PGPASSWORD="$DB_PASSWORD" pg_dump \
  -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
  --clean --if-exists --no-owner --no-privileges \
  -f "chla_postgres_full_${STAMP}.sql"
```

### 3.6 Restore into a fresh local PostGIS 16 instance

Start a clean local instance that matches [`docker-compose.local.yml`](../docker-compose.local.yml):

```bash
docker compose -f docker-compose.local.yml up -d
```

This gives you Postgres 16 + PostGIS 3.4 at `localhost:5433`, database `chla_local`, user `chla_dev`.

Ensure required extensions exist in the target DB (the dump usually creates them, but it is safe to pre-create):

```bash
PGPASSWORD=dev_password psql -h localhost -p 5433 -U chla_dev -d chla_local <<'SQL'
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS vector;
SQL
```

Restore the custom dump:

```bash
PGPASSWORD=dev_password pg_restore \
  -h localhost -p 5433 -U chla_dev -d chla_local \
  --no-owner --no-privileges \
  --clean --if-exists \
  --jobs=4 \
  --verbose \
  "chla_postgres_full_${STAMP}.dump"
```

If you produced the plain-SQL variant instead:

```bash
PGPASSWORD=dev_password psql -h localhost -p 5433 -U chla_dev -d chla_local \
  -f "chla_postgres_full_${STAMP}.sql"
```

### 3.7 Verify the restore

```bash
PGPASSWORD=dev_password psql -h localhost -p 5433 -U chla_dev -d chla_local <<'SQL'
\dx
\dt
SELECT COUNT(*) AS providers          FROM providers_v2;
SELECT COUNT(*) AS regional_centers   FROM regional_centers;
SELECT COUNT(*) AS provider_rc_links  FROM provider_regional_centers;
SELECT COUNT(*) FROM auth_user;
SQL
```

Then run a quick Django check against the restored DB:

```bash
cd maplocation
export DB_HOST=localhost DB_PORT=5433 DB_NAME=chla_local DB_USER=chla_dev DB_PASSWORD=dev_password
python3 manage.py showmigrations locations | tail -n 20
python3 manage.py check
```

All `locations` migrations should be marked applied and `manage.py check` should report no issues.

### 3.8 Hand off

- Upload `chla_postgres_full_${STAMP}.dump` (and optionally the `.sql` variant) to the approved internal storage location.
- Set access on that storage to the clinical team only.
- Post the **access link** (not the file) in the "Postgres snapshot" Slack thread, plus the restore commands above.
- Rotate / never reuse: if the dump was produced with credentials that ended up in shell history on a shared machine, rotate the source credentials.

---

## 4. RAG / Pinecone Current Copy Runbook

> Naming note: the on-disk folder is `autism_rag/` and the Pinecone index is `autism-research-rag` for historical reasons. The current clinical scope is **neurodevelopmental disorders (NDD)** broadly - autism is one of several conditions covered, alongside the separated `rare_ndd` corpus and rare-disease / NDD sources. Treat all of section 4's file paths, module names, and the index name as literal identifiers (do not rename them), but describe the system to clinicians as the **NDD Research RAG**.

The RAG system has two parts that must be handled separately:

1. The Pinecone vector index (lives outside the repo).
2. The on-disk raw + processed source artifacts under `autism_rag/data/`.

Vectors themselves do not live in the repository. A full handoff is either a Pinecone-side copy/replication or a re-ingest from the artifacts.

### 4.1 Current configuration (source of truth)

From [`autism_rag/config/settings.py`](../autism_rag/config/settings.py):

| Setting               | Default               | Notes                                  |
| --------------------- | --------------------- | -------------------------------------- |
| `pinecone_index`      | `autism-research-rag` | Index name                             |
| `pinecone_cloud`      | `aws`                 |                                        |
| `pinecone_region`     | `us-east-1`           |                                        |
| `pinecone_metric`     | `cosine`              |                                        |
| `pinecone_embed_dims` | `1536`                | Must match embedding model             |
| `cohere_embed_model`  | `embed-v4.0`          | Cohere `embed-v4.0`, 1536-dim          |
| `rerank_model`        | `rerank-english-v3.0` | Cohere reranker                        |
| `answer_model`        | `gpt-4o-mini`         | OpenAI, optional for answer generation |

From [`autism_rag/sources/registry.py`](../autism_rag/sources/registry.py), Pinecone **namespaces** in use today:

| Namespace                     | Sources feeding it                                                                                                            |
| ----------------------------- | ----------------------------------------------------------------------------------------------------------------------------- |
| `public_literature`           | PubMed, PMC OA, OpenAlex                                                                                                      |
| `clinical_trials`             | ClinicalTrials.gov                                                                                                            |
| `nih_grants`                  | NIH RePORTER                                                                                                                  |
| `sfari_gene`                  | SFARI Gene                                                                                                                    |
| `public_web`                  | Permitted web pages via Firecrawl                                                                                             |
| `controlled_metadata`         | NDA / SFARI Base / dbGaP metadata only                                                                                        |
| `approved_controlled_exports` | Reserved; requires executed DUA/IRB                                                                                           |
| `rare_ndd`                    | Separated rare disease / NDD corpus (see [`autism_rag/scripts/ingest_rare_ndd.py`](../autism_rag/scripts/ingest_rare_ndd.py)) |

### 4.2 Source policy (must be shared with the clinical team)

From [`autism_rag/README.md`](../autism_rag/README.md):

- Ingest only public APIs with documented programmatic access (PubMed, PMC OA, ClinicalTrials.gov, NIH RePORTER, OpenAlex, Semantic Scholar).
- Curated open downloads (SFARI Gene) under their published terms.
- Permitted web pages via Firecrawl, respecting robots / site terms - **explicit URLs only**, no bulk crawl.
- Controlled-access datasets (NDA, SFARI Base, SPARK, dbGaP, MSSNG) are tracked as **metadata only** until DUAs and IRB approvals are in place.
- Every chunk carries `access_class`, `license`, `source_url`, and citation identifiers.

### 4.3 Handoff Path A - Re-create the index from artifacts (recommended)

Use this when the receiving team has their own Pinecone project / API key and you want a clean, reproducible build instead of a vector-level copy.

1. **Inventory and package the artifacts.**

   ```bash
   cd autism_rag
   du -sh data/raw data/processed 2>/dev/null
   tar -C . -czf "../autism_rag_artifacts_$(date -u +%Y%m%d).tar.gz" data/raw data/processed
   ```

   Upload the tarball to the same approved internal storage location used for the Postgres dump.

2. **Receiving environment setup.**

   ```bash
   cd autism_rag
   python3 -m venv .venv
   source .venv/bin/activate
   pip install -r requirements.txt
   cp .env.example .env
   # Fill in COHERE_API_KEY, PINECONE_API_KEY, FIRECRAWL_API_KEY,
   # NCBI_API_KEY, NCBI_EMAIL, OPENAI_API_KEY (optional)
   # Pinecone index name / region / metric should match section 4.1.
   ```

   Extract the artifacts into `autism_rag/data/`:

   ```bash
   tar -xzf autism_rag_artifacts_YYYYMMDD.tar.gz
   ```

3. **Sanity check without writing to Pinecone.**

   ```bash
   source autism_rag/.venv/bin/activate
   python3 -m autism_rag.scripts.list_sources
   python3 -m autism_rag.scripts.ingest --source pubmed --query "neurodevelopmental disorders" --limit 5 --dry-run
   ```

4. **Re-ingest public sources into Pinecone (namespaces per the registry).**

   ```bash
   python3 -m autism_rag.scripts.ingest_all --limit 50
   python3 -m autism_rag.scripts.ingest_rare_ndd --limit 50
   ```

   For permitted web pages, ingest only explicit URLs. The examples below mix broad NDD sources with autism-specific ones (autism is one NDD among several):

   ```bash
   python3 -m autism_rag.scripts.ingest --source firecrawl_web \
     --urls https://www.nichd.nih.gov/health/topics/neuro/conditioninfo \
            https://www.cdc.gov/ncbddd/developmentaldisabilities/index.html \
            https://www.nimh.nih.gov/health/topics/autism-spectrum-disorders-asd
   ```

5. **Smoke-test the rebuilt index.**

   ```bash
   python3 -m autism_rag.scripts.query "What does recent literature say about early screening for neurodevelopmental disorders?"
   python3 -m autism_rag.scripts.query --corpus rare_ndd \
     "What rare diseases or rare disorders are associated with NDD?"
   python3 -m autism_rag.scripts.evaluate
   uvicorn autism_rag.api.server:app --reload   # local /ask
   ```

### 4.4 Handoff Path B - Pinecone-side copy (if the team has Pinecone access)

Use this when the receiving team should inherit the **exact** current vector state and has Pinecone tooling available.

1. From a machine with `PINECONE_API_KEY` for the source project, capture the index state:
   - Confirm index name (`autism-research-rag`), region (`us-east-1`), cloud (`aws`), metric (`cosine`), dim (`1536`).
   - List namespaces and record counts per namespace; save as `pinecone_index_inventory_YYYYMMDD.json`.
2. Use Pinecone's supported export / backup tooling (CLI or API) to copy vectors into the receiving project's index. Follow Pinecone's current docs - do not invent a copy script here.
3. Re-create the target index with the same configuration as section 4.1 before importing.
4. Validate by re-running the smoke-test queries from section 4.3 step 5 against the new index.

### 4.5 Hand off

- Upload `autism_rag_artifacts_YYYYMMDD.tar.gz` and `pinecone_index_inventory_YYYYMMDD.json` to the approved internal storage location.
- Do **not** include `.env`, raw API keys, or Pinecone keys in the tarball or in Slack.
- Post the **access link** in the "RAG snapshot" Slack thread, with a one-line note on which path (A or B) the team should follow.

---

## 5. Verification & Launch Checklist

Work top to bottom. Do not post Slack access links until the corresponding "verify" boxes are checked.

### 5.1 Postgres snapshot

- [ ] Source DB credentials are sourced from env / a secret store, not from shell history or chat.
- [ ] `pg_dump` major version matches the source server version.
- [ ] Custom-format dump (`-Fc`) produced and file size is sane (compare to previous backups if any).
- [ ] Dump restores cleanly into the local PostGIS 16 instance from [`docker-compose.local.yml`](../docker-compose.local.yml).
- [ ] `\dx` shows `postgis` (and `vector` if any embeddings exist).
- [ ] Row counts in `providers_v2`, `regional_centers`, `provider_regional_centers`, `auth_user` look correct vs production expectations.
- [ ] `python3 manage.py showmigrations locations` shows all migrations applied on the restored DB.
- [ ] `python3 manage.py check` passes against the restored DB.
- [ ] Dump uploaded to approved internal storage with access limited to the clinical team.
- [ ] Source DB credentials rotated if they touched a shared machine.

### 5.2 RAG snapshot

- [ ] Current Pinecone configuration captured (index name, region, metric, dim, namespaces, per-namespace record counts).
- [ ] Path A: artifacts tarball produced, uploaded, restoreable in a fresh environment.
- [ ] Path A: `python3 -m autism_rag.scripts.list_sources` runs in the receiving env.
- [ ] Path A: `--dry-run` ingest succeeds without API keys.
- [ ] Path A or B: smoke-test queries return cited results from the expected namespaces.
- [ ] No `.env` files, API keys, or controlled-access participant-level data included in the handoff.
- [ ] Source policy section from [`autism_rag/README.md`](../autism_rag/README.md) is in the snapshot thread.

### 5.3 Clinical documentation bundle

- [ ] Links in section 2 above resolve from the receiving team's clone / view of the repo.
- [ ] Engineering-only docs (deployment, CI/CD, env switching, stack details) are **not** posted in the clinical channel.

### 5.4 Slack channel launch

- [ ] Channel created with the agreed name and description.
- [ ] Initial members invited (clinical reviewers + product owner + 1 engineering on-call).
- [ ] Welcome message posted and pinned.
- [ ] Follow-up threads created: Product overview, Postgres snapshot, RAG snapshot, How to review, Who owns what.
- [ ] Access links (Postgres dump, RAG artifacts) posted only in their respective snapshot threads, not in the main channel.
- [ ] Product owner + engineering on-call acknowledged in-channel.
- [ ] First weekly review post scheduled.

---

**Last updated:** 2026-05-17
**Owner:** Product + Engineering
