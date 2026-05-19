"""Client for the Autism Research RAG service.

The RAG service runs separately from Django so Pinecone/Cohere/OpenAI keys and
research-specific ingestion stay isolated. Django calls it through this small
client and exposes app-facing endpoints/tools.
"""

from __future__ import annotations

import logging
from typing import Any

import requests
from django.conf import settings

logger = logging.getLogger(__name__)


class AutismResearchError(RuntimeError):
    """Raised when the Autism Research RAG service cannot satisfy a request."""


def _rag_base_url() -> str:
    return str(getattr(settings, "AUTISM_RAG_API_URL", "http://127.0.0.1:8000")).rstrip("/")


def ask_autism_research(
    question: str,
    *,
    top_k: int | None = None,
    evidence_types: list[str] | None = None,
    access_classes: list[str] | None = None,
    min_year: int | None = None,
    rerank: bool | None = None,
) -> dict[str, Any]:
    """Ask the Autism Research RAG service and return its structured response."""

    payload: dict[str, Any] = {"question": question}
    if top_k is not None:
        payload["top_k"] = top_k
    if evidence_types:
        payload["evidence_types"] = evidence_types
    if access_classes:
        payload["access_classes"] = access_classes
    if min_year is not None:
        payload["min_year"] = min_year
    if rerank is not None:
        payload["rerank"] = rerank

    url = f"{_rag_base_url()}/ask"
    try:
        response = requests.post(  # nosec B113 - timeout is supplied from settings.
            url,
            json=payload,
            timeout=getattr(settings, "AUTISM_RAG_TIMEOUT_SECONDS", 45),
        )
        response.raise_for_status()
        return response.json()
    except requests.RequestException as exc:
        logger.warning(
            "Autism Research RAG request failed",
            extra={"url": url, "error_type": type(exc).__name__},
        )
        raise AutismResearchError(str(exc)) from exc
    except ValueError as exc:
        raise AutismResearchError("Autism Research RAG returned invalid JSON") from exc


def check_autism_research_health() -> dict[str, Any]:
    """Return RAG service health details."""

    url = f"{_rag_base_url()}/healthz"
    try:
        response = requests.get(  # nosec B113 - timeout is supplied from settings.
            url,
            timeout=getattr(settings, "AUTISM_RAG_TIMEOUT_SECONDS", 45),
        )
        response.raise_for_status()
        return response.json()
    except requests.RequestException as exc:
        raise AutismResearchError(str(exc)) from exc
    except ValueError as exc:
        raise AutismResearchError("Autism Research RAG returned invalid JSON") from exc
