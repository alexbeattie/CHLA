from rest_framework import serializers
from decimal import Decimal, ROUND_HALF_UP, InvalidOperation
from .utils.geocode import geocode_address

# from rest_framework_gis.serializers import GeoFeatureModelSerializer
from .models import (
    Location,
    LocationCategory,
    RegionalCenter,
    # Provider model removed - use ProviderV2
    LocationImage,
    LocationReview,
    FundingSource,
    InsuranceCarrier,
    ServiceDeliveryModel,
    ProviderFundingSource,
    ProviderInsuranceCarrier,
    ProviderServiceModel,
    ProviderRegionalCenter,
    ProviderV2,
)


class LocationCategorySerializer(serializers.ModelSerializer):
    class Meta:
        model = LocationCategory
        fields = "__all__"


class LocationImageSerializer(serializers.ModelSerializer):
    class Meta:
        model = LocationImage
        fields = ["id", "image", "caption", "is_primary"]


class LocationReviewSerializer(serializers.ModelSerializer):
    class Meta:
        model = LocationReview
        fields = ["id", "name", "rating", "comment", "created_at"]


class LocationSerializer(serializers.ModelSerializer):
    category_name = serializers.ReadOnlyField(source="category.name")
    images = LocationImageSerializer(many=True, read_only=True)
    reviews = LocationReviewSerializer(many=True, read_only=True)
    average_rating = serializers.SerializerMethodField()

    class Meta:
        model = Location
        fields = [
            "id",
            "name",
            "address",
            "city",
            "state",
            "zip_code",
            "latitude",
            "longitude",
            "description",
            "phone",
            "website",
            "email",
            "is_active",
            "category",
            "category_name",
            "created_at",
            "updated_at",
            "rating",
            "price_level",
            "hours_of_operation",
            "has_parking",
            "is_accessible",
            "images",
            "reviews",
            "average_rating",
        ]

    def get_average_rating(self, obj):
        if obj.reviews.exists():
            return (
                sum(review.rating for review in obj.reviews.all()) / obj.reviews.count()
            )
        return 0


# Enhanced RegionalCenter serializer with service area info
class RegionalCenterSerializer(serializers.ModelSerializer):
    served_providers = serializers.SerializerMethodField()
    service_area_geojson = serializers.SerializerMethodField()
    has_service_area = serializers.SerializerMethodField()
    distance = serializers.SerializerMethodField()

    class Meta:
        model = RegionalCenter
        fields = [
            "id",
            "regional_center",
            "office_type",
            "address",
            "suite",
            "city",
            "state",
            "zip_code",
            "telephone",
            "website",
            "county_served",
            "los_angeles_health_district",
            "latitude",
            "longitude",
            "service_radius_miles",
            "has_service_area",
            "service_area_geojson",
            "served_providers",
            "distance",  # Distance in miles (when using PostGIS queries)
        ]

    def get_served_providers(self, obj):
        """Get providers served by this regional center"""
        try:
            relationships = ProviderRegionalCenter.objects.filter(
                regional_center=obj
            ).select_related("provider")
            return [
                {
                    "id": rel.provider.id,
                    "name": rel.provider.name,
                    "is_primary": rel.is_primary,
                }
                for rel in relationships
            ]
        except:
            return []

    def get_service_area_geojson(self, obj):
        """Get the service area as GeoJSON"""
        return obj.get_service_area_as_geojson()

    def get_has_service_area(self, obj):
        """Check if this regional center has a service area polygon"""
        return obj.service_area is not None

    def get_distance(self, obj):
        """Get distance in miles if available from PostGIS query"""
        if hasattr(obj, 'distance') and obj.distance is not None:
            # Distance is already converted to miles in the view
            return round(float(obj.distance), 2)
        return None


# GeoJSON serializer for regional centers with full geospatial data
class GeoJSONRegionalCenterSerializer(serializers.Serializer):
    """Serializer that returns proper GeoJSON Feature objects for regional centers"""

    def to_representation(self, obj):
        geojson_data = obj.get_service_area_as_geojson()

        if geojson_data:
            return {
                "type": "Feature",
                "geometry": geojson_data,
                "properties": {
                    "id": obj.id,
                    "regional_center": obj.regional_center,
                    "county_served": obj.county_served,
                    "office_type": obj.office_type,
                    "address": obj.address,
                    "city": obj.city,
                    "state": obj.state,
                    "zip_code": obj.zip_code,
                    "telephone": obj.telephone,
                    "website": obj.website,
                },
            }
        return None


# Simplified serializer for service areas only (for map overlays)
class ServiceAreaSerializer(serializers.Serializer):
    """Serializer that returns proper GeoJSON Feature objects for service areas only"""

    def to_representation(self, obj):
        geojson_data = obj.get_service_area_as_geojson()

        if geojson_data:
            return {
                "type": "Feature",
                "geometry": geojson_data,
                "properties": {
                    "id": obj.id,
                    "center_name": obj.regional_center,
                    "county_served": obj.county_served,
                    "service_radius_miles": obj.service_radius_miles,
                },
            }
        return None


# New serializers for the funding and service models
class FundingSourceSerializer(serializers.ModelSerializer):
    class Meta:
        model = FundingSource
        fields = ["id", "name", "description"]


class InsuranceCarrierSerializer(serializers.ModelSerializer):
    class Meta:
        model = InsuranceCarrier
        fields = ["id", "name", "description"]


class ServiceDeliveryModelSerializer(serializers.ModelSerializer):
    class Meta:
        model = ServiceDeliveryModel
        fields = ["id", "name", "description"]


# Relationship serializers
class ProviderFundingSourceSerializer(serializers.ModelSerializer):
    funding_source_name = serializers.ReadOnlyField(source="funding_source.name")

    class Meta:
        model = ProviderFundingSource
        fields = ["id", "provider", "funding_source", "funding_source_name"]


class ProviderInsuranceCarrierSerializer(serializers.ModelSerializer):
    insurance_carrier_name = serializers.ReadOnlyField(source="insurance_carrier.name")

    class Meta:
        model = ProviderInsuranceCarrier
        fields = ["id", "provider", "insurance_carrier", "insurance_carrier_name"]


class ProviderServiceModelSerializer(serializers.ModelSerializer):
    service_model_name = serializers.ReadOnlyField(source="service_model.name")

    class Meta:
        model = ProviderServiceModel
        fields = ["id", "provider", "service_model", "service_model_name"]


# ProviderRegionalCenter relationship serializer
class ProviderRegionalCenterSerializer(serializers.ModelSerializer):
    provider_name = serializers.CharField(source="provider.name", read_only=True)
    regional_center_name = serializers.CharField(
        source="regional_center.regional_center", read_only=True
    )

    class Meta:
        model = ProviderRegionalCenter
        fields = [
            "id",
            "provider",
            "regional_center",
            "is_primary",
            "notes",
            "created_at",
            "provider_name",
            "regional_center_name",
        ]


# Enhanced Provider serializer with regional centers
# REMOVED: Old ProviderSerializer - Use ProviderV2Serializer instead


# ProviderV2 serializer with enum array support
class ProviderV2Serializer(serializers.ModelSerializer):
    # Read-only computed fields for compatibility
    city = serializers.ReadOnlyField()
    state = serializers.ReadOnlyField()
    zip_code = serializers.ReadOnlyField()
    age_groups_served = serializers.ReadOnlyField()
    diagnoses_served = serializers.ReadOnlyField()
    accepts_insurance = serializers.ReadOnlyField()
    accepts_private_pay = serializers.ReadOnlyField()
    accepts_regional_center = serializers.ReadOnlyField()
    website_domain = serializers.ReadOnlyField()
    center_based_services = serializers.ReadOnlyField()
    areas = serializers.ReadOnlyField()
    specializations = serializers.ReadOnlyField()
    services = serializers.ReadOnlyField()
    coverage_areas = serializers.ReadOnlyField()
    
    # Distance field (dynamically added when using PostGIS queries)
    distance = serializers.SerializerMethodField()

    # Optional: Include related regional centers
    serving_regional_centers = serializers.SerializerMethodField()

    class Meta:
        model = ProviderV2
        fields = [
            "id",
            "name",
            "type",
            "phone",
            "email",
            "website",
            "description",
            "verified",
            "latitude",
            "longitude",
            "address",
            "hours",
            "insurance_accepted",
            "languages_spoken",
            "created_at",
            "updated_at",
            # Computed fields for frontend compatibility
            "city",
            "state",
            "zip_code",
            "age_groups_served",
            "diagnoses_served",
            "accepts_insurance",
            "accepts_private_pay",
            "accepts_regional_center",
            "website_domain",
            "center_based_services",
            "areas",
            "specializations",
            "services",
            "coverage_areas",
            "serving_regional_centers",
            "distance",  # Distance in miles (when using PostGIS queries)
        ]

    def get_distance(self, obj):
        """Get distance in miles if available from PostGIS query"""
        if hasattr(obj, 'distance') and obj.distance is not None:
            # Distance is already converted to miles in the view
            return round(float(obj.distance), 2)
        return None

    def get_serving_regional_centers(self, obj):
        """Get regional centers serving this provider"""
        return []  # Temporarily simplified


# ProviderV2 write serializer
class ProviderV2WriteSerializer(serializers.ModelSerializer):
    def validate_latitude(self, value):
        if value in (None, ""):
            return None
        try:
            dec = Decimal(str(value)).quantize(
                Decimal("0.000001"), rounding=ROUND_HALF_UP
            )
            return dec
        except (InvalidOperation, ValueError, TypeError):
            return value

    def validate_longitude(self, value):
        if value in (None, ""):
            return None
        try:
            dec = Decimal(str(value)).quantize(
                Decimal("0.000001"), rounding=ROUND_HALF_UP
            )
            return dec
        except (InvalidOperation, ValueError, TypeError):
            return value

    def validate(self, attrs):
        # If address is provided but no coordinates, try to geocode
        address = attrs.get("address")
        lat = attrs.get("latitude")
        lng = attrs.get("longitude")
        if address and (lat is None or lng is None):
            coords = geocode_address(address)
            if coords:
                attrs["latitude"], attrs["longitude"] = coords

        # Normalize coordinate precision to fit model constraints
        def normalize(value):
            if value is None or value == "":
                return None
            try:
                dec = Decimal(str(value)).quantize(
                    Decimal("0.000001"), rounding=ROUND_HALF_UP
                )
                return dec
            except (InvalidOperation, ValueError, TypeError):
                return value

        attrs["latitude"] = normalize(attrs.get("latitude"))
        attrs["longitude"] = normalize(attrs.get("longitude"))
        return attrs

    class Meta:
        model = ProviderV2
        fields = [
            "name",
            "type",
            "phone",
            "email",
            "website",
            "description",
            "verified",
            "address",
            "latitude",
            "longitude",
            "hours",
            "insurance_accepted",
            "languages_spoken",
        ]


# Provider serializer for write operations (create, update, delete)
# REMOVED: Old ProviderWriteSerializer and ProviderGeoSerializer
# Use ProviderV2WriteSerializer instead
