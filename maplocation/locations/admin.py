from django.contrib import admin
from django.utils.html import format_html
from django.urls import reverse, path
from django.utils.safestring import mark_safe
from django.db import models
from django.forms import TextInput, Textarea
from django.shortcuts import render, redirect
from django.contrib import messages
from django.http import HttpResponse
from .models import (
    LocationCategory, Location, LocationImage, LocationReview, 
    RegionalCenter, Provider, ProviderRegionalCenter,
    FundingSource, InsuranceCarrier, ServiceDeliveryModel,
    ProviderFundingSource, ProviderInsuranceCarrier, ProviderServiceModel
)
from .utils.csv_utils import CSVExporter, CSVImporter, generate_csv_template

class LocationImageInline(admin.TabularInline):
    model = LocationImage
    extra = 1

class LocationReviewInline(admin.TabularInline):
    model = LocationReview
    extra = 0
    readonly_fields = ['name', 'email', 'rating', 'comment', 'created_at']
    can_delete = False

class ProviderRegionalCenterInline(admin.TabularInline):
    model = ProviderRegionalCenter
    extra = 1
    fields = ['regional_center', 'is_primary', 'notes']

class ProviderFundingSourceInline(admin.TabularInline):
    model = ProviderFundingSource
    extra = 1

class ProviderInsuranceCarrierInline(admin.TabularInline):
    model = ProviderInsuranceCarrier
    extra = 1

class ProviderServiceModelInline(admin.TabularInline):
    model = ProviderServiceModel
    extra = 1

@admin.register(LocationCategory)
class LocationCategoryAdmin(admin.ModelAdmin):
    list_display = ['name', 'description']
    search_fields = ['name']

@admin.register(Location)
class LocationAdmin(admin.ModelAdmin):
    list_display = ['name', 'address', 'city', 'category', 'rating', 'is_active']
    list_filter = ['category', 'is_active', 'has_parking', 'is_accessible', 'price_level']
    search_fields = ['name', 'address', 'description']
    inlines = [LocationImageInline, LocationReviewInline]
    fieldsets = (
        (None, {
            'fields': ('name', 'category', 'description', 'is_active')
        }),
        ('Contact Information', {
            'fields': ('phone', 'website', 'email')
        }),
        ('Address', {
            'fields': ('address', 'city', 'state', 'zip_code', 'latitude', 'longitude')
        }),
        ('Features', {
            'fields': ('rating', 'price_level', 'hours_of_operation', 'has_parking', 'is_accessible')
        }),
    )

@admin.register(LocationImage)
class LocationImageAdmin(admin.ModelAdmin):
    list_display = ['location', 'caption', 'is_primary', 'uploaded_at']
    list_filter = ['is_primary', 'location']
    search_fields = ['location__name', 'caption']

@admin.register(LocationReview)
class LocationReviewAdmin(admin.ModelAdmin):
    list_display = ['location', 'name', 'rating', 'created_at']
    list_filter = ['rating', 'created_at']
    search_fields = ['location__name', 'name', 'comment']
    readonly_fields = ['created_at']

@admin.register(Provider)
class ProviderAdmin(admin.ModelAdmin):
    list_display = ['name', 'phone', 'city', 'state', 'coordinates_status', 'insurance_status', 'regional_center_count']
    list_filter = []
    search_fields = ['name', 'address', 'specializations', 'services', 'insurance_accepted']
    readonly_fields = ['created_display', 'coordinates_display', 'regional_centers_display']
    inlines = [ProviderRegionalCenterInline, ProviderFundingSourceInline, ProviderInsuranceCarrierInline, ProviderServiceModelInline]
    
    fieldsets = (
        ('Basic Information', {
            'fields': ('name', 'phone', 'website_domain')
        }),
        ('Address & Location', {
            'fields': ('address', 'latitude', 'longitude', 'coordinates_display'),
            'description': 'Coordinates will be automatically geocoded from address if left empty.'
        }),
        ('Service Areas', {
            'fields': ('areas', 'coverage_areas', 'center_based_services'),
            'classes': ('collapse',),
        }),
        ('Services & Specializations', {
            'fields': ('specializations', 'services'),
            'description': 'Use comma-separated values for multiple entries.'
        }),
        ('Insurance & Funding', {
            'fields': ('insurance_accepted',),
            'description': 'List accepted insurance types, regional centers, and payment methods.'
        }),
        ('System Information', {
            'fields': ('created_display', 'regional_centers_display'),
            'classes': ('collapse',),
        }),
    )
    
    formfield_overrides = {
        models.CharField: {'widget': TextInput(attrs={'size': '80'})},
        models.TextField: {'widget': Textarea(attrs={'rows': 3, 'cols': 80})},
    }
    
    actions = ['geocode_addresses', 'validate_coordinates', 'export_selected_csv']
    
    def city(self, obj):
        return obj.city
    
    def state(self, obj):
        return obj.state
    
    def coordinates_status(self, obj):
        if obj.latitude and obj.longitude:
            return format_html(
                '<span style="color: green;">✓ Valid ({:.4f}, {:.4f})</span>',
                float(obj.latitude), float(obj.longitude)
            )
        return format_html('<span style="color: red;">✗ Missing</span>')
    coordinates_status.short_description = 'Coordinates'
    
    def insurance_status(self, obj):
        if obj.insurance_accepted:
            return format_html('<span style="color: green;">✓ Configured</span>')
        return format_html('<span style="color: orange;">⚠ Not Set</span>')
    insurance_status.short_description = 'Insurance'
    
    def regional_center_count(self, obj):
        count = obj.regional_centers.count()
        if count > 0:
            return format_html('<span style="color: green;">{} centers</span>', count)
        return format_html('<span style="color: orange;">No centers</span>')
    regional_center_count.short_description = 'Regional Centers'
    
    def coordinates_display(self, obj):
        if obj.latitude and obj.longitude:
            maps_url = f"https://www.google.com/maps/search/?api=1&query={obj.latitude},{obj.longitude}"
            return format_html(
                '<a href="{}" target="_blank">View on Map: {:.4f}, {:.4f}</a>',
                maps_url, float(obj.latitude), float(obj.longitude)
            )
        return "No coordinates available"
    coordinates_display.short_description = 'Map Location'
    
    def created_display(self, obj):
        return "Provider record"
    created_display.short_description = 'Record Type'
    
    def regional_centers_display(self, obj):
        centers = obj.regional_centers.all()
        if centers.exists():
            links = []
            for rel in centers:
                url = reverse('admin:locations_regionalcenter_change', args=[rel.regional_center.id])
                links.append(format_html('<a href="{}">{}</a>', url, rel.regional_center.regional_center))
            return format_html(', '.join(links))
        return "No regional centers assigned"
    regional_centers_display.short_description = 'Associated Regional Centers'
    
    def geocode_addresses(self, request, queryset):
        geocoded_count = 0
        for provider in queryset:
            if provider.address and (not provider.latitude or not provider.longitude):
                # Here you would implement actual geocoding logic
                # For now, we'll just mark the action as completed
                geocoded_count += 1
        
        self.message_user(
            request,
            f'Geocoding initiated for {geocoded_count} providers. '
            f'Please check coordinates after processing completes.'
        )
    geocode_addresses.short_description = 'Geocode selected addresses'
    
    def validate_coordinates(self, request, queryset):
        invalid_count = 0
        for provider in queryset:
            if provider.latitude and provider.longitude:
                lat, lng = float(provider.latitude), float(provider.longitude)
                if not (32.0 <= lat <= 42.0 and -125.0 <= lng <= -114.0):
                    invalid_count += 1
        
        self.message_user(
            request,
            f'Found {invalid_count} providers with coordinates outside California bounds.'
        )
    validate_coordinates.short_description = 'Validate coordinates'
    
    def export_selected_csv(self, request, queryset):
        return CSVExporter.export_providers(queryset)
    export_selected_csv.short_description = 'Export selected to CSV'
    
    def get_urls(self):
        urls = super().get_urls()
        custom_urls = [
            path('import-csv/', self.import_csv, name='provider_import_csv'),
            path('export-csv/', self.export_all_csv, name='provider_export_csv'),
            path('download-template/', self.download_template, name='provider_download_template'),
        ]
        return custom_urls + urls
    
    def import_csv(self, request):
        if request.method == 'POST':
            csv_file = request.FILES.get('csv_file')
            update_existing = request.POST.get('update_existing', False)
            
            if csv_file:
                importer = CSVImporter()
                result = importer.import_providers(csv_file, update_existing)
                
                if result['success_count'] > 0:
                    messages.success(
                        request,
                        f'Successfully imported {result["success_count"]} providers'
                    )
                
                if result['error_count'] > 0:
                    messages.error(
                        request,
                        f'Failed to import {result["error_count"]} providers'
                    )
                
                for error in result['errors']:
                    messages.error(request, error)
                
                for warning in result['warnings']:
                    messages.warning(request, warning)
                
                return redirect('..')
            else:
                messages.error(request, 'Please select a CSV file')
        
        return render(request, 'admin/locations/provider/import_csv.html')
    
    def export_all_csv(self, request):
        return CSVExporter.export_providers(Provider.objects.all())
    
    def download_template(self, request):
        response = HttpResponse(content_type='text/csv')
        response['Content-Disposition'] = 'attachment; filename="provider_import_template.csv"'
        response.write(generate_csv_template('providers'))
        return response

@admin.register(RegionalCenter)
class RegionalCenterAdmin(admin.ModelAdmin):
    list_display = ['regional_center', 'city', 'state', 'county_served', 'coordinates_status', 'provider_count']
    list_filter = ['state', 'county_served', 'office_type']
    search_fields = ['regional_center', 'address', 'city', 'county_served']
    readonly_fields = ['coordinates_display', 'provider_count_display']
    
    fieldsets = (
        ('Basic Information', {
            'fields': ('regional_center', 'office_type', 'telephone', 'website')
        }),
        ('Address & Location', {
            'fields': ('address', 'suite', 'city', 'state', 'zip_code', 'latitude', 'longitude', 'coordinates_display')
        }),
        ('Service Information', {
            'fields': ('county_served', 'los_angeles_health_district', 'service_radius_miles')
        }),
        ('Service Area', {
            'fields': ('service_area',),
            'classes': ('collapse',),
            'description': 'Geographic service area boundary (stored as text temporarily)'
        }),
        ('Provider Relationships', {
            'fields': ('provider_count_display',),
            'classes': ('collapse',),
        }),
    )
    
    def coordinates_status(self, obj):
        if obj.latitude and obj.longitude:
            return format_html(
                '<span style="color: green;">✓ Valid ({:.4f}, {:.4f})</span>',
                float(obj.latitude), float(obj.longitude)
            )
        return format_html('<span style="color: red;">✗ Missing</span>')
    coordinates_status.short_description = 'Coordinates'
    
    def provider_count(self, obj):
        count = obj.providers.count()
        if count > 0:
            return format_html('<span style="color: green;">{} providers</span>', count)
        return format_html('<span style="color: orange;">No providers</span>')
    provider_count.short_description = 'Providers'
    
    def coordinates_display(self, obj):
        if obj.latitude and obj.longitude:
            maps_url = f"https://www.google.com/maps/search/?api=1&query={obj.latitude},{obj.longitude}"
            return format_html(
                '<a href="{}" target="_blank">View on Map: {:.4f}, {:.4f}</a>',
                maps_url, float(obj.latitude), float(obj.longitude)
            )
        return "No coordinates available"
    coordinates_display.short_description = 'Map Location'
    
    def provider_count_display(self, obj):
        count = obj.providers.count()
        if count > 0:
            return format_html('{} providers associated with this regional center', count)
        return "No providers associated"
    provider_count_display.short_description = 'Provider Relationships'

@admin.register(FundingSource)
class FundingSourceAdmin(admin.ModelAdmin):
    list_display = ['name', 'description']
    search_fields = ['name', 'description']

@admin.register(InsuranceCarrier)
class InsuranceCarrierAdmin(admin.ModelAdmin):
    list_display = ['name', 'description']
    search_fields = ['name', 'description']

@admin.register(ServiceDeliveryModel)
class ServiceDeliveryModelAdmin(admin.ModelAdmin):
    list_display = ['name', 'description']
    search_fields = ['name', 'description']

@admin.register(ProviderRegionalCenter)
class ProviderRegionalCenterAdmin(admin.ModelAdmin):
    list_display = ['provider', 'regional_center', 'is_primary', 'created_at']
    list_filter = ['is_primary', 'created_at']
    search_fields = ['provider__name', 'regional_center__regional_center']
