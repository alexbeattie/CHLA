from django.apps import apps
from django.contrib import admin
from django.core.cache import cache
import pytest
from rest_framework.test import APIClient


REPORT_URL = "/api/llm/response-reports/"
VALID_REPORT = {
    "reason": "inaccurate_or_misleading",
    "reported_response": "The assistant response that needs review.",
    "locale": "en-US",
    "platform": "android",
    "app_version": "1.4.1",
}


@pytest.fixture(autouse=True)
def clear_throttle_cache():
    cache.clear()
    yield
    cache.clear()


def report_model():
    return apps.all_models["llm"].get("assistantresponsereport")


def test_valid_anonymous_report_is_persisted_for_admin_review(db):
    response = APIClient().post(REPORT_URL, VALID_REPORT, format="json")

    assert response.status_code == 201
    assert set(response.json()) == {"id", "status"}
    assert response.json()["status"] == "received"

    model = report_model()
    assert model is not None
    report = model.objects.get()
    assert report.reason == "inaccurate_or_misleading"
    assert report.reported_response == VALID_REPORT["reported_response"]
    assert report.locale == "en-US"
    assert report.platform == "android"
    assert report.app_version == "1.4.1"
    assert model in admin.site._registry


def test_report_rejects_unsupported_reason_empty_and_oversized_content(db):
    client = APIClient()
    invalid_payloads = (
        {**VALID_REPORT, "reason": "dislike"},
        {**VALID_REPORT, "reported_response": "   "},
        {**VALID_REPORT, "reported_response": "x" * 6001},
    )

    for index, payload in enumerate(invalid_payloads, start=1):
        response = client.post(
            REPORT_URL,
            payload,
            format="json",
            REMOTE_ADDR=f"192.0.2.{index}",
        )
        assert response.status_code == 400

    model = report_model()
    assert model is not None
    assert model.objects.count() == 0


def test_report_rejects_sensitive_or_free_form_extra_fields_without_persisting(db):
    client = APIClient()

    for index, (field, value) in enumerate(
        (
            ("prompt", "private user prompt"),
            ("coordinates", {"lat": 34.0, "lng": -118.0}),
            ("zip", "90001"),
            ("profile", {"diagnosis": "private"}),
            ("device_id", "persistent-device-id"),
            ("ip_address", "203.0.113.10"),
            ("explanation", "free-form details"),
        ),
        start=1,
    ):
        response = client.post(
            REPORT_URL,
            {**VALID_REPORT, field: value},
            format="json",
            REMOTE_ADDR=f"192.0.2.{index}",
        )
        assert response.status_code == 400

    model = report_model()
    assert model is not None
    persisted_field_names = {field.name for field in model._meta.fields}
    assert persisted_field_names.isdisjoint(
        {
            "prompt",
            "coordinates",
            "zip",
            "profile",
            "device_id",
            "ip_address",
            "explanation",
        }
    )
    assert model.objects.count() == 0


def test_report_requires_android_and_caps_metadata_lengths(db):
    client = APIClient()
    invalid_payloads = (
        {**VALID_REPORT, "platform": "ios"},
        {**VALID_REPORT, "locale": "x" * 17},
        {**VALID_REPORT, "app_version": "x" * 33},
    )

    for index, payload in enumerate(invalid_payloads, start=1):
        response = client.post(
            REPORT_URL,
            payload,
            format="json",
            REMOTE_ADDR=f"192.0.2.{index}",
        )
        assert response.status_code == 400

    model = report_model()
    assert model is not None
    assert model.objects.count() == 0


def test_report_endpoint_is_anonymous_and_uses_the_dedicated_throttle():
    from llm.views import AssistantResponseReportView, ResponseReportThrottle

    assert AssistantResponseReportView.permission_classes[0].__name__ == "AllowAny"
    assert AssistantResponseReportView.throttle_classes == [ResponseReportThrottle]
    assert ResponseReportThrottle.rate == "5/hour"
