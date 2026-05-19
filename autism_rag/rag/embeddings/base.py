"""Embedding provider interface.

Defining this lets us swap Cohere for OpenAI, Voyage, or Bedrock later
without rewriting ingestion or Pinecone upsert logic.
"""

from __future__ import annotations

from abc import ABC, abstractmethod
from enum import Enum


class InputType(str, Enum):
    DOCUMENT = "search_document"
    QUERY = "search_query"


class EmbeddingProvider(ABC):
    @property
    @abstractmethod
    def model_name(self) -> str:
        ...

    @property
    @abstractmethod
    def dimension(self) -> int:
        ...

    @abstractmethod
    def embed(self, texts: list[str], *, input_type: InputType) -> list[list[float]]:
        ...
