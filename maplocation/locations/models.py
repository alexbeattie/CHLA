from django.db import models
# from django.contrib.gis.db import models as gis_models
# from django.contrib.gis.geos import Point, Polygon, MultiPolygon
# from django.contrib.gis.measure import Distance
from decimal import Decimal
import math


class LocationCategory(models.Model):
    name = models.CharField(max_length=100)
    description = models.TextField(blank=True, null=True)

    def __str__(self):
        return self.name

    class Meta:
        verbose_name_plural = "Location Categories"


class Location(models.Model):
    name = models.CharField(max_length=200)
    address = models.CharField(max_length=255)
    city = models.CharField(max_length=100)
    state = models.CharField(max_length=50)
    zip_code = models.CharField(max_length=20)
    latitude = models.DecimalField(max_digits=10, decimal_places=7)
    longitude = models.DecimalField(max_digits=10, decimal_places=7)
    description = models.TextField(blank=True, null=True)
    phone = models.CharField(max_length=20, blank=True, null=True)
    website = models.URLField(blank=True, null=True)
    email = models.EmailField(blank=True, null=True)
    is_active = models.BooleanField(default=True)
    category = models.ForeignKey(
        LocationCategory, on_delete=models.CASCADE, related_name="locations"
    )
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    # Additional fields for filter options
    rating = models.DecimalField(max_digits=3, decimal_places=1, default=0.0)
    price_level = models.IntegerField(
        choices=[(1, "$"), (2, "$$"), (3, "$$$"), (4, "$$$$")], default=1
    )
    hours_of_operation = models.TextField(blank=True, null=True)
    has_parking = models.BooleanField(default=False)
    is_accessible = models.BooleanField(default=False)

    def __str__(self):
        return self.name


class RegionalCenter(models.Model):
    regional_center = models.CharField(max_length=200)
    office_type = models.CharField(max_length=50, blank=True, null=True)
    address = models.CharField(max_length=255)
    suite = models.CharField(max_length=50, blank=True, null=True)
    city = models.CharField(max_length=100)
    state = models.CharField(max_length=50)
    zip_code = models.CharField(max_length=20)
    telephone = models.CharField(max_length=20, blank=True, null=True)
    website = models.URLField(blank=True, null=True)
    county_served = models.CharField(max_length=100, blank=True, null=True)
    los_angeles_health_district = models.CharField(
        max_length=100, blank=True, null=True
    )
    location_coordinates = models.CharField(max_length=255, blank=True, null=True)
    latitude = models.FloatField(blank=True, null=True)
    longitude = models.FloatField(blank=True, null=True)
    location = models.CharField(max_length=100, blank=True, null=True)

    # Add geometry field for service area boundaries (temporarily stored as text)
    service_area = models.TextField(
        blank=True,
        null=True,
        help_text="Geographic service area boundary for this regional center (stored as text temporarily)",
    )

    # Add approximate service radius for fallback calculations
    service_radius_miles = models.FloatField(
        default=15.0,
        help_text="Approximate service radius in miles (used when no service area polygon is available)",
    )

    def __str__(self):
        return self.regional_center

    class Meta:
        verbose_name_plural = "Regional Centers"
        db_table = "regional_centers"  # Match the existing table name

    def get_service_area_as_geojson(self):
        """Return the service area as GeoJSON"""
        if self.service_area:
            try:
                from django.db import connection
                with connection.cursor() as cursor:
                    cursor.execute(
                        "SELECT ST_AsGeoJSON(service_area) FROM regional_centers WHERE id = %s",
                        [self.id]
                    )
                    result = cursor.fetchone()
                    if result and result[0]:
                        import json
                        return json.loads(result[0])
            except Exception as e:
                print(f"Error converting geometry to GeoJSON: {e}")
                return None
        return None

    def create_approximate_service_area(self):
        """Create an approximate circular service area based on the center location and service radius"""
        # Temporarily disabled due to GIS dependencies
        return None

    @classmethod
    def find_nearest(cls, latitude, longitude, radius_miles=25, limit=10):
        """Find regional centers within radius of given coordinates"""
        from django.db import connection

        with connection.cursor() as cursor:
            cursor.execute(
                """
                SELECT id, regional_center, address, city, latitude, longitude,
                       (3959 * acos(cos(radians(%s)) * cos(radians(latitude)) * 
                       cos(radians(longitude) - radians(%s)) + sin(radians(%s)) * 
                       sin(radians(latitude)))) AS distance
                FROM regional_centers 
                WHERE latitude IS NOT NULL AND longitude IS NOT NULL
                AND (3959 * acos(cos(radians(%s)) * cos(radians(latitude)) * 
                cos(radians(longitude) - radians(%s)) + sin(radians(%s)) * 
                sin(radians(latitude)))) < %s
                ORDER BY distance
                LIMIT %s
            """,
                [
                    latitude,
                    longitude,
                    latitude,
                    latitude,
                    longitude,
                    latitude,
                    radius_miles,
                    limit,
                ],
            )

            columns = [col[0] for col in cursor.description]
            results = []
            for row in cursor.fetchall():
                center_data = dict(zip(columns, row))
                center = cls.objects.get(id=center_data["id"])
                center.distance = center_data["distance"]
                results.append(center)
            return results

    @classmethod
    def find_by_location(cls, latitude, longitude):
        """Find regional centers that serve a specific geographic point"""
        # Temporarily use only distance-based search due to GIS being disabled
        return cls.find_nearest(latitude, longitude, radius_miles=25, limit=3)

    @classmethod
    def geocode_and_search(cls, address_or_zip, radius_miles=25, limit=10):
        """Geocode an address/zip and find nearby regional centers"""
        coordinates = cls.geocode_address(address_or_zip)
        if coordinates:
            return cls.find_nearest(coordinates[0], coordinates[1], radius_miles, limit)
        return []

    @classmethod
    def geocode_address(cls, address_or_zip):
        """Convert address or zip code to coordinates"""
        # Check if it's a zip code (5 digits)
        if address_or_zip.isdigit() and len(address_or_zip) == 5:
            return cls._geocode_zip(address_or_zip)
        else:
            return cls._geocode_address(address_or_zip)

    @classmethod
    def _geocode_zip(cls, zip_code):
        """Basic zip code to coordinates mapping for LA area"""
        zip_coords = {
            "90210": (34.1030, -118.4104),  # Beverly Hills
            "90211": (34.0901, -118.4065),  # Beverly Hills
            "90028": (34.1016, -118.3267),  # Hollywood
            "90046": (34.1056, -118.3632),  # West Hollywood
            "91436": (34.1559, -118.4818),  # Encino
            "91301": (34.2209, -118.6010),  # Agoura Hills
            "90405": (34.0195, -118.4912),  # Santa Monica
            "90401": (34.0194, -118.4912),  # Santa Monica
            "91505": (34.1808, -118.3090),  # Burbank
            "91304": (34.2703, -118.7370),  # Canoga Park
            "90401": (34.0194, -118.4912),  # Santa Monica
            "90402": (34.0301, -118.4951),  # Santa Monica
            "90403": (34.0287, -118.4668),  # Santa Monica
            "90404": (34.0194, -118.4912),  # Santa Monica
            "91302": (34.1678, -118.5946),  # Calabasas
            "91307": (34.1984, -118.6120),  # West Hills
            "91316": (34.1610, -118.5079),  # Encino
            "91324": (34.2386, -118.5645),  # Northridge
            "91325": (34.2386, -118.5645),  # Northridge
            "91326": (34.2386, -118.5645),  # Northridge
            "91330": (34.2514, -118.4456),  # Hansen Dam
            "91331": (34.2642, -118.4456),  # Pacoima
            "91335": (34.2217, -118.4456),  # Reseda
            "91340": (34.2717, -118.4123),  # San Fernando
            "91342": (34.2717, -118.4456),  # Sylmar
            "91343": (34.3128, -118.4456),  # North Hills
            "91344": (34.2649, -118.5037),  # Granada Hills
            "91345": (34.2386, -118.5645),  # Northridge
            "91352": (34.2717, -118.4789),  # Sun Valley
            "91354": (34.2931, -118.4912),  # Valencia
            "91355": (34.4233, -118.5778),  # Valencia
            "91356": (34.1713, -118.5358),  # Tarzana
            "91357": (34.1713, -118.5358),  # Tarzana
            "91364": (34.1678, -118.5946),  # Woodland Hills
            "91365": (34.1678, -118.5946),  # Woodland Hills
            "91367": (34.1699, -118.6078),  # Woodland Hills
            "91401": (34.1716, -118.4192),  # Van Nuys
            "91402": (34.1686, -118.4912),  # Panorama City
            "91403": (34.1611, -118.4678),  # Sherman Oaks
            "91406": (34.2008, -118.5030),  # Van Nuys
            "91411": (34.1986, -118.4789),  # Van Nuys
            "91423": (34.1869, -118.4456),  # Sherman Oaks
            "91601": (34.1808, -118.3090),  # North Hollywood
            "91602": (34.1869, -118.3789),  # North Hollywood
            "91604": (34.1446, -118.4112),  # Studio City
            "91605": (34.1869, -118.3789),  # North Hollywood
            "91606": (34.1869, -118.3789),  # North Hollywood
            "91607": (34.1508, -118.3912),  # Valley Village
            "91608": (34.1869, -118.3789),  # Universal City
        }
        return zip_coords.get(zip_code)

    @classmethod
    def _geocode_address(cls, address):
        """Basic address geocoding"""
        address_lower = address.lower()
        if "los angeles" in address_lower:
            return (34.0522, -118.2437)
        elif "santa monica" in address_lower:
            return (34.0195, -118.4912)
        elif "beverly hills" in address_lower:
            return (34.1030, -118.4104)
        elif "encino" in address_lower:
            return (34.1559, -118.4818)
        elif "van nuys" in address_lower:
            return (34.2008, -118.5030)
        elif "burbank" in address_lower:
            return (34.1808, -118.3090)
        return None

    def get_served_providers(self):
        """Get providers that work with this regional center"""
        return self.providers.all()


class Provider(models.Model):
    # Basic Information
    name = models.CharField(max_length=255)
    phone = models.CharField(max_length=100, blank=True, null=True)
    address = models.TextField(blank=True, null=True)
    website_domain = models.URLField(blank=True, null=True)

    # Geographic Information
    latitude = models.DecimalField(
        max_digits=9, decimal_places=6, blank=True, null=True
    )
    longitude = models.DecimalField(
        max_digits=9, decimal_places=6, blank=True, null=True
    )

    # Service Areas and Centers
    center_based_services = models.TextField(blank=True, null=True)
    areas = models.TextField(blank=True, null=True)  # Geographic coverage areas

    # Service Details
    specializations = models.TextField(blank=True, null=True)
    insurance_accepted = models.TextField(blank=True, null=True)
    services = models.TextField(blank=True, null=True)

    # Legacy fields for backward compatibility
    coverage_areas = models.TextField(blank=True, null=True)

    class Meta:
        db_table = "providers"

    def __str__(self):
        return self.name

    @classmethod
    def find_nearest(cls, latitude, longitude, radius_miles=10, limit=20):
        """Find providers within radius of given coordinates"""
        from django.db import connection

        # Use Haversine formula for distance calculation
        with connection.cursor() as cursor:
            cursor.execute(
                """
                SELECT id, name, phone, address, latitude, longitude,
                       (3959 * acos(cos(radians(%s)) * cos(radians(latitude)) * 
                       cos(radians(longitude) - radians(%s)) + sin(radians(%s)) * 
                       sin(radians(latitude)))) AS distance
                FROM providers 
                WHERE latitude IS NOT NULL AND longitude IS NOT NULL
                AND (3959 * acos(cos(radians(%s)) * cos(radians(latitude)) * 
                cos(radians(longitude) - radians(%s)) + sin(radians(%s)) * 
                sin(radians(latitude)))) < %s
                ORDER BY distance
                LIMIT %s
            """,
                [
                    latitude,
                    longitude,
                    latitude,
                    latitude,
                    longitude,
                    latitude,
                    radius_miles,
                    limit,
                ],
            )

            columns = [col[0] for col in cursor.description]
            results = []
            for row in cursor.fetchall():
                provider_data = dict(zip(columns, row))
                provider = cls.objects.get(id=provider_data["id"])
                provider.distance = provider_data["distance"]
                results.append(provider)
            return results

    @classmethod
    def geocode_and_search(cls, address_or_zip, radius_miles=10, limit=20):
        """Geocode an address/zip and find nearby providers"""
        coordinates = RegionalCenter.geocode_address(address_or_zip)
        if coordinates:
            return cls.find_nearest(coordinates[0], coordinates[1], radius_miles, limit)
        return []

    def get_serving_regional_centers(self):
        """Get regional centers that serve this provider's area"""
        return self.regional_centers.all()

    def get_distance_to(self, latitude, longitude):
        """Calculate distance from provider to given coordinates"""
        if not self.latitude or not self.longitude:
            return None

        # Haversine formula
        lat1, lon1 = float(self.latitude), float(self.longitude)
        lat2, lon2 = float(latitude), float(longitude)

        R = 3959  # Earth's radius in miles
        dlat = math.radians(lat2 - lat1)
        dlon = math.radians(lon2 - lon1)
        a = math.sin(dlat / 2) * math.sin(dlat / 2) + math.cos(
            math.radians(lat1)
        ) * math.cos(math.radians(lat2)) * math.sin(dlon / 2) * math.sin(dlon / 2)
        c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
        return R * c

    # Helper properties for frontend compatibility
    @property
    def city(self):
        if self.address:
            # Extract city from address (format: "address, city, state zip")
            parts = self.address.split(",")
            if len(parts) >= 2:
                return (
                    parts[-2].strip().split()[-2]
                    if len(parts[-2].strip().split()) > 1
                    else ""
                )
        return ""

    @property
    def state(self):
        if self.address:
            # Extract state from address
            parts = self.address.split(",")
            if len(parts) >= 2:
                last_part = parts[-1].strip()
                if " " in last_part:
                    return last_part.split()[0]
        return "CA"

    @property
    def zip_code(self):
        if self.address:
            # Extract zip from address
            parts = self.address.split(",")
            if len(parts) >= 2:
                last_part = parts[-1].strip()
                if " " in last_part:
                    return last_part.split()[-1]
        return ""

    # Backward compatibility properties
    @property
    def age_groups_served(self):
        return ""

    @property
    def diagnoses_served(self):
        return self.specializations or ""

    @property
    def accepts_insurance(self):
        return bool(self.insurance_accepted)

    @property
    def accepts_private_pay(self):
        return "private pay" in (self.insurance_accepted or "").lower()

    @property
    def accepts_regional_center(self):
        return "regional center" in (self.insurance_accepted or "").lower()

    @property
    def website(self):
        return self.website_domain


class ProviderRegionalCenter(models.Model):
    """Many-to-many relationship between providers and regional centers they work with"""

    provider = models.ForeignKey(
        Provider, on_delete=models.CASCADE, related_name="regional_centers"
    )
    regional_center = models.ForeignKey(
        RegionalCenter, on_delete=models.CASCADE, related_name="providers"
    )
    is_primary = models.BooleanField(
        default=False
    )  # Primary regional center for this provider
    notes = models.TextField(blank=True, null=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        unique_together = ("provider", "regional_center")
        db_table = "provider_regional_centers"

    def __str__(self):
        return f"{self.provider.name} - {self.regional_center.regional_center}"


class LocationImage(models.Model):
    location = models.ForeignKey(
        Location, on_delete=models.CASCADE, related_name="images"
    )
    image = models.ImageField(upload_to="location_images/")
    caption = models.CharField(max_length=255, blank=True, null=True)
    is_primary = models.BooleanField(default=False)
    uploaded_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"Image for {self.location.name}"


class LocationReview(models.Model):
    location = models.ForeignKey(
        Location, on_delete=models.CASCADE, related_name="reviews"
    )
    name = models.CharField(max_length=100)
    email = models.EmailField()
    rating = models.IntegerField(
        choices=[(1, "1"), (2, "2"), (3, "3"), (4, "4"), (5, "5")]
    )
    comment = models.TextField()
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"Review for {self.location.name} by {self.name}"


# New models for provider funding and service types
class FundingSource(models.Model):
    name = models.CharField(max_length=100, unique=True)
    description = models.TextField(blank=True, null=True)

    def __str__(self):
        return self.name


class InsuranceCarrier(models.Model):
    name = models.CharField(max_length=100, unique=True)
    description = models.TextField(blank=True, null=True)

    def __str__(self):
        return self.name


class ServiceDeliveryModel(models.Model):
    name = models.CharField(max_length=100, unique=True)
    description = models.TextField(blank=True, null=True)

    def __str__(self):
        return self.name


class ProviderFundingSource(models.Model):
    provider = models.ForeignKey(
        Provider, on_delete=models.CASCADE, related_name="funding_sources"
    )
    funding_source = models.ForeignKey(
        FundingSource, on_delete=models.CASCADE, related_name="providers"
    )

    class Meta:
        unique_together = ("provider", "funding_source")

    def __str__(self):
        return f"{self.provider.name} - {self.funding_source.name}"


class ProviderInsuranceCarrier(models.Model):
    provider = models.ForeignKey(
        Provider, on_delete=models.CASCADE, related_name="insurance_carriers"
    )
    insurance_carrier = models.ForeignKey(
        InsuranceCarrier, on_delete=models.CASCADE, related_name="providers"
    )

    class Meta:
        unique_together = ("provider", "insurance_carrier")

    def __str__(self):
        return f"{self.provider.name} - {self.insurance_carrier.name}"


class ProviderServiceModel(models.Model):
    provider = models.ForeignKey(
        Provider, on_delete=models.CASCADE, related_name="service_models"
    )
    service_model = models.ForeignKey(
        ServiceDeliveryModel, on_delete=models.CASCADE, related_name="providers"
    )

    class Meta:
        unique_together = ("provider", "service_model")

    def __str__(self):
        return f"{self.provider.name} - {self.service_model.name}"
