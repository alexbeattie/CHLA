"""
Django management command to check and fix provider coordinates.

Checks:
1. Providers with missing PostGIS location
2. Providers with missing or zero latitude/longitude
3. Providers with mismatched location vs lat/lng
4. Attempts to geocode missing coordinates using addresses

Usage:
    python3 manage.py check_provider_coordinates
    python3 manage.py check_provider_coordinates --fix
    python3 manage.py check_provider_coordinates --geocode
"""

from django.core.management.base import BaseCommand
from django.contrib.gis.geos import Point
from locations.models import ProviderV2
from decimal import Decimal
import time


class Command(BaseCommand):
    help = "Check and optionally fix provider coordinate issues"

    def add_arguments(self, parser):
        parser.add_argument(
            "--fix",
            action="store_true",
            help="Fix coordinate sync issues (location <-> lat/lng)",
        )
        parser.add_argument(
            "--geocode",
            action="store_true",
            help="Attempt to geocode providers with missing coordinates",
        )
        parser.add_argument(
            "--limit",
            type=int,
            default=None,
            help="Limit number of providers to geocode (for API rate limiting)",
        )

    def handle(self, *args, **options):
        self.stdout.write("\n" + "=" * 80)
        self.stdout.write(self.style.HTTP_INFO("PROVIDER COORDINATES CHECK"))
        self.stdout.write("=" * 80 + "\n")

        fix_mode = options["fix"]
        geocode_mode = options["geocode"]
        limit = options["limit"]

        # Get all providers
        total_providers = ProviderV2.objects.count()
        self.stdout.write(f"Total providers in database: {total_providers}\n")

        # Check 1: Missing PostGIS location
        self.stdout.write(self.style.WARNING("Checking for missing PostGIS location..."))
        missing_location = ProviderV2.objects.filter(location__isnull=True)
        self.stdout.write(f"  Found: {missing_location.count()} providers\n")

        if missing_location.exists():
            for provider in missing_location[:10]:  # Show first 10
                self.stdout.write(
                    f"    - {provider.name} (lat={provider.latitude}, lng={provider.longitude})"
                )
            if missing_location.count() > 10:
                self.stdout.write(f"    ... and {missing_location.count() - 10} more\n")

        # Check 2: Zero coordinates (0.0, 0.0)
        self.stdout.write(
            self.style.WARNING("\nChecking for zero coordinates (0.0, 0.0)...")
        )
        zero_coords = ProviderV2.objects.filter(
            latitude=Decimal("0.0"), longitude=Decimal("0.0")
        )
        self.stdout.write(f"  Found: {zero_coords.count()} providers\n")

        if zero_coords.exists():
            for provider in zero_coords[:10]:
                address = provider.address[:60] if provider.address else "No address"
                self.stdout.write(f"    - {provider.name}: {address}")
            if zero_coords.count() > 10:
                self.stdout.write(f"    ... and {zero_coords.count() - 10} more\n")

        # Check 3: Valid coordinates
        self.stdout.write(self.style.SUCCESS("\nChecking for valid coordinates..."))
        valid_location = ProviderV2.objects.filter(
            location__isnull=False
        ).exclude(latitude=Decimal("0.0"), longitude=Decimal("0.0"))
        self.stdout.write(f"  Found: {valid_location.count()} providers with valid coordinates\n")

        # Check 4: Mismatch between location and lat/lng
        self.stdout.write(
            self.style.WARNING("\nChecking for coordinate mismatches...")
        )
        mismatched = []
        providers_with_location = ProviderV2.objects.filter(location__isnull=False)
        
        for provider in providers_with_location[:100]:  # Check first 100
            if provider.location and provider.latitude and provider.longitude:
                lat_diff = abs(float(provider.latitude) - provider.location.y)
                lng_diff = abs(float(provider.longitude) - provider.location.x)
                
                # Allow small floating point differences
                if lat_diff > 0.000001 or lng_diff > 0.000001:
                    mismatched.append((provider, lat_diff, lng_diff))

        if mismatched:
            self.stdout.write(f"  Found: {len(mismatched)} providers with mismatches")
            for provider, lat_diff, lng_diff in mismatched[:5]:
                self.stdout.write(
                    f"    - {provider.name}: lat_diff={lat_diff:.8f}, lng_diff={lng_diff:.8f}"
                )
        else:
            self.stdout.write("  No mismatches found in sample")

        # Summary
        self.stdout.write("\n" + "=" * 80)
        self.stdout.write(self.style.HTTP_INFO("SUMMARY"))
        self.stdout.write("=" * 80)
        self.stdout.write(f"Total Providers:           {total_providers}")
        self.stdout.write(f"Valid Coordinates:         {valid_location.count()} ({valid_location.count()/total_providers*100:.1f}%)")
        self.stdout.write(f"Missing PostGIS Location:  {missing_location.count()}")
        self.stdout.write(f"Zero Coordinates:          {zero_coords.count()}")
        self.stdout.write(f"Coordinate Mismatches:     {len(mismatched)}\n")

        # Fix mode
        if fix_mode:
            self.stdout.write("\n" + "=" * 80)
            self.stdout.write(self.style.WARNING("FIX MODE ENABLED"))
            self.stdout.write("=" * 80 + "\n")
            self.fix_coordinate_sync(missing_location)

        # Geocode mode
        if geocode_mode:
            self.stdout.write("\n" + "=" * 80)
            self.stdout.write(self.style.WARNING("GEOCODE MODE ENABLED"))
            self.stdout.write("=" * 80 + "\n")
            self.geocode_missing_coordinates(missing_location, zero_coords, limit)

    def fix_coordinate_sync(self, missing_location):
        """Fix sync issues between location and lat/lng"""
        self.stdout.write("Fixing coordinate sync issues...\n")
        
        fixed = 0
        for provider in missing_location:
            # If has valid lat/lng but no location, create PostGIS point
            if (
                provider.latitude
                and provider.longitude
                and provider.latitude != Decimal("0.0")
                and provider.longitude != Decimal("0.0")
            ):
                provider.location = Point(
                    float(provider.longitude), float(provider.latitude), srid=4326
                )
                provider.save()
                fixed += 1
                self.stdout.write(
                    self.style.SUCCESS(
                        f"  ✓ Fixed: {provider.name} - created PostGIS point from lat/lng"
                    )
                )

        self.stdout.write(f"\nFixed {fixed} providers\n")

    def geocode_missing_coordinates(self, missing_location, zero_coords, limit):
        """Attempt to geocode providers with missing coordinates"""
        from locations.utils.mapbox_geocode import geocode_with_fallback
        
        # Combine both sets
        needs_geocoding = (missing_location | zero_coords).distinct()
        
        # Filter to only those with addresses
        needs_geocoding = needs_geocoding.exclude(address="").exclude(address__isnull=True)
        
        count = needs_geocoding.count()
        self.stdout.write(f"Found {count} providers with addresses but no coordinates\n")
        
        if limit:
            needs_geocoding = needs_geocoding[:limit]
            self.stdout.write(f"Limiting to {limit} providers due to --limit flag\n")
        
        geocoded = 0
        failed = 0
        
        for i, provider in enumerate(needs_geocoding, 1):
            self.stdout.write(f"\n[{i}/{needs_geocoding.count()}] Geocoding: {provider.name}")
            # Clean address (remove newlines)
            clean_address = provider.address.replace("\n", ", ")
            self.stdout.write(f"  Address: {clean_address}")
            
            try:
                # Use the Mapbox geocoding with fallback strategies
                coordinates = geocode_with_fallback(clean_address)
                
                if coordinates:
                    lat, lng = coordinates
                    provider.latitude = Decimal(str(lat))
                    provider.longitude = Decimal(str(lng))
                    provider.location = Point(lng, lat, srid=4326)
                    provider.save()
                    geocoded += 1
                    self.stdout.write(
                        self.style.SUCCESS(f"  ✓ Success: {lat}, {lng}")
                    )
                else:
                    failed += 1
                    self.stdout.write(
                        self.style.ERROR("  ✗ Failed: No coordinates returned")
                    )
                
                # Rate limiting - sleep for 1 second between requests
                time.sleep(1)
                
            except Exception as e:
                failed += 1
                self.stdout.write(
                    self.style.ERROR(f"  ✗ Error: {str(e)}")
                )
        
        self.stdout.write("\n" + "=" * 80)
        self.stdout.write(self.style.HTTP_INFO("GEOCODING SUMMARY"))
        self.stdout.write("=" * 80)
        self.stdout.write(f"Attempted:  {geocoded + failed}")
        self.stdout.write(f"Successful: {geocoded}")
        self.stdout.write(f"Failed:     {failed}\n")

