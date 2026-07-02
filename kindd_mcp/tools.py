"""LLM-facing tool formatting layer.

These functions take a ``KinddClient``-shaped object (anything exposing
``search_providers`` and ``find_regional_center_by_zip``) and shape the raw
API responses into compact dicts suited for MCP tool results. Keeping this
separate from ``kindd_mcp.server`` lets it be tested without a running
FastMCP server or network access.
"""

from __future__ import annotations

from typing import Any, Optional, Protocol

DEFAULT_RESULT_LIMIT = 10


class SupportsKinddLookups(Protocol):
    def search_providers(self, **kwargs: Any) -> list[dict[str, Any]]: ...

    def find_regional_center_by_zip(self, zip_code: str) -> Optional[dict[str, Any]]: ...


def format_provider_search_results(
    providers: list[dict[str, Any]],
    *,
    limit: int = DEFAULT_RESULT_LIMIT,
) -> dict[str, Any]:
    """Trim raw provider records into a compact, LLM-friendly shape."""
    trimmed = providers[:limit]
    formatted = [
        {
            "name": provider.get("name"),
            "type": provider.get("type"),
            "phone": provider.get("phone"),
            "website": provider.get("website"),
            "address": provider.get("address"),
            "insurance_accepted": provider.get("insurance_carriers")
            or provider.get("insurance_accepted"),
            "specializations": provider.get("specializations"),
            "age_groups_served": provider.get("age_groups_served"),
            "distance_miles": provider.get("distance"),
        }
        for provider in trimmed
    ]
    return {
        "total_matches": len(providers),
        "returned": len(formatted),
        "providers": formatted,
    }


def format_regional_center_result(
    center: Optional[dict[str, Any]],
    zip_code: str,
) -> dict[str, Any]:
    """Shape a Regional Center API record (or absence) for an MCP tool result."""
    if center is None:
        return {
            "found": False,
            "zip_code": zip_code,
            "message": f"No Regional Center found for ZIP code {zip_code}.",
        }
    return {
        "found": True,
        "regional_center": center.get("regional_center"),
        "phone": center.get("telephone"),
        "website": center.get("website"),
        "address": center.get("address"),
        "city": center.get("city"),
        "state": center.get("state"),
        "zip_code": center.get("zip_code"),
        "county_served": center.get("county_served"),
    }


def search_providers_tool(
    client: SupportsKinddLookups,
    *,
    query: str = "",
    zip_code: Optional[str] = None,
    radius_miles: float = 15,
    insurance: Optional[list[str]] = None,
    diagnosis: Optional[str] = None,
    age: Optional[str] = None,
    specialization: Optional[str] = None,
    limit: int = DEFAULT_RESULT_LIMIT,
) -> dict[str, Any]:
    """Search KiNDD providers and return a compact, LLM-friendly result."""
    providers = client.search_providers(
        query=query,
        zip_code=zip_code,
        radius_miles=radius_miles,
        insurance=insurance,
        diagnosis=diagnosis,
        age=age,
        specialization=specialization,
    )
    return format_provider_search_results(providers, limit=limit)


def find_regional_center_tool(
    client: SupportsKinddLookups,
    zip_code: str,
) -> dict[str, Any]:
    """Look up the Regional Center serving a ZIP code."""
    center = client.find_regional_center_by_zip(zip_code)
    return format_regional_center_result(center, zip_code)
