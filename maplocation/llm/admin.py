from django.contrib import admin

from .models import AssistantResponseReport


@admin.register(AssistantResponseReport)
class AssistantResponseReportAdmin(admin.ModelAdmin):
    list_display = ("reason", "locale", "platform", "app_version", "created_at")
    list_filter = ("reason", "locale", "app_version", "created_at")
    readonly_fields = (
        "reason",
        "reported_response",
        "locale",
        "platform",
        "app_version",
        "response_fingerprint_digest",
        "created_at",
    )
    ordering = ("-created_at",)

    def has_add_permission(self, request):
        return False
