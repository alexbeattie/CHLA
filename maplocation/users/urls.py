from django.urls import path, include
from rest_framework.routers import DefaultRouter
from . import views
from . import auth_views

router = DefaultRouter()
router.register(r'profiles', views.UserProfileViewSet)

urlpatterns = [
    path('', include(router.urls)),
    # Authentication endpoints
    path('auth/login/', auth_views.login_view, name='api-login'),
    path('auth/logout/', auth_views.logout_view, name='api-logout'),
    path('auth/current/', auth_views.current_user_view, name='api-current-user'),
]