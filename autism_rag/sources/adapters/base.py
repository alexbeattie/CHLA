"""Common scaffolding shared by every ingestion adapter."""

from __future__ import annotations

from abc import ABC, abstractmethod
from collections.abc import Iterable

from ...config import Settings, get_settings
from ..models import SourceDocument, SourceRecord
from ..registry import get_source


class BaseAdapter(ABC):
    """Base class for source adapters.

    Subclasses implement :meth:`fetch` to yield normalized
    :class:`SourceDocument` instances. They should be polite about rate
    limits, propagate provenance fields, and avoid touching anything
    outside their declared source.
    """

    source_key: str = ""

    def __init__(self, settings: Settings | None = None) -> None:
        if not self.source_key:
            raise ValueError(f"{type(self).__name__} must set source_key")
        self.settings = settings or get_settings()
        self.source: SourceRecord = get_source(self.source_key)

    @abstractmethod
    def fetch(self, query: str, *, limit: int = 25, **kwargs) -> Iterable[SourceDocument]:
        """Yield normalized documents for the given query.

        ``query`` and ``limit`` are intentionally generic so the ingestion
        CLI can drive every adapter the same way. Adapters can accept
        additional, source-specific options via ``**kwargs``.
        """
