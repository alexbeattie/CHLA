import uuid

from django.db import connection
from django.db.migrations.executor import MigrationExecutor
from django.test import override_settings
import pytest


@pytest.mark.django_db(transaction=True)
def test_llm_initial_migration_applies_in_an_isolated_schema():
    """Execute llm.0001 even though the general pytest suite uses --nomigrations."""
    schema_name = f"llm_migration_test_{uuid.uuid4().hex}"
    quoted_schema = connection.ops.quote_name(schema_name)

    with connection.cursor() as cursor:
        cursor.execute(f"CREATE SCHEMA {quoted_schema}")

    try:
        with connection.cursor() as cursor:
            cursor.execute(f"SET search_path TO {quoted_schema}")

        with override_settings(MIGRATION_MODULES={"llm": "llm.migrations"}):
            executor = MigrationExecutor(connection)
            executor.migrate([("llm", "0001_initial")])

        table_names = set(connection.introspection.table_names())
        assert "llm_assistantresponsereport" in table_names
        assert "llm_responsereportthrottlewindow" in table_names
    finally:
        with connection.cursor() as cursor:
            cursor.execute("RESET search_path")
            cursor.execute(f"DROP SCHEMA IF EXISTS {quoted_schema} CASCADE")
