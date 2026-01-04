from rest_framework import viewsets, filters, status
from rest_framework.decorators import action
from rest_framework.response import Response
from django.http import JsonResponse
from django.views.decorators.http import require_GET
from django.db import connection
from django.db.models import Q, Avg
from django.core.cache import cache
from django.conf import settings
from django.utils.decorators import method_decorator
from django.views.decorators.cache import cache_page

# from django.contrib.gis.geos import Point
# from django.contrib.gis.measure import D
from django_filters.rest_framework import DjangoFilterBackend
from .models import (
    LocationCategory,
    Location,
    LocationImage,
    LocationReview,
    RegionalCenter,
    # Provider model removed - use ProviderV2
    FundingSource,
    InsuranceCarrier,
    ServiceDeliveryModel,
    ProviderFundingSource,
    ProviderInsuranceCarrier,
    ProviderServiceModel,
    ProviderRegionalCenter,
    ProviderV2,
)
from .serializers import (
    LocationCategorySerializer,
    LocationSerializer,
    LocationImageSerializer,
    LocationReviewSerializer,
    RegionalCenterSerializer,
    # Old Provider serializers removed - use ProviderV2 serializers
    FundingSourceSerializer,
    InsuranceCarrierSerializer,
    ServiceDeliveryModelSerializer,
    ProviderRegionalCenterSerializer,
    GeoJSONRegionalCenterSerializer,
    ServiceAreaSerializer,
    ProviderV2Serializer,
    ProviderV2WriteSerializer,
)
import math
from rest_framework.decorators import api_view
from django.db.models.expressions import RawSQL


@require_GET
def health_check(request):
    """
    Health check endpoint for deployment verification.
    Checks database connectivity and returns system status.
    """
    try:
        # Test database connection
        with connection.cursor() as cursor:
            cursor.execute("SELECT 1")

        # Get basic stats
        provider_count = ProviderV2.objects.count()
        rc_count = RegionalCenter.objects.count()

        return JsonResponse(
            {
                "status": "healthy",
                "database": "connected",
                "providers": provider_count,
                "regional_centers": rc_count,
                "version": "2.0.0",
            }
        )
    except Exception as e:
        return JsonResponse({"status": "unhealthy", "error": str(e)}, status=503)


class LocationCategoryViewSet(viewsets.ReadOnlyModelViewSet):
    queryset = LocationCategory.objects.all()
    serializer_class = LocationCategorySerializer


class LocationViewSet(viewsets.ReadOnlyModelViewSet):
    queryset = Location.objects.filter(is_active=True)
    serializer_class = LocationSerializer
    filter_backends = [
        DjangoFilterBackend,
        filters.SearchFilter,
        filters.OrderingFilter,
    ]
    filterset_fields = ["category", "price_level", "has_parking", "is_accessible"]
    search_fields = ["name", "description", "address", "city", "state"]
    ordering_fields = ["name", "rating", "created_at"]

    @action(detail=False, methods=["get"])
    def nearby(self, request):
        """
        Find locations near a specific lat/lng point.
        Query parameters:
        - lat: latitude (required)
        - lng: longitude (required)
        - radius: search radius in miles (default: 5)
        """
        try:
            # Get parameters with proper error handling
            lat_param = request.query_params.get("lat")
            lng_param = request.query_params.get("lng")
            radius_param = request.query_params.get("radius", "5")  # Default to 5 miles

            # Validate parameters
            if not lat_param or not lng_param:
                return Response(
                    {"error": "lat and lng parameters are required"}, status=400
                )

            try:
                lat = float(lat_param)
                lng = float(lng_param)
                radius = float(radius_param)
            except (ValueError, TypeError):
                return Response(
                    {"error": "lat, lng, and radius must be valid numbers"}, status=400
                )

            # Use PostGIS for efficient spatial queries
            # No limit - return all providers within radius to support accurate filtering
            nearby_locations = Location.find_nearest(lat, lng, radius, limit=1000)

            # Serialize the data
            serializer = self.get_serializer(nearby_locations, many=True)

            # Add distance to serialized data
            serializer_data = list(serializer.data)
            for i, location in enumerate(nearby_locations):
                if i < len(serializer_data) and hasattr(location, "distance"):
                    serializer_data[i] = dict(serializer_data[i])
                    serializer_data[i]["distance"] = round(location.distance, 2)

            return Response(serializer_data)

        except Exception as e:
            # Log the error for debugging
            import traceback

            print(f"Error in nearby endpoint: {str(e)}")
            print(traceback.format_exc())
            return Response(
                {"error": "An unexpected error occurred", "detail": str(e)}, status=500
            )

    @action(detail=False, methods=["get"])
    def filters(self, request):
        """Return available filter options"""
        categories = LocationCategory.objects.all()
        price_levels = Location.objects.values_list("price_level", flat=True).distinct()

        return Response(
            {
                "categories": LocationCategorySerializer(categories, many=True).data,
                "price_levels": sorted(list(price_levels)),
                "amenities": [
                    {"id": "has_parking", "name": "Parking Available"},
                    {"id": "is_accessible", "name": "Accessibility Features"},
                ],
            }
        )


class LocationImageViewSet(viewsets.ReadOnlyModelViewSet):
    queryset = LocationImage.objects.all()
    serializer_class = LocationImageSerializer
    filter_backends = [DjangoFilterBackend]
    filterset_fields = ["location", "is_primary"]


class LocationReviewViewSet(viewsets.ModelViewSet):
    queryset = LocationReview.objects.all()
    serializer_class = LocationReviewSerializer
    filter_backends = [DjangoFilterBackend]
    filterset_fields = ["location"]

    # Only allow creating and reading reviews
    http_method_names = ["get", "post"]


class RegionalCenterViewSet(viewsets.ReadOnlyModelViewSet):
    """
    Regional Center API Endpoints

    Standard endpoints:
    - GET /api/regional-centers/ - List all regional centers (supports search, filtering, ordering)
    - GET /api/regional-centers/{id}/ - Get single regional center by ID

    Custom action endpoints:
    - GET /api/regional-centers/service_area_boundaries/ - **CRITICAL** Returns GeoJSON with all LA County
      regional centers including their polygon geometries AND complete ZIP code arrays for each center
    - GET /api/regional-centers/lookup_by_zip/?zip_code={zip} - Find regional center for specific ZIP code
    - GET /api/regional-centers/nearby/?lat={lat}&lng={lng}&radius={miles} - Find centers near coordinates
    - GET /api/regional-centers/by_location/?location={address_or_zip} - Geocode and find centers

    The service_area_boundaries endpoint is the primary source for:
    1. Regional center polygon geometries (MultiPolygon GeoJSON)
    2. Complete ZIP code coverage for each center
    3. Map visualization data
    """

    queryset = RegionalCenter.objects.all()
    serializer_class = RegionalCenterSerializer
    filter_backends = [
        DjangoFilterBackend,
        filters.SearchFilter,
        filters.OrderingFilter,
    ]
    search_fields = [
        "regional_center",
        "address",
        "city",
        "state",
        "county_served",
        "los_angeles_health_district",
    ]
    ordering_fields = ["regional_center", "city", "county_served"]

    def list(self, request, *args, **kwargs):
        """
        List all regional centers with caching.
        Cached for 1 hour since regional center data rarely changes.
        """
        # Build cache key from query parameters
        import hashlib
        query_str = "&".join(sorted(f"{k}={v}" for k, v in request.query_params.items()))
        cache_key = f"regional_centers_list_{hashlib.md5(query_str.encode()).hexdigest()}"
        
        # Check cache first
        cached_data = cache.get(cache_key)
        if cached_data is not None:
            return Response(cached_data)
        
        # Get the response from parent class
        response = super().list(request, *args, **kwargs)
        
        # Cache for 1 hour
        cache_timeout = getattr(settings, 'CACHE_TIMEOUT_REGIONAL_CENTERS', 3600)
        cache.set(cache_key, response.data, cache_timeout)
        
        return response

    @action(detail=False, methods=["get"])
    def nearby(self, request):
        """
        Find regional centers near given coordinates.
        Query parameters:
        - lat: Latitude (required)
        - lng: Longitude (required)
        - radius: Search radius in miles (default: 25)
        - limit: Maximum results (default: 10)
        """
        try:
            # Get coordinates
            lat = request.query_params.get("lat")
            lng = request.query_params.get("lng")
            radius = float(request.query_params.get("radius", 25))
            limit = int(request.query_params.get("limit", 10))

            # Validate coordinates
            if not lat or not lng:
                return Response(
                    {"error": "Latitude and longitude are required"},
                    status=status.HTTP_400_BAD_REQUEST,
                )

            lat = float(lat)
            lng = float(lng)

            # Find nearby regional centers
            centers = RegionalCenter.find_nearest(lat, lng, radius, limit)

            # Serialize with distance
            data = []
            for center in centers:
                center_data = self.get_serializer(center).data
                center_data["distance"] = round(center.distance, 2)
                data.append(center_data)

            return Response(data)

        except ValueError:
            return Response(
                {"error": "Invalid coordinate or numeric values"},
                status=status.HTTP_400_BAD_REQUEST,
            )
        except Exception as e:
            return Response(
                {"error": str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )

    @action(detail=False, methods=["get"])
    def by_location(self, request):
        """
        Find regional centers by address or zip code.
        Query parameters:
        - location: Address or zip code (required)
        - radius: Search radius in miles (default: 25)
        - limit: Maximum results (default: 10)
        """
        try:
            # Get location parameter
            location = request.query_params.get("location")
            radius = float(request.query_params.get("radius", 25))
            limit = int(request.query_params.get("limit", 10))

            if not location:
                return Response(
                    {"error": "Location parameter is required"},
                    status=status.HTTP_400_BAD_REQUEST,
                )

            # Geocode and search
            centers = RegionalCenter.geocode_and_search(location, radius, limit)

            if not centers:
                return Response(
                    {
                        "error": "Could not geocode location or no regional centers found"
                    },
                    status=status.HTTP_404_NOT_FOUND,
                )

            # Serialize with distance
            data = []
            for center in centers:
                center_data = self.get_serializer(center).data
                center_data["distance"] = round(center.distance, 2)
                data.append(center_data)

            return Response(data)

        except ValueError:
            return Response(
                {"error": "Invalid numeric values"}, status=status.HTTP_400_BAD_REQUEST
            )
        except Exception as e:
            return Response(
                {"error": str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )

    @action(detail=False, methods=["get"])
    def service_areas(self, request):
        """
        Get service area polygons for all regional centers as GeoJSON FeatureCollection.
        Query parameters:
        - format: 'geojson' or 'simplified' (default: 'geojson')
        """
        try:
            format_type = request.query_params.get("format", "geojson")

            # Get centers with service areas
            centers = RegionalCenter.objects.filter(service_area__isnull=False)

            if format_type == "simplified":
                serializer = ServiceAreaSerializer(centers, many=True)
                features = serializer.data
            else:
                # Full GeoJSON format
                serializer = GeoJSONRegionalCenterSerializer(centers, many=True)
                features = serializer.data

            # Filter out None results (centers without valid geometry)
            features = [f for f in features if f is not None]

            return Response({"type": "FeatureCollection", "features": features})

        except Exception as e:
            return Response(
                {"error": str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )

    @action(detail=True, methods=["get"])
    def service_area(self, request, pk=None):
        """Get service area polygon for a specific regional center"""
        try:
            center = self.get_object()
            geojson = center.get_service_area_as_geojson()

            if geojson:
                return Response(geojson)
            else:
                return Response(
                    {"error": "No service area defined for this regional center"},
                    status=status.HTTP_404_NOT_FOUND,
                )

        except Exception as e:
            return Response(
                {"error": str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )

    @action(detail=False, methods=["get"])
    def by_service_area(self, request):
        """
        Find regional centers that serve a specific geographic point.
        Query parameters:
        - lat: Latitude (required)
        - lng: Longitude (required)
        """
        try:
            lat = request.query_params.get("lat")
            lng = request.query_params.get("lng")

            if not lat or not lng:
                return Response(
                    {"error": "Latitude and longitude are required"},
                    status=status.HTTP_400_BAD_REQUEST,
                )

            lat = float(lat)
            lng = float(lng)

            # Find centers that serve this location
            centers = RegionalCenter.find_by_location(lat, lng)

            serializer = self.get_serializer(centers, many=True)
            return Response(serializer.data)

        except ValueError:
            return Response(
                {"error": "Invalid coordinate values"},
                status=status.HTTP_400_BAD_REQUEST,
            )
        except Exception as e:
            return Response(
                {"error": str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )

    @action(detail=True, methods=["get"])
    def providers(self, request, pk=None):
        """Get providers associated with this regional center"""
        try:
            center = self.get_object()
            # Get the actual Provider objects, not the relationship objects
            relationships = ProviderRegionalCenter.objects.filter(
                regional_center=center
            ).select_related("provider")
            providers = [rel.provider for rel in relationships]

            serializer = ProviderV2Serializer(providers, many=True)
            return Response(serializer.data)

        except Exception as e:
            return Response(
                {"error": str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )

    @action(detail=False, methods=["get"])
    def by_zip_code(self, request):
        """
        Find regional center that serves a specific ZIP code (LA-specific).
        Query parameters:
        - zip_code: ZIP code (required)
        """
        try:
            zip_code = request.query_params.get("zip_code")

            if not zip_code:
                return Response(
                    {"error": "ZIP code is required"},
                    status=status.HTTP_400_BAD_REQUEST,
                )

            # Find regional center that serves this ZIP code
            center = RegionalCenter.find_by_zip_code(zip_code)

            if center:
                serializer = self.get_serializer(center)
                return Response(serializer.data)
            else:
                return Response(
                    {"error": f"No regional center found for ZIP code {zip_code}"},
                    status=status.HTTP_404_NOT_FOUND,
                )

        except Exception as e:
            return Response(
                {"error": str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )

    @action(detail=False, methods=["get"])
    def service_area_boundaries(self, request):
        """
        Get real geographic service area boundaries for LA Regional Centers.
        Returns GeoJSON with actual geographic boundaries that fit together like puzzle pieces.
        Cached for 1 hour since this data rarely changes.
        """
        # Check cache first
        cache_key = "regional_centers_service_area_boundaries"
        cached_data = cache.get(cache_key)
        if cached_data is not None:
            return Response(cached_data)

        try:
            # Get only LA regional centers
            la_centers = RegionalCenter.objects.filter(is_la_regional_center=True)

            if not la_centers.exists():
                return Response(
                    {"error": "No LA regional centers found"},
                    status=status.HTTP_404_NOT_FOUND,
                )

            # Create GeoJSON with real geographic boundaries
            features = []

            for center in la_centers:
                if center.zip_codes:
                    # Create a feature for this regional center
                    feature = {
                        "type": "Feature",
                        "properties": {
                            "name": center.regional_center,
                            "phone": center.telephone,
                            "address": f"{center.address}, {center.city}, {center.state} {center.zip_code}",
                            "website": center.website,
                            "service_areas": center.service_areas or [],
                            "zip_codes": center.zip_codes,
                            "center_id": center.id,
                            "office_type": center.office_type,
                            "county_served": center.county_served,
                            "latitude": center.latitude,
                            "longitude": center.longitude,
                            "suite": center.suite,
                            "city": center.city,
                            "state": center.state,
                            "zip_code": center.zip_code,
                            "address_street": center.address,
                        },
                        "geometry": self._create_service_area_geometry(center),
                    }
                    features.append(feature)

            geojson = {"type": "FeatureCollection", "features": features}

            # Cache for 1 hour
            cache_timeout = getattr(settings, 'CACHE_TIMEOUT_SERVICE_AREAS', 3600)
            cache.set(cache_key, geojson, cache_timeout)

            return Response(geojson)

        except Exception as e:
            return Response(
                {"error": str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )

    def _create_service_area_geometry(self, center):
        """
        Get the stored service area geometry from the database.
        This should contain realistic geographic boundaries that fit together like puzzle pieces.
        """
        if center.service_areas and isinstance(center.service_areas, dict):
            # Return the stored GeoJSON geometry
            return center.service_areas.get("geometry")

        # Fallback to basic geometry if no stored data
        if not center.zip_codes:
            return None

        # Get the center coordinates
        center_lat = center.latitude or 34.0522  # Default to LA center
        center_lng = center.longitude or -118.2437

        # Create a basic fallback polygon
        coordinates = [
            [center_lng - 0.1, center_lat + 0.1],
            [center_lng + 0.1, center_lat + 0.1],
            [center_lng + 0.1, center_lat - 0.1],
            [center_lng - 0.1, center_lat - 0.1],
            [center_lng - 0.1, center_lat + 0.1],
        ]

        return {"type": "Polygon", "coordinates": [coordinates]}

    @action(detail=False, methods=["get"])
    def zip_code_analysis(self, request):
        """
        Analyze ZIP code coverage across all Regional Centers.
        Returns statistics about ZIP code distribution and identifies potential gaps.
        """
        try:
            # Get all LA County Regional Centers with ZIP codes
            la_centers = RegionalCenter.objects.filter(is_la_regional_center=True)

            analysis = {
                "total_regional_centers": la_centers.count(),
                "centers": [],
                "total_unique_zips": 0,
                "zip_distribution": {},
            }

            all_zips = set()

            for center in la_centers:
                zip_codes = center.zip_codes or []
                all_zips.update(zip_codes)

                center_info = {
                    "name": center.regional_center,
                    "id": center.id,
                    "zip_count": len(zip_codes),
                    "sample_zips": sorted(zip_codes)[:10] if zip_codes else [],
                }
                analysis["centers"].append(center_info)

            analysis["total_unique_zips"] = len(all_zips)

            # Analyze ZIP code ranges
            sorted_zips = sorted(all_zips)
            analysis["zip_range"] = {
                "min": sorted_zips[0] if sorted_zips else None,
                "max": sorted_zips[-1] if sorted_zips else None,
            }

            # Sample missing ZIPs (known problem areas)
            known_problem_zips = [
                "91403",
                "91401",
                "91405",
                "91406",
                "91411",
                "91423",
                "91436",
            ]
            missing_zips = [z for z in known_problem_zips if z not in all_zips]

            analysis["known_missing_zips"] = {
                "count": len(missing_zips),
                "examples": missing_zips,
                "note": "These are known Sherman Oaks/Van Nuys area ZIPs that are missing",
            }

            return Response(analysis)

        except Exception as e:
            return Response(
                {"error": "Failed to analyze ZIP codes", "details": str(e)},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )


# Reference data ViewSets
class FundingSourceViewSet(viewsets.ReadOnlyModelViewSet):
    queryset = FundingSource.objects.all()
    serializer_class = FundingSourceSerializer
    filter_backends = [filters.SearchFilter]
    search_fields = ["name", "description"]


class InsuranceCarrierViewSet(viewsets.ReadOnlyModelViewSet):
    queryset = InsuranceCarrier.objects.all()
    serializer_class = InsuranceCarrierSerializer
    filter_backends = [filters.SearchFilter]
    search_fields = ["name", "description"]


class ServiceDeliveryModelViewSet(viewsets.ReadOnlyModelViewSet):
    queryset = ServiceDeliveryModel.objects.all()
    serializer_class = ServiceDeliveryModelSerializer
    filter_backends = [filters.SearchFilter]
    search_fields = ["name", "description"]


# REMOVED: Old ProviderViewSet - Use ProviderV2ViewSet instead
# Legacy /api/providers-legacy/ endpoint has been removed


# ProviderV2 ViewSet with enum array support
class ProviderV2ViewSet(viewsets.ModelViewSet):
    queryset = ProviderV2.objects.all()
    serializer_class = ProviderV2Serializer
    filter_backends = [
        DjangoFilterBackend,
        filters.SearchFilter,
        filters.OrderingFilter,
    ]
    # Use real ProviderV2 fields
    search_fields = [
        "name",
        "type",
        "description",
        "address",
        "insurance_accepted",
        "languages_spoken",
    ]
    ordering_fields = ["name"]

    def get_serializer_class(self):
        """Use different serializers for different operations"""
        if self.action in ["create", "update", "partial_update"]:
            return ProviderV2WriteSerializer
        return ProviderV2Serializer

    def list(self, request, *args, **kwargs):
        """
        List all providers with caching.
        Cached for 5 minutes.
        """
        import hashlib
        
        # Build cache key from query parameters
        query_str = "&".join(sorted(f"{k}={v}" for k, v in request.query_params.items()))
        cache_key = f"providers_v2_list_{hashlib.md5(query_str.encode()).hexdigest()}"
        
        # Check cache first
        cached_data = cache.get(cache_key)
        if cached_data is not None:
            return Response(cached_data)
        
        # Get the response from parent class
        response = super().list(request, *args, **kwargs)
        
        # Cache for 5 minutes
        cache_timeout = getattr(settings, 'CACHE_TIMEOUT_PROVIDERS', 300)
        cache.set(cache_key, response.data, cache_timeout)
        
        return response

    @action(detail=False, methods=["get"])
    def by_area(self, request):
        """
        Find providers serving a specific area.
        Query parameters:
        - area: Area name (required)
        """
        try:
            # Get area parameter
            area_param = request.query_params.get("area")

            # Validate parameters
            if not area_param:
                return Response(
                    {"error": "Area parameter is required"},
                    status=status.HTTP_400_BAD_REQUEST,
                )

            # Search for providers serving the area
            providers = ProviderV2.objects.filter(
                Q(coverage_areas__contains=[area_param])
                | Q(areas__icontains=area_param)
            )

            serializer = self.get_serializer(providers, many=True)
            return Response(serializer.data)

        except Exception as e:
            return Response(
                {"error": str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )

    @action(detail=False, methods=["get"])
    def nearby(self, request):
        """
        Find providers near given coordinates.
        Query parameters:
        - lat: Latitude (required)
        - lng: Longitude (required)
        - radius: Search radius in miles (default: 10)
        - limit: Maximum results (default: 20)
        """
        try:
            # Get coordinates
            lat = request.query_params.get("lat")
            lng = request.query_params.get("lng")
            radius = float(request.query_params.get("radius", 10))
            limit = int(request.query_params.get("limit", 20))

            # Validate coordinates
            if not lat or not lng:
                return Response(
                    {"error": "Latitude and longitude are required"},
                    status=status.HTTP_400_BAD_REQUEST,
                )

            lat = float(lat)
            lng = float(lng)

            # Find nearby providers
            providers = ProviderV2.find_nearest(lat, lng, radius, limit)

            # Serialize with distance
            data = []
            for provider in providers:
                provider_data = self.get_serializer(provider).data
                provider_data["distance"] = round(provider.distance, 2)
                data.append(provider_data)

            return Response(data)

        except ValueError:
            return Response(
                {"error": "Invalid coordinate or numeric values"},
                status=status.HTTP_400_BAD_REQUEST,
            )
        except Exception as e:
            return Response(
                {"error": str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )

    @action(detail=False, methods=["get"])
    def by_location(self, request):
        """
        Find providers by address or zip code.
        Query parameters:
        - location: Address or zip code (required)
        - radius: Search radius in miles (default: 10)
        - limit: Maximum results (default: 20)
        """
        try:
            # Get location parameter
            location = request.query_params.get("location")
            radius = float(request.query_params.get("radius", 10))
            limit = int(request.query_params.get("limit", 20))

            if not location:
                return Response(
                    {"error": "Location parameter is required"},
                    status=status.HTTP_400_BAD_REQUEST,
                )

            # Geocode and search
            providers = ProviderV2.geocode_and_search(location, radius, limit)

            if not providers:
                return Response(
                    {"error": "Could not geocode location or no providers found"},
                    status=status.HTTP_404_NOT_FOUND,
                )

            # Serialize with distance
            data = []
            for provider in providers:
                provider_data = self.get_serializer(provider).data
                provider_data["distance"] = round(provider.distance, 2)
                data.append(provider_data)

            return Response(data)

        except ValueError:
            return Response(
                {"error": "Invalid numeric values"}, status=status.HTTP_400_BAD_REQUEST
            )
        except Exception as e:
            return Response(
                {"error": str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )

    @action(detail=False, methods=["get"])
    def comprehensive_search(self, request):
        """
        Comprehensive search with multiple filters for ProviderV2.
        Query parameters:
        - q: Search query (searches name, specializations, services)
        - location: Address or zip code for geographic filtering
        - radius: Search radius in miles (default: 15)
        - insurance: Insurance filter (can be multiple values)
        - specialization: Specialization filter
        - lat: Latitude for geographic filtering
        - lng: Longitude for geographic filtering
        - age: Age for age group filtering
        - diagnosis: Diagnosis for specialization filtering
        """
        try:
            query = request.query_params.get("q", "")
            location = request.query_params.get("location")
            radius = float(request.query_params.get("radius", 15))
            lat = request.query_params.get("lat")
            lng = request.query_params.get("lng")
            age = request.query_params.get("age")
            diagnosis = request.query_params.get("diagnosis")

            # Get all insurance filter values (can be multiple)
            insurance_filters = request.query_params.getlist("insurance")
            specialization = request.query_params.get("specialization")

            # Start with all providers
            providers = ProviderV2.objects.all()

            # Apply text search
            if query:
                # Text search across common text fields, including address
                providers = providers.filter(
                    Q(name__icontains=query)
                    | Q(address__icontains=query)
                    | Q(type__icontains=query)
                    | Q(description__icontains=query)
                    | Q(insurance_accepted__icontains=query)
                )

            # Apply insurance filters using ProviderInsuranceCarrier relationship
            if insurance_filters:
                from locations.models import ProviderInsuranceCarrier, InsuranceCarrier

                insurance_q = Q()
                for insurance_type in insurance_filters:
                    insurance_lower = insurance_type.lower()

                    # Map common insurance type names to carriers in the database
                    if insurance_lower in [
                        "insurance",
                        "accepts insurance",
                        "private insurance",
                    ]:
                        # Get all providers that accept ANY insurance
                        # This returns providers that have at least one insurance carrier relationship
                        insurance_provider_ids = (
                            ProviderInsuranceCarrier.objects.values_list(
                                "provider_id", flat=True
                            ).distinct()
                        )
                        insurance_q |= Q(id__in=insurance_provider_ids)
                    elif insurance_lower in [
                        "private pay",
                        "private payment",
                        "self pay",
                    ]:
                        # Private pay is implicit - all providers accept it
                        # For now, don't filter out any providers for private pay
                        pass
                    elif insurance_lower in [
                        "regional center",
                        "regional center funding",
                    ]:
                        # Filter by Regional Center insurance carrier
                        try:
                            carrier = InsuranceCarrier.objects.get(
                                name__iexact="Regional Center"
                            )
                            carrier_provider_ids = (
                                ProviderInsuranceCarrier.objects.filter(
                                    insurance_carrier=carrier
                                ).values_list("provider_id", flat=True)
                            )
                            insurance_q |= Q(id__in=carrier_provider_ids)
                        except InsuranceCarrier.DoesNotExist:
                            # Fallback: search in legacy field
                            insurance_q |= Q(
                                insurance_accepted__icontains="regional center"
                            )
                    else:
                        # Try to match specific insurance carrier names
                        try:
                            carrier = InsuranceCarrier.objects.get(
                                name__iexact=insurance_type
                            )
                            carrier_provider_ids = (
                                ProviderInsuranceCarrier.objects.filter(
                                    insurance_carrier=carrier
                                ).values_list("provider_id", flat=True)
                            )
                            insurance_q |= Q(id__in=carrier_provider_ids)
                        except InsuranceCarrier.DoesNotExist:
                            # Fallback: search in legacy insurance_accepted text field
                            insurance_q |= Q(
                                insurance_accepted__icontains=insurance_type
                            )

                if insurance_q:
                    providers = providers.filter(insurance_q)

            # Apply specialization filter (diagnosis) using text field operations
            if specialization:
                providers = providers.filter(type__icontains=specialization)

            # Apply diagnosis filter using JSON field operations
            if diagnosis:
                # Count how many providers have diagnosis data
                providers_with_diagnosis_data = (
                    providers.exclude(diagnoses_treated__isnull=True)
                    .exclude(diagnoses_treated=[])
                    .count()
                )
                total_providers = providers.count()

                # Only apply diagnosis filter if at least 10% of providers have diagnosis data
                # This prevents filtering to 2-3 providers when the field is rarely populated
                if (
                    total_providers > 0
                    and (providers_with_diagnosis_data / total_providers) >= 0.1
                ):
                    diagnosis_filtered = providers.filter(
                        diagnoses_treated__contains=[diagnosis]
                    )
                    if diagnosis_filtered.exists():
                        providers = diagnosis_filtered
                # Otherwise skip diagnosis filter (field not widely populated yet)

            # Apply therapy filters (multiple allowed) using JSON field operations
            therapy_values = request.query_params.getlist("therapy")
            if therapy_values:
                # Try to filter by therapy types, but if no results, fall back to no therapy filter
                therapy_filtered = providers.filter(
                    therapy_types__contains=therapy_values
                )
                if therapy_filtered.exists():
                    providers = therapy_filtered
                # If no providers match therapy filter, keep all providers (lenient approach)

            # Apply location-based filtering using provided coordinates
            if lat and lng:
                try:
                    lat_float = float(lat)
                    lng_float = float(lng)

                    # Get IDs of already filtered providers
                    filtered_provider_ids = list(providers.values_list("id", flat=True))
                    print(
                        f"🔍 [comprehensive_search] Radius: {radius} miles, Providers before distance filter: {len(filtered_provider_ids)}"
                    )

                    if filtered_provider_ids:
                        # Filter by distance using raw SQL with proper parameterization
                        from django.db import connection

                        # Build placeholders for the IN clause
                        placeholders = ",".join(["%s"] * len(filtered_provider_ids))

                        with connection.cursor() as cursor:
                            # Use proper parameterized query
                            sql = f"""
                                SELECT id FROM providers_v2 
                                WHERE id IN ({placeholders})
                                AND latitude IS NOT NULL AND longitude IS NOT NULL
                                AND (3959 * acos(cos(radians(%s)) * cos(radians(latitude)) * 
                                cos(radians(longitude) - radians(%s)) + sin(radians(%s)) * 
                                sin(radians(latitude)))) < %s
                            """
                            params = list(filtered_provider_ids) + [
                                lat_float,
                                lng_float,
                                lat_float,
                                radius,
                            ]
                            cursor.execute(sql, params)
                            nearby_ids = [row[0] for row in cursor.fetchall()]

                        print(
                            f"🎯 [comprehensive_search] Providers after {radius} mile radius filter: {len(nearby_ids)}"
                        )
                        providers = providers.filter(id__in=nearby_ids)
                    else:
                        # If no providers match the non-geographic filters, return empty queryset
                        providers = providers.none()
                except (ValueError, TypeError):
                    pass  # Skip location filtering if coordinates are invalid

            # Apply location-based filtering using address geocoding (fallback)
            elif location:
                coordinates = RegionalCenter.geocode_address(location)
                if coordinates:
                    lat_coord, lng_coord = coordinates

                    # Get IDs of already filtered providers
                    filtered_provider_ids = list(providers.values_list("id", flat=True))

                    if filtered_provider_ids:
                        # Filter by distance using raw SQL with proper parameterization
                        from django.db import connection

                        # Build placeholders for the IN clause
                        placeholders = ",".join(["%s"] * len(filtered_provider_ids))

                        with connection.cursor() as cursor:
                            # Use proper parameterized query
                            sql = f"""
                                SELECT id FROM providers_v2 
                                WHERE id IN ({placeholders})
                                AND latitude IS NOT NULL AND longitude IS NOT NULL
                                AND (3959 * acos(cos(radians(%s)) * cos(radians(latitude)) * 
                                cos(radians(longitude) - radians(%s)) + sin(radians(%s)) * 
                                sin(radians(latitude)))) < %s
                            """
                            params = list(filtered_provider_ids) + [
                                lat_coord,
                                lng_coord,
                                lat_coord,
                                radius,
                            ]
                            cursor.execute(sql, params)
                            nearby_ids = [row[0] for row in cursor.fetchall()]

                        providers = providers.filter(id__in=nearby_ids)
                    else:
                        # If no providers match the non-geographic filters, return empty queryset
                        providers = providers.none()

            # Apply age filtering
            if age:
                # "All Ages" means no age filter - user wants all providers regardless of age
                # Also skip filter if user explicitly selects "All Ages" as they want to see everything
                if age.lower() == "all ages":
                    # No filtering - return all providers regardless of age groups
                    pass
                else:
                    # Filter by specific age group
                    # Include providers that:
                    # 1. Have the specific age group in their age_groups array
                    # 2. Have "All Ages" in their array (they serve all ages)
                    # 3. Have NULL age_groups (defaulted to "All Ages" in serializer)
                    age_filtered = providers.filter(
                        Q(age_groups__contains=[age])
                        | Q(age_groups__contains=["All Ages"])
                        | Q(age_groups__isnull=True)
                    )
                    if age_filtered.exists():
                        providers = age_filtered
                    # If no providers match age filter, keep all providers (lenient approach)

            # Apply limit - increased to support larger radius searches
            providers = providers[:1000]  # Support large radius searches

            print(
                f"✅ [comprehensive_search] Final provider count after all filters: {len(providers)}"
            )

            serializer = self.get_serializer(providers, many=True)
            return Response(serializer.data)

        except Exception as e:
            # Log the full error for debugging
            import traceback

            print("\n" + "=" * 80)
            print("❌ ERROR in comprehensive_search:")
            print(f"Error type: {type(e).__name__}")
            print(f"Error message: {str(e)}")
            print("Full traceback:")
            traceback.print_exc()
            print("=" * 80 + "\n")

            return Response(
                {"error": str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )

    @action(detail=False, methods=["get"])
    def by_regional_center(self, request):
        """
        Filter providers by regional center ZIP codes.
        Query parameters:
        - regional_center_id: ID of the regional center
        - zip_code: ZIP code to look up regional center (alternative to regional_center_id)
        - Additional filters: insurance, age, diagnosis, therapy
        Cached based on query parameters for 1 minute.
        """
        try:
            from locations.models import RegionalCenter
            import re
            import hashlib

            # Build cache key from query parameters
            query_str = "&".join(sorted(f"{k}={v}" for k, v in request.query_params.items()))
            cache_key = f"providers_by_rc_{hashlib.md5(query_str.encode()).hexdigest()}"
            
            # Check cache first
            cached_data = cache.get(cache_key)
            if cached_data is not None:
                return Response(cached_data)

            # Get regional center either by ID or ZIP lookup
            regional_center_id = request.query_params.get("regional_center_id")
            zip_code = request.query_params.get("zip_code")

            if regional_center_id:
                regional_center = RegionalCenter.objects.get(id=regional_center_id)
            elif zip_code:
                regional_center = RegionalCenter.find_by_zip_code(zip_code)
                if not regional_center:
                    return Response(
                        {"error": f"No regional center found for ZIP {zip_code}"},
                        status=status.HTTP_404_NOT_FOUND,
                    )
            else:
                return Response(
                    {"error": "Either regional_center_id or zip_code is required"},
                    status=status.HTTP_400_BAD_REQUEST,
                )

            if not regional_center.zip_codes:
                return Response(
                    {"error": "Regional center has no ZIP codes defined"},
                    status=status.HTTP_404_NOT_FOUND,
                )

            # Filter providers whose address contains one of the regional center's ZIP codes
            # Using Python-based filtering (fast with caching enabled)
            rc_zip_set = set(regional_center.zip_codes)
            filtered_provider_ids = []

            # Get all providers and filter by ZIP code match
            for provider in ProviderV2.objects.only('id', 'address'):
                address_str = provider.address if isinstance(provider.address, str) else str(provider.address)
                # Look for 5-digit ZIP codes in the address
                zip_matches = re.findall(r'\d{5}', address_str)
                # Check if any ZIP matches the regional center's ZIPs
                if any(z in rc_zip_set for z in zip_matches):
                    filtered_provider_ids.append(provider.id)

            # Filter queryset to only matching providers
            providers = ProviderV2.objects.filter(id__in=filtered_provider_ids)

            # Apply additional filters
            # Apply insurance filter using ProviderInsuranceCarrier relationship
            insurance = request.query_params.get("insurance")
            if insurance:
                from locations.models import ProviderInsuranceCarrier, InsuranceCarrier

                insurance_lower = insurance.lower()
                if insurance_lower in [
                    "insurance",
                    "accepts insurance",
                    "private insurance",
                ]:
                    # Get all providers that accept ANY insurance
                    insurance_provider_ids = (
                        ProviderInsuranceCarrier.objects.values_list(
                            "provider_id", flat=True
                        ).distinct()
                    )
                    providers = providers.filter(id__in=insurance_provider_ids)
                elif insurance_lower in ["private pay", "private payment", "self pay"]:
                    # Private pay - all providers implicitly accept it, no filtering needed
                    pass
                elif insurance_lower in ["regional center", "regional center funding"]:
                    # Regional center funding - no filtering for now
                    pass
                else:
                    # Try to match specific insurance carrier name
                    try:
                        carrier = InsuranceCarrier.objects.get(name__iexact=insurance)
                        carrier_provider_ids = ProviderInsuranceCarrier.objects.filter(
                            insurance_carrier=carrier
                        ).values_list("provider_id", flat=True)
                        providers = providers.filter(id__in=carrier_provider_ids)
                    except InsuranceCarrier.DoesNotExist:
                        # No matching carrier found, return empty results
                        providers = providers.none()

            age = request.query_params.get("age")
            if age:
                # "All Ages" means no age filter - user wants all providers regardless of age
                if age.lower() == "all ages":
                    pass  # No filtering
                else:
                    # Filter by specific age group, including providers with "All Ages" or NULL
                    providers = providers.filter(
                        Q(age_groups__contains=[age])
                        | Q(age_groups__contains=["All Ages"])
                        | Q(age_groups__isnull=True)
                    )

            diagnosis = request.query_params.get("diagnosis")
            if diagnosis:
                # Only filter by diagnosis if field is widely populated (10%+ of providers)
                providers_with_diagnosis_data = (
                    providers.exclude(diagnoses_treated__isnull=True)
                    .exclude(diagnoses_treated=[])
                    .count()
                )
                total_providers = providers.count()

                if (
                    total_providers > 0
                    and (providers_with_diagnosis_data / total_providers) >= 0.1
                ):
                    providers = providers.filter(
                        diagnoses_treated__contains=[diagnosis]
                    )

            therapy = request.query_params.get("therapy")
            if therapy:
                providers = providers.filter(therapy_types__contains=[therapy])

            # Serialize and return
            serializer = self.get_serializer(providers, many=True)

            response_data = {
                "count": providers.count(),
                "regional_center": {
                    "id": regional_center.id,
                    "name": regional_center.regional_center,
                    "zip_codes": regional_center.zip_codes,
                },
                "results": serializer.data,
            }

            # Cache for 1 minute (search results may change more frequently)
            cache_timeout = getattr(settings, 'CACHE_TIMEOUT_PROVIDER_SEARCH', 60)
            cache.set(cache_key, response_data, cache_timeout)

            return Response(response_data)

        except RegionalCenter.DoesNotExist:
            return Response(
                {"error": "Regional center not found"}, status=status.HTTP_404_NOT_FOUND
            )
        except Exception as e:
            # Log the full error for debugging
            import traceback

            print("\n" + "=" * 80)
            print("❌ ERROR in by_regional_center:")
            print(f"Error type: {type(e).__name__}")
            print(f"Error message: {str(e)}")
            print("Full traceback:")
            traceback.print_exc()
            print("=" * 80 + "\n")

            return Response(
                {"error": str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )

    @action(detail=True, methods=["get"])
    def regional_centers(self, request, pk=None):
        """Get regional centers associated with this provider"""
        try:
            provider = self.get_object()
            # Get the actual RegionalCenter objects, not the relationship objects
            relationships = ProviderRegionalCenter.objects.filter(
                provider=provider
            ).select_related("regional_center")
            regional_centers = [rel.regional_center for rel in relationships]

            serializer = RegionalCenterSerializer(regional_centers, many=True)
            return Response(serializer.data)

        except Exception as e:
            return Response(
                {"error": str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )

    @action(detail=False, methods=["get"])
    def filters(self, request):
        """Get available filter options"""
        try:
            # Get unique values for common filters
            specializations = set()
            insurance_types = set()
            areas = set()

            for provider in ProviderV2.objects.all():
                if provider.specializations:
                    specializations.update(
                        [s.strip() for s in provider.specializations.split(",")]
                    )
                if provider.insurance_accepted:
                    insurance_types.update(
                        [i.strip() for i in provider.insurance_accepted.split(",")]
                    )
                if provider.coverage_areas:
                    areas.update(
                        [a.strip() for a in provider.coverage_areas.split(",")]
                    )

            return Response(
                {
                    "specializations": sorted(list(specializations)),
                    "insurance_types": sorted(list(insurance_types)),
                    "coverage_areas": sorted(list(areas)),
                }
            )

        except Exception as e:
            return Response(
                {"error": str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )


@api_view(["GET"])
def california_counties(request):
    """
    Return real California county boundaries as GeoJSON
    """
    try:
        import json
        import os
        from django.conf import settings

        # Path to the counties file
        counties_file = os.path.join(settings.BASE_DIR, "ca_counties.geojson")

        # Load and return the GeoJSON data
        with open(counties_file, "r") as f:
            counties_data = json.load(f)

        # Add some processing to ensure consistent format
        for feature in counties_data["features"]:
            # Ensure each feature has an ID
            if "id" not in feature:
                feature["id"] = (
                    feature["properties"].get("name", "").replace(" ", "_").lower()
                )

        return Response(counties_data)

    except FileNotFoundError:
        return Response(
            {
                "error": "California counties data not found",
                "message": "Please ensure ca_counties.geojson exists",
            },
            status=status.HTTP_404_NOT_FOUND,
        )

    except json.JSONDecodeError:
        return Response(
            {
                "error": "Invalid GeoJSON data",
                "message": "California counties file is corrupted",
            },
            status=status.HTTP_500_INTERNAL_SERVER_ERROR,
        )

    except Exception as e:
        return Response(
            {"error": "Unexpected error", "message": str(e)},
            status=status.HTTP_500_INTERNAL_SERVER_ERROR,
        )


@api_view(["GET"])
def api_documentation(request):
    """
    Comprehensive API Documentation
    Lists all available endpoints including @action routes that are not visible in the default API root.

    Access this at: /api/docs/
    """
    base_url = request.build_absolute_uri("/api/")

    docs = {
        "message": "CHLA Provider Map - Complete API Reference",
        "version": "2.0",
        "base_url": base_url,
        "core_endpoints": {
            "regional_centers": f"{base_url}regional-centers/",
            "providers": f"{base_url}providers/",
            "providers_v2": f"{base_url}providers-v2/",
            "providers_legacy": f"{base_url}providers-legacy/",
            "funding_sources": f"{base_url}funding-sources/",
            "insurance_carriers": f"{base_url}insurance-carriers/",
            "service_models": f"{base_url}service-models/",
        },
        "broken_endpoints": {
            "description": "These endpoints exist in the router but return 500 errors - they should be removed or fixed",
            "endpoints": [
                f"{base_url}categories/",
                f"{base_url}locations/",
                f"{base_url}images/",
                f"{base_url}reviews/",
            ],
        },
        "regional_center_actions": {
            "description": "Extended Regional Center endpoints",
            "endpoints": {
                "service_area_boundaries": {
                    "url": f"{base_url}regional-centers/service_area_boundaries/",
                    "method": "GET",
                    "description": "⭐ CRITICAL - Returns GeoJSON with all LA County Regional Centers including polygon geometries AND complete ZIP code arrays",
                    "parameters": None,
                    "example": f"{base_url}regional-centers/service_area_boundaries/",
                },
                "lookup_by_zip": {
                    "url": f"{base_url}regional-centers/lookup_by_zip/",
                    "method": "GET",
                    "description": "Find Regional Center by ZIP code",
                    "parameters": {"zip_code": "5-digit ZIP code (required)"},
                    "example": f"{base_url}regional-centers/lookup_by_zip/?zip_code=90001",
                },
                "nearby": {
                    "url": f"{base_url}regional-centers/nearby/",
                    "method": "GET",
                    "description": "Find Regional Centers near coordinates",
                    "parameters": {
                        "lat": "Latitude (required)",
                        "lng": "Longitude (required)",
                        "radius": "Search radius in miles (default: 25)",
                        "limit": "Max results (default: 10)",
                    },
                    "example": f"{base_url}regional-centers/nearby/?lat=34.0522&lng=-118.2437&radius=25",
                },
                "by_location": {
                    "url": f"{base_url}regional-centers/by_location/",
                    "method": "GET",
                    "description": "Find Regional Centers by address or ZIP (with geocoding)",
                    "parameters": {
                        "location": "Address or ZIP code (required)",
                        "radius": "Search radius in miles (default: 25)",
                        "limit": "Max results (default: 10)",
                    },
                    "example": f"{base_url}regional-centers/by_location/?location=Los+Angeles",
                },
                "zip_code_analysis": {
                    "url": f"{base_url}regional-centers/zip_code_analysis/",
                    "method": "GET",
                    "description": "Analyze ZIP code coverage across all Regional Centers",
                    "parameters": None,
                    "example": f"{base_url}regional-centers/zip_code_analysis/",
                },
            },
        },
        "provider_actions": {
            "description": "Extended Provider search endpoints",
            "endpoints": {
                "by_regional_center": {
                    "url": f"{base_url}providers-v2/by_regional_center/",
                    "method": "GET",
                    "description": "Search providers by Regional Center ZIP code with filters",
                    "parameters": {
                        "zip_code": "5-digit ZIP code (required)",
                        "insurance": "Insurance filter (optional)",
                        "therapy": "Therapy type filter (optional)",
                        "age": "Age group filter (optional)",
                        "diagnosis": "Diagnosis filter (optional)",
                    },
                    "example": f"{base_url}providers-v2/by_regional_center/?zip_code=90001",
                },
                "comprehensive_search": {
                    "url": f"{base_url}providers-v2/comprehensive_search/",
                    "method": "GET",
                    "description": "Comprehensive provider search with location and filters",
                    "parameters": {
                        "lat": "Latitude (optional, requires lng)",
                        "lng": "Longitude (optional, requires lat)",
                        "radius": "Search radius in miles (default: 25)",
                        "q": "Text search query (optional)",
                        "location": "Location string (optional)",
                        "insurance": "Insurance filter (optional)",
                        "therapy": "Therapy type filter (optional)",
                        "age": "Age group filter (optional)",
                        "diagnosis": "Diagnosis filter (optional)",
                    },
                    "example": f"{base_url}providers-v2/comprehensive_search/?lat=34.0522&lng=-118.2437&radius=25",
                },
                "nearby": {
                    "url": f"{base_url}providers-v2/nearby/",
                    "method": "GET",
                    "description": "Find providers near coordinates",
                    "parameters": {
                        "lat": "Latitude (required)",
                        "lng": "Longitude (required)",
                        "radius": "Search radius in miles (default: 25)",
                    },
                    "example": f"{base_url}providers-v2/nearby/?lat=34.0522&lng=-118.2437",
                },
                "by_location": {
                    "url": f"{base_url}providers-v2/by_location/",
                    "method": "GET",
                    "description": "Find providers by address or ZIP (with geocoding)",
                    "parameters": {
                        "location": "Address or ZIP code (required)",
                        "radius": "Search radius in miles (default: 25)",
                    },
                    "example": f"{base_url}providers-v2/by_location/?location=90001",
                },
            },
        },
        "location_actions": {
            "description": "Extended Location endpoints",
            "endpoints": {
                "nearby": {
                    "url": f"{base_url}locations/nearby/",
                    "method": "GET",
                    "description": "Find locations near coordinates",
                    "parameters": {
                        "lat": "Latitude (required)",
                        "lng": "Longitude (required)",
                        "radius": "Search radius in miles (default: 5)",
                    },
                    "example": f"{base_url}locations/nearby/?lat=34.0522&lng=-118.2437&radius=10",
                },
                "by_category": {
                    "url": f"{base_url}locations/by_category/",
                    "method": "GET",
                    "description": "Get locations by category ID",
                    "parameters": {"category_id": "Category ID (required)"},
                    "example": f"{base_url}locations/by_category/?category_id=1",
                },
            },
        },
        "utility_endpoints": {
            "california_counties": {
                "url": f"{base_url}california-counties/",
                "method": "GET",
                "description": "List all California counties",
                "example": f"{base_url}california-counties/",
            },
            "api_docs": {
                "url": f"{base_url}docs/",
                "method": "GET",
                "description": "This comprehensive API documentation endpoint",
                "example": f"{base_url}docs/",
            },
        },
        "notes": [
            "⚠️ All endpoints support standard DRF features: pagination, search, filtering, ordering",
            "⚠️ Use ?format=json to get JSON responses instead of browsable API HTML",
            "⚠️ The service_area_boundaries endpoint is the primary source for Regional Center polygon data and ZIP codes",
            "⚠️ Known issue: Some LA County ZIP codes (e.g., 914xx Sherman Oaks/Van Nuys area) are missing from Regional Center data",
        ],
    }

    return Response(docs)
