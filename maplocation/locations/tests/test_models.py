"""
Tests for location models.
"""

import pytest
from locations.models import RegionalCenter, ProviderV2


@pytest.mark.django_db
class TestRegionalCenter:
    """Tests for RegionalCenter model."""

    def test_create_regional_center(self, sample_regional_center):
        """Test creating a regional center."""
        assert sample_regional_center.id is not None
        assert sample_regional_center.regional_center == "Test Regional Center"

    def test_zip_codes_field(self, sample_regional_center):
        """Test ZIP codes JSON field."""
        assert isinstance(sample_regional_center.zip_codes, list)
        assert "90001" in sample_regional_center.zip_codes

    def test_find_by_zip_code(self, sample_regional_center):
        """Test finding regional center by ZIP code."""
        center = RegionalCenter.find_by_zip_code("90001")
        assert center is not None
        assert center.id == sample_regional_center.id

    def test_find_by_invalid_zip_code(self, sample_regional_center):
        """Test finding regional center with invalid ZIP."""
        center = RegionalCenter.find_by_zip_code("99999")
        assert center is None


@pytest.mark.django_db
class TestProviderV2:
    """Tests for ProviderV2 model."""

    def test_create_provider(self, sample_provider):
        """Test creating a provider."""
        assert sample_provider.id is not None
        assert sample_provider.name == "Test Provider"

    def test_provider_insurance_flags(self, sample_provider):
        """Test provider insurance acceptance flags."""
        assert sample_provider.accepts_insurance is True
        assert sample_provider.accepts_private_pay is True
        assert sample_provider.accepts_regional_center is True

    def test_provider_coordinates(self, sample_provider):
        """Test provider has valid coordinates."""
        assert sample_provider.latitude is not None
        assert sample_provider.longitude is not None
        assert -90 <= sample_provider.latitude <= 90
        assert -180 <= sample_provider.longitude <= 180
