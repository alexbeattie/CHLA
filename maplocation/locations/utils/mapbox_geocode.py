import requests
from typing import Optional, Tuple
import os
from django.conf import settings

# Mapbox API configuration
MAPBOX_ACCESS_TOKEN = os.environ.get(
    "MAPBOX_ACCESS_TOKEN",
    "pk.eyJ1IjoiYWxleGJlYXR0aWUiLCJhIjoiOVVEYU52WSJ9.S_uekMjvfZC5_s0dVVJgQg",
)
MAPBOX_GEOCODING_URL = "https://api.mapbox.com/geocoding/v5/mapbox.places"


def geocode_address(address: str) -> Optional[Tuple[float, float]]:
    """
    Geocode an address using Mapbox Geocoding API.

    Args:
        address: The address string to geocode

    Returns:
        Tuple of (latitude, longitude) or None if not found/failed
    """
    if not address or not address.strip():
        return None

    try:
        # Clean up the address
        clean_address = address.strip()

        # Make request to Mapbox Geocoding API
        response = requests.get(
            f"{MAPBOX_GEOCODING_URL}/{clean_address}.json",
            params={
                "access_token": MAPBOX_ACCESS_TOKEN,
                "country": "US",  # Focus on US addresses
                "types": "address,poi,place,postcode,locality,neighborhood",  # Prioritize specific locations
                "limit": 1,
                "proximity": "-118.2437,34.0522",  # Bias towards Los Angeles area
            },
            timeout=10,
        )

        if response.status_code != 200:
            print(f"Mapbox API error: {response.status_code} - {response.text}")
            return None

        data = response.json()

        if not data.get("features"):
            print(f"No results found for address: {address}")
            return None

        # Get the first (best) result
        feature = data["features"][0]
        coordinates = feature["geometry"]["coordinates"]  # [longitude, latitude]

        # Mapbox returns [lng, lat], we need [lat, lng]
        longitude, latitude = coordinates
        return float(latitude), float(longitude)

    except requests.exceptions.RequestException as e:
        print(f"Request error during geocoding: {e}")
        return None
    except (KeyError, ValueError, IndexError) as e:
        print(f"Error parsing geocoding response: {e}")
        return None
    except Exception as e:
        print(f"Unexpected error during geocoding: {e}")
        return None


def geocode_with_fallback(address: str) -> Optional[Tuple[float, float]]:
    """
    Geocode an address with multiple fallback strategies.

    Args:
        address: The address string to geocode

    Returns:
        Tuple of (latitude, longitude) or None if not found/failed
    """
    if not address or not address.strip():
        return None

    # Strategy 1: Try the full address
    result = geocode_address(address)
    if result:
        print(f"✅ Geocoded full address: {address} -> {result}")
        return result

    # Strategy 2: Try with just city, state, zip
    parts = address.split(",")
    if len(parts) >= 2:
        # Extract city, state, zip part
        city_state_zip = ",".join(parts[-2:]).strip()
        result = geocode_address(city_state_zip)
        if result:
            print(f"✅ Geocoded city/state/zip: {city_state_zip} -> {result}")
            return result

    # Strategy 3: Try with just the last part (usually city, state, zip)
    if len(parts) >= 1:
        last_part = parts[-1].strip()
        result = geocode_address(last_part)
        if result:
            print(f"✅ Geocoded last part: {last_part} -> {result}")
            return result

    # Strategy 4: Try extracting just ZIP code
    import re

    zip_match = re.search(r"\b\d{5}(-\d{4})?\b", address)
    if zip_match:
        zip_code = zip_match.group()
        result = geocode_address(zip_code)
        if result:
            print(f"✅ Geocoded ZIP code: {zip_code} -> {result}")
            return result

    print(f"❌ Failed to geocode address: {address}")
    return None


def reverse_geocode(latitude: float, longitude: float) -> Optional[str]:
    """
    Reverse geocode coordinates to get an address.

    Args:
        latitude: Latitude coordinate
        longitude: Longitude coordinate

    Returns:
        Address string or None if not found/failed
    """
    try:
        response = requests.get(
            f"{MAPBOX_GEOCODING_URL}/{longitude},{latitude}.json",
            params={
                "access_token": MAPBOX_ACCESS_TOKEN,
                "types": "address,poi,place,postcode,locality",
                "limit": 1,
            },
            timeout=10,
        )

        if response.status_code != 200:
            return None

        data = response.json()

        if not data.get("features"):
            return None

        feature = data["features"][0]
        return feature.get("place_name", "")

    except Exception as e:
        print(f"Error during reverse geocoding: {e}")
        return None
