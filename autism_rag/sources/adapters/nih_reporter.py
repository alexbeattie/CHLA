"""NIH RePORTER API v2 adapter.

We search the projects endpoint for autism-relevant grants. The response
includes project abstracts, PI info, organization, and fiscal year, which
is enough context for a useful "what funded research exists?" RAG channel.
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

REPORTER_URL = "https://api.reporter.nih.gov/v2/projects/search"


class NIHReporterAdapter(BaseAdapter):
    source_key = "nih_reporter"

    @retry(
        wait=wait_exponential(multiplier=1, min=1, max=10),
        stop=stop_after_attempt(3),
        reraise=True,
    )
    def fetch(self, query: str, *, limit: int = 25, **_: object) -> Iterable[SourceDocument]:
        search_text = query or "autism"
        payload = {
            "criteria": {
                "advanced_text_search": {
                    "operator": "advanced",
                    # RePORTER accepts comma-separated fields poorly in some
                    # clients; "all" keeps the API payload stable while still
                    # sorting by relevance.
                    "search_field": "all",
                    "search_text": _to_boolean_query(search_text),
                }
            },
            "include_fields": [
                "ProjectNum",
                "ProjectTitle",
                "AbstractText",
                "ContactPiName",
                "Organization",
                "FiscalYear",
                "ProjectStartDate",
                "ProjectEndDate",
                "AgencyIcAdmin",
                "AwardAmount",
                "PrincipalInvestigators",
            ],
            "limit": min(limit, 500),
            "offset": 0,
        }
        data = _http.post_json(REPORTER_URL, json=payload)
        return list(self._normalize(data.get("results", [])))

    def _normalize(self, results: list[dict]) -> Iterable[SourceDocument]:
        for record in results:
            project_num = record.get("project_num") or record.get("ProjectNum")
            abstract = record.get("abstract_text") or record.get("AbstractText") or ""
            title = record.get("project_title") or record.get("ProjectTitle") or ""
            if not project_num or not abstract.strip():
                continue

            pis: list[str] = []
            for pi in record.get("principal_investigators", []) or []:
                full_name = pi.get("full_name")
                if full_name:
                    pis.append(full_name)
            if not pis and record.get("contact_pi_name"):
                pis.append(record["contact_pi_name"])

            organization = (record.get("organization") or {}).get("org_name", "")
            fiscal_year = record.get("fiscal_year")
            published_at: datetime | None = None
            if fiscal_year and str(fiscal_year).isdigit():
                published_at = datetime(int(fiscal_year), 1, 1, tzinfo=timezone.utc)

            text = "\n\n".join(
                p for p in [
                    f"Title: {title}",
                    f"Principal investigators: {', '.join(pis)}" if pis else "",
                    f"Organization: {organization}" if organization else "",
                    f"Fiscal year: {fiscal_year}" if fiscal_year else "",
                    f"Abstract: {abstract.strip()}",
                ]
                if p
            )
            url = f"https://reporter.nih.gov/project-details/{project_num}"
            yield SourceDocument(
                source_key=self.source_key,
                source_id=str(project_num),
                title=title or f"NIH project {project_num}",
                text=text,
                url=url,
                evidence_type=EvidenceType.GRANT,
                access_class=AccessClass.PUBLIC_OPEN,
                published_at=published_at,
                authors=pis,
                citation_ids={"NIH_PROJECT": str(project_num)},
                extra={
                    "organization": organization,
                    "agency_ic_admin": record.get("agency_ic_admin", {}),
                    "fiscal_year": fiscal_year,
                    "award_amount": record.get("award_amount"),
                },
            )


def _to_boolean_query(query: str) -> str:
    """Make RePORTER advanced search require autism relevance.

    The plain API search behaves like a broad OR and can return unrelated
    awards. Require an autism term, then optionally require the user's other
    terms when they are specific enough.
    """

    lowered = query.lower()
    autism_clause = '("autism spectrum disorder" OR autism OR ASD)'
    extras = []
    for term in ("gene", "genes", "genetic", "genetics", "genomics", "discovery"):
        if term in lowered:
            extras.append(term)
    if extras:
        extra_clause = "(" + " OR ".join(f'"{term}"' for term in sorted(set(extras))) + ")"
        return f'"autism spectrum disorder" AND {extra_clause}'
    return autism_clause
