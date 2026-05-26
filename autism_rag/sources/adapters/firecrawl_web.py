"""Firecrawl adapter for explicitly permitted web pages.

We do NOT bulk-crawl arbitrary autism sites. Each URL must be added to an
explicit allowlist and conform to the site's terms. Firecrawl returns
clean Markdown which we ingest as a single document per URL.
"""

from __future__ import annotations

import logging
from collections.abc import Iterable
from datetime import datetime, timezone

import requests
from tenacity import retry, stop_after_attempt, wait_exponential

from ..models import AccessClass, EvidenceType, SourceDocument
from .base import BaseAdapter

logger = logging.getLogger(__name__)

FIRECRAWL_SCRAPE_URL = "https://api.firecrawl.dev/v2/scrape"


class FirecrawlWebAdapter(BaseAdapter):
    source_key = "firecrawl_web"
    evidence_type = EvidenceType.WEB
    access_class = AccessClass.PUBLIC_OPEN

    def fetch(
        self,
        query: str = "",
        *,
        limit: int = 10,
        urls: list[str] | None = None,
        **_: object,
    ) -> Iterable[SourceDocument]:
        if not self.settings.firecrawl_api_key:
            logger.warning(
                "Firecrawl: no FIRECRAWL_API_KEY set; skipping web ingestion."
            )
            return []
        if not urls:
            logger.info("Firecrawl: no URLs supplied; nothing to do.")
            return []
        urls = urls[:limit]
        results: list[SourceDocument] = []
        for url in urls:
            doc = self._scrape(url)
            if doc is not None:
                results.append(doc)
        return results

    @retry(
        wait=wait_exponential(multiplier=1, min=1, max=10),
        stop=stop_after_attempt(2),
        reraise=False,
    )
    def _scrape(self, url: str) -> SourceDocument | None:
        headers = {
            "Authorization": f"Bearer {self.settings.firecrawl_api_key}",
            "Content-Type": "application/json",
        }
        payload = {"url": url, "formats": ["markdown"]}
        response = requests.post(FIRECRAWL_SCRAPE_URL, headers=headers, json=payload, timeout=60.0)
        if response.status_code >= 400:
            logger.warning("Firecrawl %s -> %s %s", url, response.status_code, response.text[:200])
            return None
        data = response.json()
        body = (data.get("data") or {}).get("markdown") or ""
        if not body.strip():
            logger.info("Firecrawl: empty body for %s", url)
            return None
        metadata = (data.get("data") or {}).get("metadata") or {}
        title = metadata.get("title") or url
        return SourceDocument(
            source_key=self.source_key,
            source_id=url,
            title=title,
            text=body,
            url=url,
            evidence_type=self.evidence_type,
            access_class=self.access_class,
            published_at=datetime.now(timezone.utc),
            citation_ids={"URL": url},
            extra={"source_site": metadata.get("sourceURL", url)},
        )


class NDAMetadataAdapter(FirecrawlWebAdapter):
    """Firecrawl-backed public metadata ingestion for NIMH NDA pages."""

    source_key = "nda_metadata"
    evidence_type = EvidenceType.DATASET_METADATA
    access_class = AccessClass.CONTROLLED_METADATA


class SFARIBaseMetadataAdapter(FirecrawlWebAdapter):
    """Firecrawl-backed public metadata ingestion for SFARI Base/SPARK pages."""

    source_key = "sfari_base_metadata"
    evidence_type = EvidenceType.DATASET_METADATA
    access_class = AccessClass.CONTROLLED_METADATA


class DbGaPMetadataAdapter(FirecrawlWebAdapter):
    """Firecrawl-backed public metadata ingestion for dbGaP autism study pages."""

    source_key = "dbgap_metadata"
    evidence_type = EvidenceType.DATASET_METADATA
    access_class = AccessClass.CONTROLLED_METADATA
