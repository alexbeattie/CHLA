"""Vector store factory."""

from __future__ import annotations

from ...config import get_settings
from .base import VectorStore
from .pinecone_store import PineconeVectorStore


def get_vector_store(name: str = "pinecone") -> VectorStore:
    settings = get_settings()
    if name == "pinecone":
        return PineconeVectorStore(settings=settings)
    raise ValueError(f"Unknown vector store {name!r}.")
