"""Cohere ``embed-v4.0`` embeddings provider."""

from __future__ import annotations

import logging
import random
import time
from importlib import import_module
from typing import Any

from ...config import Settings, get_settings
from .base import EmbeddingProvider, InputType

logger = logging.getLogger(__name__)


class CohereEmbeddings(EmbeddingProvider):
    def __init__(self, settings: Settings | None = None) -> None:
        self.settings = settings or get_settings()
        if not self.settings.cohere_api_key:
            raise RuntimeError(
                "COHERE_API_KEY is not set. Add it to .env or the environment."
            )
        # Lazy import keeps the rest of the package importable without cohere.
        cohere = import_module("cohere")
        self._client = cohere.ClientV2(api_key=self.settings.cohere_api_key)
        self._model = self.settings.cohere_embed_model
        self._dim = self.settings.cohere_embed_dim
        self._batch_size = max(1, self.settings.cohere_embed_batch_size)
        self._pause_seconds = max(0.0, self.settings.cohere_embed_pause_seconds)
        self._max_retries = max(1, self.settings.cohere_embed_max_retries)

    @property
    def model_name(self) -> str:
        return self._model

    @property
    def dimension(self) -> int:
        return self._dim

    def embed(self, texts: list[str], *, input_type: InputType) -> list[list[float]]:
        if not texts:
            return []
        # Long scraped pages can exhaust token-per-minute quotas even with
        # modest request counts. Keep batches configurable and pace requests
        # before the provider has to rate-limit us.
        batches = _chunked(texts, size=self._batch_size)
        all_vectors: list[list[float]] = []
        for index, batch in enumerate(batches):
            if index > 0:
                time.sleep(self._pause_seconds)
            embeddings = self._embed_batch(batch, input_type=input_type)
            if not embeddings:
                raise RuntimeError("Cohere embed returned no vectors")
            all_vectors.extend(embeddings)
        return all_vectors

    def _embed_batch(self, texts: list[str], *, input_type: InputType) -> list[list[float]]:
        kwargs: dict[str, Any] = {
            "model": self._model,
            "texts": texts,
            "input_type": input_type.value,
            "embedding_types": ["float"],
        }
        for attempt in range(self._max_retries):
            try:
                response = self._client.embed(**kwargs)
                return _extract_embeddings(response)
            except Exception as exc:
                is_last_attempt = attempt >= self._max_retries - 1
                if not _is_retryable(exc) or is_last_attempt:
                    raise
                delay = _retry_delay(exc, attempt)
                logger.warning(
                    "Cohere embed request throttled/failed (%s); retrying in %.1fs",
                    _error_status(exc) or type(exc).__name__,
                    delay,
                )
                time.sleep(delay)
        return []


def _chunked(items: list[str], *, size: int) -> list[list[str]]:
    return [items[i : i + size] for i in range(0, len(items), size)]


def _is_retryable(exc: Exception) -> bool:
    status = _error_status(exc)
    return status == 429 or (status is not None and status >= 500)


def _retry_delay(exc: Exception, attempt: int) -> float:
    retry_after = _retry_after_seconds(exc)
    if retry_after is not None:
        return min(max(retry_after, 1.0), 180.0)
    base = min(2 ** attempt * 8.0, 120.0)
    return base + random.uniform(0.0, 2.0)


def _error_status(exc: Exception) -> int | None:
    for attr in ("status_code", "status"):
        value = getattr(exc, attr, None)
        if isinstance(value, int):
            return value
    response = getattr(exc, "response", None)
    value = getattr(response, "status_code", None)
    return value if isinstance(value, int) else None


def _retry_after_seconds(exc: Exception) -> float | None:
    response = getattr(exc, "response", None)
    headers = getattr(response, "headers", None)
    if not headers:
        return None
    value = headers.get("retry-after") or headers.get("Retry-After")
    if value is None:
        return None
    try:
        return float(value)
    except ValueError:
        return None


def _extract_embeddings(response: Any) -> list[list[float]]:
    # The Cohere v2 client returns an object with ``embeddings`` that may be
    # a ``EmbeddingsFloats`` dataclass with ``float_`` or a plain list of lists.
    embeddings = getattr(response, "embeddings", None)
    if embeddings is None:
        return []
    if hasattr(embeddings, "float_"):
        return list(embeddings.float_)
    if hasattr(embeddings, "float"):
        return list(embeddings.float)
    if isinstance(embeddings, list):
        return embeddings
    return []
