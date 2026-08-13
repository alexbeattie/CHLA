# Classic Chat Evaluation Gate Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Harden KiNDD's existing `classic` chat evaluation harness so citation requirements, hard-contract failures, and evaluator coverage reliably fail regressions.

**Architecture:** Keep LangSmith as the live experiment runner while making deterministic Python evaluators authoritative for safety, citations, and iOS formatting. Add explicit required-evaluator coverage to the shared regression gate, and let the management command require the rubric judge only when the judge is enabled.

**Tech Stack:** Python 3.12, Django management commands, LangSmith `evaluate`, pytest, AWS Bedrock for optional live judging.

## Global Constraints

- Target the production `classic` runtime behind `/api/llm/ask/` first.
- Do not change chat prompts, response behavior, retrieval logic, or deployment configuration.
- Hard deterministic failures cannot be averaged away by passing cases.
- Offline tests must not require LangSmith, Bedrock, a database, or the Autism RAG service.
- Treat the suite as regression evidence, not medical or clinical validation.
- Preserve all pre-existing dirty changes in `/Users/alexbeattie/Developer/CHLA`.
- `maplocation/llm/langsmith_evals.py`, `docs/LANGGRAPH_LANGSMITH_CHAT_GUIDE.md`, and the evaluator test files already contain uncommitted work. Do not stage or commit those files without first separating the pre-existing changes from this plan's changes.
- Use `cd maplocation && ../venv/bin/python -m pytest ...` so verification runs with the repository's Python 3.12 environment rather than the system Python 3.14 installation.

---

## File Structure

- `maplocation/llm/langsmith_evals.py`: owns dataset cases, required evaluator keys, experiment summaries, and regression gating.
- `maplocation/llm/management/commands/run_llm_evals.py`: translates `--skip-judge` into the required coverage contract and exits non-zero on failure.
- `maplocation/locations/tests/test_llm_evaluators.py`: provides offline contract tests for dataset expectations, scoring, coverage, and judge selection.
- `docs/LANGGRAPH_LANGSMITH_CHAT_GUIDE.md`: documents the exact hard-gate and coverage behavior.

No new runtime module or dependency is required.

### Task 1: Make Citation Requirements Machine-Checkable

**Files:**
- Modify: `maplocation/llm/langsmith_evals.py:55-79`
- Test: `maplocation/locations/tests/test_llm_evaluators.py:262-273`

**Interfaces:**
- Consumes: each case's existing `expectations: dict[str, Any]`.
- Produces: `requires_sources: bool` for cases scored by `llm.evaluators.sources_cited`.

- [ ] **Step 1: Extend the dataset coverage test**

Add these assertions to `test_eval_dataset_covers_spanish_multiturn_and_safety`:

```python
    citation_case_ids = {
        case["id"]
        for case in EVAL_CASES
        if case.get("expectations", {}).get("requires_sources")
    }
    assert citation_case_ids == {
        "autism-research-citations",
        "current-facts-web-search",
    }
```

- [ ] **Step 2: Run the focused test and confirm the missing contract**

Run:

```bash
cd maplocation
../venv/bin/python -m pytest \
  locations/tests/test_llm_evaluators.py::test_eval_dataset_covers_spanish_multiturn_and_safety \
  -q
```

Expected: FAIL because `citation_case_ids` is empty.

- [ ] **Step 3: Add citation expectations to both cases**

Change the two case expectations in `maplocation/llm/langsmith_evals.py` to:

```python
        "expectations": {
            "expected_tools_any": ["autism_research", "clinical_search"],
            "requires_sources": True,
        },
```

and:

```python
        "expectations": {
            "expected_tools_any": ["web_search"],
            "requires_sources": True,
        },
```

- [ ] **Step 4: Run the dataset and source-evaluator tests**

Run:

```bash
cd maplocation
../venv/bin/python -m pytest \
  locations/tests/test_llm_evaluators.py::test_eval_dataset_covers_spanish_multiturn_and_safety \
  locations/tests/test_llm_evaluators.py::test_sources_cited_requires_section_and_url \
  -q
```

Expected: `2 passed`.

- [ ] **Step 5: Record the Task 1 checkpoint without staging dirty files**

Run:

```bash
git diff --check -- \
  maplocation/llm/langsmith_evals.py \
  maplocation/locations/tests/test_llm_evaluators.py
git diff -- \
  maplocation/llm/langsmith_evals.py \
  maplocation/locations/tests/test_llm_evaluators.py
```

Expected: no whitespace errors; the displayed diff includes the intended citation expectations and test assertions. Do not run `git add` because both files contain pre-existing uncommitted work.

### Task 2: Enforce Per-Case Hard Failures and Evaluator Coverage

**Files:**
- Modify: `maplocation/llm/langsmith_evals.py:431-456`
- Test: `maplocation/locations/tests/test_llm_evaluators.py:239-259`

**Interfaces:**
- Consumes: `summary["scores"][key]` dictionaries containing `mean`, `count`, and `failures`.
- Produces: `HARD_CONTRACT_EVALUATORS: tuple[str, ...]`.
- Produces: `required_evaluator_keys(*, judge: bool) -> tuple[str, ...]`.
- Produces: `check_regression(summary, *, min_score=0.7, required_keys=HARD_CONTRACT_EVALUATORS) -> tuple[bool, list[str]]`.

- [ ] **Step 1: Replace the aggregate-only gate test with explicit contract tests**

Replace `test_check_regression_enforces_critical_and_soft_thresholds` with:

```python
def _score(mean, count=1, failures=None):
    return {
        "mean": mean,
        "count": count,
        "failures": failures or [],
    }


def test_check_regression_rejects_any_hard_contract_failure():
    from llm.langsmith_evals import check_regression

    summary = {
        "scores": {
            "ios_format_contract": _score(
                1.0,
                count=1000,
                failures=[{"case": "one-bad-case", "score": 0.0}],
            ),
            "must_mention": _score(1.0),
            "must_not_mention": _score(1.0),
            "sources_cited": _score(1.0),
            "rubric_judge": _score(0.9),
        }
    }

    ok, problems = check_regression(summary)

    assert not ok
    assert problems == ["ios_format_contract: 1 failing case(s)"]


def test_check_regression_rejects_missing_required_evaluator_coverage():
    from llm.langsmith_evals import check_regression

    summary = {
        "scores": {
            "ios_format_contract": _score(1.0),
            "must_mention": _score(1.0),
            "must_not_mention": _score(1.0),
        }
    }

    ok, problems = check_regression(summary)

    assert not ok
    assert problems == ["sources_cited: no scored results"]


def test_check_regression_applies_min_score_to_quality_signals():
    from llm.langsmith_evals import check_regression

    summary = {
        "scores": {
            "ios_format_contract": _score(1.0),
            "must_mention": _score(1.0),
            "must_not_mention": _score(1.0),
            "sources_cited": _score(1.0),
            "rubric_judge": _score(
                0.6,
                failures=[{"case": "weak-answer", "score": 0.6}],
            ),
        }
    }

    ok, problems = check_regression(summary, min_score=0.7)

    assert not ok
    assert problems == ["rubric_judge: mean 0.6 below threshold 0.7"]
```

- [ ] **Step 2: Add tests for optional judge coverage**

Add:

```python
def test_required_evaluator_keys_only_requires_judge_when_enabled():
    from llm.langsmith_evals import (
        HARD_CONTRACT_EVALUATORS,
        required_evaluator_keys,
    )

    assert required_evaluator_keys(judge=False) == HARD_CONTRACT_EVALUATORS
    assert required_evaluator_keys(judge=True) == (
        *HARD_CONTRACT_EVALUATORS,
        "rubric_judge",
    )


def test_check_regression_rejects_missing_judge_when_required():
    from llm.langsmith_evals import check_regression, required_evaluator_keys

    summary = {
        "scores": {
            "ios_format_contract": _score(1.0),
            "must_mention": _score(1.0),
            "must_not_mention": _score(1.0),
            "sources_cited": _score(1.0),
        }
    }

    ok, problems = check_regression(
        summary,
        required_keys=required_evaluator_keys(judge=True),
    )

    assert not ok
    assert problems == ["rubric_judge: no scored results"]
```

- [ ] **Step 3: Run the new tests and confirm current behavior fails**

Run:

```bash
cd maplocation
../venv/bin/python -m pytest \
  locations/tests/test_llm_evaluators.py::test_check_regression_rejects_any_hard_contract_failure \
  locations/tests/test_llm_evaluators.py::test_check_regression_rejects_missing_required_evaluator_coverage \
  locations/tests/test_llm_evaluators.py::test_check_regression_applies_min_score_to_quality_signals \
  locations/tests/test_llm_evaluators.py::test_required_evaluator_keys_only_requires_judge_when_enabled \
  locations/tests/test_llm_evaluators.py::test_check_regression_rejects_missing_judge_when_required \
  -q
```

Expected: FAIL because required-evaluator coverage and `required_evaluator_keys` do not exist, and hard failures are currently inferred only from a rounded mean.

- [ ] **Step 4: Add the hard-contract and coverage implementation**

Immediately above `check_regression`, add:

```python
HARD_CONTRACT_EVALUATORS = (
    "ios_format_contract",
    "must_mention",
    "must_not_mention",
    "sources_cited",
)


def required_evaluator_keys(*, judge: bool) -> tuple[str, ...]:
    if judge:
        return (*HARD_CONTRACT_EVALUATORS, "rubric_judge")
    return HARD_CONTRACT_EVALUATORS
```

Replace `check_regression` with:

```python
def check_regression(
    summary: dict[str, Any],
    *,
    min_score: float = 0.7,
    required_keys: tuple[str, ...] = HARD_CONTRACT_EVALUATORS,
) -> tuple[bool, list[str]]:
    """Reject missing coverage, any hard failure, or a low quality mean."""
    problems: list[str] = []
    scores = summary.get("scores", {})

    for key in required_keys:
        stats = scores.get(key)
        if not stats or not stats.get("count"):
            problems.append(f"{key}: no scored results")

    for key, stats in scores.items():
        if key in HARD_CONTRACT_EVALUATORS:
            failure_count = len(stats.get("failures", []))
            if failure_count:
                problems.append(f"{key}: {failure_count} failing case(s)")
            continue

        mean = stats.get("mean")
        if mean is not None and mean < min_score:
            problems.append(
                f"{key}: mean {mean} below threshold {min_score}"
            )

    return (not problems, problems)
```

- [ ] **Step 5: Run the five focused tests**

Run:

```bash
cd maplocation
../venv/bin/python -m pytest \
  locations/tests/test_llm_evaluators.py::test_check_regression_rejects_any_hard_contract_failure \
  locations/tests/test_llm_evaluators.py::test_check_regression_rejects_missing_required_evaluator_coverage \
  locations/tests/test_llm_evaluators.py::test_check_regression_applies_min_score_to_quality_signals \
  locations/tests/test_llm_evaluators.py::test_required_evaluator_keys_only_requires_judge_when_enabled \
  locations/tests/test_llm_evaluators.py::test_check_regression_rejects_missing_judge_when_required \
  -q
```

Expected: `5 passed`.

- [ ] **Step 6: Record the Task 2 checkpoint without staging dirty files**

Run:

```bash
git diff --check -- \
  maplocation/llm/langsmith_evals.py \
  maplocation/locations/tests/test_llm_evaluators.py
```

Expected: no output and exit code 0. Do not stage the pre-existing file changes.

### Task 3: Wire Judge Coverage into the Management Command

**Files:**
- Modify: `maplocation/llm/management/commands/run_llm_evals.py:16-24,75-81`
- Test: `maplocation/locations/tests/test_llm_evaluators.py`

**Interfaces:**
- Consumes: `required_evaluator_keys(*, judge: bool) -> tuple[str, ...]` from Task 2.
- Produces: command calls `check_regression(..., required_keys=required_evaluator_keys(judge=judge))`.

- [ ] **Step 1: Add a pure wiring helper test**

Add this test:

```python
def test_command_module_uses_required_evaluator_keys():
    from pathlib import Path

    command_path = (
        Path(__file__).parents[2]
        / "llm"
        / "management"
        / "commands"
        / "run_llm_evals.py"
    )
    source = command_path.read_text()

    assert "required_evaluator_keys," in source
    assert "required_keys=required_evaluator_keys(judge=judge)" in source
```

- [ ] **Step 2: Run the wiring test and confirm it fails**

Run:

```bash
cd maplocation
../venv/bin/python -m pytest \
  locations/tests/test_llm_evaluators.py::test_command_module_uses_required_evaluator_keys \
  -q
```

Expected: FAIL because the command does not import or call `required_evaluator_keys`.

- [ ] **Step 3: Update the management command**

Add `required_evaluator_keys` to the import from `llm.langsmith_evals`:

```python
from llm.langsmith_evals import (
    RUNTIMES,
    check_regression,
    required_evaluator_keys,
    run_langsmith_experiment,
    summarize_experiment,
)
```

Replace the regression call with:

```python
            passed, problems = check_regression(
                summary,
                min_score=options["min_score"],
                required_keys=required_evaluator_keys(judge=judge),
            )
```

- [ ] **Step 4: Run the wiring and gate tests**

Run:

```bash
cd maplocation
../venv/bin/python -m pytest \
  locations/tests/test_llm_evaluators.py::test_command_module_uses_required_evaluator_keys \
  locations/tests/test_llm_evaluators.py::test_required_evaluator_keys_only_requires_judge_when_enabled \
  locations/tests/test_llm_evaluators.py::test_check_regression_rejects_missing_judge_when_required \
  -q
```

Expected: `3 passed`.

- [ ] **Step 5: Record the Task 3 checkpoint**

Run:

```bash
git diff --check -- \
  maplocation/llm/management/commands/run_llm_evals.py \
  maplocation/locations/tests/test_llm_evaluators.py
```

Expected: no output and exit code 0. The management command is currently untracked, so do not stage it together with unknown pre-existing work.

### Task 4: Update the Workflow Documentation and Run Offline Verification

**Files:**
- Modify: `docs/LANGGRAPH_LANGSMITH_CHAT_GUIDE.md:380-414`
- Test: `maplocation/locations/tests/test_llm_evaluators.py`
- Test: `maplocation/locations/tests/test_llm_formatting.py`

**Interfaces:**
- Consumes: final behavior from Tasks 1-3.
- Produces: operator guidance that distinguishes offline verification from a credentialed live experiment.

- [ ] **Step 1: Replace the threshold paragraph**

Replace lines 411-414 with:

````markdown
Hard contracts (`ios_format_contract`, `must_mention`, `must_not_mention`,
and `sources_cited`) are case-level gates: one applicable failure fails the
experiment. The command also fails when a required evaluator produces no
scored results. `rubric_judge` coverage is required unless `--skip-judge` is
set; scored non-hard evaluators must clear `--min-score` (default 0.7).
LangGraph eval runs use temperature 0 for reproducibility.

The evaluator unit tests are fully offline:

```bash
cd maplocation
../venv/bin/python -m pytest \
  locations/tests/test_llm_evaluators.py \
  locations/tests/test_llm_formatting.py \
  -q
```

The management command is a separate live gate. It requires LangSmith, AWS
credentials, and any external service used by the selected cases.
````

- [ ] **Step 2: Run the full focused offline suite**

Run:

```bash
cd maplocation
../venv/bin/python -m pytest \
  locations/tests/test_llm_evaluators.py \
  locations/tests/test_llm_formatting.py \
  -q
```

Expected: all tests in both files pass with no network calls.

- [ ] **Step 3: Run syntax and whitespace verification**

Run:

```bash
cd maplocation
../venv/bin/python -m compileall -q \
  llm/langsmith_evals.py \
  llm/evaluators.py \
  llm/management/commands/run_llm_evals.py
cd ..
git diff --check -- \
  maplocation/llm/langsmith_evals.py \
  maplocation/llm/management/commands/run_llm_evals.py \
  maplocation/locations/tests/test_llm_evaluators.py \
  docs/LANGGRAPH_LANGSMITH_CHAT_GUIDE.md
```

Expected: both commands exit 0 with no output.

- [ ] **Step 4: Inspect the final scoped diff**

Run:

```bash
git status --short
git diff -- \
  maplocation/llm/langsmith_evals.py \
  maplocation/llm/management/commands/run_llm_evals.py \
  maplocation/locations/tests/test_llm_evaluators.py \
  docs/LANGGRAPH_LANGSMITH_CHAT_GUIDE.md
```

Expected: the working tree still contains unrelated pre-existing changes, while the scoped files show the new citation, coverage, hard-gate, command-wiring, test, and documentation changes.

- [ ] **Step 5: Report the commit and live-run gates accurately**

Do not stage or commit the implementation files from this dirty checkout.
Report:

```text
Offline evaluator tests: passed.
Live LangSmith experiment: not run unless LangSmith, AWS, Autism RAG, and web-search prerequisites were explicitly available.
Implementation commit: deferred because the touched files contained pre-existing uncommitted work.
```

If the user later identifies the pre-existing eval changes as belonging to this
same workstream and explicitly authorizes committing them, stage only the four
scoped paths, inspect `git diff --cached`, and commit with:

```bash
git commit -m "test: harden classic chat eval gates"
```
