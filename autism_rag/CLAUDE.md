# autism_rag

API-first RAG service over public autism research sources (PubMed, ClinicalTrials.gov, NIH RePORTER, SFARI Gene, permitted web pages via Firecrawl).

## Stack

- Python, FastAPI + Uvicorn (API), Cohere `embed-v4.0` embeddings, Pinecone vector store
- Anthropic (Bedrock) / OpenAI for answer generation, pytest + pytest-asyncio for tests
- Config via `pydantic-settings` / `.env` (see `.env.example`)

## Commands

| Task | Command |
| --- | --- |
| Setup | `cd autism_rag && python3 -m venv .venv && source .venv/bin/activate && pip install -r requirements.txt` |
| Run API | `uvicorn autism_rag.api.server:app --reload` (from repo root, venv active) |
| Query | `python3 -m autism_rag.scripts.query "<question>"` (from repo root, venv active) |
| Test | `pytest autism_rag/tests/` (from repo root, venv active) |
| Evaluate | `python3 -m autism_rag.scripts.evaluate` (from repo root, venv active) |

Defer to the repo root `AGENTS.md` for working agreement; log changes in root `FIXES.md`.
