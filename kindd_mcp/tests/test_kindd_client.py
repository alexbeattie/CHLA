"""Tests for the thin HTTP client wrapping the public KiNDD REST API."""

import pytest


class _FakeResponse:
    def __init__(self, status_code, json_data=None):
        self.status_code = status_code
        self._json_data = json_data

    def json(self):
        return self._json_data

    def raise_for_status(self):
        if self.status_code >= 400:
            raise RuntimeError(f"HTTP {self.status_code}")


class _FakeSession:
    def __init__(self, response):
        self.response = response
        self.calls = []

    def get(self, url, params=None, timeout=None):
        self.calls.append({"url": url, "params": params, "timeout": timeout})
        return self.response


def test_search_providers_calls_comprehensive_search_endpoint_with_mapped_params():
    from kindd_mcp.kindd_client import KinddClient

    fake_session = _FakeSession(_FakeResponse(200, json_data=[]))
    client = KinddClient(base_url="https://api.kinddhelp.com/api", session=fake_session)

    client.search_providers(
        query="speech therapy",
        zip_code="90001",
        radius_miles=20,
        insurance=["Medi-Cal", "Regional Center"],
        diagnosis="autism",
        age="4",
        specialization="ABA",
    )

    assert len(fake_session.calls) == 1
    call = fake_session.calls[0]
    assert call["url"] == "https://api.kinddhelp.com/api/providers-v2/comprehensive_search/"
    assert call["params"]["q"] == "speech therapy"
    assert call["params"]["location"] == "90001"
    assert call["params"]["radius"] == 20
    assert call["params"]["insurance"] == ["Medi-Cal", "Regional Center"]
    assert call["params"]["diagnosis"] == "autism"
    assert call["params"]["age"] == "4"
    assert call["params"]["specialization"] == "ABA"


def test_search_providers_returns_parsed_provider_list():
    from kindd_mcp.kindd_client import KinddClient

    providers = [{"name": "Example Therapy Center"}]
    fake_session = _FakeSession(_FakeResponse(200, json_data=providers))
    client = KinddClient(base_url="https://api.kinddhelp.com/api", session=fake_session)

    result = client.search_providers(zip_code="90001")

    assert result == providers


def test_find_regional_center_by_zip_returns_dict_on_success():
    from kindd_mcp.kindd_client import KinddClient

    center = {"regional_center": "South Central Los Angeles Regional Center"}
    fake_session = _FakeSession(_FakeResponse(200, json_data=center))
    client = KinddClient(base_url="https://api.kinddhelp.com/api", session=fake_session)

    result = client.find_regional_center_by_zip("90001")

    assert result == center
    assert fake_session.calls[0]["params"] == {"zip_code": "90001"}


def test_find_regional_center_by_zip_returns_none_when_not_found():
    from kindd_mcp.kindd_client import KinddClient

    fake_session = _FakeSession(_FakeResponse(404, json_data={"error": "not found"}))
    client = KinddClient(base_url="https://api.kinddhelp.com/api", session=fake_session)

    result = client.find_regional_center_by_zip("00000")

    assert result is None


def test_find_regional_center_by_zip_raises_on_server_error():
    from kindd_mcp.kindd_client import KinddClient

    fake_session = _FakeSession(_FakeResponse(500, json_data={"error": "boom"}))
    client = KinddClient(base_url="https://api.kinddhelp.com/api", session=fake_session)

    with pytest.raises(RuntimeError):
        client.find_regional_center_by_zip("90001")
