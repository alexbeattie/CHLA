from django.urls import path, include
from rest_framework.routers import DefaultRouter
from . import views
from .update_orange_county_view import update_orange_county_zips

router = DefaultRouter()
router.register(r"categories", views.LocationCategoryViewSet)
router.register(r"locations", views.LocationViewSet)
router.register(r"images", views.LocationImageViewSet)
router.register(r"reviews", views.LocationReviewViewSet)
router.register(r"regional-centers", views.RegionalCenterViewSet)
router.register(r"providers", views.ProviderV2ViewSet)
router.register(
    r"providers-v2", views.ProviderV2ViewSet, basename="providers-v2"
)  # compatibility alias with unique basename
# REMOVED: providers-legacy endpoint (old Provider model has been removed)

# Register new reference data viewsets
router.register(r"funding-sources", views.FundingSourceViewSet)
router.register(r"insurance-carriers", views.InsuranceCarrierViewSet)
router.register(r"service-models", views.ServiceDeliveryModelViewSet)

# HMGL (Help Me Grow LA) locations from external hmgl.location table
router.register(r"hmgl-locations", views.HMGLLocationViewSet, basename="hmgl-locations")

urlpatterns = [
    path("", include(router.urls)),
    path("health/", views.health_check, name="health-check"),
    path("docs/", views.api_documentation, name="api-docs"),
    path("california-counties/", views.california_counties, name="california-counties"),
    path(
        "update-orange-county-zips/",
        update_orange_county_zips,
        name="update_orange_county_zips",
    ),
]
