# KiNDD Project Onboarding Packet

Prepared for nonprofit, clinical, program, operations, and technical stakeholders.

**Current as of:** 2026-05-17
**Project:** KiNDD - Neurodevelopmental Disorder Resource Navigator
**Primary audience:** Professionals onboarding into the project, including non-technical partners
**Use case:** Presentation, stakeholder orientation, discovery review, and development handoff

## 1. Executive Summary

KiNDD is a care-navigation platform for families seeking developmental disability services in Los Angeles County. The project combines a searchable provider directory, Regional Center service-area logic, a web map, an iOS application, and AI-assisted guidance.

The central idea is simple: a family's location determines their Regional Center. The location also can help provide guidance around other relevant providers and next steps. The technology exists to support that service-navigation model, not to replace professional judgment or public-service workflows.

The project is especially relevant to nonprofit and institutional partners because it addresses:

- Access to developmental disability services.
- Navigation across a fragmented public-service ecosystem.
- Bilingual family support.
- Place-based service assignment through Regional Centers.
- Responsible use of AI over trusted provider and eligibility context.
- Data quality, care access, and service-gap discovery.

## 2. Recommended Presentation Flow

Use this order for a professional onboarding presentation.

| Order | Topic | Purpose | Primary document |
| ----- | ------------------------------- | ------------------------------------------------------------------------------ | --------------------------------------------------------------- |
| 1 | Project overview | Explain what KiNDD is and who it serves | `README.md` |
| 2 | Mission and platform strategy | Explain why the project matters to families and institutions | `docs/PLATFORM_STRATEGY_BRIEF.md` |
| 3 | Regional Center model | Explain the core service-navigation concept | `docs/REGIONAL_CENTERS_CONCEPT.md` |
| 4 | Family-facing product | Show how the experience appears to families | `chla-ios/README.md` |
| 5 | Current mobile direction | Explain the home screen, Ask KiNDD, and Regional Center UX | `chla-ios/HOME_PAGE_REDESIGN_SPEC.md` |
| 6 | Public-facing language | Review how the app is described to families | `chla-ios/APP_STORE_SUBMISSION.md` |
| 7 | Data and clinical handoff | Explain what clinical stakeholders can review and how access should be handled | `docs/CLINICAL_TEAM_HANDOFF.md` |
| 8 | Research assistant | Explain public-source RAG and citation-based research answers | `autism_rag/README.md` |
| 9 | Data quality and discovery gaps | Align stakeholders on known gaps and validation needs | `docs/API_AND_DATA_ISSUES.md`, `ADMIN_DATA_ENTRY_GUIDE.md` |
| 10 | Development and operations | Give technical contributors the setup, deployment, and architecture path | `QUICK_START.md`, `docs/DEPLOYMENT.md`, `maplocation/README.md` |

## 3. Stakeholder Reading Paths

### Nonprofit Leaders and Program Directors

Start with these documents:

1. `README.md`
2. `docs/PLATFORM_STRATEGY_BRIEF.md`
3. `docs/REGIONAL_CENTERS_CONCEPT.md`
4. `chla-ios/APP_STORE_SUBMISSION.md`
5. `docs/CLINICAL_TEAM_HANDOFF.md`

What they should understand:

- KiNDD helps families locate developmental disability services in LA County.
- Regional Center assignment is geographic and based on ZIP code.
- The project has both family-facing surfaces and infrastructure value.
- The platform can support health equity, bilingual access, care navigation, and service-gap discovery.
- Data quality and governance are central to trust.

### Clinical, Research, and Care-Navigation Professionals

Start with these documents:

1. `docs/CLINICAL_TEAM_HANDOFF.md`
2. `docs/REGIONAL_CENTERS_CONCEPT.md`
3. `docs/PLATFORM_STRATEGY_BRIEF.md`
4. `chla-ios/HOME_PAGE_REDESIGN_SPEC.md`
5. `autism_rag/README.md`
6. `ADMIN_DATA_ENTRY_GUIDE.md`

What they should understand:

- The clinical review role is to validate service logic, provider data, language, and safety boundaries.
- The RAG system is limited to public and permitted sources unless controlled-access approvals are in place.
- Clinical review should avoid patient-specific information in shared channels.
- Provider data needs structured validation for diagnoses, age groups, service types, and coverage.

### Product, Design, and Communications Contributors

Start with these documents:

1. `docs/PLATFORM_STRATEGY_BRIEF.md`
2. `chla-ios/HOME_PAGE_REDESIGN_SPEC.md`
3. `chla-ios/APP_STORE_SUBMISSION.md`
4. `docs/SEO_STRATEGY.md`
5. `docs/SOCIAL_IMAGES_GUIDE.md`
6. `docs/SEO_IMPLEMENTATION_CHECKLIST.md`

What they should understand:

- The product should communicate clearly to families, not just technical users.
- Ask KiNDD is the primary guided-help action in the iOS direction.
- Regional Centers should be framed as geography and service access, not insurance.
- Public-facing copy should remain plain, bilingual, and careful about clinical claims.

### Engineers and Technical Contributors

Start with these documents:

1. `QUICK_START.md`
2. `maplocation/README.md`
3. `map-frontend/ENVIRONMENT_SWITCHING.md`
4. `docs/DEPLOYMENT.md`
5. `docs/GITHUB_ACTIONS.md`
6. `docs/DATABASE_SYNC.md`
7. `maplocation/BEDROCK_SETUP.md`
8. `STACK_DOCUMENTATION.md`

What they should understand:

- The system includes a Django backend, Vue web frontend, SwiftUI iOS app, PostgreSQL/PostGIS, and AI services.
- Environment variables and credentials must remain outside source control and presentation materials.
- Deployment is primarily through GitHub Actions, with manual AWS paths documented for fallback.
- The architecture docs may include operational identifiers and should be treated as engineering-only.

## 4. Current Project Snapshot

| Area | Current state |
| --------------------- | -------------------------------------------------------------------------------------------- |
| Family-facing web app | Vue 3 and Vite web map at `kinddhelp.com` |
| Backend API | Django 5.2 and Django REST Framework in `maplocation` |
| Mobile app | SwiftUI iOS app in `chla-ios` |
| Database | PostgreSQL with PostGIS for provider and Regional Center geography |
| AI layer | AWS Bedrock, embeddings, streaming chat, image/document analysis, and tool-based agent paths |
| Research RAG | Separate Pinecone/Cohere/OpenAI-based RAG under `autism_rag` for public research sources |
| Geography focus | Los Angeles County, with all 7 LA County Regional Centers named in public materials |
| Public positioning | Free developmental services navigator with bilingual support and no required registration |

## 5. Core Concepts for Non-Technical Stakeholders

### KiNDD

KiNDD is a developmental services navigator. It helps families understand what resources may be relevant to them based on location, service needs, and available provider information.

### Regional Centers

Regional Centers are geographic service areas in California. A family does not choose a Regional Center as an insurance type. A family's ZIP code determines which Regional Center serves them.

The core flow is:

```text
User location -> ZIP code -> Regional Center assignment -> relevant providers and guidance
```

### Provider Data

Provider records include location, contact information, service types, insurance or funding information, Regional Center relationships, and other attributes used for search and filtering. This data needs ongoing validation.

### AI Assistance

The AI layer is intended to help families understand next steps and interpret available information. It should be presented as guided support over trusted data, not as medical diagnosis, legal advice, or a substitute for professional care coordination.

### Research RAG

The research assistant retrieves information from public or permitted sources and provides citation-based answers. Controlled-access research datasets are metadata-only unless formal approvals are completed.

## 6. Documents to Share in the Onboarding Packet

These documents are appropriate to share with a broader professional nonprofit group.

| Document | Audience | Why it matters |
| ------------------------------------- | ---------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| `README.md` | Everyone | Main project overview, live URLs, core features, and architecture summary |
| `docs/PLATFORM_STRATEGY_BRIEF.md` | Leadership, program, technical, funder-facing stakeholders | Most complete strategic explanation of the platform, GIS layer, AI layer, growth directions, and collaboration opportunities |
| `docs/REGIONAL_CENTERS_CONCEPT.md` | Everyone | Explains the central ZIP-to-Regional-Center-to-provider logic |
| `chla-ios/README.md` | Product, clinical, technical | Explains the iOS app surface, features, and API integration |
| `chla-ios/HOME_PAGE_REDESIGN_SPEC.md` | Product, design, clinical | Explains the current family-facing UX direction and Ask KiNDD emphasis |
| `chla-ios/APP_STORE_SUBMISSION.md` | Communications, product, clinical | Shows public-facing English and Spanish positioning |
| `docs/CLINICAL_TEAM_HANDOFF.md` | Clinical, research, operations, engineering leads | Provides clinical onboarding, review workflow, and data snapshot handling guidance |
| `autism_rag/README.md` | Clinical, research, AI reviewers | Explains the public-source research RAG and source policy |
| `ADMIN_DATA_ENTRY_GUIDE.md` | Data validation team, clinical reviewers, operations | Explains provider data fields that need review and how to structure updates |
| `docs/API_AND_DATA_ISSUES.md` | Data, clinical, product, engineering | Documents known data quality issues and action items |
| `docs/SEO_STRATEGY.md` | Communications, outreach, growth | Useful for nonprofit discovery, family reach, and search visibility planning |

## 7. Engineering-Only or Restricted Documents

These documents are useful, but should not be included in a broad nonprofit onboarding packet without review or redaction.

| Document | Why restricted |
| ----------------------------------------------------------- | ------------------------------------------------------------------- |
| `STACK_DOCUMENTATION.md` | Contains infrastructure identifiers and operational details |
| `docs/DEPLOYMENT.md` | Contains deployment procedures and infrastructure details |
| `docs/GITHUB_ACTIONS.md` | Contains CI/CD and secrets setup context |
| `.github/SECRETS.md` and `.github/GITHUB_SECRETS_SETUP.md` | Secret-management documentation |
| `ENV_SETTINGS.md` | Environment configuration; may contain sensitive or outdated values |
| `maplocation/BEDROCK_SETUP.md` | AI infrastructure, IAM, model access, and production setup details |
| `maplocation/DATABASE_SYNC_GUIDE.md` | Database sync details; may include operational values |
| `map-frontend/ENVIRONMENT_SWITCHING.md` | Developer environment operations |
| Refactor session notes under `docs/` | Useful historical context, but too detailed for general onboarding |
| Archived docs under `docs/archive/` and `archive/old-docs/` | Historical only; use only when tracing decisions |

## 8. Discovery and Development Documentation Map

### Mission, Strategy, and Discovery

Use these to explain the nonprofit relevance of the project:

- `docs/PLATFORM_STRATEGY_BRIEF.md`
- `docs/REGIONAL_CENTERS_CONCEPT.md`
- `docs/CLINICAL_TEAM_HANDOFF.md`
- `docs/SEO_STRATEGY.md`
- `docs/SEARCH_CONSOLE_SETUP.md`
- `docs/SEO_QUICK_ACTIONS.md`
- `docs/SEO_COMPLETION_SUMMARY.md`

### Product and User Experience

Use these to explain the family-facing surfaces:

- `chla-ios/README.md`
- `chla-ios/HOME_PAGE_REDESIGN_SPEC.md`
- `chla-ios/APP_STORE_SUBMISSION.md`
- `chla-ios/AppStoreMetadata.md`
- `chla-ios/CHANGELOG.md`
- `map-frontend/DRIVING_DIRECTIONS_INTEGRATION.md`

### Clinical, Data, and Content Review

Use these to support validation work:

- `docs/CLINICAL_TEAM_HANDOFF.md`
- `ADMIN_DATA_ENTRY_GUIDE.md`
- `docs/API_AND_DATA_ISSUES.md`
- `docs/CSV_IMPORT_GEOCODING_REPORT.md`
- `docs/PROVIDER_COORDINATES_VALIDATION_REPORT.md`
- `docs/IMPORT_VIA_ADMIN.md`
- `docs/PRODUCTION_DATA_IMPORT.md`

### AI and Research

Use these for AI and research onboarding:

- `docs/PLATFORM_STRATEGY_BRIEF.md`
- `autism_rag/README.md`
- `maplocation/BEDROCK_SETUP.md`
- `LLM_INTEGRATION_GAMEPLAN.md`
- `.agents/PINECONE.md` and related Pinecone guides, for agent implementation only

### Engineering and Operations

Use these for developer onboarding:

- `QUICK_START.md`
- `maplocation/README.md`
- `docs/DEPLOYMENT.md`
- `docs/GITHUB_ACTIONS.md`
- `docs/DATABASE_SYNC.md`
- `.github/CICD_GUIDE.md`
- `.github/copilot-instructions.md`
- `AGENTS.md`
- `CURSOR.md`
- `CLAUDE.md`

## 9. Important Caveats and Current Gaps

These points should be disclosed clearly during professional onboarding.

| Area | Current caveat | Why it matters |
| ----------------------- | ------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------- |
| Data quality | Some docs identify Regional Center duplicates, missing ZIP coverage, and incomplete provider metadata | The platform's trust depends on validated geography and provider data |
| Regional Center logic | Backend and iOS may have overlapping Regional Center matching logic | A single authoritative assignment path is recommended |
| AI safety | The platform has data-grounded AI and throttling, but deterministic guardrails remain a recommended next investment | Healthcare-adjacent AI needs explicit boundaries |
| Authentication | Some LLM endpoints are documented as publicly accessible with throttling | Stronger auth is recommended before broader sensitive use |
| Documentation freshness | Some docs are current as of 2026, while others are 2024 or 2025 historical notes | Onboarding should rely on the curated order in this packet |
| Public claims | Avoid implying diagnosis, guaranteed eligibility, or provider availability | Communications should remain service-navigation focused |
| Source access | Controlled research datasets require formal approvals before use | Do not mix controlled-access data with public RAG material |

## 10. Suggested 60-Minute Onboarding Agenda

| Time | Topic | Lead |
| ------------- | ------------------------------------------------------------- | ------------------------- |
| 0-5 minutes | Welcome, purpose, and stakeholder roles | Project lead |
| 5-15 minutes | What KiNDD is and who it serves | Product or nonprofit lead |
| 15-25 minutes | Regional Center concept and why geography matters | Clinical or program lead |
| 25-35 minutes | Product walkthrough: web, iOS, Ask KiNDD, Regional Centers | Product/design lead |
| 35-45 minutes | Data, AI, RAG, and safety boundaries | Technical lead |
| 45-55 minutes | Known gaps, validation needs, and collaboration opportunities | Project lead |
| 55-60 minutes | Questions, next steps, and document access | Project lead |

## 11. Recommended Follow-Up Workstreams

| Workstream | Owner type | First documents |
| ------------------------------ | ------------------------------------------------ | ------------------------------------------------------------------------- |
| Provider data validation | Clinical operations, data stewards | `ADMIN_DATA_ENTRY_GUIDE.md`, `docs/API_AND_DATA_ISSUES.md` |
| Regional Center coverage audit | Clinical operations, GIS/data contributors | `docs/REGIONAL_CENTERS_CONCEPT.md`, `docs/API_AND_DATA_ISSUES.md` |
| Family-facing copy review | Clinical, communications, bilingual reviewers | `chla-ios/APP_STORE_SUBMISSION.md`, `chla-ios/HOME_PAGE_REDESIGN_SPEC.md` |
| AI safety and governance | Responsible AI, clinical, legal/privacy advisors | `docs/PLATFORM_STRATEGY_BRIEF.md`, `maplocation/BEDROCK_SETUP.md` |
| Research RAG review | Clinical research, data governance | `autism_rag/README.md`, `docs/CLINICAL_TEAM_HANDOFF.md` |
| Technical onboarding | Engineering | `QUICK_START.md`, `maplocation/README.md`, `docs/DEPLOYMENT.md` |

## 12. Presentation Language Guide

Use this framing:

- "KiNDD is a developmental services navigation platform."
- "Regional Center assignment is based on geography and ZIP code."
- "The AI layer helps translate trusted data into clearer next steps."
- "The platform supports families, clinicians, nonprofit navigators, and institutional partners."
- "The work ahead is data validation, safety hardening, partner workflow design, and expansion."

Avoid this framing:

- "KiNDD diagnoses families or children."
- "The AI gives medical advice."
- "Regional Center is an insurance option."
- "The provider list is complete or guaranteed current."
- "Controlled-access research data is available without approval."
- "Any public launch claim that has not been validated by the clinical and data teams."

## 13. Final Onboarding Checklist

Before presenting to a professional group:

- [ ] Use this packet as the document index and reading order.
- [ ] Share only the broad stakeholder documents from section 6.
- [ ] Keep engineering-only and restricted documents out of general circulation unless redacted.
- [ ] Confirm whether current provider counts, app version, and AI capabilities match the latest deployed state.
- [ ] Confirm who owns clinical review, data validation, technical questions, and access requests.
- [ ] Avoid sharing raw database dumps, environment files, API keys, hostnames, passwords, or uncontrolled archives.
- [ ] Identify the next meeting outcome: review feedback, data validation plan, pilot planning, funding discussion, or technical handoff.

## 14. One-Page Summary for the Group

KiNDD helps families navigate developmental disability services in Los Angeles County by connecting location, Regional Center service areas, provider data, and guided help. The platform includes a web map, iOS app, backend API, geospatial database, and AI-assisted support.

For nonprofit and institutional partners, the project should be understood as public-interest service-access infrastructure. Its value is not only a directory of providers, but a structured way to help families understand where to go, what questions to ask, and which services may be relevant based on geography and need.

The most important onboarding documents are:

1. `README.md`
2. `docs/PLATFORM_STRATEGY_BRIEF.md`
3. `docs/REGIONAL_CENTERS_CONCEPT.md`
4. `chla-ios/README.md`
5. `chla-ios/HOME_PAGE_REDESIGN_SPEC.md`
6. `chla-ios/APP_STORE_SUBMISSION.md`
7. `docs/CLINICAL_TEAM_HANDOFF.md`
8. `autism_rag/README.md`
9. `ADMIN_DATA_ENTRY_GUIDE.md`
10. `docs/API_AND_DATA_ISSUES.md`

The next professional discussion should focus on validation, governance, and collaboration: which data needs review, what claims are safe to make publicly, how clinical stakeholders should provide feedback, and what partner workflows the platform should support next.
