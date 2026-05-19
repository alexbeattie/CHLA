# Autism Research RAG

API-first retrieval-augmented generation over public autism research sources.

## What it does

Ingests public autism research data (literature metadata, clinical trials, NIH
grants, curated gene evidence, and permitted web pages), embeds it with
Cohere `embed-v4.0`, stores it in Pinecone with per-source namespaces, and
exposes a small RAG API that answers research questions with citations.

Controlled-access datasets (NDA, SFARI Base, SPARK, dbGaP, MSSNG) are tracked
as metadata only until proper data-use approvals are in place. The pipeline
exposes a separate ingestion path for approved exports so they never mix with
public material.

## Layout

```
autism_rag/
  config/            # Environment + source registry config
  sources/           # Source registry + per-source ingestion adapters
  ingestion/         # Normalization, chunking, embedding, upsert pipeline
  rag/               # Embeddings, vector store, retrieval, generation
  evaluation/        # Eval sets and retrieval/generation evaluation
  api/               # FastAPI service exposing /ask
  scripts/           # CLI entry points (ingest, query, eval)
  tests/             # Unit + integration tests
  data/
    raw/             # Raw API/HTTP responses (not committed)
    processed/       # Normalized documents (not committed)
```

## Setup

```bash
cd autism_rag
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
# Fill in COHERE_API_KEY, PINECONE_API_KEY, FIRECRAWL_API_KEY, NCBI_API_KEY,
# OPENAI_API_KEY (optional, for answer generation), etc.
cd ..
```

## Quick start

```bash
# Run these from the parent repository root after activating autism_rag/.venv.
source autism_rag/.venv/bin/activate

# Inspect what is in the source registry
python3 -m autism_rag.scripts.list_sources

# Dry-run an ingestion (no API keys required, no Pinecone writes)
python3 -m autism_rag.scripts.ingest --source pubmed --query "autism" --limit 5 --dry-run

# Full ingestion of one source into Pinecone
python3 -m autism_rag.scripts.ingest --source pubmed --query "autism spectrum disorder" --limit 25

# Broad ingestion across public sources
python3 -m autism_rag.scripts.ingest_all --limit 50

# Separate rare disease / rare disorder NDD corpus
python3 -m autism_rag.scripts.ingest_rare_ndd --limit 50

# Query the RAG
python3 -m autism_rag.scripts.query "What does recent literature say about early autism screening?"

# Query only the separated rare NDD corpus
python3 -m autism_rag.scripts.query --corpus rare_ndd \
  "What rare diseases or rare disorders are associated with NDD?"

# Evaluate retrieval, citation, and safety guardrails
python3 -m autism_rag.scripts.evaluate

# Run the API
uvicorn autism_rag.api.server:app --reload
```

## Firecrawl

For permitted web pages only. Install the CLI and authenticate once:

```bash
npm install -g firecrawl-cli
firecrawl login --browser   # or set FIRECRAWL_API_KEY in .env
firecrawl --status
```

Then ingest explicit URLs (do not bulk-crawl):

```bash
python3 -m autism_rag.scripts.ingest --source firecrawl_web \
  --urls https://www.cdc.gov/autism/data-research/index.html \
         https://www.nimh.nih.gov/health/topics/autism-spectrum-disorders-asd

# Add permitted rare disease / NDD pages to the separated rare NDD corpus
python3 -m autism_rag.scripts.ingest_rare_ndd \
  --skip-apis \
  --web-url https://example.org/permitted-rare-ndd-page
```

## Source policy

This system only ingests material we are allowed to use:

- Public APIs with documented programmatic access (PubMed, PMC OA, ClinicalTrials.gov, NIH RePORTER, OpenAlex, Semantic Scholar).
- Curated open data downloads (SFARI Gene) under their published terms.
- Permitted web pages via Firecrawl, respecting robots/terms.
- Controlled datasets only after credentials, DUAs, and IRB approvals are in place.

Every chunk in the vector store carries `access_class`, `license`,
`source_url`, and citation identifiers so retrieval and answers can enforce
and disclose provenance.
