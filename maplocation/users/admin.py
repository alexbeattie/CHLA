from django.contrib import admin
from .models import UserProfile

@admin.register(UserProfile)
class UserProfileAdmin(admin.ModelAdmin):
    list_display = ['user', 'age', 'diagnosis', 'city', 'zip_code']
    list_filter = ['diagnosis', 'city', 'state']
    search_fields = ['user__username', 'user__email', 'address', 'city', 'zip_code']
    raw_id_fields = ['user']
    fieldsets = (
        (None, {
            'fields': ('user', 'age', 'diagnosis', 'other_diagnosis')
        }),
        ('Location', {
            'fields': ('address', 'city', 'state', 'zip_code', 'latitude', 'longitude')
        }),
    )