from django.db import models


class AssistantResponseReport(models.Model):
    class Reason(models.TextChoices):
        UNSAFE_OR_INAPPROPRIATE = (
            "unsafe_or_inappropriate",
            "Unsafe or inappropriate",
        )
        INACCURATE_OR_MISLEADING = (
            "inaccurate_or_misleading",
            "Inaccurate or misleading",
        )
        OTHER = "other", "Other"

    reason = models.CharField(max_length=32, choices=Reason.choices)
    reported_response = models.TextField()
    locale = models.CharField(max_length=16)
    platform = models.CharField(max_length=16, choices=(("android", "Android"),))
    app_version = models.CharField(max_length=32)
    response_fingerprint_digest = models.CharField(
        max_length=64,
        unique=True,
        editable=False,
    )
    created_at = models.DateTimeField(auto_now_add=True, db_index=True)

    class Meta:
        ordering = ("-created_at",)
        verbose_name = "assistant response report"
        verbose_name_plural = "assistant response reports"

    def __str__(self):
        return f"{self.get_reason_display()} ({self.created_at:%Y-%m-%d %H:%M})"


class ResponseReportThrottleWindow(models.Model):
    """One global request count per fixed window; contains no client identity."""

    window_start = models.DateTimeField(primary_key=True)
    request_count = models.PositiveIntegerField(default=0)

    class Meta:
        verbose_name = "response report throttle window"
        verbose_name_plural = "response report throttle windows"
