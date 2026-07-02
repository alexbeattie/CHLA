"""KiNDD MCP server.

Exposes read-only KiNDD Resource Navigator data (provider search and
Regional Center lookup) as MCP tools over Streamable HTTP, so AI clients
(Claude Desktop, Cursor, ChatGPT, or KiNDD's own agent) can query it
directly.

Run for local development:

    python3 -m kindd_mcp.server

Run for production behind an ASGI server (recommended):

    uvicorn kindd_mcp.server:app --host 0.0.0.0 --port 8800
"""

from __future__ import annotations

import os
from typing import Optional

from dotenv import load_dotenv
from fastmcp import FastMCP
from starlette.responses import JSONResponse

from .auth import build_auth_provider
from .kindd_client import KinddClient
from .tools import find_regional_center_tool, search_providers_tool

load_dotenv()

mcp = FastMCP("KiNDD Resource Navigator", auth=build_auth_provider())
_client = KinddClient()


@mcp.custom_route("/health", methods=["GET"])
async def health_check(request):  # noqa: ANN001 - Starlette request type
    return JSONResponse({"status": "ok", "service": "kindd-mcp"})


@mcp.tool()
def search_providers(
    zip_code: Optional[str] = None,
    query: str = "",
    radius_miles: float = 15,
    insurance: Optional[list[str]] = None,
    diagnosis: Optional[str] = None,
    age: Optional[str] = None,
    specialization: Optional[str] = None,
    limit: int = 10,
) -> dict:
    """Search KiNDD healthcare providers (ABA, speech, OT, and more).

    Args:
        zip_code: ZIP code or address to search near.
        query: Free-text search across provider name, type, and services.
        radius_miles: Search radius in miles (default 15).
        insurance: Insurance carrier names to filter by (e.g. "Medi-Cal").
        diagnosis: Diagnosis to filter by (e.g. "autism").
        age: Age or age-group to filter by (e.g. "4", "All Ages").
        specialization: Specialization/service type to filter by.
        limit: Maximum number of providers to return (default 10).

    Returns a compact summary with ``total_matches`` (how many matched
    before truncation) and ``providers`` (the trimmed, returned list).
    """
    return search_providers_tool(
        _client,
        query=query,
        zip_code=zip_code,
        radius_miles=radius_miles,
        insurance=insurance,
        diagnosis=diagnosis,
        age=age,
        specialization=specialization,
        limit=limit,
    )


@mcp.tool()
def find_regional_center_by_zip(zip_code: str) -> dict:
    """Find the California Regional Center that serves a given ZIP code.

    Args:
        zip_code: A 5-digit US ZIP code.

    Returns ``found`` (bool) plus Regional Center contact details, or a
    ``message`` explaining that no Regional Center was found.
    """
    return find_regional_center_tool(_client, zip_code)


app = mcp.http_app()


if __name__ == "__main__":
    mcp.run(
        transport="http",
        host=os.environ.get("KINDD_MCP_HOST", "0.0.0.0"),
        port=int(os.environ.get("KINDD_MCP_PORT", "8800")),
    )
