# Classic Chat Evaluation Design

## Goal

Create a trustworthy first evaluation gate for KiNDD's production `classic`
chat runtime. The first slice must catch safety, grounding, language, and
iOS-format regressions without requiring LangSmith or AWS for its offline test
suite.

## Scope

This design covers:

- the `classic` runtime behind `/api/llm/ask/`;
- the existing regression cases in `maplocation/llm/langsmith_evals.py`;
- deterministic evaluators in `maplocation/llm/evaluators.py`;
- regression aggregation and command-line pass/fail behavior;
- focused offline tests; and
- the LangSmith guide for the resulting workflow.

This slice does not add new chat behavior, change prompts, compare Strands with
LangGraph, measure retrieval ranking, configure CI, or run a production
deployment.

## Approach

Harden the existing in-flight evaluation harness rather than replacing it.
LangSmith remains the experiment runner and result viewer, while product
contracts are enforced by deterministic Python evaluators.

The workflow has two layers:

1. Offline tests prove evaluator logic, dataset coverage, and regression-gate
   behavior without network calls.
2. A live LangSmith experiment invokes the production `classic` runtime and
   optionally a Bedrock rubric judge.

The deterministic layer is authoritative for hard product contracts. The LLM
judge provides a quality signal but cannot override a deterministic safety
failure.

## Dataset Contract

Each case has:

- a stable case ID;
- query, locale, optional user context, and optional conversation history;
- a human-readable rubric for the LLM judge; and
- explicit machine-checkable expectations where a hard requirement exists.

Research and current-fact cases that require citations must set
`requires_sources: true`. Safety cases must identify their required or
forbidden language explicitly.

The first dataset continues to cover:

- urgent symptom escalation in English and Spanish;
- medication-dosage refusal;
- provider grounding;
- research and changing-fact citations;
- Spanish output;
- multi-turn context;
- Early Start eligibility;
- out-of-scope redirection;
- prompt-injection resistance; and
- iOS-safe Markdown.

## Evaluator Contract

Deterministic evaluators remain small and independently testable:

- `ios_format_contract`
- `must_mention`
- `must_not_mention`
- `sources_cited`
- `language_match`
- `expected_tools_used`

An evaluator returns a numeric score when it applies and `score=None` only when
the case legitimately does not require that check.

Hard requirements are evaluated per case. A single applicable failure in
format, required safety language, forbidden content, or required sources fails
the experiment. Passing cases cannot average away that failure.

The regression gate also verifies coverage. If an expected hard evaluator
produces no scored results, the run fails instead of silently passing.
Tool-use checks remain informational for the `classic` runtime because that
pipeline retrieves context before the model call rather than exposing callable
tools.

## LLM Judge

The Bedrock rubric judge remains optional. It runs at temperature zero and
returns a bounded score with a short reason.

Judge failures or unparseable judge output are reported as skipped rather than
misrepresented as answer failures. However, a live run configured to require
the judge must fail coverage validation if the judge produces no scores.

The initial quality threshold remains configurable. It is a regression signal,
not a claim of medical or clinical validation.

## Command Behavior

The management command continues to support:

```bash
python3 manage.py run_llm_evals --runtime classic
python3 manage.py run_llm_evals --runtime classic --skip-judge --json
```

The command must:

- fail fast when required external services are unavailable;
- print case-specific failures;
- return non-zero for any hard-contract failure;
- return non-zero when required evaluator coverage is missing; and
- keep JSON output suitable for later CI integration.

Offline unit tests do not require LangSmith, Bedrock, a database, or the Autism
RAG service.

## Verification

Focused verification will cover:

1. citation-required cases actually exercise `sources_cited`;
2. one hard failure causes the overall gate to fail even when other cases pass;
3. missing hard-evaluator coverage fails the gate;
4. judge coverage is required only when the judge is enabled;
5. classic-runtime tool checks remain skipped;
6. dataset safety, Spanish, multi-turn, and citation coverage remains present;
7. the focused evaluator and formatting tests pass in the repository's Python
   environment.

A live LangSmith run is a separate verification gate because it requires
credentials and external services. If it cannot be run, the handoff must say
so explicitly.

## Risks and Follow-up

The starter dataset is a regression suite, not a held-out clinical benchmark.
Its cases should later be expanded from sanitized, reviewed failure examples
and clinician or domain-expert feedback.

Retrieval quality needs a separate phase with labeled expected documents or
provider records and ranking metrics such as recall at k and reciprocal rank.
Strands and LangGraph comparison should begin only after the production
baseline is stable.
