"""Pinecone serverless vector store implementation."""

from __future__ import annotations

import logging
from importlib import import_module
from typing import Any

from tenacity import retry, stop_after_attempt, wait_exponential

from ...config import Settings, get_settings
from .base import VectorHit, VectorRecord, VectorStore

logger = logging.getLogger(__name__)


class PineconeVectorStore(VectorStore):
    def __init__(self, settings: Settings | None = None) -> None:
        self.settings = settings or get_settings()
        if not self.settings.pinecone_api_key:
            raise RuntimeError("PINECONE_API_KEY is not set.")
        pinecone: Any = import_module("pinecone")
        self._pc = pinecone.Pinecone(api_key=self.settings.pinecone_api_key)
        self._index_name = self.settings.pinecone_index
        self._index = None

    def ensure_index(self, *, dimension: int, metric: str = "cosine") -> None:
        pinecone: Any = import_module("pinecone")

        existing = {idx.name for idx in self._pc.list_indexes()}
        if self._index_name not in existing:
            logger.info(
                "Creating Pinecone index %s (dim=%d, metric=%s, cloud=%s, region=%s)",
                self._index_name,
                dimension,
                metric,
                self.settings.pinecone_cloud,
                self.settings.pinecone_region,
            )
            self._pc.create_index(
                name=self._index_name,
                dimension=dimension,
                metric=metric,
                spec=pinecone.ServerlessSpec(
                    cloud=self.settings.pinecone_cloud,
                    region=self.settings.pinecone_region,
                ),
            )
        self._index = self._pc.Index(self._index_name)

    def _index_handle(self):
        if self._index is None:
            self._index = self._pc.Index(self._index_name)
        return self._index

    @retry(
        wait=wait_exponential(multiplier=1, min=1, max=10),
        stop=stop_after_attempt(3),
        reraise=True,
    )
    def upsert(self, records: list[VectorRecord], *, namespace: str) -> None:
        if not records:
            return
        vectors = [
            {"id": r.id, "values": r.values, "metadata": _sanitize_metadata(r.metadata)}
            for r in records
        ]
        # Pinecone caps batch sizes; 100 is conservative and matches docs.
        index = self._index_handle()
        for i in range(0, len(vectors), 100):
            batch = vectors[i : i + 100]
            index.upsert(vectors=batch, namespace=namespace)

    @retry(
        wait=wait_exponential(multiplier=1, min=1, max=10),
        stop=stop_after_attempt(3),
        reraise=True,
    )
    def delete(self, *, namespace: str, metadata_filter: dict[str, Any]) -> None:
        self._index_handle().delete(namespace=namespace, filter=metadata_filter)

    @retry(
        wait=wait_exponential(multiplier=1, min=1, max=8),
        stop=stop_after_attempt(3),
        reraise=True,
    )
    def query(
        self,
        *,
        vector: list[float],
        namespace: str | None,
        top_k: int,
        metadata_filter: dict[str, Any] | None = None,
        include_namespaces: list[str] | None = None,
    ) -> list[VectorHit]:
        index = self._index_handle()
        if include_namespaces:
            hits: list[VectorHit] = []
            for ns in include_namespaces:
                hits.extend(
                    self._query_one(
                        index,
                        vector=vector,
                        namespace=ns,
                        top_k=top_k,
                        metadata_filter=metadata_filter,
                    )
                )
            hits.sort(key=lambda h: h.score, reverse=True)
            return hits[:top_k]
        return self._query_one(
            index,
            vector=vector,
            namespace=namespace,
            top_k=top_k,
            metadata_filter=metadata_filter,
        )

    def _query_one(
        self,
        index,
        *,
        vector: list[float],
        namespace: str | None,
        top_k: int,
        metadata_filter: dict[str, Any] | None,
    ) -> list[VectorHit]:
        kwargs: dict[str, Any] = {
            "vector": vector,
            "top_k": top_k,
            "include_metadata": True,
        }
        if namespace:
            kwargs["namespace"] = namespace
        if metadata_filter:
            kwargs["filter"] = metadata_filter
        response = index.query(**kwargs)
        matches = response.get("matches", []) if isinstance(response, dict) else getattr(response, "matches", [])
        hits: list[VectorHit] = []
        for match in matches:
            if isinstance(match, dict):
                meta = match.get("metadata", {}) or {}
                hits.append(
                    VectorHit(
                        id=match.get("id", ""),
                        score=float(match.get("score", 0.0)),
                        metadata=meta,
                        text=meta.get("text", ""),
                    )
                )
            else:
                meta = getattr(match, "metadata", {}) or {}
                hits.append(
                    VectorHit(
                        id=getattr(match, "id", ""),
                        score=float(getattr(match, "score", 0.0)),
                        metadata=dict(meta),
                        text=meta.get("text", "") if isinstance(meta, dict) else "",
                    )
                )
        return hits

    def describe(self) -> dict[str, Any]:
        try:
            stats = self._index_handle().describe_index_stats()
            return _to_jsonable(stats)
        except Exception as exc:  # pragma: no cover - diagnostic helper
            logger.warning("Pinecone describe failed: %s", exc)
            return {"error": str(exc)}


def _sanitize_metadata(metadata: dict[str, Any]) -> dict[str, Any]:
    """Pinecone metadata must be string, number, bool, or list of strings.

    We coerce best-effort and drop anything we can't represent.
    """

    safe: dict[str, Any] = {}
    for key, value in metadata.items():
        if value is None:
            continue
        if isinstance(value, (str, int, float, bool)):
            safe[key] = value
        elif isinstance(value, list):
            string_items = [str(v) for v in value if isinstance(v, (str, int, float, bool))]
            if string_items:
                safe[key] = string_items
        else:
            safe[key] = str(value)
    return safe


def _to_jsonable(value: Any) -> Any:
    """Convert Pinecone SDK response objects into JSON-serializable values."""

    if value is None or isinstance(value, (str, int, float, bool)):
        return value
    if isinstance(value, dict):
        return {str(k): _to_jsonable(v) for k, v in value.items()}
    if isinstance(value, (list, tuple)):
        return [_to_jsonable(item) for item in value]
    if hasattr(value, "to_dict"):
        return _to_jsonable(value.to_dict())
    if hasattr(value, "model_dump"):
        return _to_jsonable(value.model_dump())
    if hasattr(value, "__dict__"):
        return {
            str(k): _to_jsonable(v)
            for k, v in vars(value).items()
            if not str(k).startswith("_")
        }
    return str(value)
