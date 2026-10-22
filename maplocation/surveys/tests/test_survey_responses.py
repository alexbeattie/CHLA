import csv
import io
from unittest.mock import patch

import pytest
from django.contrib import admin
from django.contrib.auth.models import User
from django.test import RequestFactory, override_settings
from rest_framework.test import APIClient

from surveys.admin import SurveyResponseAdmin
from surveys.models import SurveyResponse

SURVEY_URL = "/api/surveys/responses/"

pytestmark = pytest.mark.django_db


def full_survey(**overrides):
    payload = {
        "diagnosis": "Autism spectrum disorder",
        "diagnosis_status": "",
        "ease_of_use": 4,
        "helpfulness": 5,
        "difficulty_before": 1,
        "confidence_after": 4,
        "needs_addressed": 3,
        "experience_before": "Called six places before anyone answered.",
        "unmet_needs": "Feeding therapy near Lancaster.",
        "improvements": "Show wait times.",
        "additional_comments": "",
        "locale": "en",
        "platform": "web",
        "app_version": "",
    }
    payload.update(overrides)
    return payload


def post(payload):
    return APIClient().post(SURVEY_URL, payload, format="json")


def test_anonymous_submission_is_saved():
    response = post(full_survey())

    assert response.status_code == 201
    saved = SurveyResponse.objects.get(pk=response.json()["id"])
    assert saved.helpfulness == 5
    assert saved.likert_label("difficulty_before") == "Very difficult"
    assert saved.unmet_needs == "Feeding therapy near Lancaster."


def test_partial_submission_with_one_answer_is_accepted():
    response = post({"needs_addressed": 2, "locale": "es", "platform": "android"})

    assert response.status_code == 201
    saved = SurveyResponse.objects.get()
    assert saved.ease_of_use is None
    assert saved.locale == "es"


def test_submission_with_no_answers_is_rejected():
    response = post({"diagnosis": "   ", "locale": "en", "platform": "ios"})

    assert response.status_code == 400
    assert not SurveyResponse.objects.exists()


@pytest.mark.parametrize("value", [0, 6])
def test_out_of_range_rating_is_rejected(value):
    assert post(full_survey(ease_of_use=value)).status_code == 400


def test_unknown_fields_are_rejected():
    response = post(full_survey(email="parent@example.com"))

    assert response.status_code == 400
    assert not SurveyResponse.objects.exists()


def test_unknown_platform_is_rejected():
    assert post(full_survey(platform="desktop")).status_code == 400


@override_settings(
    SURVEY_NOTIFY_EMAILS="researcher@example.org, second@example.org",
    SURVEY_FROM_EMAIL="survey@example.org",
)
def test_configured_notification_emails_the_full_response(
    django_capture_on_commit_callbacks,
):
    with patch("surveys.notifications.boto3.client") as client:
        with django_capture_on_commit_callbacks(execute=True):
            response = post(full_survey())

    assert response.status_code == 201
    kwargs = client.return_value.send_email.call_args.kwargs
    assert kwargs["FromEmailAddress"] == "survey@example.org"
    assert kwargs["Destination"]["ToAddresses"] == [
        "researcher@example.org",
        "second@example.org",
    ]
    body = kwargs["Content"]["Simple"]["Body"]["Text"]["Data"]
    assert "Extremely helpful" in body
    assert "Feeding therapy near Lancaster." in body
    assert "Additional comments\n  (no answer)" in body


@override_settings(SURVEY_NOTIFY_EMAILS="", SURVEY_FROM_EMAIL="")
def test_unconfigured_notification_does_not_call_ses(django_capture_on_commit_callbacks):
    with patch("surveys.notifications.boto3.client") as client:
        with django_capture_on_commit_callbacks(execute=True):
            assert post(full_survey()).status_code == 201

    client.assert_not_called()


@override_settings(
    SURVEY_NOTIFY_EMAILS="researcher@example.org",
    SURVEY_FROM_EMAIL="survey@example.org",
)
def test_ses_failure_still_saves_the_response(django_capture_on_commit_callbacks):
    with patch("surveys.notifications.boto3.client") as client:
        client.return_value.send_email.side_effect = RuntimeError("sandbox")
        with django_capture_on_commit_callbacks(execute=True):
            response = post(full_survey())

    assert response.status_code == 201
    assert SurveyResponse.objects.count() == 1


@override_settings(SURVEY_GLOBAL_RATE_LIMIT=2)
def test_global_rate_limit_returns_429():
    statuses = [post(full_survey()).status_code for _ in range(3)]

    assert statuses == [201, 201, 429]
    assert SurveyResponse.objects.count() == 2


def test_admin_csv_download_includes_labels():
    post(full_survey())
    post(full_survey(helpfulness=None, diagnosis_status="under_evaluation"))
    request = RequestFactory().post("/client-portal/surveys/surveyresponse/")
    request.user = User(is_staff=True, is_superuser=True)
    model_admin = SurveyResponseAdmin(SurveyResponse, admin.site)

    response = model_admin.download_csv(request, SurveyResponse.objects.all())

    rows = list(csv.reader(io.StringIO(response.content.decode("utf-8-sig"))))
    header, first, second = rows
    helpfulness = header.index(
        "How helpful was KiNDD in identifying relevant services?"
    )
    assert first[helpfulness] == "5 - Extremely helpful"
    assert second[helpfulness] == ""
    assert (
        second[header.index("Evaluation or diagnosis status")]
        == "Currently undergoing an evaluation"
    )
