"""
Pytest configuration and fixtures for the entire project.
"""
import pytest
from django.conf import settings


@pytest.fixture(scope='session')
def django_db_setup():
    """Set up test database configuration."""
    settings.DATABASES['default'] = {
        'ENGINE': 'django.db.backends.postgresql',
        'NAME': 'test_db',
        'USER': 'postgres',
        'PASSWORD': 'postgres',
        'HOST': 'localhost',
        'PORT': '5432',
    }


@pytest.fixture
def api_client():
    """Create a DRF API client for testing."""
    from rest_framework.test import APIClient
    return APIClient()


@pytest.fixture
def sample_regional_center(db):
    """Create a sample regional center for testing."""
    from locations.models import RegionalCenter
    return RegionalCenter.objects.create(
        regional_center='Test Regional Center',
        address='123 Test St',
        city='Los Angeles',
        state='CA',
        zip_code='90001',
        telephone='555-1234',
        latitude=34.0522,
        longitude=-118.2437,
        is_la_regional_center=True,
        zip_codes=['90001', '90002', '90003']
    )


@pytest.fixture
def sample_provider(db):
    """Create a sample provider for testing."""
    from locations.models import ProviderV2
    return ProviderV2.objects.create(
        name='Test Provider',
        type='Therapy',
        address='456 Provider Ave, Los Angeles, CA 90001',
        latitude=34.0522,
        longitude=-118.2437,
        accepts_insurance=True,
        accepts_private_pay=True,
        accepts_regional_center=True
    )
