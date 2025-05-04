from rest_framework import viewsets, filters
from rest_framework.decorators import action
from rest_framework.response import Response
from django.db.models import Q, Avg
from django_filters.rest_framework import DjangoFilterBackend
from .models import LocationCategory, Location, LocationImage, LocationReview
from .serializers import (
    LocationCategorySerializer, 
    LocationSerializer, 
    LocationImageSerializer, 
    LocationReviewSerializer
)
import math

class LocationCategoryViewSet(viewsets.ReadOnlyModelViewSet):
    queryset = LocationCategory.objects.all()
    serializer_class = LocationCategorySerializer

class LocationViewSet(viewsets.ReadOnlyModelViewSet):
    queryset = Location.objects.filter(is_active=True)
    serializer_class = LocationSerializer
    filter_backends = [DjangoFilterBackend, filters.SearchFilter, filters.OrderingFilter]
    filterset_fields = ['category', 'price_level', 'has_parking', 'is_accessible']
    search_fields = ['name', 'description', 'address', 'city', 'state']
    ordering_fields = ['name', 'rating', 'created_at']
    
    @action(detail=False, methods=['get'])
    def nearby(self, request):
        """
        Find locations near a specific lat/lng point.
        Query parameters:
        - lat: latitude (required)
        - lng: longitude (required)
        - radius: search radius in kilometers (default: 5)
        """
        try:
            # Get parameters with proper error handling
            lat_param = request.query_params.get('lat')
            lng_param = request.query_params.get('lng')
            radius_param = request.query_params.get('radius', '5')  # Default to 5 km
            
            # Validate parameters
            if not lat_param or not lng_param:
                return Response({"error": "lat and lng parameters are required"}, status=400)
            
            try:
                lat = float(lat_param)
                lng = float(lng_param)
                radius = float(radius_param)
            except (ValueError, TypeError):
                return Response({"error": "lat, lng, and radius must be valid numbers"}, status=400)
            
            # Earth's radius in kilometers
            earth_radius = 6371
            
            # Get all active locations
            locations = Location.objects.filter(is_active=True)
            
            # Filter locations based on distance
            nearby_locations = []
            for location in locations:
                try:
                    # Ensure latitude and longitude are valid numbers
                    loc_lat = float(location.latitude)
                    loc_lng = float(location.longitude)
                    
                    # Convert to radians
                    lat1, lng1 = math.radians(lat), math.radians(lng)
                    lat2, lng2 = math.radians(loc_lat), math.radians(loc_lng)
                    
                    # Haversine formula
                    dlng = lng2 - lng1
                    dlat = lat2 - lat1
                    a = math.sin(dlat/2)**2 + math.cos(lat1) * math.cos(lat2) * math.sin(dlng/2)**2
                    c = 2 * math.asin(math.sqrt(a))
                    distance = earth_radius * c  # in kilometers
                    
                    if distance <= radius:
                        # Add distance to location object
                        location.distance = round(distance, 2)
                        nearby_locations.append(location)
                except (ValueError, TypeError):
                    # Skip locations with invalid coordinates
                    continue
            
            # Sort by distance
            nearby_locations.sort(key=lambda x: getattr(x, 'distance', float('inf')))
            
            # Serialize the data
            serializer = self.get_serializer(nearby_locations, many=True)
            
            # Add distance to serialized data, ensuring we're working with mutable data
            serializer_data = list(serializer.data)
            for i, location in enumerate(nearby_locations):
                if i < len(serializer_data):  # Safety check
                    serializer_data[i] = dict(serializer_data[i])  # Make mutable if it's not
                    serializer_data[i]['distance'] = location.distance
            
            return Response(serializer_data)
            
        except Exception as e:
            # Log the error for debugging
            import traceback
            print(f"Error in nearby endpoint: {str(e)}")
            print(traceback.format_exc())
            return Response({"error": "An unexpected error occurred", "detail": str(e)}, status=500)

    @action(detail=False, methods=['get'])
    def filters(self, request):
        """Return available filter options"""
        categories = LocationCategory.objects.all()
        price_levels = Location.objects.values_list('price_level', flat=True).distinct()
        
        return Response({
            'categories': LocationCategorySerializer(categories, many=True).data,
            'price_levels': sorted(list(price_levels)),
            'amenities': [
                {'id': 'has_parking', 'name': 'Parking Available'},
                {'id': 'is_accessible', 'name': 'Accessibility Features'}
            ]
        })

class LocationImageViewSet(viewsets.ReadOnlyModelViewSet):
    queryset = LocationImage.objects.all()
    serializer_class = LocationImageSerializer
    filter_backends = [DjangoFilterBackend]
    filterset_fields = ['location', 'is_primary']

class LocationReviewViewSet(viewsets.ModelViewSet):
    queryset = LocationReview.objects.all()
    serializer_class = LocationReviewSerializer
    filter_backends = [DjangoFilterBackend]
    filterset_fields = ['location']
    
    # Only allow creating and reading reviews
    http_method_names = ['get', 'post']
