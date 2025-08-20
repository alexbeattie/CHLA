import requests
from typing import Optional, Tuple

USER_AGENT = "chla-maplocation/1.0 (contact: dev@localhost)"

def geocode_address(address: str) -> Optional[Tuple[float, float]]:
    """Geocode an address via OpenStreetMap Nominatim.

    Returns (lat, lng) floats or None if not found/failed.
    """
    if not address:
        return None
    try:
        resp = requests.get(
            "https://nominatim.openstreetmap.org/search",
            params={"q": address, "format": "json", "limit": 1},
            headers={"User-Agent": USER_AGENT},
            timeout=8,
        )
        if resp.status_code != 200:
            return None
        data = resp.json()
        if not data:
            return None
        item = data[0]
        return float(item["lat"]), float(item["lon"])  # lat, lon
    except Exception:
        return None


