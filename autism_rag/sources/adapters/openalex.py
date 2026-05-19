"""OpenAlex adapter for citation-graph-aware literature ingestion.

Used primarily to find recent/highly-cited autism research that PubMed may
not surface first, and to provide a citation graph (referenced_works,
related_works) for downstream enrichment.
"""

from __future__ import annotations

import logging
from collections.abc import Iterable
from datetime import datetime, timezone

from tenacity import retry, stop_after_attempt, wait_exponential

from ..models import AccessClass, EvidenceType, SourceDocument
from . import _http
from .base import BaseAdapter

logger = logging.getLogger(__name__)

OPENALEX_URL = "https://api.openalex.org/works"


class OpenAlexAdapter(BaseAdapter):
    source_key = "openalex"

    @retry(
        wait=wait_exponential(multiplier=1, min=1, max=10),
        stop=stop_after_attempt(3),
        reraise=True,
    )
    def fetch(self, query: str, *, limit: int = 25, **_: object) -> Iterable[SourceDocument]:
        params = {
            "search": query or "autism spectrum disorder",
            "per_page": min(limit, 200),
            "sort": "cited_by_count:desc",
            "filter": "type:article,has_abstract:true",
        }
        headers: dict[str, str] = {}
        if self.settings.ncbi_email:
            params["mailto"] = self.settings.ncbi_email
        data = _http.get_json(OPENALEX_URL, params=params, headers=headers)
        return list(self._normalize(data.get("results", [])))

    def _normalize(self, results: list[dict]) -> Iterable[SourceDocument]:
        for work in results:
            work_id = (work.get("id") or "").rsplit("/", 1)[-1]
            if not work_id:
                continue
            title = work.get("title") or work.get("display_name") or ""
            abstract = _reconstruct_abstract(work.get("abstract_inverted_index"))
            if not abstract:
                continue
            authors = [
                a.get("author", {}).get("display_name", "")
                for a in work.get("authorships", [])
                if a.get("author")
            ]
            authors = [a for a in authors if a]
            pub_date = work.get("publication_date")
            published_at = _parse_date(pub_date)
            doi = (work.get("doi") or "").replace("https://doi.org/", "")
            citation_ids = {"OPENALEX": work_id}
            if doi:
                citation_ids["DOI"] = doi

            text_parts = [
                f"Title: {title}",
                f"Authors: {', '.join(authors)}" if authors else "",
                f"Abstract: {abstract}",
            ]
            text = "\n\n".join(p for p in text_parts if p)

            yield SourceDocument(
                source_key=self.source_key,
                source_id=work_id,
                title=title,
                text=text,
                url=work.get("id", ""),
                evidence_type=EvidenceType.LITERATURE,
                access_class=AccessClass.PUBLIC_OPEN,
                published_at=published_at,
                authors=authors,
                citation_ids=citation_ids,
                extra={
                    "cited_by_count": work.get("cited_by_count"),
                    "doi": doi,
                    "host_venue": (work.get("primary_location") or {}).get("source", {}).get("display_name", ""),
                },
            )


def _reconstruct_abstract(inverted: dict | None) -> str:
    if not inverted:
        return ""
    # OpenAlex stores abstracts as ``{word: [positions...]}``. Rebuild the
    # ordered text by placing each word at every position it occupies.
    positions: list[tuple[int, str]] = []
    for word, idxs in inverted.items():
        for idx in idxs:
            positions.append((idx, word))
    positions.sort()
    return " ".join(word for _, word in positions)


def _parse_date(value: str | None) -> datetime | None:
    if not value:
        return None
    try:
        return datetime.strptime(value, "%Y-%m-%d").replace(tzinfo=timezone.utc)
    except ValueError:
        return None
