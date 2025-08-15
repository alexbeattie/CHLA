from rest_framework import serializers
# from rest_framework_gis.serializers import GeoFeatureModelSerializer
from .models import (
    Location,
    LocationCategory,
    RegionalCenter,
    Provider,
    LocationImage,
    LocationReview,
    FundingSource,
    InsuranceCarrier,
    ServiceDeliveryModel,
    ProviderFundingSource,
    ProviderInsuranceCarrier,
    ProviderServiceModel,
    ProviderRegionalCenter,
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
                }
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
                }
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
class ProviderSerializer(serializers.ModelSerializer):
    # Read-only computed fields for compatibility
    city = serializers.ReadOnlyField()
    state = serializers.ReadOnlyField()
    zip_code = serializers.ReadOnlyField()
    age_groups_served = serializers.ReadOnlyField()
    diagnoses_served = serializers.ReadOnlyField()
    accepts_insurance = serializers.ReadOnlyField()
    accepts_private_pay = serializers.ReadOnlyField()
    accepts_regional_center = serializers.ReadOnlyField()
    website = serializers.ReadOnlyField()

    # Optional: Include related regional centers
    serving_regional_centers = serializers.SerializerMethodField()

    class Meta:
        model = Provider
        fields = [
            "id",
            "name",
            "phone",
            "address",
            "website_domain",
            "latitude",
            "longitude",
            "center_based_services",
            "areas",
            "specializations",
            "insurance_accepted",
            "services",
            "coverage_areas",
            # Computed fields for frontend compatibility
            "city",
            "state",
            "zip_code",
            "age_groups_served",
            "diagnoses_served",
            "accepts_insurance",
            "accepts_private_pay",
            "accepts_regional_center",
            "website",
            "serving_regional_centers",
        ]

    def get_serving_regional_centers(self, obj):
        """Get regional centers serving this provider"""
        try:
            relationships = ProviderRegionalCenter.objects.filter(
                provider=obj
            ).select_related("regional_center")
            return [
                {
                    "id": rel.regional_center.id,
                    "name": rel.regional_center.regional_center,
                    "is_primary": rel.is_primary,
                }
                for rel in relationships
            ]
        except:
            return []


# GeoJSON serializer for Provider (simplified)
class ProviderGeoSerializer(serializers.ModelSerializer):
    # Virtual fields for compatibility with frontend
    address = serializers.ReadOnlyField()
    city = serializers.ReadOnlyField()
    state = serializers.ReadOnlyField()
    zip_code = serializers.ReadOnlyField()
    latitude = serializers.ReadOnlyField()
    longitude = serializers.ReadOnlyField()
    age_groups_served = serializers.ReadOnlyField()
    diagnoses_served = serializers.ReadOnlyField()
    accepts_insurance = serializers.ReadOnlyField()
    accepts_private_pay = serializers.ReadOnlyField()
    accepts_regional_center = serializers.ReadOnlyField()
    accepts_school_funding = serializers.ReadOnlyField()
    distance = serializers.FloatField(required=False, read_only=True)

    class Meta:
        model = Provider
        fields = [
            "id",
            "name",
            "phone",
            "coverage_areas",
            "center_based_services",
            "areas",
            "address",
            "city",
            "state",
            "zip_code",
            "latitude",
            "longitude",
            "age_groups_served",
            "diagnoses_served",
            "accepts_insurance",
            "accepts_private_pay",
            "accepts_regional_center",
            "accepts_school_funding",
            "distance",
        ]
