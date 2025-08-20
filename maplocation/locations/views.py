from rest_framework import viewsets, filters, status
from rest_framework.decorators import action
from rest_framework.response import Response
from django.db.models import Q, Avg
# from django.contrib.gis.geos import Point
# from django.contrib.gis.measure import D
from django_filters.rest_framework import DjangoFilterBackend
from .models import (
    LocationCategory,
    Location,
    LocationImage,
    LocationReview,
    RegionalCenter,
    Provider,
    FundingSource,
    InsuranceCarrier,
    ServiceDeliveryModel,
    ProviderFundingSource,
    ProviderInsuranceCarrier,
    ProviderServiceModel,
    ProviderRegionalCenter,
)
from .serializers import (
    LocationCategorySerializer,
    LocationSerializer,
    LocationImageSerializer,
    LocationReviewSerializer,
    RegionalCenterSerializer,
    ProviderSerializer,
    ProviderWriteSerializer,
    ProviderGeoSerializer,
    FundingSourceSerializer,
    InsuranceCarrierSerializer,
    ServiceDeliveryModelSerializer,
    ProviderRegionalCenterSerializer,
    GeoJSONRegionalCenterSerializer,
    ServiceAreaSerializer,
)
import math
from rest_framework.decorators import api_view
from django.db.models.expressions import RawSQL


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
        - radius: search radius in kilometers (default: 5)
        """
        try:
            # Get parameters with proper error handling
            lat_param = request.query_params.get("lat")
            lng_param = request.query_params.get("lng")
            radius_param = request.query_params.get("radius", "5")  # Default to 5 km

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
                    a = (
                        math.sin(dlat / 2) ** 2
                        + math.cos(lat1) * math.cos(lat2) * math.sin(dlng / 2) ** 2
                    )
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
            nearby_locations.sort(key=lambda x: getattr(x, "distance", float("inf")))

            # Serialize the data
            serializer = self.get_serializer(nearby_locations, many=True)

            # Add distance to serialized data, ensuring we're working with mutable data
            serializer_data = list(serializer.data)
            for i, location in enumerate(nearby_locations):
                if i < len(serializer_data):  # Safety check
                    serializer_data[i] = dict(
                        serializer_data[i]
                    )  # Make mutable if it's not
                    serializer_data[i]["distance"] = location.distance

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

            serializer = ProviderSerializer(providers, many=True)
            return Response(serializer.data)

        except Exception as e:
            return Response(
                {"error": str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR
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


class ProviderViewSet(viewsets.ModelViewSet):
    queryset = Provider.objects.all()
    serializer_class = ProviderSerializer
    filter_backends = [
        DjangoFilterBackend,
        filters.SearchFilter,
        filters.OrderingFilter,
    ]
    search_fields = ["name", "coverage_areas", "center_based_services"]
    ordering_fields = ["name"]
    
    def get_serializer_class(self):
        """Use different serializers for different operations"""
        if self.action in ['create', 'update', 'partial_update']:
            return ProviderWriteSerializer
        return ProviderSerializer

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
            providers = Provider.objects.filter(
                Q(coverage_areas__icontains=area_param) | Q(areas__icontains=area_param)
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
            providers = Provider.find_nearest(lat, lng, radius, limit)

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
            providers = Provider.geocode_and_search(location, radius, limit)

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
        Comprehensive search with multiple filters.
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
            providers = Provider.objects.all()

            # Apply text search
            if query:
                # Text search across common text fields, including address
                providers = providers.filter(
                    Q(name__icontains=query)
                    | Q(address__icontains=query)
                    | Q(specializations__icontains=query)
                    | Q(services__icontains=query)
                    | Q(center_based_services__icontains=query)
                    | Q(areas__icontains=query)
                    | Q(coverage_areas__icontains=query)
                    | Q(insurance_accepted__icontains=query)
                )

            # Apply insurance filters
            if insurance_filters:
                insurance_q = Q()
                for insurance_type in insurance_filters:
                    if insurance_type.lower() == "insurance":
                        # Filter for providers that accept any form of insurance
                        # Look for common insurance-related terms and specific insurers
                        insurance_q |= (
                            Q(insurance_accepted__icontains="insurance")
                            | Q(insurance_accepted__icontains="medical")
                            | Q(insurance_accepted__icontains="health")
                            | Q(insurance_accepted__icontains="medi-cal")
                            | Q(insurance_accepted__icontains="medicaid")
                            | Q(insurance_accepted__icontains="blue")
                            | Q(insurance_accepted__icontains="anthem")
                            | Q(insurance_accepted__icontains="aetna")
                            | Q(insurance_accepted__icontains="cigna")
                            | Q(insurance_accepted__icontains="united")
                            | Q(insurance_accepted__icontains="kaiser")
                            | Q(insurance_accepted__icontains="optum")
                            | Q(insurance_accepted__icontains="magellan")
                            | Q(insurance_accepted__icontains="beacon")
                            | Q(insurance_accepted__icontains="molina")
                            | Q(insurance_accepted__icontains="humana")
                            | Q(insurance_accepted__icontains="tricare")
                            |
                            # Exclude null/empty values
                            Q(insurance_accepted__isnull=False)
                            & ~Q(insurance_accepted="")
                        )
                    elif insurance_type.lower() == "regional center":
                        # Filter for providers that accept regional center funding
                        insurance_q |= Q(
                            insurance_accepted__icontains="regional center"
                        ) | Q(insurance_accepted__icontains="regional centre")
                    elif insurance_type.lower() == "private pay":
                        # Filter for providers that accept private pay
                        insurance_q |= (
                            Q(insurance_accepted__icontains="private pay")
                            | Q(insurance_accepted__icontains="private")
                            | Q(insurance_accepted__icontains="self pay")
                        )
                    else:
                        # Generic insurance filter
                        insurance_q |= Q(insurance_accepted__icontains=insurance_type)

                providers = providers.filter(insurance_q)

            # Apply specialization filter (diagnosis) against enum[] using UNNEST
            if specialization:
                providers = providers.filter(
                    RawSQL(
                        "EXISTS (SELECT 1 FROM unnest(specializations) s WHERE lower(s::text)=lower(%s))",
                        [specialization],
                    )
                )

            # Apply diagnosis filter (enum[])
            if diagnosis:
                providers = providers.filter(
                    RawSQL(
                        "EXISTS (SELECT 1 FROM unnest(specializations) s WHERE lower(s::text)=lower(%s))",
                        [diagnosis],
                    )
                )

            # Apply therapy filters (multiple allowed)
            therapy_values = request.query_params.getlist("therapy")
            for therapy in therapy_values:
                providers = providers.filter(
                    RawSQL(
                        "EXISTS (SELECT 1 FROM unnest(services) sv WHERE lower(sv::text)=lower(%s))",
                        [therapy],
                    )
                )

            # Apply location-based filtering using provided coordinates
            if lat and lng:
                try:
                    lat_float = float(lat)
                    lng_float = float(lng)

                    # Get IDs of already filtered providers
                    filtered_provider_ids = list(providers.values_list('id', flat=True))
                    
                    if filtered_provider_ids:
                        # Filter by distance using raw SQL, but only for already filtered providers
                        from django.db import connection

                        # Convert provider IDs to a comma-separated string for SQL IN clause
                        id_list = ','.join(map(str, filtered_provider_ids))
                        
                        with connection.cursor() as cursor:
                            cursor.execute(
                                f"""
                                SELECT id FROM providers 
                                WHERE id IN ({id_list})
                                AND latitude IS NOT NULL AND longitude IS NOT NULL
                                AND (3959 * acos(cos(radians(%s)) * cos(radians(latitude)) * 
                                cos(radians(longitude) - radians(%s)) + sin(radians(%s)) * 
                                sin(radians(latitude)))) < %s
                            """,
                                [lat_float, lng_float, lat_float, radius],
                            )
                            nearby_ids = [row[0] for row in cursor.fetchall()]

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
                    filtered_provider_ids = list(providers.values_list('id', flat=True))
                    
                    if filtered_provider_ids:
                        # Filter by distance using raw SQL, but only for already filtered providers
                        from django.db import connection

                        # Convert provider IDs to a comma-separated string for SQL IN clause
                        id_list = ','.join(map(str, filtered_provider_ids))
                        
                        with connection.cursor() as cursor:
                            cursor.execute(
                                f"""
                                SELECT id FROM providers 
                                WHERE id IN ({id_list})
                                AND latitude IS NOT NULL AND longitude IS NOT NULL
                                AND (3959 * acos(cos(radians(%s)) * cos(radians(latitude)) * 
                                cos(radians(longitude) - radians(%s)) + sin(radians(%s)) * 
                                sin(radians(latitude)))) < %s
                            """,
                                [lat_coord, lng_coord, lat_coord, radius],
                            )
                            nearby_ids = [row[0] for row in cursor.fetchall()]

                        providers = providers.filter(id__in=nearby_ids)
                    else:
                        # If no providers match the non-geographic filters, return empty queryset
                        providers = providers.none()

            # Limit results
            providers = providers[:50]

            serializer = self.get_serializer(providers, many=True)
            return Response(serializer.data)

        except Exception as e:
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

            for provider in Provider.objects.all():
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
