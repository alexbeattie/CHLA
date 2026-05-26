"""Embedding provider factory."""

from __future__ import annotations

from ...config import get_settings
from .base import EmbeddingProvider
from .cohere import CohereEmbeddings


def get_embedding_provider(name: str = "cohere") -> EmbeddingProvider:
    settings = get_settings()
    if name == "cohere":
        return CohereEmbeddings(settings=settings)
    raise ValueError(
        f"Unknown embedding provider {name!r}. "
        "Add a new EmbeddingProvider subclass and register it here."
    )
