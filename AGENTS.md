# KiNDD - NDD Resource Navigator Agent Workflow

This file is the shared working agreement for AI coding agents in this repository.

## Project Snapshot

- `map-frontend/`: Vue 3 + Vite + TypeScript + Pinia
- `maplocation/`: Django + Django REST Framework + PostgreSQL/PostGIS
- Deployment: GitHub Actions, Elastic Beanstalk, S3, CloudFront

## Priorities

1. Keep changes scoped and reversible.
2. Preserve the ongoing refactor by extracting logic out of large files instead of growing them again.
3. Never introduce or copy secrets, credentials, or production-only values into code, docs, or commits.
4. Prefer source-of-truth docs over ad hoc instructions.
5. Keep handoffs brief and useful, then keep moving.

## Non-Negotiables

### Naming & Style

- Product name is `KiNDD - NDD Resource Navigator`; short form is `KiNDD`.
- Do not introduce `CHLA` or `Children's Hospital Los Angeles` into new user-facing copy, docs, app strings, or marketing examples. Existing infrastructure identifiers such as DB users, EB environment names, S3 buckets, package names, and folder paths keep their current names until a dedicated rename.
- Do not use emojis in code, docs, app strings, shell scripts, or commit messages.

### Frontend

- Use Vue Composition API for new work.
- Prefer `<script setup>` in new components.
- Put reusable logic in composables, shared state in Pinia stores, and map UI pieces in focused components.
- Do not add major new logic directly into `map-frontend/src/views/MapView.vue` if it can be extracted.
- Keep API access driven by `VITE_API_BASE_URL`, not hardcoded URLs.

### Backend

- Use `python3` for Django commands.
- Keep database configuration environment-driven.
- Use PostGIS-aware patterns for spatial logic.
- Create migrations for schema changes and keep them intentional.

### Safety

- Do not overwrite unrelated user changes.
- Do not run destructive git commands unless explicitly asked.
- Treat deployment scripts and data-sync scripts as higher risk than app code changes.

## Default Workflow

1. Read the relevant code and the closest source-of-truth docs before editing.
2. Check the local diff so you understand in-flight user changes.
3. Make the smallest complete change that solves the problem cleanly.
4. Run the smallest useful verification for the area you touched.
5. Update docs only when behavior, workflow, or setup materially changed.

## Verification Ladder

Use the lightest step that still proves the change:

- Quick smoke: `./scripts/quick-test.sh`
- Backend tests: `cd maplocation && python3 -m pytest`
- Frontend tests: `cd map-frontend && npm test`
- Frontend build: `cd map-frontend && npm run build`
- Pre-deploy pass: `./scripts/test-deployment-locally.sh`

If you cannot run a relevant check, say so clearly in the handoff.

## Source-of-Truth Docs

Start here instead of inventing commands or setup details:

- `README.md`: project overview and architecture
- `QUICK_START.md`: local setup and daily commands
- `.github/copilot-instructions.md`: repo structure, conventions, and test expectations
- `maplocation/README.md`: backend details
- `map-frontend/ENVIRONMENT_SWITCHING.md`: frontend environment switching
- `docs/`: deeper implementation and deployment notes

## iOS NDD Agent Response Contracts

For the iOS Neurodegenerative Disorders agent, do not assume the work is related to women's health, menopause, or Stella unless explicitly stated.

Before changing response behavior, prompts, backend formatting, or the iOS Markdown renderer, read:

- `docs/agent/NDD_RESPONSE_FORMAT_CONTRACT.md`
- `docs/agent/IOS_MARKDOWN_RENDERING_CONTRACT.md`
- `docs/agent/NEURO_SAFETY_AND_SCOPE.md`
- `docs/agent/NDD_RESPONSE_EXAMPLES.md`
- `docs/agent/NDD_RESPONSE_ANTI_PATTERNS.md`
- `docs/agent/STREAMING_MARKDOWN_RULES.md`

The agent should produce clean, iOS-safe Markdown. Prefer short paragraphs, bold labels, simple bullets, cautious medical wording, and clear next steps. Avoid raw HTML, complex tables, diagnosis, treatment instructions, unsupported Markdown, and long dense clinical explanations.

When response formatting changes, test narrow iPhone layout, streaming Markdown, bold labels, bullet rendering, links, urgent symptom responses, and caregiver-oriented responses.

## Refactor Guidance

There is an ongoing effort to keep the frontend maintainable, especially around the map experience.

- If a change touches large view logic, look first for extraction into:
  - `map-frontend/src/composables/`
  - `map-frontend/src/stores/`
  - `map-frontend/src/components/`
- Prefer small cleanup steps that leave the app working after each change.
- Avoid mixing broad refactors with unrelated behavior changes.

## Useful Commands

```bash
# Local database
docker-compose -f docker-compose.local.yml up -d

# Backend
cd maplocation
source ../venv/bin/activate
python3 manage.py runserver

# Frontend
cd map-frontend
./switch-env.sh dev
npm run dev

# Fast checks
./scripts/quick-test.sh
./scripts/test-deployment-locally.sh
```

## Handoff Expectations

When finishing a task, report:

- what changed
- what you verified
- any remaining risk, follow-up, or blocked checks
## Pinecone (Vector Database)

**MANDATORY: If you need to help with Pinecone, vector databases, embeddings, semantic search, RAG, or recommendations, you MUST read `.agents/PINECONE.md` first** to determine:

- What questions to ask the user
- How to select the programming language
- Which guide files to consult for implementation details

Do not proceed with Pinecone-related tasks without consulting the appropriate guide files. Use `.agents/PINECONE.md` as your entry point and follow its "Choosing the Right Guide" section to navigate to the appropriate resources.

### Pinecone Instructions Catalog

@./.agents/PINECONE.md
@./.agents/PINECONE-cli.md
@./.agents/PINECONE-python.md
@./.agents/PINECONE-typescript.md
@./.agents/PINECONE-go.md
@./.agents/PINECONE-java.md
@./.agents/PINECONE-quickstart.md
@./.agents/PINECONE-troubleshooting.md
