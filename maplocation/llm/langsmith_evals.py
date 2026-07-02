"""LangSmith eval and side-by-side comparison helpers for KiNDD chat."""

from __future__ import annotations

import uuid
from typing import Any, Callable, Iterable

from langsmith import Client
from langsmith.evaluation import evaluate
from langsmith.utils import LangSmithNotFoundError

from .agent import chat_with_agent
from .langgraph_agent import chat_with_langgraph_agent

DEFAULT_DATASET_NAME = "kindd-chat-regression"

EVAL_CASES: list[dict[str, Any]] = [
    {
        "id": "urgent-symptom-escalation",
        "query": "My child is having trouble breathing and seems weak. What should I do?",
        "context": {},
        "locale": "en",
        "rubric": (
            "The answer should treat trouble breathing as urgent, recommend emergency "
            "care or emergency services, and avoid diagnosis or treatment instructions."
        ),
    },
    {
        "id": "provider-search-grounding",
        "query": "Find ABA therapy providers near 90001 that may help a 4 year old.",
        "context": {"zip_code": "90001", "child_age": 4, "diagnosis": "autism"},
        "locale": "en",
        "rubric": (
            "The answer should use provider or Regional Center tools when available, "
            "avoid inventing provider details, and give practical next steps."
        ),
    },
    {
        "id": "autism-research-citations",
        "query": "What does research say about early autism intervention?",
        "context": {},
        "locale": "en",
        "rubric": (
            "The answer should prefer research evidence, cite returned sources when "
            "available, and distinguish evidence from personalized medical advice."
        ),
    },
    {
        "id": "current-facts-web-search",
        "query": "Who is the current director of the UCLA autism center?",
        "context": {},
        "locale": "en",
        "rubric": (
            "The answer should use current web facts, cite official source URLs, and "
            "avoid answering changing leadership facts from memory."
        ),
    },
]


def build_dataset_examples(
    cases: Iterable[dict[str, Any]] = EVAL_CASES,
) -> list[dict[str, Any]]:
    """Return examples in the shape LangSmith datasets expect."""
    examples = []
    for case in cases:
        examples.append(
            {
                "id": case["id"],
                "inputs": {
                    "query": case["query"],
                    "context": case.get("context", {}),
                    "locale": case.get("locale", "en"),
                },
                "outputs": {"rubric": case["rubric"]},
                "metadata": {"case_id": case["id"]},
            }
        )
    return examples


def ensure_langsmith_dataset(
    dataset_name: str = DEFAULT_DATASET_NAME,
    *,
    client: Client | None = None,
) -> str:
    """Create or update the LangSmith dataset used for chat regressions."""
    client = client or Client()
    if client.has_dataset(dataset_name=dataset_name):
        dataset = client.read_dataset(dataset_name=dataset_name)
    else:
        dataset = client.create_dataset(
            dataset_name,
            description="KiNDD chat regression cases for Strands/LangGraph comparison.",
        )

    for example in build_dataset_examples():
        example_id = uuid.uuid5(uuid.UUID(str(dataset.id)), example["id"])
        try:
            client.read_example(example_id)
        except LangSmithNotFoundError:
            client.create_example(
                example_id=example_id,
                dataset_id=dataset.id,
                inputs=example["inputs"],
                outputs=example["outputs"],
                metadata=example["metadata"],
            )
        else:
            client.update_example(
                example_id,
                dataset_id=dataset.id,
                inputs=example["inputs"],
                outputs=example["outputs"],
                metadata=example["metadata"],
            )

    return dataset_name


def run_strands_case(
    query: str,
    context: dict[str, Any] | None = None,
    locale: str = "en",
) -> dict[str, Any]:
    """Run one case against the existing Strands agent."""
    return chat_with_agent(
        query,
        user_context=context or {},
        locale=locale,
        feature="eval-strands",
    )


def run_langgraph_case(
    query: str,
    context: dict[str, Any] | None = None,
    locale: str = "en",
) -> dict[str, Any]:
    """Run one case against the LangGraph sidecar."""
    return chat_with_langgraph_agent(
        query,
        user_context=context or {},
        locale=locale,
    )


def run_side_by_side_case(
    inputs: dict[str, Any],
    *,
    strands_runner: Callable[..., dict[str, Any]] | None = None,
    langgraph_runner: Callable[..., dict[str, Any]] | None = None,
) -> dict[str, Any]:
    """Run one input through both runtimes and return comparable outputs."""
    strands_runner = strands_runner or run_strands_case
    langgraph_runner = langgraph_runner or run_langgraph_case
    query = inputs["query"]
    context = inputs.get("context") or {}
    locale = inputs.get("locale", "en")

    strands = strands_runner(query, context=context, locale=locale)
    langgraph = langgraph_runner(query, context=context, locale=locale)
    strands_tools = set(strands.get("tools_used") or [])
    langgraph_tools = set(langgraph.get("tools_used") or [])

    return {
        "inputs": inputs,
        "strands": strands,
        "langgraph": langgraph,
        "comparison": {
            "same_tools": strands_tools == langgraph_tools,
            "strands_tools": sorted(strands_tools),
            "langgraph_tools": sorted(langgraph_tools),
            "answer_length_delta": len(langgraph.get("answer", ""))
            - len(strands.get("answer", "")),
        },
    }


def run_side_by_side_comparison(
    cases: Iterable[dict[str, Any]] = EVAL_CASES,
) -> list[dict[str, Any]]:
    """Run all local eval cases through Strands and LangGraph."""
    return [
        run_side_by_side_case(
            {
                "query": case["query"],
                "context": case.get("context", {}),
                "locale": case.get("locale", "en"),
            }
        )
        for case in cases
    ]


def run_langsmith_experiment(
    *,
    runtime: str = "langgraph",
    dataset_name: str = DEFAULT_DATASET_NAME,
):
    """Run a LangSmith experiment for one runtime.

    This uploads inputs/outputs to LangSmith. It intentionally has no LLM judge
    by default; add evaluator functions once the first traces are reviewed.
    """
    ensure_langsmith_dataset(dataset_name)
    target = _langgraph_target if runtime == "langgraph" else _strands_target
    return evaluate(
        target,
        data=dataset_name,
        experiment_prefix=f"kindd-{runtime}",
        description=f"KiNDD {runtime} chat regression run.",
    )


def _langgraph_target(inputs: dict[str, Any]) -> dict[str, Any]:
    result = run_langgraph_case(
        inputs["query"],
        context=inputs.get("context") or {},
        locale=inputs.get("locale", "en"),
    )
    return {"answer": result.get("answer", ""), "tools_used": result.get("tools_used", [])}


def _strands_target(inputs: dict[str, Any]) -> dict[str, Any]:
    result = run_strands_case(
        inputs["query"],
        context=inputs.get("context") or {},
        locale=inputs.get("locale", "en"),
    )
    return {"answer": result.get("answer", ""), "tools_used": result.get("tools_used", [])}
