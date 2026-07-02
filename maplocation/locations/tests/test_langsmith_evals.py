"""Tests for LangSmith eval and side-by-side comparison helpers."""

import uuid

from langsmith.utils import LangSmithNotFoundError


class _FakeDataset:
    id = uuid.UUID("3d60339d-6f32-4df6-8c7e-f34fb8f8e5ad")


class _FakeLangSmithClient:
    def __init__(self):
        self.created_examples = []
        self.updated_examples = []

    def has_dataset(self, dataset_name):
        return True

    def read_dataset(self, dataset_name):
        return _FakeDataset()

    def read_example(self, example_id):
        raise LangSmithNotFoundError("missing")

    def create_example(self, **kwargs):
        self.created_examples.append(kwargs)

    def update_example(self, *args, **kwargs):
        self.updated_examples.append((args, kwargs))


def test_eval_cases_cover_core_chat_risks():
    from llm.langsmith_evals import EVAL_CASES

    case_ids = {case["id"] for case in EVAL_CASES}

    assert "urgent-symptom-escalation" in case_ids
    assert "provider-search-grounding" in case_ids
    assert "autism-research-citations" in case_ids
    assert "current-facts-web-search" in case_ids


def test_dataset_examples_match_langsmith_input_shape():
    from llm.langsmith_evals import build_dataset_examples

    examples = build_dataset_examples()

    assert examples
    assert all("inputs" in example for example in examples)
    assert all("outputs" in example for example in examples)
    assert all("metadata" in example for example in examples)
    assert {"query", "context", "locale"} <= set(examples[0]["inputs"])
    assert "rubric" in examples[0]["outputs"]


def test_ensure_langsmith_dataset_scopes_example_ids_to_dataset():
    from llm.langsmith_evals import EVAL_CASES, ensure_langsmith_dataset

    client = _FakeLangSmithClient()

    ensure_langsmith_dataset(client=client)

    expected_example_id = uuid.uuid5(_FakeDataset.id, EVAL_CASES[0]["id"])
    assert client.created_examples[0]["example_id"] == expected_example_id
    assert client.created_examples[0]["dataset_id"] == _FakeDataset.id


def test_side_by_side_case_runner_returns_both_runtime_outputs(monkeypatch):
    from llm import langsmith_evals

    def fake_strands_runner(query, context=None, locale="en"):
        return {"answer": f"strands: {query}", "tools_used": ["search_providers"]}

    def fake_langgraph_runner(query, context=None, locale="en"):
        return {"answer": f"langgraph: {query}", "tools_used": ["search_providers"]}

    monkeypatch.setattr(langsmith_evals, "run_strands_case", fake_strands_runner)
    monkeypatch.setattr(langsmith_evals, "run_langgraph_case", fake_langgraph_runner)

    result = langsmith_evals.run_side_by_side_case(
        {"query": "Find ABA providers", "context": {}, "locale": "en"}
    )

    assert result["strands"]["answer"] == "strands: Find ABA providers"
    assert result["langgraph"]["answer"] == "langgraph: Find ABA providers"
    assert result["comparison"]["same_tools"] is True
