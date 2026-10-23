"""Use pytest-django's real test-database lifecycle for this focused suite."""

from pytest_django.fixtures import django_db_setup


__all__ = ("django_db_setup",)
