"""Tests for the import_csv_providers management command.

These run without a database: the ProviderV2 manager is stubbed so we can
assert exactly which fields the command assigns. The point under test is
that columns absent from the CSV header are never written, so a partial
export (e.g. the cleaned Google Sheets dataset) cannot blank fields like
description, type, or coordinates.
"""

import csv
from decimal import Decimal
from io import StringIO
from unittest import mock

from django.core.management import call_command

PARTIAL_HEADER = [
    "id",
    "name",
    "address",
    "phone",
    "website",
    "therapy_types",
    "insurance_accepted",
    "diagnoses_treated",
    "age_groups",
]

PROVIDER_ID = "614389c4-abd4-444c-a3fe-2de1cd718934"


class StubProvider:
    def __init__(self):
        self.id = PROVIDER_ID
        self.name = "Existing Provider"
        self.type = "Service Provider"
        self.description = "SAN FERNANDO VALLEY,SAN GABRIEL VALLEY"
        self.email = "contact@example.org"
        self.address = "814 12th St. Santa Monica, CA 90403"
        self.phone = "000-000-0000"
        self.website = "https://old.example.org"
        self.insurance_accepted = "Private Pay"
        self.therapy_types = None
        self.diagnoses_treated = None
        self.age_groups = None
        self.latitude = Decimal("34.01950000")
        self.longitude = Decimal("-118.49120000")
        self.location = object()
        self.saved = False

    def save(self):
        self.saved = True


class StubManager:
    def __init__(self, existing):
        self.existing = existing

    def filter(self, **kwargs):
        return self

    def first(self):
        return self.existing

    def create(self, **kwargs):
        raise AssertionError("create() should not be called in update-only tests")


def run_command(csv_path, existing):
    stub_model = mock.Mock()
    stub_model.objects = StubManager(existing)
    with mock.patch(
        "locations.management.commands.import_csv_providers.ProviderV2", stub_model
    ):
        call_command(
            "import_csv_providers", str(csv_path), "--update-only", stdout=StringIO()
        )


def write_csv(path, header, row):
    with path.open("w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(header)
        writer.writerow(row)


def test_partial_csv_updates_lists_and_preserves_absent_columns(tmp_path):
    csv_path = tmp_path / "partial.csv"
    write_csv(
        csv_path,
        PARTIAL_HEADER,
        [
            PROVIDER_ID,
            "Existing Provider",
            '{"street": "814 12th St. #4", "city": "Santa Monica",'
            ' "state": "CA", "zip": "90403"}',
            "323-328-8412",
            "https://new.example.org",
            "['Speech therapy']",
            "Private Pay, Medi-Cal",
            '["Autism Spectrum Disorder"]',
            '["0-5"]',
        ],
    )

    existing = StubProvider()
    run_command(csv_path, existing)

    assert existing.saved
    # Columns present in the CSV are applied
    assert existing.therapy_types == ["Speech therapy"]
    assert existing.diagnoses_treated == ["Autism Spectrum Disorder"]
    assert existing.age_groups == ["0-5"]
    assert existing.phone == "323-328-8412"
    assert existing.website == "https://new.example.org"
    assert existing.insurance_accepted == "Private Pay, Medi-Cal"
    # JSON-blob address is flattened to plain text
    assert existing.address == "814 12th St. #4, Santa Monica, CA, 90403"
    # Columns absent from the CSV are left untouched
    assert existing.description == "SAN FERNANDO VALLEY,SAN GABRIEL VALLEY"
    assert existing.type == "Service Provider"
    assert existing.email == "contact@example.org"
    assert existing.latitude == Decimal("34.01950000")
    assert existing.longitude == Decimal("-118.49120000")


def test_full_csv_still_overwrites_present_columns(tmp_path):
    csv_path = tmp_path / "full.csv"
    write_csv(
        csv_path,
        ["id", "name", "description", "type"],
        [PROVIDER_ID, "Existing Provider", "Updated description", "Clinic"],
    )

    existing = StubProvider()
    run_command(csv_path, existing)

    assert existing.saved
    assert existing.description == "Updated description"
    assert existing.type == "Clinic"


def test_empty_cell_in_present_column_clears_field(tmp_path):
    # Present-but-empty columns do overwrite; this documents the behavior
    # that made the pre-import data audit necessary.
    csv_path = tmp_path / "empty.csv"
    write_csv(
        csv_path,
        ["id", "name", "therapy_types"],
        [PROVIDER_ID, "Existing Provider", ""],
    )

    existing = StubProvider()
    existing.therapy_types = ["ABA therapy"]
    run_command(csv_path, existing)

    assert existing.saved
    assert existing.therapy_types is None
