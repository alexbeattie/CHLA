"""Shared data models for sources, documents, and chunks.

Every chunk that lands in Pinecone carries enough provenance to recover the
original source, citation identifiers, license/access class, and retrieval
time. Retrieval filters and answer guardrails depend on these fields, so
treat them as required at ingestion time.
"""

from __future__ import annotations

from datetime import datetime, timezone
from enum import Enum
from typing import Any

from pydantic import BaseModel, Field


class AccessClass(str, Enum):
    """How freely the source data can be used.

    Drives namespace selection in the vector store and answer guardrails.
    """

    PUBLIC_OPEN = "public_open"
    PUBLIC_METADATA_ONLY = "public_metadata_only"
    CONTROLLED_METADATA = "controlled_metadata"
    APPROVED_CONTROLLED = "approved_controlled"


class EvidenceType(str, Enum):
    """Coarse evidence category to power retrieval filtering."""

    LITERATURE = "literature"
    CLINICAL_TRIAL = "clinical_trial"
    GRANT = "grant"
    GENE_EVIDENCE = "gene_evidence"
    DATASET_METADATA = "dataset_metadata"
    WEB = "web"


class SourceRecord(BaseModel):
    """Static description of one ingestion source.

    Stored in :mod:`autism_rag.sources.registry` so adapters, scripts, and
    docs all reference the same canonical metadata.
    """

    key: str
    name: str
    homepage: str
    access_class: AccessClass
    evidence_type: EvidenceType
    license: str
    access_method: str
    refresh_cadence: str
    pinecone_namespace: str
    notes: str = ""


class SourceDocument(BaseModel):
    """A normalized document produced by an adapter, before chunking."""

    source_key: str
    source_id: str
    title: str
    text: str
    url: str
    evidence_type: EvidenceType
    access_class: AccessClass
    published_at: datetime | None = None
    authors: list[str] = Field(default_factory=list)
    citation_ids: dict[str, str] = Field(default_factory=dict)
    extra: dict[str, Any] = Field(default_factory=dict)
    retrieved_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))

    def stable_id(self) -> str:
        """Stable identifier used as the base for chunk IDs in Pinecone."""

        return f"{self.source_key}:{self.source_id}"


class DocumentChunk(BaseModel):
    """A chunk that will be embedded and upserted into Pinecone."""

    chunk_id: str
    source_key: str
    source_id: str
    namespace: str
    text: str
    section: str | None = None
    position: int = 0
    metadata: dict[str, Any] = Field(default_factory=dict)
