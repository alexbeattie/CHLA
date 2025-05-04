from django.urls import path, include
from rest_framework.routers import DefaultRouter
from . import views

router = DefaultRouter()
router.register(r'categories', views.LocationCategoryViewSet)
router.register(r'locations', views.LocationViewSet)
router.register(r'images', views.LocationImageViewSet)
router.register(r'reviews', views.LocationReviewViewSet)

urlpatterns = [
    path('', include(router.urls)),
]