# Data Export And Drive Upload Runbook

Operator-facing runbook to produce a full Postgres/PostGIS snapshot of the operational database and a current snapshot of the NDD Research RAG corpus, then upload both to the restricted Drive folder.

This runbook is intentionally exact: copy/paste commands, replace `YYYYMMDD` with the actual UTC date, and follow the verification checks before posting anything to Slack.

Drive folder for outputs:

<https://drive.google.com/drive/folders/1FMphfJJ10m4WVNLeTgr4P8EG_T1fbr1Q?usp=share_link>

Target subfolder: `03_Data_Snapshots_Restricted`

---

## 0. Naming Conventions

| File                   | Pattern                                                                |
| ---------------------- | ---------------------------------------------------------------------- |
| Postgres dump          | `kindd_postgres_full_YYYYMMDD.dump` (custom format from `pg_dump -Fc`) |
| Postgres checksum      | `kindd_postgres_full_YYYYMMDD.sha256`                                  |
| RAG artifacts tarball  | `ndd_rag_artifacts_YYYYMMDD.tar.gz`                                    |
| RAG artifacts checksum | `ndd_rag_artifacts_YYYYMMDD.sha256`                                    |
| Pinecone inventory     | `pinecone_index_inventory_YYYYMMDD.json`                               |

`YYYYMMDD` is the UTC export date. Pick one date for all files in a single snapshot pass.

```bash
STAMP="$(date -u +%Y%m%d)"
echo "Snapshot stamp: ${STAMP}"
```

---

## 1. Safety And Access Rules

- Source credentials come from environment variables or a secret store. Never paste them into Slack, commits, or the Drive folder description.
- Produce snapshots on a trusted machine. Avoid shared/public shells.
- Restore-test every snapshot before sharing the access link.
- Drive access for `03_Data_Snapshots_Restricted` is by request only. Posts in Slack link to the folder, never attach the files.
- If credentials touched a shared machine during export, rotate them after upload.

---

## 2. Postgres / PostGIS Operational Snapshot

The operational database is configured by [maplocation/maplocation/settings.py](../maplocation/maplocation/settings.py). The schema includes providers, regional centers, ZIP coverage, insurance carriers, funding sources, service models, relationship tables, Django auth/admin tables, and any `pgvector` columns added by recent migrations.

### 2.1 Prerequisites

- `pg_dump` major version must match the source server major version (currently Postgres 16).
- Postgres client tools installed (`pg_dump`, `pg_restore`, `psql`).
- Network access to the source database.
- Environment variables for the source DB exported in your shell:

```bash
export DB_HOST="..."        # source DB host
export DB_PORT="5432"
export DB_USER="..."
export DB_PASSWORD="..."    # do not commit, do not paste in Slack
export DB_NAME="..."
```

### 2.2 Produce the dump

```bash
STAMP="$(date -u +%Y%m%d)"
OUTFILE="kindd_postgres_full_${STAMP}.dump"

PGPASSWORD="${DB_PASSWORD}" pg_dump \
  -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" \
  -Fc --no-owner --no-privileges \
  --verbose \
  -f "${OUTFILE}"
```

Notes:

- `-Fc` (custom format) supports parallel restore and selective table restore later.
- `--no-owner --no-privileges` keeps the dump portable across environments.
- Do not use `--data-only`. Stakeholders need schema, extensions, and data.

If TLS is required, add `?sslmode=require` to the connection or set `PGSSLMODE=require`.

### 2.3 Generate the checksum

```bash
shasum -a 256 "${OUTFILE}" > "${OUTFILE%.dump}.sha256"
cat "${OUTFILE%.dump}.sha256"
```

### 2.4 Restore-test into a fresh local PostGIS 16 instance

A clean local instance is defined in [docker-compose.local.yml](../docker-compose.local.yml). Start it:

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
  "${OUTFILE}"
```

### 2.5 Verify the restore

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

Then run Django checks against the restored DB:

```bash
cd maplocation
export DB_HOST=localhost DB_PORT=5433 DB_NAME=chla_local DB_USER=chla_dev DB_PASSWORD=dev_password
python3 manage.py showmigrations locations | tail -n 20
python3 manage.py check
cd -
```

All `locations` migrations should be marked applied and `manage.py check` should report no issues.

### 2.6 Upload to Drive

Upload these two files to:

`03_Data_Snapshots_Restricted/Postgres/`

- `kindd_postgres_full_${STAMP}.dump`
- `kindd_postgres_full_${STAMP}.sha256`

Then verify the upload by re-downloading the `.sha256` from Drive and re-running:

```bash
shasum -a 256 -c "kindd_postgres_full_${STAMP}.sha256"
```

---

## 3. NDD Research RAG Snapshot

The RAG system has two parts that must be handled separately:

1. The Pinecone vector index (lives outside the repo).
2. The on-disk raw + processed source artifacts under `autism_rag/data/`.

> Naming note: the on-disk folder is `autism_rag/` and the Pinecone index is `autism-research-rag` for historical reasons. The clinical scope is neurodevelopmental disorders broadly. Treat the folder/index names as literal identifiers; describe the system as the NDD Research RAG when posting.

Current configuration source of truth: [autism_rag/config/settings.py](../autism_rag/config/settings.py).

| Setting               | Default               | Notes                         |
| --------------------- | --------------------- | ----------------------------- |
| `pinecone_index`      | `autism-research-rag` | Index name                    |
| `pinecone_cloud`      | `aws`                 |                               |
| `pinecone_region`     | `us-east-1`           |                               |
| `pinecone_metric`     | `cosine`              |                               |
| `pinecone_embed_dims` | `1536`                | Must match embedding model    |
| `cohere_embed_model`  | `embed-v4.0`          | Cohere `embed-v4.0`, 1536-dim |
| `rerank_model`        | `rerank-english-v3.0` | Cohere reranker               |
| `answer_model`        | `gpt-4o-mini`         | OpenAI, optional              |

Namespaces today (from [autism_rag/sources/registry.py](../autism_rag/sources/registry.py)): `public_literature`, `clinical_trials`, `nih_grants`, `sfari_gene`, `public_web`, `controlled_metadata`, `approved_controlled_exports`, `rare_ndd`.

### 3.1 Package the on-disk artifacts

```bash
STAMP="$(date -u +%Y%m%d)"
cd autism_rag
du -sh data/raw data/processed 2>/dev/null
tar -C . -czf "../ndd_rag_artifacts_${STAMP}.tar.gz" data/raw data/processed
cd -

shasum -a 256 "ndd_rag_artifacts_${STAMP}.tar.gz" > "ndd_rag_artifacts_${STAMP}.sha256"
cat "ndd_rag_artifacts_${STAMP}.sha256"
```

If `data/raw` or `data/processed` does not exist, run the dry-run ingestion first per [autism_rag/README.md](../autism_rag/README.md) to materialize them, then re-run the tar step.

### 3.2 Capture Pinecone inventory

You need `PINECONE_API_KEY` for the source project. Use the official Pinecone client (already a project dependency).

```bash
STAMP="$(date -u +%Y%m%d)"
PINECONE_API_KEY="..." python3 - <<'PY'
import json, os, datetime
from pinecone import Pinecone

pc = Pinecone(api_key=os.environ["PINECONE_API_KEY"])
index_name = os.environ.get("PINECONE_INDEX", "autism-research-rag")
idx = pc.Index(index_name)
desc = pc.describe_index(index_name)
stats = idx.describe_index_stats()

inventory = {
    "captured_at_utc": datetime.datetime.utcnow().isoformat() + "Z",
    "index": index_name,
    "describe_index": dict(desc) if not isinstance(desc, dict) else desc,
    "describe_index_stats": stats.to_dict() if hasattr(stats, "to_dict") else dict(stats),
}
out = f"pinecone_index_inventory_{os.environ['STAMP']}.json"
with open(out, "w", encoding="utf-8") as fh:
    json.dump(inventory, fh, indent=2, default=str)
print(out)
PY
```

The output file `pinecone_index_inventory_YYYYMMDD.json` records:

- Index name, region, cloud, metric, dimension.
- Per-namespace vector counts.
- Capture timestamp.

If a direct vector-export tool is unavailable, this inventory plus the artifacts tarball is enough for the receiving team to rebuild the index using the ingestion scripts.

### 3.3 Validate the rebuild path (optional but recommended)

In a scratch environment:

```bash
cd autism_rag
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
# Fill in COHERE_API_KEY, PINECONE_API_KEY, FIRECRAWL_API_KEY,
# NCBI_API_KEY, NCBI_EMAIL, OPENAI_API_KEY (optional)
# Pinecone index name/region/metric should match section 3 above.

tar -xzf ../ndd_rag_artifacts_${STAMP}.tar.gz

python3 -m autism_rag.scripts.list_sources
python3 -m autism_rag.scripts.ingest --source pubmed \
  --query "neurodevelopmental disorders" --limit 5 --dry-run
```

A successful dry-run confirms the package is restorable.

### 3.4 Upload to Drive

Upload these three files to:

`03_Data_Snapshots_Restricted/RAG/`

- `ndd_rag_artifacts_${STAMP}.tar.gz`
- `ndd_rag_artifacts_${STAMP}.sha256`
- `pinecone_index_inventory_${STAMP}.json`

Verify after upload:

```bash
shasum -a 256 -c "ndd_rag_artifacts_${STAMP}.sha256"
```

Confirm `pinecone_index_inventory_${STAMP}.json` opens and contains the expected per-namespace counts.

---

## 4. Source Policy Reminder

From [autism_rag/README.md](../autism_rag/README.md):

- Ingest only public APIs with documented programmatic access (PubMed, PMC OA, ClinicalTrials.gov, NIH RePORTER, OpenAlex, Semantic Scholar).
- Curated open downloads (SFARI Gene) under their published terms.
- Permitted web pages via Firecrawl, respecting robots / site terms, explicit URLs only.
- Controlled-access datasets (NDA, SFARI Base, SPARK, dbGaP, MSSNG) are metadata-only until DUAs and IRB approvals are in place.

If the snapshot includes any non-public material by mistake, stop, delete the file from Drive, rotate any exposed credentials, and notify the product owner before re-running.

---

## 5. Pre-Upload Verification Checklist

Postgres

- [ ] `pg_dump` major version matches source server.
- [ ] Custom-format dump produced with `-Fc`.
- [ ] Checksum file produced.
- [ ] Dump restores cleanly into local PostGIS 16.
- [ ] `\dx` shows `postgis` (and `vector` if any embeddings exist).
- [ ] Row counts in `providers_v2`, `regional_centers`, `provider_rc_links`, `auth_user` look correct.
- [ ] `python3 manage.py showmigrations locations` shows all applied.
- [ ] `python3 manage.py check` passes.

RAG

- [ ] Artifacts tarball produced and checksum file produced.
- [ ] Pinecone inventory file produced and includes per-namespace counts.
- [ ] Optional dry-run ingest succeeds in a scratch environment.
- [ ] No `.env`, raw API keys, or controlled-access participant-level data inside the tarball.

Both

- [ ] Files uploaded to `03_Data_Snapshots_Restricted/{Postgres,RAG}` with the exact names above.
- [ ] Drive access for `03_Data_Snapshots_Restricted` restricted to approved stakeholders.
- [ ] Source credentials rotated if they touched a shared machine.
- [ ] Slack post 3 updated with the snapshot date.

Only after every box is checked, link the Drive folder from Slack post 3.
