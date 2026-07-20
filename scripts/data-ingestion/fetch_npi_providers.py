"""Pull provider records from the NPPES NPI Registry API into the KiNDD deliverable CSV format.

API docs: https://npiregistry.cms.hhs.gov/api-page (version 2.1, no key required).
Pagination is capped by the API at limit=200 and skip=1000, so one query segment
returns at most 1,200 rows; segment large taxonomies with repeated --city flags.

Example:
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

    return {
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


def fetch_segment(session, taxonomy, city, state, cache_dir, rate_limit_seconds):
    records = []
    for skip in range(0, MAX_SKIP + 1, PAGE_SIZE):
        params = {
            "version": API_VERSION,
            "taxonomy_description": taxonomy,
            "state": state,
            "limit": PAGE_SIZE,
            "skip": skip,
        }
        if city:
            params["city"] = city
        cache_name = f"npi_{slugify(taxonomy)}_{slugify(state)}_{slugify(city or 'all')}_{skip}.json"
        data, from_cache = fetch_page(session, params, cache_dir / cache_name, rate_limit_seconds)
        page = data.get("results", [])
        source = "cache" if from_cache else "api"
        print(f"  {city or 'all cities'} skip={skip}: {len(page)} results ({source})")
        records.extend(page)
        if len(page) < PAGE_SIZE:
            return records, False
    return records, True


def main():
    load_env()
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--taxonomy", required=True, help='e.g. "Behavior Analyst"')
    parser.add_argument("--city", action="append", default=[], help="Repeatable; segments the query")
    parser.add_argument("--state", default="CA")
    parser.add_argument("--out", default="output/npi_providers.csv")
    parser.add_argument("--cache-dir", default="cache")
    parser.add_argument("--contact-email", default=os.environ.get("CONTACT_EMAIL", ""))
    parser.add_argument("--rate-limit", type=float, default=float(os.environ.get("RATE_LIMIT_SECONDS", "1.0")))
    args = parser.parse_args()

    if not args.contact_email:
        sys.exit("Set CONTACT_EMAIL in .env or pass --contact-email (goes in the User-Agent).")

    cache_dir = Path(args.cache_dir)
    cache_dir.mkdir(parents=True, exist_ok=True)
    out_path = Path(args.out)
    out_path.parent.mkdir(parents=True, exist_ok=True)

    session = requests.Session()
    session.headers["User-Agent"] = f"KiNDD-data-ingestion (contact: {args.contact_email})"

    fetched_at = datetime.now(timezone.utc).isoformat(timespec="seconds")
    rows = {}
    truncated_segments = []
    for city in args.city or [None]:
        print(f"Fetching taxonomy={args.taxonomy!r} city={city or 'all'} state={args.state}")
        records, truncated = fetch_segment(
            session, args.taxonomy, city, args.state, cache_dir, args.rate_limit
        )
        if truncated:
            truncated_segments.append(city or "all")
        source_url = (
            f"{API_URL}?version={API_VERSION}&taxonomy_description={args.taxonomy}"
            f"&state={args.state}" + (f"&city={city}" if city else "")
        )
        for record in records:
            row = flatten(record, source_url, fetched_at)
            if row["npi"]:
                rows[row["npi"]] = row

    with out_path.open("w", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=CSV_COLUMNS)
        writer.writeheader()
        writer.writerows(sorted(rows.values(), key=lambda r: r["name"]))

    print(f"\nWrote {len(rows)} unique providers to {out_path}")
    if truncated_segments:
        print(
            "WARNING: these segments hit the API's 1,200-row pagination cap and are "
            f"incomplete: {', '.join(truncated_segments)}. Narrow them with more --city values.",
            file=sys.stderr,
        )


if __name__ == "__main__":
    main()
