from rest_framework import serializers
from .models import LocationCategory, Location, LocationImage, LocationReview

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