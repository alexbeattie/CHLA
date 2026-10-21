import logging

import boto3
from django.conf import settings

logger = logging.getLogger(__name__)

QUESTION_LABELS = {
    "diagnosis": "Child's diagnosis or diagnoses",
    "diagnosis_status": "Evaluation or diagnosis status",
    "ease_of_use": "How easy was it to navigate and use KiNDD?",
    "helpfulness": "How helpful was KiNDD in identifying relevant services?",
    "difficulty_before": "Before KiNDD, how difficult was it to find information?",
    "confidence_after": "After KiNDD, how confident do you feel finding services?",
    "needs_addressed": "Did KiNDD help identify services for your child's needs?",
    "experience_before": "Experience finding services before KiNDD",
    "unmet_needs": "Services or resources still needed",
    "improvements": "What to add or improve in KiNDD",
    "additional_comments": "Additional comments",
}


def format_survey_email(response):
    lines = [
        f"Submitted: {response.created_at:%Y-%m-%d %H:%M %Z}",
        f"Platform: {response.get_platform_display()}  Language: {response.locale}",
        "",
    ]
    for field, label in QUESTION_LABELS.items():
        if field in response.LIKERT_FIELDS:
            value = response.likert_label(field)
        elif field == "diagnosis_status":
            value = response.get_diagnosis_status_display()
        else:
            value = getattr(response, field).strip()
        lines.append(f"{label}\n  {value or '(no answer)'}\n")
    return "\n".join(lines)


def send_survey_notification(response):
    recipients = [
        address.strip()
        for address in settings.SURVEY_NOTIFY_EMAILS.split(",")
        if address.strip()
    ]
    if not recipients or not settings.SURVEY_FROM_EMAIL:
        logger.info("Survey notification skipped: email settings are not configured.")
        return
    try:
        boto3.client("sesv2", region_name=settings.SURVEY_SES_REGION).send_email(
            FromEmailAddress=settings.SURVEY_FROM_EMAIL,
            Destination={"ToAddresses": recipients},
            Content={
                "Simple": {
                    "Subject": {"Data": f"KiNDD survey response #{response.pk}"},
                    "Body": {"Text": {"Data": format_survey_email(response)}},
                }
            },
        )
    except Exception:
        logger.exception("Survey notification failed for response %s", response.pk)
