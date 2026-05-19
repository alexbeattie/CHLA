"""Shared HTTP session helper for adapters.

We use ``requests`` because some public research APIs (e.g.
ClinicalTrials.gov via Cloudflare) reject httpx's TLS fingerprint. Centralizing
the session here keeps headers, timeouts, and retry policy consistent.
"""

from __future__ import annotations

from typing import Any

import requests

DEFAULT_TIMEOUT = 30.0
DEFAULT_UA = "Mozilla/5.0 (compatible; autism_research_rag/0.1)"


def session(*, headers: dict[str, str] | None = None) -> requests.Session:
    s = requests.Session()
    s.headers.update({"User-Agent": DEFAULT_UA, "Accept": "application/json"})
    if headers:
        s.headers.update(headers)
    return s


def get_json(url: str, *, params: dict[str, Any] | None = None, headers: dict[str, str] | None = None, timeout: float = DEFAULT_TIMEOUT) -> Any:
    with session(headers=headers) as s:
        response = s.get(url, params=params, timeout=timeout)
        response.raise_for_status()
        return response.json()


def post_json(url: str, *, json: Any, headers: dict[str, str] | None = None, timeout: float = DEFAULT_TIMEOUT) -> Any:
    with session(headers=headers) as s:
        response = s.post(url, json=json, timeout=timeout)
        response.raise_for_status()
        return response.json()


def get_text(url: str, *, params: dict[str, Any] | None = None, headers: dict[str, str] | None = None, timeout: float = DEFAULT_TIMEOUT) -> str:
    with session(headers=headers) as s:
        response = s.get(url, params=params, timeout=timeout)
        response.raise_for_status()
        return response.text
