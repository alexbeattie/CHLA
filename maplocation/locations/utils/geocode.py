import requests
import re
import time
from typing import Optional, Tuple, List

USER_AGENT = "chla-maplocation/1.0 (contact: dev@localhost)"


def clean_address_aggressive(address: str) -> List[str]:
    """
    Clean address with multiple aggressive strategies.
    Returns list of cleaned address variations to try.
    """
    if not address:
        return []

    variations = []

    # Strategy 1: Original address
    variations.append(address)

    # Strategy 2: Fix double commas and extra spaces
    cleaned = re.sub(r',+', ',', address)  # Multiple commas -> single comma
    cleaned = re.sub(r'\s+', ' ', cleaned)  # Multiple spaces -> single space
    cleaned = cleaned.strip()
    variations.append(cleaned)

    # Strategy 3: Remove suite/unit numbers (they often break geocoding)
    no_suite = re.sub(r'(Suite|Ste|Unit|Apt|#)\s*[A-Z0-9\-/]+', '', cleaned, flags=re.IGNORECASE)
    no_suite = re.sub(r'\s+', ' ', no_suite).strip()
    no_suite = re.sub(r',\s*,', ',', no_suite)  # Remove double commas
    variations.append(no_suite)

    # Strategy 4: Simplify to just street address + city + state + zip
    # Match pattern: street, city, state zip
    match = re.search(r'([^,]+),\s*([^,]+),\s*([A-Z]{2})\s*(\d{5})', no_suite)
    if match:
        street, city, state, zipcode = match.groups()
        simple = f"{street.strip()}, {city.strip()}, {state} {zipcode}"
        variations.append(simple)

    # Strategy 5: Just city, state, zip (least specific, but might work)
    match = re.search(r'([A-Za-z\s]+),\s*([A-Z]{2})\s*(\d{5})', address)
    if match:
        city, state, zipcode = match.groups()
        city_only = f"{city.strip()}, {state} {zipcode}"
        variations.append(city_only)

    # Strategy 6: Remove trailing periods and fix common issues
    cleaned_periods = re.sub(r'\.$', '', address)  # Remove trailing period
    cleaned_periods = re.sub(r'\s+', ' ', cleaned_periods).strip()
    variations.append(cleaned_periods)

    # Strategy 7: Fix "Blvd." -> "Blvd", "St." -> "St", etc.
    no_periods = re.sub(r'\b(Blvd|St|Ave|Dr|Rd|Ct|Ln|Way)\.\s*', r'\1 ', address, flags=re.IGNORECASE)
    no_periods = re.sub(r'\s+', ' ', no_periods).strip()
    variations.append(no_periods)

    # Deduplicate while preserving order
    seen = set()
    unique_variations = []
    for v in variations:
        if v and v not in seen:
            seen.add(v)
            unique_variations.append(v)

    return unique_variations


def geocode_with_nominatim(address: str, delay: float = 1.0) -> Optional[Tuple[float, float]]:
    """
    Geocode using OpenStreetMap Nominatim.
    Includes rate limiting delay.
    """
    try:
        time.sleep(delay)  # Rate limiting
        resp = requests.get(
            "https://nominatim.openstreetmap.org/search",
            params={"q": address, "format": "json", "limit": 1},
            headers={"User-Agent": USER_AGENT},
            timeout=10,
        )
        if resp.status_code != 200:
            return None
        data = resp.json()
        if not data:
            return None
        item = data[0]
        return float(item["lat"]), float(item["lon"])
    except Exception:
        return None


def geocode_address(address: str) -> Optional[Tuple[float, float]]:
    """
    Geocode an address via OpenStreetMap Nominatim.
    Uses multiple fallback strategies for better success rate.

    Returns (lat, lng) floats or None if not found/failed.
    """
    if not address:
        return None

    # Get all address variations
    variations = clean_address_aggressive(address)

    # Try each variation with progressively longer delays
    for i, variant in enumerate(variations):
        # Use longer delay for later attempts to respect rate limits
        delay = 1.0 if i == 0 else 1.5
        result = geocode_with_nominatim(variant, delay=delay)
        if result:
            return result

    return None
