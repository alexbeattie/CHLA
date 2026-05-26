"""Tests for inclusive playground import command."""

from io import StringIO

import pytest
from django.core.management import call_command

from locations.models import Location, LocationCategory


@pytest.mark.django_db
def test_import_inclusive_playgrounds_creates_location(tmp_path):
    csv_path = tmp_path / "playgrounds.csv"
    csv_path.write_text(
        "\n".join(
            [
                "name,opened_date,venue,address,city,state,zip_code,source,notes,latitude,longitude",
                "Test Playground,1/1/23,Test Park,123 Play St,Los Angeles,CA,90001,test.pdf,,34.0522,-118.2437",
            ]
        ),
        encoding="utf-8",
    )

    output = StringIO()
    call_command("import_inclusive_playgrounds", "--file", str(csv_path), stdout=output)

    category = LocationCategory.objects.get(name="Inclusive Playgrounds")
    location = Location.objects.get(name="Test Playground", category=category)
    assert location.city == "Los Angeles"
    assert location.is_accessible is True
    assert location.location is not None


@pytest.mark.django_db
def test_import_inclusive_playgrounds_dry_run_does_not_create_location(tmp_path):
    csv_path = tmp_path / "playgrounds.csv"
    csv_path.write_text(
        "\n".join(
            [
                "name,opened_date,venue,address,city,state,zip_code,source,notes,latitude,longitude",
                "Dry Run Playground,1/1/23,Test Park,123 Play St,Los Angeles,CA,90001,test.pdf,,34.0522,-118.2437",
            ]
        ),
        encoding="utf-8",
    )

    output = StringIO()
    call_command(
        "import_inclusive_playgrounds",
        "--file",
        str(csv_path),
        "--dry-run",
        stdout=output,
    )

    assert "Would import: Dry Run Playground" in output.getvalue()
    assert not Location.objects.filter(name="Dry Run Playground").exists()
