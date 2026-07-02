"""Thin HTTP client wrapping the public KiNDD REST API.

This module intentionally stays free of any MCP-specific concerns so it can
be tested and reused independently of the tool/transport layer in
``kindd_mcp.server``.
"""

from __future__ import annotations

import os
from typing import Any, Optional

import requests

DEFAULT_BASE_URL = "https://api.kinddhelp.com/api"
DEFAULT_TIMEOUT_SECONDS = 10.0


class KinddClient:
    """Small wrapper around the public KiNDD provider/regional-center API."""

    def __init__(
        self,
        base_url: Optional[str] = None,
        session: Optional[Any] = None,
        timeout: float = DEFAULT_TIMEOUT_SECONDS,
    ) -> None:
        self.base_url = (
            base_url or os.environ.get("KINDD_API_BASE_URL", DEFAULT_BASE_URL)
        ).rstrip("/")
        self.session = session or requests.Session()
        self.timeout = timeout

    def search_providers(
        self,
        *,
        query: str = "",
        zip_code: Optional[str] = None,
        radius_miles: float = 15,
        insurance: Optional[list[str]] = None,
        diagnosis: Optional[str] = None,
        age: Optional[str] = None,
        specialization: Optional[str] = None,
    ) -> list[dict[str, Any]]:
        """Search providers via ``/providers-v2/comprehensive_search/``."""
        params: dict[str, Any] = {"q": query, "radius": radius_miles}
        if zip_code:
            params["location"] = zip_code
        if insurance:
            params["insurance"] = insurance
        if diagnosis:
            params["diagnosis"] = diagnosis
        if age:
            params["age"] = age
        if specialization:
            params["specialization"] = specialization

        response = self.session.get(
            f"{self.base_url}/providers-v2/comprehensive_search/",
            params=params,
            timeout=self.timeout,
        )
        response.raise_for_status()
        return response.json()

    def find_regional_center_by_zip(self, zip_code: str) -> Optional[dict[str, Any]]:
        """Look up the Regional Center serving a ZIP code, or ``None``."""
        response = self.session.get(
            f"{self.base_url}/regional-centers/by_zip_code/",
            params={"zip_code": zip_code},
            timeout=self.timeout,
        )
        if response.status_code == 404:
            return None
        response.raise_for_status()
        return response.json()
