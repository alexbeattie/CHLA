from django.urls import path, include
from rest_framework.routers import DefaultRouter
from . import views

router = DefaultRouter()
router.register(r"categories", views.LocationCategoryViewSet)
router.register(r"locations", views.LocationViewSet)
router.register(r"images", views.LocationImageViewSet)
router.register(r"reviews", views.LocationReviewViewSet)
router.register(r"regional-centers", views.RegionalCenterViewSet)
router.register(r"providers", views.ProviderV2ViewSet)
router.register(r"providers-v2", views.ProviderV2ViewSet)  # compatibility alias
router.register(r"providers-legacy", views.ProviderViewSet)

# Register new reference data viewsets
router.register(r"funding-sources", views.FundingSourceViewSet)
router.register(r"insurance-carriers", views.InsuranceCarrierViewSet)
router.register(r"service-models", views.ServiceDeliveryModelViewSet)

urlpatterns = [
    path("", include(router.urls)),
    path("california-counties/", views.california_counties, name="california-counties"),
]
