import csv

from django.contrib import admin
from django.http import HttpResponse

from .models import SurveyResponse
from .notifications import QUESTION_LABELS

CSV_COLUMNS = ("id", "created_at", "platform", "locale", "app_version", *QUESTION_LABELS)


def survey_csv_row(response):
    row = []
    for field in CSV_COLUMNS:
        if field in SurveyResponse.LIKERT_FIELDS:
            value = getattr(response, field)
            row.append("" if value is None else f"{value} - {response.likert_label(field)}")
        elif field == "created_at":
            row.append(response.created_at.isoformat())
        elif field == "diagnosis_status":
            row.append(response.get_diagnosis_status_display())
        else:
            row.append(getattr(response, field))
    return row


@admin.register(SurveyResponse)
class SurveyResponseAdmin(admin.ModelAdmin):
    list_display = (
        "id",
        "created_at",
        "platform",
        "locale",
        "ease_of_use",
        "helpfulness",
        "confidence_after",
    )
    list_filter = ("platform", "locale", "diagnosis_status", "created_at")
    readonly_fields = [field.name for field in SurveyResponse._meta.fields]
    ordering = ("-created_at",)
    actions = ["download_csv"]

    def has_add_permission(self, request):
        return False

    def has_change_permission(self, request, obj=None):
        return False

    @admin.action(description="Download selected responses as CSV")
    def download_csv(self, request, queryset):
        response = HttpResponse(content_type="text/csv; charset=utf-8")
        response["Content-Disposition"] = 'attachment; filename="kindd-survey-responses.csv"'
        response.write("\ufeff")
        writer = csv.writer(response)
        writer.writerow(
            [QUESTION_LABELS.get(column, column) for column in CSV_COLUMNS]
        )
        for survey in queryset.order_by("created_at"):
            writer.writerow(survey_csv_row(survey))
        return response
