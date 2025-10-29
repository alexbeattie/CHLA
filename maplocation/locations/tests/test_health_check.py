"""
Tests for health check endpoint.
"""

import pytest
from django.urls import reverse


@pytest.mark.django_db
class TestHealthCheck:
    """Tests for the health check endpoint."""

    def test_health_check_returns_200(self, api_client):
        """Test that health check returns 200 OK."""
        url = reverse("health-check")
        response = api_client.get(url)
        assert response.status_code == 200

    def test_health_check_returns_json(self, api_client):
        """Test that health check returns JSON."""
        url = reverse("health-check")
        response = api_client.get(url)
        assert response["Content-Type"] == "application/json"

    def test_health_check_contains_status(self, api_client):
        """Test that health check contains status field."""
        url = reverse("health-check")
        response = api_client.get(url)
        data = response.json()
        assert "status" in data
        assert data["status"] == "healthy"

    def test_health_check_contains_database_status(self, api_client):
        """Test that health check contains database connection status."""
        url = reverse("health-check")
        response = api_client.get(url)
        data = response.json()
        assert "database" in data
        assert data["database"] == "connected"

    def test_health_check_contains_provider_count(self, api_client, sample_provider):
        """Test that health check returns provider count."""
        url = reverse("health-check")
        response = api_client.get(url)
        data = response.json()
        assert "providers" in data
        assert data["providers"] >= 1

    def test_health_check_contains_version(self, api_client):
        """Test that health check returns version."""
        url = reverse("health-check")
        response = api_client.get(url)
        data = response.json()
        assert "version" in data
