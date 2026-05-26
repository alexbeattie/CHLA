"""End-to-end ingestion pipeline.

Steps:
  1. Adapter -> normalized ``SourceDocument`` stream.
  2. Chunker -> ``DocumentChunk`` per source document.
  3. Embedding provider -> dense vectors with ``input_type=search_document``.
  4. Vector store -> upsert into the chunk's namespace.

The pipeline writes the chunk text into Pinecone metadata so retrieval can
return human-readable context without a secondary lookup.
"""

from __future__ import annotations

import json
import logging
from collections.abc import Iterable
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

from ..config import Settings, get_settings
from ..rag.embeddings import EmbeddingProvider, InputType, get_embedding_provider
from ..rag.vectorstore import VectorRecord, VectorStore, get_vector_store
from ..sources.adapters import BaseAdapter
from ..sources.models import DocumentChunk, SourceDocument
from .chunker import chunk_document

logger = logging.getLogger(__name__)


@dataclass
class IngestionResult:
    source_key: str
    documents: int = 0
    chunks: int = 0
    upserts_by_namespace: dict[str, int] = field(default_factory=dict)
    sample_ids: list[str] = field(default_factory=list)


class IngestionPipeline:
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

    def prepare_index(self) -> None:
        self.vector_store.ensure_index(
            dimension=self.embedder.dimension,
            metric=self.settings.pinecone_metric,
        )

    def run(
        self,
        adapter: BaseAdapter,
        *,
        query: str,
        limit: int,
        save_raw: bool = True,
        namespace_prefix: str | None = None,
        metadata_tags: dict[str, Any] | None = None,
        replace_source: bool = False,
        **adapter_kwargs: Any,
    ) -> IngestionResult:
        self.prepare_index()
        result = IngestionResult(source_key=adapter.source_key)
        docs: list[SourceDocument] = list(adapter.fetch(query=query, limit=limit, **adapter_kwargs))
        result.documents = len(docs)
        if not docs:
            logger.info("Ingestion: %s returned no documents for %r", adapter.source_key, query)
            return result

        if save_raw:
            _save_raw(self.settings.processed_dir / f"{adapter.source_key}-{_safe_slug(query)}.jsonl", docs)

        chunks: list[DocumentChunk] = []
        for doc in docs:
            chunks.extend(chunk_document(doc))
        result.chunks = len(chunks)
        if not chunks:
            return result

        texts = [c.text for c in chunks]
        vectors = self.embedder.embed(texts, input_type=InputType.DOCUMENT)
        if len(vectors) != len(chunks):
            raise RuntimeError(
                f"Embedding count {len(vectors)} != chunk count {len(chunks)}"
            )

        per_namespace: dict[str, list[VectorRecord]] = {}
        clean_prefix = _safe_token(namespace_prefix)
        clean_tags = _safe_metadata_tags(metadata_tags or {})
        for chunk, vector in zip(chunks, vectors, strict=True):
            metadata = dict(chunk.metadata)
            metadata["text"] = chunk.text
            metadata["embedding_model"] = self.embedder.model_name
            if clean_prefix:
                metadata["base_namespace"] = chunk.namespace
                metadata["namespace_prefix"] = clean_prefix
            metadata.update(clean_tags)
            record = VectorRecord(id=chunk.chunk_id, values=vector, metadata=metadata)
            namespace = _target_namespace(chunk.namespace, clean_prefix)
            per_namespace.setdefault(namespace, []).append(record)

        replaced_namespaces: set[str] = set()
        for namespace, records in per_namespace.items():
            if replace_source and namespace not in replaced_namespaces:
                self.vector_store.delete(
                    namespace=namespace,
                    metadata_filter={"source_key": {"$eq": adapter.source_key}},
                )
                replaced_namespaces.add(namespace)
            self.vector_store.upsert(records, namespace=namespace)
            result.upserts_by_namespace[namespace] = (
                result.upserts_by_namespace.get(namespace, 0) + len(records)
            )
            result.sample_ids.extend(r.id for r in records[:3])
        return result


def _safe_slug(value: str) -> str:
    return "".join(c if c.isalnum() else "_" for c in value[:60]).strip("_") or "all"


def _safe_token(value: str | None) -> str | None:
    if not value:
        return None
    token = "".join(c.lower() if c.isalnum() else "_" for c in value[:40]).strip("_")
    return token or None


def _target_namespace(base_namespace: str, prefix: str | None) -> str:
    if not prefix:
        return base_namespace
    return f"{prefix}_{base_namespace}"


def _safe_metadata_tags(tags: dict[str, Any]) -> dict[str, Any]:
    safe: dict[str, Any] = {}
    for key, value in tags.items():
        safe_key = _safe_token(str(key))
        if not safe_key or value is None:
            continue
        if isinstance(value, (str, int, float, bool)):
            safe[safe_key] = value
        elif isinstance(value, list):
            scalar_items = [str(item) for item in value if isinstance(item, (str, int, float, bool))]
            if scalar_items:
                safe[safe_key] = scalar_items[:10]
    return safe


def _save_raw(path: Path, docs: Iterable[SourceDocument]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as fh:
        for doc in docs:
            fh.write(json.dumps(doc.model_dump(mode="json")) + "\n")
