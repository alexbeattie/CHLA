"""Geocoding service for address lookup."""
import os
import time
import requests
from decimal import Decimal
from typing import Optional, Dict
from django.core.management.base import OutputWrapper


class GeocodingService:
    """Handles geocoding of addresses using Mapbox API."""

    def __init__(self, stdout: Optional[OutputWrapper] = None, rate_limit_delay: float = 0.2):
        """
        Initialize geocoding service.
        
        Args:
            stdout: Optional Django command output wrapper for logging
            rate_limit_delay: Delay between requests in seconds (default: 0.2)
        """
        self.stdout = stdout
        self.rate_limit_delay = rate_limit_delay
        self.mapbox_token = os.environ.get("MAPBOX_ACCESS_TOKEN")
        self._last_request_time = 0

    def is_available(self) -> bool:
        """Check if geocoding is available (API token is set)."""
        return bool(self.mapbox_token)

    def geocode_address(self, address: str) -> Optional[Dict[str, Decimal]]:
        """
        Geocode an address using Mapbox Geocoding API.
        
        Args:
            address: Full address string to geocode
            
        Returns:
            Dictionary with 'latitude' and 'longitude' keys, or None if failed
        """
        if not self.is_available():
            if self.stdout:
                self.stdout.write(
                    "MAPBOX_ACCESS_TOKEN not set, skipping geocoding"
                )
            return None

        # Rate limiting
        self._apply_rate_limit()

        # Clean up address
        clean_address = address.replace("\n", ", ").strip()

        # Make API request
        url = f"https://api.mapbox.com/geocoding/v5/mapbox.places/{clean_address}.json"
        params = {
            "access_token": self.mapbox_token,
            "country": "US",
            "limit": 1,
        }

        try:
            response = requests.get(url, params=params, timeout=10)
            response.raise_for_status()
            data = response.json()

            if data.get("features"):
                feature = data["features"][0]
                longitude, latitude = feature["geometry"]["coordinates"]
                return {
                    "latitude": Decimal(str(latitude)),
                    "longitude": Decimal(str(longitude)),
                }
                
        except Exception as e:
            if self.stdout:
                self.stdout.write(f"Geocoding failed for {clean_address}: {e}")

        return None

    def _apply_rate_limit(self):
        """Apply rate limiting between requests."""
        if self._last_request_time:
            elapsed = time.time() - self._last_request_time
            if elapsed < self.rate_limit_delay:
                time.sleep(self.rate_limit_delay - elapsed)
        
        self._last_request_time = time.time()

