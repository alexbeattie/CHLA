"""Retrieval orchestration.

Embeds the query with ``input_type=search_query``, searches Pinecone with
metadata filters, optionally reranks with Cohere, and returns hits ready
for citation.
"""

from __future__ import annotations

import logging
from dataclasses import dataclass
from typing import Any

from ..config import Settings, get_settings
from ..sources.models import AccessClass, EvidenceType
from ..sources.registry import SOURCE_REGISTRY
from .embeddings import EmbeddingProvider, InputType, get_embedding_provider
from .vectorstore import VectorHit, VectorStore, get_vector_store

logger = logging.getLogger(__name__)


@dataclass
class RetrievalFilters:
    evidence_types: list[EvidenceType] | None = None
    access_classes: list[AccessClass] | None = None
    namespaces: list[str] | None = None
    namespace_prefix: str | None = None
    min_year: int | None = None

    def to_pinecone_filter(self) -> dict[str, Any] | None:
        terms: dict[str, Any] = {}
        if self.evidence_types:
            terms["evidence_type"] = {"$in": [e.value for e in self.evidence_types]}
        if self.access_classes:
            terms["access_class"] = {"$in": [a.value for a in self.access_classes]}
        if self.min_year is not None:
            terms["published_year"] = {"$gte": self.min_year}
        return terms or None


class Retriever:
    def __init__(
        self,
        *,
        embedder: EmbeddingProvider | None = None,
        vector_store: VectorStore | None = None,
        settings: Settings | None = None,
    ) -> None:
        self.settings = settings or get_settings()
        self.embedder = embedder or get_embedding_provider("cohere")
        self.vector_store = vector_store or get_vector_store("pinecone")

    def search(
        self,
        query: str,
        *,
        top_k: int | None = None,
        filters: RetrievalFilters | None = None,
        rerank: bool | None = None,
    ) -> list[VectorHit]:
        top_k = top_k or self.settings.default_top_k
        filters = filters or RetrievalFilters(
            access_classes=[AccessClass.PUBLIC_OPEN, AccessClass.PUBLIC_METADATA_ONLY]
        )
        vector = self.embedder.embed([query], input_type=InputType.QUERY)[0]
        namespaces = filters.namespaces or default_namespaces(
            filters.access_classes,
            namespace_prefix=filters.namespace_prefix,
        )
        hits = self.vector_store.query(
            vector=vector,
            namespace=None,
            include_namespaces=namespaces,
            top_k=max(top_k * 3, top_k),  # over-fetch so rerank has signal
            metadata_filter=filters.to_pinecone_filter(),
        )
        if (rerank if rerank is not None else self.settings.rerank_enabled) and hits:
            hits = _rerank_with_cohere(
                query=query,
                hits=hits,
                model=self.settings.rerank_model,
                api_key=self.settings.cohere_api_key,
                top_n=top_k,
            )
        return hits[:top_k]


def default_namespaces(
    access_classes: list[AccessClass] | None,
    *,
    namespace_prefix: str | None = None,
) -> list[str]:
    """Namespaces to search by default given the requested access classes."""

    if not access_classes:
        access_classes = [AccessClass.PUBLIC_OPEN]
    allowed = set(access_classes)
    namespaces = sorted(
        {
            source.pinecone_namespace
            for source in SOURCE_REGISTRY.values()
            if source.access_class in allowed
        }
    )
    prefix = _safe_namespace_prefix(namespace_prefix)
    if not prefix:
        return namespaces
    return [f"{prefix}_{namespace}" for namespace in namespaces]


def _safe_namespace_prefix(value: str | None) -> str | None:
    if not value:
        return None
    prefix = "".join(c.lower() if c.isalnum() else "_" for c in value[:40]).strip("_")
    return prefix or None


def _rerank_with_cohere(
    *,
    query: str,
    hits: list[VectorHit],
    model: str,
    api_key: str,
    top_n: int,
) -> list[VectorHit]:
    if not api_key:
        logger.info("Rerank disabled: no Cohere API key.")
        return hits
    try:
        import cohere  # noqa: WPS433

        client = cohere.ClientV2(api_key=api_key)
        documents = [hit.text or hit.metadata.get("title", "") for hit in hits]
        response = client.rerank(
            model=model,
            query=query,
            documents=documents,
            top_n=min(top_n, len(documents)),
        )
        ordered: list[VectorHit] = []
        for result in response.results:
            index = result.index
            if 0 <= index < len(hits):
                hit = hits[index]
                hit.score = float(getattr(result, "relevance_score", hit.score))
                ordered.append(hit)
        return ordered
    except Exception as exc:  # pragma: no cover - external service guard
        logger.warning("Cohere rerank failed (%s); returning raw hits.", exc)
        return hits
