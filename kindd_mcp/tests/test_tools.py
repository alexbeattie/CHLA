"""Tests for the MCP tool formatting/wrapper layer.

These tests exercise ``kindd_mcp.tools`` against a fake client so the
LLM-facing formatting logic can be verified without any HTTP calls or a
running FastMCP server.
"""


class _FakeClient:
    def __init__(self, providers=None, regional_center=None):
        self._providers = providers if providers is not None else []
        self._regional_center = regional_center
        self.search_calls = []
        self.lookup_calls = []

    def search_providers(self, **kwargs):
        self.search_calls.append(kwargs)
        return self._providers

    def find_regional_center_by_zip(self, zip_code):
        self.lookup_calls.append(zip_code)
        return self._regional_center


def test_search_providers_tool_returns_compact_fields_and_counts():
    from kindd_mcp.tools import search_providers_tool

    providers = [
        {
            "name": "Example Therapy Center",
            "type": "ABA Therapy",
            "phone": "555-0100",
            "website": "https://example.org",
            "address": "123 Main St, Los Angeles, CA",
            "insurance_carriers": ["Medi-Cal", "Regional Center"],
            "specializations": ["Autism", "ADHD"],
            "age_groups_served": ["0-5", "6-12"],
            "distance": 3.2,
        }
    ]
    client = _FakeClient(providers=providers)

    result = search_providers_tool(client, zip_code="90001")

    assert result["total_matches"] == 1
    assert result["returned"] == 1
    provider = result["providers"][0]
    assert provider["name"] == "Example Therapy Center"
    assert provider["insurance_accepted"] == ["Medi-Cal", "Regional Center"]
    assert provider["distance_miles"] == 3.2
    assert client.search_calls[0]["zip_code"] == "90001"


def test_search_providers_tool_respects_limit_but_reports_total_matches():
    from kindd_mcp.tools import search_providers_tool

    providers = [{"name": f"Provider {i}"} for i in range(25)]
    client = _FakeClient(providers=providers)

    result = search_providers_tool(client, zip_code="90001", limit=5)

    assert result["total_matches"] == 25
    assert result["returned"] == 5
    assert len(result["providers"]) == 5


def test_search_providers_tool_falls_back_to_legacy_insurance_field():
    from kindd_mcp.tools import search_providers_tool

    providers = [{"name": "Legacy Provider", "insurance_accepted": "Medi-Cal"}]
    client = _FakeClient(providers=providers)

    result = search_providers_tool(client, zip_code="90001")

    assert result["providers"][0]["insurance_accepted"] == "Medi-Cal"


def test_find_regional_center_tool_returns_found_true_with_expected_fields():
    from kindd_mcp.tools import find_regional_center_tool

    center = {
        "regional_center": "South Central Los Angeles Regional Center",
        "telephone": "(213) 744-7000",
        "website": "www.sclarc.org",
        "address": "2500 South Western Avenue",
        "city": "Los Angeles",
        "state": "CA",
        "zip_code": "90018",
        "county_served": "Los Angeles",
    }
    client = _FakeClient(regional_center=center)

    result = find_regional_center_tool(client, "90001")

    assert result["found"] is True
    assert result["regional_center"] == "South Central Los Angeles Regional Center"
    assert result["phone"] == "(213) 744-7000"
    assert client.lookup_calls == ["90001"]


def test_find_regional_center_tool_returns_found_false_when_missing():
    from kindd_mcp.tools import find_regional_center_tool

    client = _FakeClient(regional_center=None)

    result = find_regional_center_tool(client, "00000")

    assert result["found"] is False
    assert result["zip_code"] == "00000"
    assert "00000" in result["message"]
