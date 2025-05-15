from rest_framework import viewsets, filters
from rest_framework.decorators import action
from rest_framework.response import Response
from django.db.models import Q, Avg
from django.contrib.gis.geos import Point
from django.contrib.gis.measure import D
from django_filters.rest_framework import DjangoFilterBackend
from .models import (
    LocationCategory, Location, LocationImage, LocationReview, 
    RegionalCenter, Provider, FundingSource, InsuranceCarrier, 
    ServiceDeliveryModel, ProviderFundingSource, ProviderInsuranceCarrier,
    ProviderServiceModel
)
from .serializers import (
    LocationCategorySerializer, LocationSerializer, LocationImageSerializer, 
    LocationReviewSerializer, RegionalCenterSerializer, ProviderSerializer,
    ProviderGeoSerializer, FundingSourceSerializer, InsuranceCarrierSerializer,
    ServiceDeliveryModelSerializer
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
            
            # Earth's radius in kilometers (6371) converted to miles (3959)
            earth_radius = 3959
            
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

class RegionalCenterViewSet(viewsets.ReadOnlyModelViewSet):
    queryset = RegionalCenter.objects.all()
    serializer_class = RegionalCenterSerializer
    filter_backends = [DjangoFilterBackend, filters.SearchFilter, filters.OrderingFilter]
    search_fields = ['regional_center', 'address', 'city', 'state', 'county_served', 'los_angeles_health_district']
    ordering_fields = ['regional_center', 'city', 'county_served']
    
    @action(detail=False, methods=['get'])
    def nearby(self, request):
        """
        Find regional centers near a specific lat/lng point.
        Query parameters:
        - lat: latitude (required)
        - lng: longitude (required)
        - radius: search radius in kilometers (default: 25) - larger default radius for regional centers
        """
        try:
            # Get parameters with proper error handling
            lat_param = request.query_params.get('lat')
            lng_param = request.query_params.get('lng')
            radius_param = request.query_params.get('radius', '25')  # Default to 25 km for regional centers
            
            # Validate parameters
            if not lat_param or not lng_param:
                return Response({"error": "lat and lng parameters are required"}, status=400)
            
            try:
                lat = float(lat_param)
                lng = float(lng_param)
                radius = float(radius_param)
            except (ValueError, TypeError):
                return Response({"error": "lat, lng, and radius must be valid numbers"}, status=400)
            
            # Earth's radius in miles
            earth_radius = 3959
            
            # Get all regional centers
            centers = RegionalCenter.objects.all()
            
            # Filter centers based on distance
            nearby_centers = []
            for center in centers:
                try:
                    # Ensure latitude and longitude are valid numbers
                    center_lat = float(center.latitude)
                    center_lng = float(center.longitude)
                    
                    # Convert to radians
                    lat1, lng1 = math.radians(lat), math.radians(lng)
                    lat2, lng2 = math.radians(center_lat), math.radians(center_lng)
                    
                    # Haversine formula
                    dlng = lng2 - lng1
                    dlat = lat2 - lat1
                    a = math.sin(dlat/2)**2 + math.cos(lat1) * math.cos(lat2) * math.sin(dlng/2)**2
                    c = 2 * math.asin(math.sqrt(a))
                    distance = earth_radius * c  # in kilometers
                    
                    if distance <= radius:
                        # Add distance to center object
                        center.distance = round(distance, 2)
                        nearby_centers.append(center)
                except (ValueError, TypeError):
                    # Skip centers with invalid coordinates
                    continue
            
            # Sort by distance
            nearby_centers.sort(key=lambda x: getattr(x, 'distance', float('inf')))
            
            # Serialize the data
            serializer = self.get_serializer(nearby_centers, many=True)
            
            # Add distance to serialized data
            serializer_data = list(serializer.data)
            for i, center in enumerate(nearby_centers):
                if i < len(serializer_data):  # Safety check
                    serializer_data[i] = dict(serializer_data[i])  # Make mutable if it's not
                    serializer_data[i]['distance'] = center.distance
                    
                    # Add name field for compatibility with generic components
                    if 'regional_center' in serializer_data[i] and 'name' not in serializer_data[i]:
                        serializer_data[i]['name'] = serializer_data[i]['regional_center']
                    
                    # Add phone field for compatibility with generic components
                    if 'telephone' in serializer_data[i] and 'phone' not in serializer_data[i]:
                        serializer_data[i]['phone'] = serializer_data[i]['telephone']
            
            return Response(serializer_data)
            
        except Exception as e:
            # Log the error for debugging
            import traceback
            print(f"Error in regional centers nearby endpoint: {str(e)}")
            print(traceback.format_exc())
            return Response({"error": "An unexpected error occurred", "detail": str(e)}, status=500)

# Reference data ViewSets
class FundingSourceViewSet(viewsets.ReadOnlyModelViewSet):
    queryset = FundingSource.objects.all()
    serializer_class = FundingSourceSerializer
    filter_backends = [filters.SearchFilter]
    search_fields = ['name', 'description']

class InsuranceCarrierViewSet(viewsets.ReadOnlyModelViewSet):
    queryset = InsuranceCarrier.objects.all()
    serializer_class = InsuranceCarrierSerializer
    filter_backends = [filters.SearchFilter]
    search_fields = ['name', 'description']

class ServiceDeliveryModelViewSet(viewsets.ReadOnlyModelViewSet):
    queryset = ServiceDeliveryModel.objects.all()
    serializer_class = ServiceDeliveryModelSerializer
    filter_backends = [filters.SearchFilter]
    search_fields = ['name', 'description']

class ProviderViewSet(viewsets.ReadOnlyModelViewSet):
    queryset = Provider.objects.all()
    serializer_class = ProviderSerializer
    filter_backends = [DjangoFilterBackend, filters.SearchFilter, filters.OrderingFilter]
    search_fields = ['name', 'coverage_areas', 'diagnoses_served', 'age_groups_served']
    ordering_fields = ['name']
    filterset_fields = [
        'accepts_insurance', 'accepts_private_pay', 'accepts_regional_center', 
        'accepts_school_funding', 'telehealth_available', 'waiting_list'
    ]
    
    @action(detail=False, methods=['get'])
    def by_area(self, request):
        """
        Find providers serving a specific area.
        Query parameters:
        - area: Area name (required)
        """
        try:
            # Get area parameter
            area_param = request.query_params.get('area')
            
            # Validate parameters
            if not area_param:
                return Response({"error": "area parameter is required"}, status=400)
            
            area = area_param.upper()  # Areas are stored in uppercase
            
            # Find providers serving this area
            # First try exact match in the areas array
            providers_in_area = Provider.objects.filter(areas__contains=[area])
            
            # If no results, try partial match in coverage_areas text field
            if not providers_in_area.exists():
                providers_in_area = Provider.objects.filter(coverage_areas__icontains=area)
            
            serializer = self.get_serializer(providers_in_area, many=True)
            return Response(serializer.data)
            
        except Exception as e:
            # Log the error for debugging
            import traceback
            print(f"Error in providers by_area endpoint: {str(e)}")
            print(traceback.format_exc())
            return Response({"error": "An unexpected error occurred", "detail": str(e)}, status=500)
    
    @action(detail=False, methods=['get'])
    def nearby(self, request):
        """
        Find providers near a specific lat/lng point.
        Query parameters:
        - lat: latitude (required)
        - lng: longitude (required)
        - radius: search radius in miles (default: 25)
        - age: age of client (optional)
        - diagnosis: diagnosis type (optional)
        - funding: funding source name (optional)
        - insurance: insurance carrier name (optional)
        - service_model: service delivery model name (optional)
        """
        try:
            # Get parameters with proper error handling
            lat_param = request.query_params.get('lat')
            lng_param = request.query_params.get('lng')
            radius_param = request.query_params.get('radius', '25')  # Default to 25 miles
            age_param = request.query_params.get('age')
            diagnosis_param = request.query_params.get('diagnosis')
            funding_param = request.query_params.get('funding')
            insurance_param = request.query_params.get('insurance')
            service_model_param = request.query_params.get('service_model')
            
            # Validate parameters
            if not lat_param or not lng_param:
                return Response({"error": "lat and lng parameters are required"}, status=400)
            
            try:
                lat = float(lat_param)
                lng = float(lng_param)
                radius = float(radius_param)
            except (ValueError, TypeError):
                return Response({"error": "lat, lng, and radius must be valid numbers"}, status=400)
            
            # Create point from user location
            user_location = Point(lng, lat, srid=4326)
            
            # Base queryset
            queryset = Provider.objects.all()
            
            # Filter by funding source if provided
            if funding_param:
                queryset = queryset.filter(
                    Q(funding_sources__funding_source__name__icontains=funding_param) |
                    Q(accepts_private_pay=True, funding_sources__funding_source__name='Private Pay') |
                    Q(accepts_regional_center=True, funding_sources__funding_source__name='Regional Center') |
                    Q(accepts_school_funding=True, funding_sources__funding_source__name='School/IEP')
                ).distinct()
            
            # Filter by insurance carrier if provided
            if insurance_param:
                queryset = queryset.filter(
                    insurance_carriers__insurance_carrier__name__icontains=insurance_param
                ).distinct()
            
            # Filter by service model if provided
            if service_model_param:
                queryset = queryset.filter(
                    service_models__service_model__name__icontains=service_model_param
                ).distinct()
            
            # Filter by diagnosis if provided
            if diagnosis_param:
                queryset = queryset.filter(
                    Q(diagnoses_served__icontains=diagnosis_param)
                )
            
            # Filter by age if provided
            if age_param:
                try:
                    age = int(age_param)
                    # This is a simplified approach - in real life, you'd want to parse age ranges
                    # more accurately, but this is a good starting point
                    queryset = queryset.filter(
                        Q(age_groups_served__icontains=f"{age}") |
                        Q(age_groups_served__icontains=f"0-{age}") |
                        Q(age_groups_served__icontains=f"{age}-") |
                        Q(age_groups_served__icontains=f"-{age}") |
                        Q(age_groups_served__icontains=f"{age}+")
                    )
                except ValueError:
                    # If age is not a valid number, ignore this filter
                    pass
            
            # Now filter by distance using PostGIS
            if user_location:
                # Convert radius from miles to meters (1 mile = 1609.34 meters)
                radius_meters = radius * 1609.34
                
                # Use spatial filter with the location field
                # Only include providers with valid location data
                queryset = queryset.filter(
                    location__isnull=False
                ).filter(
                    location__distance_lte=(user_location, D(m=radius_meters))
                )
                
                # Add distance as a property to each provider
                for provider in queryset:
                    if provider.location:
                        # Calculate distance in miles
                        distance_meters = user_location.distance(provider.location)
                        provider.distance = distance_meters / 1609.34  # Convert meters to miles
                    else:
                        provider.distance = None
                
                # Sort by distance
                queryset = sorted(queryset, key=lambda x: x.distance if x.distance is not None else float('inf'))
            
            # Use GeoJSON serializer if format=geojson is specified
            if request.query_params.get('format') == 'geojson':
                serializer = ProviderGeoSerializer(queryset, many=True)
            else:
                serializer = self.get_serializer(queryset, many=True)
            
            return Response(serializer.data)
            
        except Exception as e:
            # Log the error for debugging
            import traceback
            print(f"Error in providers nearby endpoint: {str(e)}")
            print(traceback.format_exc())
            return Response({"error": "An unexpected error occurred", "detail": str(e)}, status=500)
    
    @action(detail=False, methods=['get'])
    def by_funding(self, request):
        """
        Find providers by funding source.
        Query parameters:
        - source: Funding source name (required) - 'private', 'insurance', 'regional_center', 'school'
        - insurance: Insurance carrier name (optional, only used when source=insurance)
        """
        try:
            # Get funding source parameter
            source_param = request.query_params.get('source')
            insurance_param = request.query_params.get('insurance')
            
            # Validate parameters
            if not source_param:
                return Response({"error": "source parameter is required"}, status=400)
            
            # Base queryset
            queryset = Provider.objects.all()
            
            # Filter by funding source
            if source_param.lower() == 'private':
                queryset = queryset.filter(accepts_private_pay=True)
            elif source_param.lower() == 'regional_center':
                queryset = queryset.filter(accepts_regional_center=True)
            elif source_param.lower() == 'school':
                queryset = queryset.filter(accepts_school_funding=True)
            elif source_param.lower() == 'insurance':
                queryset = queryset.filter(accepts_insurance=True)
                
                # Filter by specific insurance carrier if provided
                if insurance_param:
                    queryset = queryset.filter(
                        insurance_carriers__insurance_carrier__name__icontains=insurance_param
                    ).distinct()
            
            serializer = self.get_serializer(queryset, many=True)
            return Response(serializer.data)
            
        except Exception as e:
            # Log the error for debugging
            import traceback
            print(f"Error in providers by_funding endpoint: {str(e)}")
            print(traceback.format_exc())
            return Response({"error": "An unexpected error occurred", "detail": str(e)}, status=500)
    
    @action(detail=False, methods=['get'])
    def by_diagnosis(self, request):
        """
        Find providers serving a specific diagnosis.
        Query parameters:
        - diagnosis: Diagnosis name (required)
        """
        try:
            # Get diagnosis parameter
            diagnosis_param = request.query_params.get('diagnosis')
            
            # Validate parameters
            if not diagnosis_param:
                return Response({"error": "diagnosis parameter is required"}, status=400)
            
            # Find providers serving this diagnosis
            providers_for_diagnosis = Provider.objects.filter(diagnoses_served__icontains=diagnosis_param)
            
            serializer = self.get_serializer(providers_for_diagnosis, many=True)
            return Response(serializer.data)
            
        except Exception as e:
            # Log the error for debugging
            import traceback
            print(f"Error in providers by_diagnosis endpoint: {str(e)}")
            print(traceback.format_exc())
            return Response({"error": "An unexpected error occurred", "detail": str(e)}, status=500)
    
    @action(detail=False, methods=['get'])
    def by_age_group(self, request):
        """
        Find providers serving a specific age group.
        Query parameters:
        - age: Client age (required)
        """
        try:
            # Get age parameter
            age_param = request.query_params.get('age')
            
            # Validate parameters
            if not age_param:
                return Response({"error": "age parameter is required"}, status=400)
            
            try:
                age = int(age_param)
            except ValueError:
                return Response({"error": "age must be a valid number"}, status=400)
            
            # Find providers serving this age group
            # This is a simplified approach - in real life, you'd want to parse age ranges
            # more accurately, but this is a good starting point
            providers = Provider.objects.filter(
                Q(age_groups_served__icontains=f"{age}") |
                Q(age_groups_served__icontains=f"0-{age}") |
                Q(age_groups_served__icontains=f"{age}-") |
                Q(age_groups_served__icontains=f"-{age}") |
                Q(age_groups_served__icontains=f"{age}+")
            )
            
            serializer = self.get_serializer(providers, many=True)
            return Response(serializer.data)
            
        except Exception as e:
            # Log the error for debugging
            import traceback
            print(f"Error in providers by_age_group endpoint: {str(e)}")
            print(traceback.format_exc())
            return Response({"error": "An unexpected error occurred", "detail": str(e)}, status=500)
    
    @action(detail=False, methods=['get'])
    def filters(self, request):
        """Return available filter options for providers"""
        funding_sources = FundingSource.objects.all()
        insurance_carriers = InsuranceCarrier.objects.all()
        service_models = ServiceDeliveryModel.objects.all()
        
        return Response({
            'funding_sources': FundingSourceSerializer(funding_sources, many=True).data,
            'insurance_carriers': InsuranceCarrierSerializer(insurance_carriers, many=True).data,
            'service_models': ServiceDeliveryModelSerializer(service_models, many=True).data,
            'other_filters': [
                {'id': 'telehealth_available', 'name': 'Telehealth Available'},
                {'id': 'waiting_list', 'name': 'No Waiting List', 'value': False}
            ]
        })
    
    @action(detail=False, methods=['get'])
    def comprehensive_search(self, request):
        """
        Comprehensive search for providers with many criteria.
        Query parameters:
        - q: Text search query
        - lat/lng: User's location
        - radius: Search radius in miles
        - zip: ZIP code to search near
        - age: Client age
        - diagnosis: Diagnosis (e.g., 'autism')
        - funding_source: ID or name of funding source
        - insurance: ID or name of insurance carrier
        - service_model: ID or name of service delivery model
        - telehealth: Whether telehealth services are required (true/false)
        - waiting_list: Filter by waiting list status (true/false)
        """
        try:
            # Get all parameters
            q = request.query_params.get('q', '').strip()
            lat_param = request.query_params.get('lat')
            lng_param = request.query_params.get('lng')
            radius_param = request.query_params.get('radius', '25')
            zip_param = request.query_params.get('zip')
            age_param = request.query_params.get('age')
            diagnosis_param = request.query_params.get('diagnosis')
            funding_source_param = request.query_params.get('funding_source')
            insurance_param = request.query_params.get('insurance')
            service_model_param = request.query_params.get('service_model')
            telehealth_param = request.query_params.get('telehealth')
            waiting_list_param = request.query_params.get('waiting_list')
            
            # Base queryset
            queryset = Provider.objects.all()
            
            # Apply text search if provided
            if q:
                queryset = queryset.filter(
                    Q(name__icontains=q) |
                    Q(coverage_areas__icontains=q) |
                    Q(center_based_services__icontains=q) |
                    Q(diagnoses_served__icontains=q) |
                    Q(age_groups_served__icontains=q) |
                    Q(regional_centers_served__icontains=q)
                ).distinct()
            
            # Filter by funding source if provided
            if funding_source_param:
                try:
                    # Try to parse as ID first
                    funding_id = int(funding_source_param)
                    queryset = queryset.filter(funding_sources__funding_source_id=funding_id)
                except ValueError:
                    # Otherwise treat as name
                    queryset = queryset.filter(
                        funding_sources__funding_source__name__icontains=funding_source_param
                    )
                queryset = queryset.distinct()
            
            # Filter by insurance carrier if provided
            if insurance_param:
                try:
                    # Try to parse as ID first
                    insurance_id = int(insurance_param)
                    queryset = queryset.filter(insurance_carriers__insurance_carrier_id=insurance_id)
                except ValueError:
                    # Otherwise treat as name
                    queryset = queryset.filter(
                        insurance_carriers__insurance_carrier__name__icontains=insurance_param
                    )
                queryset = queryset.distinct()
            
            # Filter by service model if provided
            if service_model_param:
                try:
                    # Try to parse as ID first
                    service_model_id = int(service_model_param)
                    queryset = queryset.filter(service_models__service_model_id=service_model_id)
                except ValueError:
                    # Otherwise treat as name
                    queryset = queryset.filter(
                        service_models__service_model__name__icontains=service_model_param
                    )
                queryset = queryset.distinct()
            
            # Filter by telehealth availability if provided
            if telehealth_param is not None:
                telehealth_required = telehealth_param.lower() in ('true', 't', '1', 'yes')
                queryset = queryset.filter(telehealth_available=telehealth_required)
            
            # Filter by waiting list status if provided
            if waiting_list_param is not None:
                has_waiting_list = waiting_list_param.lower() in ('true', 't', '1', 'yes')
                queryset = queryset.filter(waiting_list=has_waiting_list)
            
            # Filter by diagnosis if provided
            if diagnosis_param:
                queryset = queryset.filter(diagnoses_served__icontains=diagnosis_param)
            
            # Filter by age if provided
            if age_param:
                try:
                    age = int(age_param)
                    queryset = queryset.filter(
                        Q(age_groups_served__icontains=f"{age}") |
                        Q(age_groups_served__icontains=f"0-{age}") |
                        Q(age_groups_served__icontains=f"{age}-") |
                        Q(age_groups_served__icontains=f"-{age}") |
                        Q(age_groups_served__icontains=f"{age}+")
                    )
                except ValueError:
                    # If age is not a valid number, ignore this filter
                    pass
            
            # Filter by location if lat/lng provided
            if lat_param and lng_param:
                try:
                    lat = float(lat_param)
                    lng = float(lng_param)
                    radius = float(radius_param)
                    
                    # Create user location point
                    user_location = Point(lng, lat, srid=4326)
                    
                    # Convert radius from miles to meters
                    radius_meters = radius * 1609.34
                    
                    # Filter by distance
                    queryset = queryset.filter(
                        location__isnull=False
                    ).filter(
                        location__distance_lte=(user_location, D(m=radius_meters))
                    )
                    
                    # Add distance to each provider
                    for provider in queryset:
                        if provider.location:
                            distance_meters = user_location.distance(provider.location)
                            provider.distance = distance_meters / 1609.34  # Convert to miles
                        else:
                            provider.distance = None
                    
                    # Sort by distance
                    queryset = sorted(queryset, key=lambda x: x.distance if x.distance is not None else float('inf'))
                except (ValueError, TypeError):
                    # Invalid coordinates, skip distance filtering
                    pass
            
            # Use GeoJSON serializer if format=geojson is specified
            if request.query_params.get('format') == 'geojson':
                serializer = ProviderGeoSerializer(queryset, many=True)
            else:
                serializer = self.get_serializer(queryset, many=True)
            
            return Response(serializer.data)
            
        except Exception as e:
            # Log the error for debugging
            import traceback
            print(f"Error in comprehensive_search endpoint: {str(e)}")
            print(traceback.format_exc())
            return Response({"error": "An unexpected error occurred", "detail": str(e)}, status=500)
