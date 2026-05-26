"""ClinicalTrials.gov API v2 adapter.

The v2 API returns JSON with structured "protocolSection" subfields. We
flatten the most useful sections (eligibility, conditions, interventions,
description, outcomes) into a single document, with the NCT identifier as
the source-side primary key.
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

CT_API_BASE = "https://clinicaltrials.gov/api/v2/studies"


class ClinicalTrialsAdapter(BaseAdapter):
    source_key = "clinicaltrials"

    @retry(
        wait=wait_exponential(multiplier=1, min=1, max=10),
        stop=stop_after_attempt(3),
        reraise=True,
    )
    def fetch(
        self,
        query: str,
        *,
        limit: int = 25,
        location: str | None = None,
        **_: object,
    ) -> Iterable[SourceDocument]:
        params: dict[str, str | int] = {
            "query.cond": query or "autism spectrum disorder",
            "pageSize": min(limit, 100),
            "format": "json",
        }
        if location:
            params["query.locn"] = location
        data = _http.get_json(CT_API_BASE, params=params)
        studies = data.get("studies", [])
        return list(self._normalize(studies))

    def _normalize(self, studies: list[dict]) -> Iterable[SourceDocument]:
        for study in studies:
            protocol = study.get("protocolSection", {})
            identification = protocol.get("identificationModule", {})
            nct_id = identification.get("nctId")
            if not nct_id:
                continue

            title = (
                identification.get("officialTitle")
                or identification.get("briefTitle")
                or f"ClinicalTrials.gov {nct_id}"
            )

            description = protocol.get("descriptionModule", {})
            conditions = protocol.get("conditionsModule", {}).get("conditions", [])
            arms = protocol.get("armsInterventionsModule", {})
            interventions = [
                i.get("name", "")
                for i in arms.get("interventions", [])
                if i.get("name")
            ]
            eligibility = protocol.get("eligibilityModule", {})
            outcomes = protocol.get("outcomesModule", {})
            status = protocol.get("statusModule", {})
            locations = protocol.get("contactsLocationsModule", {}).get("locations", [])

            parts = [
                f"Title: {title}",
                f"Conditions: {', '.join(conditions)}" if conditions else "",
                f"Interventions: {', '.join(interventions)}" if interventions else "",
                f"Locations: {_format_locations(locations)}" if locations else "",
                f"Brief summary: {description.get('briefSummary', '')}",
                f"Detailed description: {description.get('detailedDescription', '')}",
                f"Eligibility criteria: {eligibility.get('eligibilityCriteria', '')}",
                f"Primary outcomes: {_format_outcomes(outcomes.get('primaryOutcomes', []))}",
                f"Secondary outcomes: {_format_outcomes(outcomes.get('secondaryOutcomes', []))}",
                f"Overall status: {status.get('overallStatus', '')}",
            ]
            text = "\n\n".join(p for p in parts if p and not p.endswith(": "))
            if not text.strip():
                continue

            start_date = status.get("startDateStruct", {}).get("date")
            published_at = _parse_date(start_date)

            url = f"https://clinicaltrials.gov/study/{nct_id}"
            yield SourceDocument(
                source_key=self.source_key,
                source_id=nct_id,
                title=title,
                text=text,
                url=url,
                evidence_type=EvidenceType.CLINICAL_TRIAL,
                access_class=AccessClass.PUBLIC_OPEN,
                published_at=published_at,
                citation_ids={"NCT": nct_id},
                extra={
                    "status": status.get("overallStatus", ""),
                    "phase": protocol.get("designModule", {}).get("phases", []),
                    "study_type": protocol.get("designModule", {}).get("studyType", ""),
                    "conditions": conditions,
                    "interventions": interventions,
                    **_location_metadata(locations),
                },
            )


def _format_outcomes(outcomes: list[dict]) -> str:
    parts: list[str] = []
    for outcome in outcomes:
        measure = outcome.get("measure", "")
        time_frame = outcome.get("timeFrame", "")
        if measure:
            parts.append(f"{measure} ({time_frame})" if time_frame else measure)
    return "; ".join(parts)


def _format_locations(locations: list[dict]) -> str:
    parts: list[str] = []
    for location in locations[:12]:
        facility = location.get("facility", "")
        city = location.get("city", "")
        state = location.get("state", "")
        country = location.get("country", "")
        status = location.get("status", "")
        place = ", ".join(part for part in [city, state, country] if part)
        label = " - ".join(part for part in [facility, place, status] if part)
        if label:
            parts.append(label)
    return "; ".join(parts)


def _location_metadata(locations: list[dict]) -> dict[str, list[str]]:
    metadata: dict[str, list[str]] = {
        "location_facilities": [],
        "location_cities": [],
        "location_states": [],
        "location_zips": [],
        "location_countries": [],
        "location_statuses": [],
        "location_coordinates": [],
    }
    for location in locations[:25]:
        geo = location.get("geoPoint") or {}
        _append_unique(metadata["location_facilities"], location.get("facility", ""))
        _append_unique(metadata["location_cities"], location.get("city", ""))
        _append_unique(metadata["location_states"], location.get("state", ""))
        _append_unique(metadata["location_zips"], location.get("zip", ""))
        _append_unique(metadata["location_countries"], location.get("country", ""))
        _append_unique(metadata["location_statuses"], location.get("status", ""))
        if geo.get("lat") is not None and geo.get("lon") is not None:
            _append_unique(metadata["location_coordinates"], f"{geo['lat']},{geo['lon']}")
    return {key: value for key, value in metadata.items() if value}


def _append_unique(values: list[str], value: object) -> None:
    if value is None:
        return
    text = str(value).strip()
    if text and text not in values:
        values.append(text)


def _parse_date(value: str | None) -> datetime | None:
    if not value:
        return None
    for fmt in ("%Y-%m-%d", "%Y-%m", "%Y"):
        try:
            return datetime.strptime(value, fmt).replace(tzinfo=timezone.utc)
        except ValueError:
            continue
    return None
