"""
Minimal admin configuration - only models with existing database tables
"""
from django.contrib import admin
from django.forms import TextInput, Textarea, ModelForm
from django.db import models
import json
from decimal import Decimal, ROUND_HALF_UP

# Only import models we know exist in the database
from .models import ProviderV2, RegionalCenter
from .utils.geocode import geocode_address

class ProviderV2Form(ModelForm):
    class Meta:
        model = ProviderV2
        fields = '__all__'

    def clean(self):
        cleaned = super().clean()

        # Normalize insurance_accepted and languages_spoken
        def normalize_text_or_json(value):
            if value is None:
                return value
            text = str(value).strip()
            if not text:
                return ''
            # If JSON array provided, convert to comma-separated string
            if text.startswith('[') and text.endswith(']'):
                try:
                    arr = json.loads(text)
                    if isinstance(arr, list):
                        return ', '.join([str(x).strip() for x in arr if str(x).strip()])
                except:
                    pass
            return text

        cleaned['insurance_accepted'] = normalize_text_or_json(cleaned.get('insurance_accepted'))
        cleaned['languages_spoken'] = normalize_text_or_json(cleaned.get('languages_spoken'))

        # Geocode if address provided and coordinates missing
        address = cleaned.get('address')
        lat = cleaned.get('latitude')
        lng = cleaned.get('longitude')
        if address and (lat in (None, '') or lng in (None, '')):
            coords = geocode_address(address)
            if coords:
                def quant(x):
                    return Decimal(str(x)).quantize(Decimal('0.000001'), rounding=ROUND_HALF_UP)
                cleaned['latitude'] = quant(coords[0])
                cleaned['longitude'] = quant(coords[1])

        return cleaned

@admin.register(ProviderV2)
class ProviderV2Admin(admin.ModelAdmin):
    form = ProviderV2Form
    list_display = ['name', 'type', 'phone', 'verified']
    list_filter = ['verified', 'type']
    search_fields = ['name', 'address', 'type', 'insurance_accepted', 'languages_spoken']
    list_per_page = 50  # Paginate to avoid loading all records at once
    
    fieldsets = (
        ('Basic Information', {
            'fields': ('name', 'type', 'description', 'verified')
        }),
        ('Contact', {
            'fields': ('phone', 'email', 'website')
        }),
        ('Address & Location', {
            'fields': ('address', 'latitude', 'longitude'),
            'description': 'Coordinates will be automatically geocoded from address if left empty.'
        }),
        ('Service Details', {
            'fields': ('hours', 'insurance_accepted', 'languages_spoken'),
            'classes': ('collapse',),
        }),
        ('System Information', {
            'fields': ('id', 'created_at', 'updated_at'),
            'classes': ('collapse',),
        }),
    )
    
    readonly_fields = ['id', 'created_at', 'updated_at']
    
    formfield_overrides = {
        models.CharField: {'widget': TextInput(attrs={'size': '80'})},
        models.TextField: {'widget': Textarea(attrs={'rows': 3, 'cols': 80})},
    }
    
    def save_model(self, request, obj, form, change):
        """Override to add any custom save logic"""
        super().save_model(request, obj, form, change)
        
    def get_queryset(self, request):
        """Optimize queryset to avoid N+1 queries"""
        qs = super().get_queryset(request)
        # Add any select_related or prefetch_related here if needed
        return qs

@admin.register(RegionalCenter)
class RegionalCenterAdmin(admin.ModelAdmin):
    list_display = ['regional_center', 'office_type', 'city', 'county_served', 'telephone']
    list_filter = ['office_type', 'county_served', 'city']
    search_fields = ['regional_center', 'address', 'city', 'county_served']
    list_per_page = 50
    
    fieldsets = (
        ('Basic Information', {
            'fields': ('regional_center', 'office_type', 'county_served', 'los_angeles_health_district')
        }),
        ('Contact Information', {
            'fields': ('telephone', 'website')
        }),
        ('Address', {
            'fields': ('address', 'suite', 'city', 'state', 'zip_code'),
        }),
        ('Location Data', {
            'fields': ('latitude', 'longitude', 'location_coordinates', 'location'),
            'classes': ('collapse',),
        }),
        ('Service Area', {
            'fields': ('service_area', 'service_radius_miles'),
            'classes': ('collapse',),
        }),
    )
    
    readonly_fields = ['location_coordinates']
    
    formfield_overrides = {
        models.CharField: {'widget': TextInput(attrs={'size': '60'})},
        models.TextField: {'widget': Textarea(attrs={'rows': 3, 'cols': 60})},
    }

# Customize admin site header
admin.site.site_header = "CHLA Provider Portal"
admin.site.site_title = "Provider Portal"
admin.site.index_title = "Provider Management"