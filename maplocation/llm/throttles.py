"""Privacy-preserving throttles for assistant response reports."""

from datetime import timedelta

from django.conf import settings
from django.db.models import F
from django.utils import timezone
from rest_framework.throttling import BaseThrottle

from .models import ResponseReportThrottleWindow


class GlobalResponseReportThrottle(BaseThrottle):
    """Apply one atomic global fixed-window limit without identifying clients."""

    default_limit = 60
    retention = timedelta(days=1)

    def allow_request(self, request, view):
        limit = int(
            getattr(
                settings,
                "RESPONSE_REPORT_GLOBAL_RATE_LIMIT",
                self.default_limit,
            )
        )
        window_start = timezone.now().replace(second=0, microsecond=0)
        _, created = ResponseReportThrottleWindow.objects.get_or_create(
            window_start=window_start,
            defaults={"request_count": 0},
        )
        consumed = ResponseReportThrottleWindow.objects.filter(
            window_start=window_start,
            request_count__lt=limit,
        ).update(request_count=F("request_count") + 1)

        if created:
            ResponseReportThrottleWindow.objects.filter(
                window_start__lt=window_start - self.retention
            ).delete()

        return consumed == 1
