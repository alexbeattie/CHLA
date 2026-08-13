"""Pull provider records from the NPPES NPI Registry API into the KiNDD deliverable CSV format.

API docs: https://npiregistry.cms.hhs.gov/api-page (version 2.1, no key required).
Pagination is capped by the API at limit=200 and skip=1000, so one query segment
returns at most 1,200 rows; segment large taxonomies with repeated --city flags.

Examples:
    # County-wide search (recommended for deliverables)
    python fetch_npi_providers.py \
        --taxonomy "Behavior Analyst" \
        --out deliverables/npi_behavior_analysts.csv

    # Or narrow API queries by city (still post-filters to county ZIPs)
    python fetch_npi_providers.py \
        --taxonomy "Behavior Analyst" \
        --city "Los Angeles" --city "Long Beach" \
        --out output/npi_behavior_analysts.csv
"""

import argparse
import csv
import json
import os
import re
import sys
import time
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path

import requests

API_URL = "https://npiregistry.cms.hhs.gov/api/"
API_VERSION = "2.1"
PAGE_SIZE = 200
MAX_SKIP = 1000

CSV_COLUMNS = [
    "name", "address", "latitude", "longitude", "phone", "website",
    "therapy_types", "insurance_accepted", "diagnoses_treated", "age_groups",
    "regional_centers", "description", "type", "email", "npi",
    "source_name", "source_url", "fetched_at",
]


def load_env(path=".env"):
    env_file = Path(path)
    if not env_file.is_file():
        return
    for line in env_file.read_text().splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, _, value = line.partition("=")
        os.environ.setdefault(key.strip(), value.strip())


def slugify(text):
    return re.sub(r"[^a-z0-9]+", "-", text.lower()).strip("-") or "all"


def normalize_phone(raw):
    digits = re.sub(r"\D", "", raw or "")
    if len(digits) == 11 and digits.startswith("1"):
        digits = digits[1:]
    if len(digits) != 10:
        return ""
    return f"{digits[0:3]}-{digits[3:6]}-{digits[6:10]}"


def load_county_zips(path):
    """Return the set of 5-digit ZIP strings from a county ZIP CSV."""
    zip_file = Path(path)
    if not zip_file.is_file():
        sys.exit(f"County ZIP file not found: {path}")
    zips = set()
    with zip_file.open() as f:
        for row in csv.DictReader(f):
            z = (row.get("zip") or "").strip()
            if z:
                zips.add(z)
    if not zips:
        sys.exit(f"County ZIP file is empty: {path}")
    return zips


def fetch_page(session, params, cache_path, rate_limit_seconds):
    if cache_path.is_file():
        return json.loads(cache_path.read_text()), True
    for attempt in range(4):
        time.sleep(rate_limit_seconds * (2 ** attempt if attempt else 1))
        response = session.get(API_URL, params=params, timeout=30)
        if response.status_code == 200:
            data = response.json()
            if "Errors" in data:
                sys.exit(f"API error: {data['Errors']}")
            cache_path.write_text(json.dumps(data))
            return data, False
        print(f"  HTTP {response.status_code}, retrying", file=sys.stderr)
    sys.exit("Giving up after repeated request failures.")


def flatten(record, source_url, fetched_at):
    """Return (practice_zip, row_dict) for one NPPES record."""
    basic = record.get("basic", {})
    if record.get("enumeration_type") == "NPI-2":
        name = basic.get("organization_name", "").strip()
        provider_type = "organization"
    else:
        parts = [basic.get("first_name", ""), basic.get("last_name", "")]
        name = " ".join(p.strip() for p in parts if p.strip()).title()
        credential = basic.get("credential", "").strip()
        if credential:
            name = f"{name}, {credential}"
        provider_type = "individual"

    location = next(
        (a for a in record.get("addresses", []) if a.get("address_purpose") == "LOCATION"),
        {},
    )
    street = " ".join(
        s.strip() for s in [location.get("address_1", ""), location.get("address_2", "")] if s.strip()
    ).title()
    city = location.get("city", "").strip().title()
    state = location.get("state", "").strip()
    postal = (location.get("postal_code", "") or "")[:5]
    address = f"{street}, {city}, {state} {postal}".strip(", ") if street else ""

    primary_taxonomy = next(
        (t for t in record.get("taxonomies", []) if t.get("primary")),
        (record.get("taxonomies") or [{}])[0],
    )

    return postal, {
        "name": name,
        "address": address,
        "latitude": "",
        "longitude": "",
        "phone": normalize_phone(location.get("telephone_number")),
        "website": "",
        "therapy_types": primary_taxonomy.get("desc", ""),
        "insurance_accepted": "",
        "diagnoses_treated": "",
        "age_groups": "",
        "regional_centers": "",
        "description": "",
        "type": provider_type,
        "email": "",
        "npi": record.get("number", ""),
        "source_name": "NPPES",
        "source_url": source_url,
        "fetched_at": fetched_at,
    }


def fetch_segment(session, taxonomy, city, state, cache_dir, rate_limit_seconds, postal_code=None):
    records = []
    for skip in range(0, MAX_SKIP + 1, PAGE_SIZE):
        params = {
            "version": API_VERSION,
            "taxonomy_description": taxonomy,
            "state": state,
            "address_purpose": "LOCATION",
            "limit": PAGE_SIZE,
            "skip": skip,
        }
        if city:
            params["city"] = city
        if postal_code:
            params["postal_code"] = postal_code
        geo = city or postal_code or "all"
        cache_name = f"npi_{slugify(taxonomy)}_{slugify(state)}_{slugify(geo)}_{skip}.json"
        data, from_cache = fetch_page(session, params, cache_dir / cache_name, rate_limit_seconds)
        page = data.get("results", [])
        source = "cache" if from_cache else "api"
        print(f"  {geo} skip={skip}: {len(page)} results ({source})")
        records.extend(page)
        if len(page) < PAGE_SIZE:
            return records, False
    return records, True


def main():
    load_env()
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--taxonomy", required=True, help='e.g. "Behavior Analyst"')
    parser.add_argument("--city", action="append", default=[], help="Repeatable; segments the query")
    parser.add_argument("--county-zips",
                        default=str(Path(__file__).resolve().parent / "la_county_zips.csv"),
                        help="Path to county ZIP CSV (default: la_county_zips.csv)")
    parser.add_argument("--state", default="CA")
    parser.add_argument("--out", default="output/npi_providers.csv")
    parser.add_argument("--cache-dir", default="cache")
    parser.add_argument("--contact-email", default=os.environ.get("CONTACT_EMAIL", ""))
    parser.add_argument("--rate-limit", type=float, default=float(os.environ.get("RATE_LIMIT_SECONDS", "1.0")))
    args = parser.parse_args()

    if not args.contact_email:
        sys.exit("Set CONTACT_EMAIL in .env or pass --contact-email (goes in the User-Agent).")

    county_zips = load_county_zips(args.county_zips)
    print(f"Loaded {len(county_zips)} county ZIPs from {args.county_zips}")

    cache_dir = Path(args.cache_dir)
    cache_dir.mkdir(parents=True, exist_ok=True)
    out_path = Path(args.out)
    out_path.parent.mkdir(parents=True, exist_ok=True)

    session = requests.Session()
    session.headers["User-Agent"] = f"KiNDD-data-ingestion (contact: {args.contact_email})"

    fetched_at = datetime.now(timezone.utc).isoformat(timespec="seconds")
    rows = {}
    truncated_segments = []
    skipped_outside = 0
    skipped_taxonomy = Counter()

    if args.city:
        segments = [(c, None) for c in args.city]
    else:
        segments = [(None, z) for z in sorted(county_zips)]

    for i, (city, postal_code) in enumerate(segments, 1):
        geo = city or postal_code or "all"
        print(f"[{i}/{len(segments)}] Fetching taxonomy={args.taxonomy!r} {geo} state={args.state}")
        records, truncated = fetch_segment(
            session, args.taxonomy, city, args.state, cache_dir, args.rate_limit,
            postal_code=postal_code,
        )
        if truncated:
            truncated_segments.append(geo)
        source_url = (
            f"{API_URL}?version={API_VERSION}&taxonomy_description={args.taxonomy}"
            f"&state={args.state}"
            + (f"&city={city}" if city else "")
            + (f"&postal_code={postal_code}" if postal_code else "")
        )
        for record in records:
            practice_zip, row = flatten(record, source_url, fetched_at)
            if not row["npi"]:
                continue
            if practice_zip not in county_zips:
                skipped_outside += 1
                continue
            if row["therapy_types"] != args.taxonomy:
                skipped_taxonomy[row["therapy_types"]] += 1
                continue
            rows[row["npi"]] = row

    with out_path.open("w", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=CSV_COLUMNS)
        writer.writeheader()
        writer.writerows(sorted(rows.values(), key=lambda r: r["name"]))

    print(f"\nWrote {len(rows)} unique providers to {out_path}")
    if skipped_outside:
        print(f"Filtered out {skipped_outside} providers with practice locations outside county ZIP set.")
    if skipped_taxonomy:
        print(f"Filtered out {sum(skipped_taxonomy.values())} providers whose primary taxonomy "
              f"is not {args.taxonomy!r}:")
        for tax, count in skipped_taxonomy.most_common():
            print(f"  {tax}: {count}")
    if truncated_segments:
        print(
            "WARNING: these segments hit the API's 1,200-row pagination cap and are "
            f"incomplete: {', '.join(truncated_segments)}. Narrow them with more --city values.",
            file=sys.stderr,
        )


if __name__ == "__main__":
    main()
