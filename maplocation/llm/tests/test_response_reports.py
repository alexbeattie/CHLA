import hashlib
from unittest.mock import patch

from django.apps import apps
from django.contrib import admin
from django.core.cache import cache
from django.core import signing
import pytest
from rest_framework.test import APIClient
from rest_framework.throttling import AnonRateThrottle, BaseThrottle


REPORT_URL = "/api/llm/response-reports/"
REPORTED_RESPONSE = "The assistant response that needs review."
FINGERPRINT_SALT = "llm.response-report.v1"
FINGERPRINT_MAX_AGE_SECONDS = 24 * 60 * 60

pytestmark = pytest.mark.django_db


def signed_fingerprint(response=REPORTED_RESPONSE, nonce="test-nonce"):
    return signing.dumps(
        {
            "answer_sha256": hashlib.sha256(response.encode("utf-8")).hexdigest(),
            "nonce": nonce,
        },
        salt=FINGERPRINT_SALT,
        compress=True,
    )


def valid_report(response=REPORTED_RESPONSE, fingerprint=None):
    return {
        "reason": "inaccurate_or_misleading",
        "reported_response": response,
        "locale": "en-US",
        "platform": "android",
        "app_version": "1.4.1",
        "response_fingerprint": fingerprint or signed_fingerprint(response),
    }


@pytest.fixture(autouse=True)
def clear_throttle_cache():
    cache.clear()
    yield
    cache.clear()


def report_model():
    return apps.all_models["llm"].get("assistantresponsereport")


def test_valid_anonymous_report_is_persisted_for_admin_review(db):
    payload = valid_report()
    response = APIClient().post(REPORT_URL, payload, format="json")

    assert response.status_code == 201
    assert set(response.json()) == {"id", "status"}
    assert response.json()["status"] == "received"

    model = report_model()
    assert model is not None
    report = model.objects.get()
    assert report.reason == "inaccurate_or_misleading"
    assert report.reported_response == payload["reported_response"]
    assert report.locale == "en-US"
    assert report.platform == "android"
    assert report.app_version == "1.4.1"
    assert len(report.response_fingerprint_digest) == 64
    assert report.response_fingerprint_digest != payload["response_fingerprint"]
    assert "response_fingerprint" not in {field.name for field in model._meta.fields}
    assert model in admin.site._registry


def test_report_rejects_unsupported_reason_empty_and_oversized_content(db):
    client = APIClient()
    invalid_payloads = (
        {
            key: value
            for key, value in valid_report().items()
            if key != "response_fingerprint"
        },
        {**valid_report(), "reason": "dislike"},
        {**valid_report(), "reported_response": "   "},
        valid_report(response="x" * 12001),
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


def test_report_accepts_response_at_12000_character_boundary(db):
    payload = valid_report(response="x" * 12000)

    response = APIClient().post(REPORT_URL, payload, format="json")

    assert response.status_code == 201
    assert report_model().objects.get().reported_response == payload["reported_response"]


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
            {
                **valid_report(
                    fingerprint=signed_fingerprint(nonce=f"sensitive-{index}")
                ),
                field: value,
            },
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
        {**valid_report(), "platform": "ios"},
        {**valid_report(), "locale": "x" * 17},
        {**valid_report(), "app_version": "x" * 33},
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


def test_report_endpoint_uses_identity_free_database_throttle():
    from llm.views import AssistantResponseReportView, ResponseFingerprintThrottle

    assert AssistantResponseReportView.permission_classes[0].__name__ == "AllowAny"
    assert AssistantResponseReportView.throttle_classes == [ResponseFingerprintThrottle]
    assert issubclass(ResponseFingerprintThrottle, BaseThrottle)
    assert not issubclass(ResponseFingerprintThrottle, AnonRateThrottle)
    assert not hasattr(ResponseFingerprintThrottle(), "cache")


def test_ask_issues_answer_bound_response_fingerprint(monkeypatch):
    monkeypatch.setattr(
        "llm.views.answer_query",
        lambda *args, **kwargs: {
            "answer": REPORTED_RESPONSE,
            "providers_referenced": [],
            "regional_center": None,
        },
    )

    response = APIClient().post(
        "/api/llm/ask/",
        {"query": "A question", "locale": "en"},
        format="json",
        REMOTE_ADDR="192.0.2.20",
    )

    assert response.status_code == 200
    token = response.json()["response_fingerprint"]
    assert isinstance(token, str)
    assert REPORTED_RESPONSE not in token
    payload = signing.loads(
        token,
        salt=FINGERPRINT_SALT,
        max_age=FINGERPRINT_MAX_AGE_SECONDS,
    )
    assert payload["answer_sha256"] == hashlib.sha256(
        REPORTED_RESPONSE.encode("utf-8")
    ).hexdigest()
    assert payload["nonce"]
    assert set(payload) == {"answer_sha256", "nonce"}


def test_report_rejects_token_answer_mismatch_and_expired_token(db):
    client = APIClient()
    mismatch = client.post(
        REPORT_URL,
        valid_report(
            response="A different assistant response.",
            fingerprint=signed_fingerprint(REPORTED_RESPONSE, nonce="mismatch"),
        ),
        format="json",
        REMOTE_ADDR="192.0.2.21",
    )
    assert mismatch.status_code == 400

    with patch("django.core.signing.time.time", return_value=1_000_000):
        expired_token = signed_fingerprint(nonce="expired")
    with patch(
        "django.core.signing.time.time",
        return_value=1_000_000 + FINGERPRINT_MAX_AGE_SECONDS + 1,
    ):
        expired = client.post(
            REPORT_URL,
            valid_report(fingerprint=expired_token),
            format="json",
            REMOTE_ADDR="192.0.2.22",
        )
    assert expired.status_code == 400
    assert report_model().objects.count() == 0


def test_duplicate_report_is_rejected_from_database_after_cache_clear(db):
    client = APIClient()
    payload = valid_report(fingerprint=signed_fingerprint(nonce="duplicate"))

    first = client.post(REPORT_URL, payload, format="json", REMOTE_ADDR="192.0.2.23")
    cache.clear()
    duplicate = client.post(
        REPORT_URL,
        payload,
        format="json",
        REMOTE_ADDR="198.51.100.90",
    )

    assert first.status_code == 201
    assert duplicate.status_code == 429
    assert report_model().objects.count() == 1


def test_saturated_ask_anon_throttle_does_not_block_reporting(db, monkeypatch):
    monkeypatch.setattr(
        "llm.views.answer_query",
        lambda *args, **kwargs: {
            "answer": REPORTED_RESPONSE,
            "providers_referenced": [],
            "regional_center": None,
        },
    )
    client = APIClient()
    first_ask = None
    for _ in range(30):
        response = client.post(
            "/api/llm/ask/",
            {"query": "A question", "locale": "en"},
            format="json",
            REMOTE_ADDR="192.0.2.24",
        )
        assert response.status_code == 200
        first_ask = first_ask or response.json()
    blocked_ask = client.post(
        "/api/llm/ask/",
        {"query": "A question", "locale": "en"},
        format="json",
        REMOTE_ADDR="192.0.2.24",
    )

    report = client.post(
        REPORT_URL,
        valid_report(fingerprint=first_ask["response_fingerprint"]),
        format="json",
        REMOTE_ADDR="192.0.2.24",
    )

    assert blocked_ask.status_code == 429
    assert report.status_code == 201


@pytest.mark.parametrize("payload", [[], "not-an-object", 17])
def test_report_rejects_non_object_json_with_controlled_400(payload):
    response = APIClient().post(REPORT_URL, payload, format="json")

    assert response.status_code == 400
