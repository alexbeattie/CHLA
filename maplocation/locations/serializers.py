from rest_framework import serializers
from rest_framework_gis.serializers import GeoFeatureModelSerializer
from .models import (
    LocationCategory, Location, LocationImage, LocationReview, 
    RegionalCenter, Provider, FundingSource, InsuranceCarrier, 
    ServiceDeliveryModel, ProviderFundingSource, ProviderInsuranceCarrier,
    ProviderServiceModel
)

class LocationCategorySerializer(serializers.ModelSerializer):
    class Meta:
        model = LocationCategory
        fields = '__all__'

class LocationImageSerializer(serializers.ModelSerializer):
    class Meta:
        model = LocationImage
        fields = ['id', 'image', 'caption', 'is_primary']

class LocationReviewSerializer(serializers.ModelSerializer):
    class Meta:
        model = LocationReview
        fields = ['id', 'name', 'rating', 'comment', 'created_at']

class LocationSerializer(serializers.ModelSerializer):
    category_name = serializers.ReadOnlyField(source='category.name')
    images = LocationImageSerializer(many=True, read_only=True)
    reviews = LocationReviewSerializer(many=True, read_only=True)
    average_rating = serializers.SerializerMethodField()
    
    class Meta:
        model = Location
        fields = [
            'id', 'name', 'address', 'city', 'state', 'zip_code',
            'latitude', 'longitude', 'description', 'phone',
            'website', 'email', 'is_active', 'category', 'category_name',
            'created_at', 'updated_at', 'rating', 'price_level',
            'hours_of_operation', 'has_parking', 'is_accessible',
            'images', 'reviews', 'average_rating'
        ]
    
    def get_average_rating(self, obj):
        if obj.reviews.exists():
            return sum(review.rating for review in obj.reviews.all()) / obj.reviews.count()
        return 0

class RegionalCenterSerializer(serializers.ModelSerializer):
    class Meta:
        model = RegionalCenter
        fields = [
            'id', 'regional_center', 'office_type', 'address', 'suite',
            'city', 'state', 'zip_code', 'telephone', 'website',
            'county_served', 'los_angeles_health_district',
            'location_coordinates', 'latitude', 'longitude', 'location'
        ]

# New serializers for the funding and service models
class FundingSourceSerializer(serializers.ModelSerializer):
    class Meta:
        model = FundingSource
        fields = ['id', 'name', 'description']

class InsuranceCarrierSerializer(serializers.ModelSerializer):
    class Meta:
        model = InsuranceCarrier
        fields = ['id', 'name', 'description']

class ServiceDeliveryModelSerializer(serializers.ModelSerializer):
    class Meta:
        model = ServiceDeliveryModel
        fields = ['id', 'name', 'description']

# Relationship serializers
class ProviderFundingSourceSerializer(serializers.ModelSerializer):
    funding_source_name = serializers.ReadOnlyField(source='funding_source.name')
    
    class Meta:
        model = ProviderFundingSource
        fields = ['id', 'provider', 'funding_source', 'funding_source_name']

class ProviderInsuranceCarrierSerializer(serializers.ModelSerializer):
    insurance_carrier_name = serializers.ReadOnlyField(source='insurance_carrier.name')
    
    class Meta:
        model = ProviderInsuranceCarrier
        fields = ['id', 'provider', 'insurance_carrier', 'insurance_carrier_name']

class ProviderServiceModelSerializer(serializers.ModelSerializer):
    service_model_name = serializers.ReadOnlyField(source='service_model.name')
    
    class Meta:
        model = ProviderServiceModel
        fields = ['id', 'provider', 'service_model', 'service_model_name']

# Enhanced Provider serializer
class ProviderSerializer(serializers.ModelSerializer):
    # Virtual fields for compatibility with frontend
    distance = serializers.FloatField(required=False, read_only=True)
    funding_sources = ProviderFundingSourceSerializer(many=True, read_only=True)
    insurance_carriers = ProviderInsuranceCarrierSerializer(many=True, read_only=True)
    service_models = ProviderServiceModelSerializer(many=True, read_only=True)
    
    # Convenience fields for display
    funding_source_names = serializers.SerializerMethodField()
    insurance_carrier_names = serializers.SerializerMethodField()
    service_model_names = serializers.SerializerMethodField()
    
    class Meta:
        model = Provider
        fields = [
            'id', 'name', 'phone', 'coverage_areas', 'center_based_services', 'areas',
            'address', 'city', 'state', 'zip_code', 'latitude', 'longitude',
            'website', 'age_groups_served', 'diagnoses_served',
            'accepts_insurance', 'accepts_private_pay', 'accepts_regional_center', 'accepts_school_funding',
            'regional_centers_served', 'telehealth_available', 'application_process',
            'eligibility_requirements', 'waiting_list', 'waiting_list_time',
            'distance', 'funding_sources', 'insurance_carriers', 'service_models',
            'funding_source_names', 'insurance_carrier_names', 'service_model_names'
        ]
    
    def get_funding_source_names(self, obj):
        """Get a list of funding source names for this provider"""
        return [
            pfs.funding_source.name for pfs in 
            obj.funding_sources.select_related('funding_source').all()
        ]
    
    def get_insurance_carrier_names(self, obj):
        """Get a list of insurance carrier names for this provider"""
        return [
            pic.insurance_carrier.name for pic in 
            obj.insurance_carriers.select_related('insurance_carrier').all()
        ]
    
    def get_service_model_names(self, obj):
        """Get a list of service model names for this provider"""
        return [
            psm.service_model.name for psm in 
            obj.service_models.select_related('service_model').all()
        ]

# GeoJSON serializer for Provider (useful for map display)
class ProviderGeoSerializer(GeoFeatureModelSerializer):
    # Include all fields from regular serializer
    funding_sources = ProviderFundingSourceSerializer(many=True, read_only=True)
    insurance_carriers = ProviderInsuranceCarrierSerializer(many=True, read_only=True)
    service_models = ProviderServiceModelSerializer(many=True, read_only=True)
    funding_source_names = serializers.SerializerMethodField()
    insurance_carrier_names = serializers.SerializerMethodField()
    service_model_names = serializers.SerializerMethodField()
    distance = serializers.FloatField(required=False, read_only=True)
    
    class Meta:
        model = Provider
        geo_field = "location"
        fields = [
            'id', 'name', 'phone', 'coverage_areas', 'center_based_services', 'areas',
            'address', 'city', 'state', 'zip_code', 'latitude', 'longitude',
            'website', 'age_groups_served', 'diagnoses_served',
            'accepts_insurance', 'accepts_private_pay', 'accepts_regional_center', 'accepts_school_funding',
            'regional_centers_served', 'telehealth_available', 'application_process',
            'eligibility_requirements', 'waiting_list', 'waiting_list_time',
            'distance', 'funding_sources', 'insurance_carriers', 'service_models',
            'funding_source_names', 'insurance_carrier_names', 'service_model_names'
        ]
    
    def get_funding_source_names(self, obj):
        """Get a list of funding source names for this provider"""
        return [
            pfs.funding_source.name for pfs in 
            obj.funding_sources.select_related('funding_source').all()
        ]
    
    def get_insurance_carrier_names(self, obj):
        """Get a list of insurance carrier names for this provider"""
        return [
            pic.insurance_carrier.name for pic in 
            obj.insurance_carriers.select_related('insurance_carrier').all()
        ]
    
    def get_service_model_names(self, obj):
        """Get a list of service model names for this provider"""
        return [
            psm.service_model.name for psm in 
            obj.service_models.select_related('service_model').all()
        ]
