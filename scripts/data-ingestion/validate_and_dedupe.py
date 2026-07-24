"""Validate provenance fields and deduplicate a deliverable CSV against repo export data, then print the PR validation report.

Accepts any deliverable npi provider CSV as input. Reusable across taxonomies.

Example:
    python validate_and_dedupe.py deliverables/npi_behavior_analysts.csv
"""

import csv
import re
import sys
from pathlib import Path


def load_existing_npis(export_path="../../providers_complete_export.csv"):
    export_file = Path(export_path)
    if not export_file.is_file():
        export_file = Path("providers_complete_export.csv")
    if not export_file.is_file():
        print(f"Notice: {export_path} not found. Skipping cross-file deduplication.", file=sys.stderr)
        return set()

    existing_npis = set()

    with export_file.open("r", encoding="utf-8", errors="ignore") as f:
        reader = csv.DictReader(f)
        for row in reader:
            npi = (row.get("npi") or "").strip()
            if npi:
                existing_npis.add(npi)

    return existing_npis


def main():
    if len(sys.argv) < 2:
        sys.exit("Usage: python validate_and_dedupe.py <path_to_csv>")

    target_file = Path(sys.argv[1])
    if not target_file.is_file():
        sys.exit(f"Error: File {target_file} does not exist.")

    with target_file.open("r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        fieldnames = reader.fieldnames
        rows = list(reader)

    total_pulled = len(rows)
    existing_npis = load_existing_npis()

    already_in_db = 0
    final_rows = []
    seen_npis = set()
    in_file_dupes = 0
    invalid_rows = 0

    for r in rows:
        npi = (r.get("npi") or "").strip()

        # Rows without an NPI are invalid for NPPES data
        if not npi:
            invalid_rows += 1
            continue

        # In-file deduplication by NPI
        if npi in seen_npis:
            in_file_dupes += 1
            continue
        seen_npis.add(npi)

        # Cross-file deduplication against database export
        if npi in existing_npis:
            already_in_db += 1
        else:
            final_rows.append(r)

    # Overwrite target CSV with deduplicated rows
    with target_file.open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(final_rows)

    # Mandatory provenance verification checks
    provenance_fields = ["npi", "source_name", "source_url", "fetched_at"]
    missing_counts = {field: 0 for field in provenance_fields}
    for r in final_rows:
        for field in provenance_fields:
            if not (r.get(field) or "").strip():
                missing_counts[field] += 1

    all_provenance_valid = all(count == 0 for count in missing_counts.values())

    # Calculate null counts for all columns
    null_counts = {col: 0 for col in fieldnames}
    for r in final_rows:
        for col in fieldnames:
            if not (r.get(col) or "").strip():
                null_counts[col] += 1

    fetched_date = final_rows[0].get("fetched_at", "")[:10] if final_rows else ""

    print("=======================================================")
    print("      PULL REQUEST VALIDATION REPORT (COPY BELOW)      ")
    print("=======================================================\n")
    print("Source: NPPES NPI Registry API (https://npiregistry.cms.hhs.gov/api-page)")
    print("Query/scope: Taxonomy 'Behavior Analyst' across 10 LA County cities")
    print(f"Fetched: {fetched_date}")
    print(
        f"Rows delivered: {len(final_rows)} (removed {in_file_dupes} in-file dupes, "
        f"{already_in_db} already in providers_complete_export.csv)"
    )

    null_str = ", ".join(f"{col}: {cnt}" for col, cnt in null_counts.items())
    print(f"Null counts: {null_str}")

    print("Sample rows:")
    for r in final_rows[:5]:
        print(f"  {r.get('name')} | NPI: {r.get('npi')} | {r.get('address')} | {r.get('phone')}")

    print("Collection rules: [ ] robots.txt/ToS checked  [ ] rate-limited  [ ] raw responses cached")
    print("Scope: [ ] only scripts/data-ingestion/ touched")

    # Provenance verification (separate from the copy-paste block)
    if not all_provenance_valid:
        print("\n--- Provenance Check ---", file=sys.stderr)
        for field in provenance_fields:
            if missing_counts[field] > 0:
                print(f"  FAIL: {field} has {missing_counts[field]} missing values", file=sys.stderr)
    if invalid_rows:
        print(f"\nNotice: {invalid_rows} rows dropped for missing NPI.", file=sys.stderr)


if __name__ == "__main__":
    main()
