"""
URL configuration for maplocation project.

The `urlpatterns` list routes URLs to views. For more information please see:
    https://docs.djangoproject.com/en/5.2/topics/http/urls/
Examples:
Function views
    1. Add an import:  from my_app import views
    2. Add a URL to urlpatterns:  path('', views.home, name='home')
Class-based views
    1. Add an import:  from other_app.views import Home
    2. Add a URL to urlpatterns:  path('', Home.as_view(), name='home')
Including another URLconf
    1. Import the include() function: from django.urls import include, path
    2. Add a URL to urlpatterns:  path('blog/', include('blog.urls'))
"""
from django.contrib import admin
from django.urls import path, include
from django.conf import settings
from django.conf.urls.static import static
from django.views.generic import TemplateView
from django.http import HttpResponse
from graphene_django.views import GraphQLView
from django.views.decorators.csrf import csrf_exempt

# Customize admin site
admin.site.site_header = "CHLA Provider Portal"
admin.site.site_title = "Provider Portal"
admin.site.index_title = "Welcome to the Provider Management Portal"

urlpatterns = [
    path('client-portal/', admin.site.urls),
    path('api/', include('locations.urls')),
    path('api/users/', include('users.urls')),
    path('api-auth/', include('rest_framework.urls')),
    path('graphql/', csrf_exempt(GraphQLView.as_view(graphiql=True))),
    path('basic/', TemplateView.as_view(template_name='vue_app/basic.html'), name='basic'),
    path('mapbox/', TemplateView.as_view(template_name='vue_app/mapbox.html'), name='mapbox'),
    path('public/', TemplateView.as_view(template_name='vue_app/mapbox_public.html'), name='mapbox_public'),
    path('leaflet/', TemplateView.as_view(template_name='vue_app/leaflet.html'), name='leaflet'),
    path('vue/', TemplateView.as_view(template_name='vue_app/vue_mapbox_raw.html'), name='vue_mapbox'),
    path('simple/', TemplateView.as_view(template_name='vue_app/simple.html'), name='simple'),
    # Simple health check endpoint for load balancer
    path('', lambda request: HttpResponse('OK'), name='health'),
]

# Serve media files in development
if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
    urlpatterns += static(settings.STATIC_URL, document_root=settings.STATICFILES_DIRS[0])
