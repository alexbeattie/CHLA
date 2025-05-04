from django.contrib import admin
from .models import LocationCategory, Location, LocationImage, LocationReview

class LocationImageInline(admin.TabularInline):
    model = LocationImage
    extra = 1

class LocationReviewInline(admin.TabularInline):
    model = LocationReview
    extra = 0
    readonly_fields = ['name', 'email', 'rating', 'comment', 'created_at']
    can_delete = False

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
