"""
Tests for API endpoints.
"""

import pytest
from django.urls import reverse

from locations.models import Location, LocationCategory


@pytest.mark.django_db
class TestRegionalCenterAPI:
    """Tests for Regional Center API endpoints."""

    def test_list_regional_centers(self, api_client, sample_regional_center):
        """Test listing regional centers."""
        url = reverse("regionalcenter-list")
        response = api_client.get(url)
        assert response.status_code == 200
        data = response.json()
        assert data["count"] >= 1

    def test_get_regional_center_detail(self, api_client, sample_regional_center):
        """Test getting regional center detail."""
        url = reverse("regionalcenter-detail", args=[sample_regional_center.id])
        response = api_client.get(url)
        assert response.status_code == 200
        data = response.json()
        assert data["regional_center"] == "Test Regional Center"

    def test_service_area_boundaries(self, api_client, sample_regional_center):
        """Test service area boundaries endpoint."""
        url = reverse("regionalcenter-service-area-boundaries")
        response = api_client.get(url)
        assert response.status_code == 200
        data = response.json()
        assert data["type"] == "FeatureCollection"
        assert "features" in data


@pytest.mark.django_db
class TestProviderAPI:
    """Tests for Provider API endpoints."""

    def test_list_providers(self, api_client, sample_provider):
        """Test listing providers."""
        url = reverse("providers-v2-list")
        response = api_client.get(url)
        assert response.status_code == 200
        data = response.json()
        assert data["count"] >= 1

    def test_get_provider_detail(self, api_client, sample_provider):
        """Test getting provider detail."""
        url = reverse("providers-v2-detail", args=[sample_provider.id])
        response = api_client.get(url)
        assert response.status_code == 200
        data = response.json()
        assert data["name"] == "Test Provider"

    def test_comprehensive_search(self, api_client, sample_provider):
        """Test comprehensive provider search."""
        url = reverse("providers-v2-comprehensive-search")
        response = api_client.get(url, {"q": "Test"})
        assert response.status_code == 200
        data = response.json()
        assert len(data) >= 1

    def test_comprehensive_search_with_location(self, api_client, sample_provider):
        """Test comprehensive search with location."""
        url = reverse("providers-v2-comprehensive-search")
        response = api_client.get(
            url, {"lat": "34.0522", "lng": "-118.2437", "radius": "10"}
        )
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)


@pytest.mark.django_db
class TestLocationAPI:
    """Tests for Location API endpoints."""

    def test_list_playground_locations(self, api_client):
        """Test listing inclusive playground locations."""
        category = LocationCategory.objects.create(name="Inclusive Playgrounds")
        Location.objects.create(
            name="Test Playground",
            address="123 Play St",
            city="Los Angeles",
            state="CA",
            zip_code="90001",
            latitude=34.0522,
            longitude=-118.2437,
            category=category,
            is_accessible=True,
        )

        url = reverse("location-list")
        response = api_client.get(url, {"category": category.id})

        assert response.status_code == 200
        data = response.json()
        assert data["count"] == 1
        assert data["results"][0]["name"] == "Test Playground"
