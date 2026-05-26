"""FastAPI service exposing the autism RAG.

Endpoints:
  GET  /healthz       - liveness + index status
  GET  /sources       - list configured sources
  POST /ask           - retrieve + answer with citations
"""

from __future__ import annotations

import logging

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from ..rag.generation import Answerer
from ..rag.retrieval import RetrievalFilters, Retriever
from ..sources.models import AccessClass, EvidenceType
from ..sources.registry import SOURCE_REGISTRY

logger = logging.getLogger(__name__)

app = FastAPI(title="Autism Research RAG", version="0.1.0")

_retriever: Retriever | None = None
_answerer: Answerer | None = None


def get_retriever() -> Retriever:
    global _retriever
    if _retriever is None:
        _retriever = Retriever()
    return _retriever


def get_answerer() -> Answerer:
    global _answerer
    if _answerer is None:
        _answerer = Answerer()
    return _answerer


class AskRequest(BaseModel):
    question: str
    top_k: int | None = Field(default=None, ge=1, le=50)
    evidence_types: list[EvidenceType] | None = None
    access_classes: list[AccessClass] | None = None
    namespaces: list[str] | None = None
    corpus: str | None = None
    min_year: int | None = None
    rerank: bool | None = None


@app.get("/healthz")
def healthz() -> dict:
    try:
        stats = get_retriever().vector_store.describe()
    except Exception as exc:
        return {"status": "degraded", "error": str(exc)}
    return {"status": "ok", "vector_store": stats}


@app.get("/sources")
def list_sources() -> list[dict]:
    return [
        {
            "key": key,
            "name": source.name,
            "evidence_type": source.evidence_type.value,
            "access_class": source.access_class.value,
            "namespace": source.pinecone_namespace,
            "license": source.license,
            "access_method": source.access_method,
            "refresh_cadence": source.refresh_cadence,
        }
        for key, source in SOURCE_REGISTRY.items()
    ]


@app.post("/ask")
def ask(payload: AskRequest) -> dict:
    if not payload.question.strip():
        raise HTTPException(status_code=400, detail="question is required")
    filters = RetrievalFilters(
        evidence_types=payload.evidence_types,
        access_classes=payload.access_classes or [AccessClass.PUBLIC_OPEN],
        namespaces=payload.namespaces,
        namespace_prefix=payload.corpus,
        min_year=payload.min_year,
    )
    hits = get_retriever().search(
        payload.question,
        top_k=payload.top_k,
        filters=filters,
        rerank=payload.rerank,
    )
    response = get_answerer().answer(payload.question, hits)
    return response.to_dict()
