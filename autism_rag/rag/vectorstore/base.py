"""Vector store interface used by ingestion + retrieval."""

from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Any

from pydantic import BaseModel, Field


class VectorRecord(BaseModel):
    id: str
    values: list[float]
    metadata: dict[str, Any] = Field(default_factory=dict)


class VectorHit(BaseModel):
    id: str
    score: float
    metadata: dict[str, Any] = Field(default_factory=dict)
    text: str = ""


class VectorStore(ABC):
    @abstractmethod
    def ensure_index(self, *, dimension: int, metric: str = "cosine") -> None:
        ...

    @abstractmethod
    def upsert(self, records: list[VectorRecord], *, namespace: str) -> None:
        ...

    @abstractmethod
    def delete(self, *, namespace: str, metadata_filter: dict[str, Any]) -> None:
        ...

    @abstractmethod
    def query(
        self,
        *,
        vector: list[float],
        namespace: str | None,
        top_k: int,
        metadata_filter: dict[str, Any] | None = None,
        include_namespaces: list[str] | None = None,
    ) -> list[VectorHit]:
        ...

    @abstractmethod
    def describe(self) -> dict[str, Any]:
        ...
