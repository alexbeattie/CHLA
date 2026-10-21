from django.core.validators import MaxValueValidator, MinValueValidator
from django.db import models

LIKERT_VALIDATORS = [MinValueValidator(1), MaxValueValidator(5)]

# Index 0 is the label for a stored value of 1; 5 is always the most positive answer.
EASE_LABELS = (
    "Very difficult",
    "Somewhat difficult",
    "Neither easy nor difficult",
    "Somewhat easy",
    "Very easy",
)
HELPFULNESS_LABELS = (
    "Not at all helpful",
    "Slightly helpful",
    "Moderately helpful",
    "Very helpful",
    "Extremely helpful",
)
CONFIDENCE_LABELS = (
    "Not at all confident",
    "Slightly confident",
    "Moderately confident",
    "Very confident",
    "Extremely confident",
)
EXTENT_LABELS = (
    "Not at all",
    "A little",
    "A moderate amount",
    "A lot",
    "Completely",
)


def likert_field():
    return models.PositiveSmallIntegerField(
        null=True, blank=True, validators=LIKERT_VALIDATORS
    )


class SurveyResponse(models.Model):
    """One anonymous family survey submission. Holds no contact or location data."""

    class DiagnosisStatus(models.TextChoices):
        UNDER_EVALUATION = "under_evaluation", "Currently undergoing an evaluation"
        NOT_YET_DIAGNOSED = "not_yet_diagnosed", "Has not yet received a diagnosis"

    class Platform(models.TextChoices):
        WEB = "web", "Web"
        IOS = "ios", "iPhone"
        ANDROID = "android", "Android"

    diagnosis = models.TextField(blank=True, max_length=1000)
    diagnosis_status = models.CharField(
        max_length=32, blank=True, choices=DiagnosisStatus.choices
    )
    ease_of_use = likert_field()
    helpfulness = likert_field()
    difficulty_before = likert_field()
    confidence_after = likert_field()
    needs_addressed = likert_field()
    experience_before = models.TextField(blank=True, max_length=4000)
    unmet_needs = models.TextField(blank=True, max_length=4000)
    improvements = models.TextField(blank=True, max_length=4000)
    additional_comments = models.TextField(blank=True, max_length=4000)
    locale = models.CharField(max_length=16)
    platform = models.CharField(max_length=16, choices=Platform.choices)
    app_version = models.CharField(max_length=32, blank=True)
    created_at = models.DateTimeField(auto_now_add=True, db_index=True)

    LIKERT_FIELDS = {
        "ease_of_use": EASE_LABELS,
        "helpfulness": HELPFULNESS_LABELS,
        "difficulty_before": EASE_LABELS,
        "confidence_after": CONFIDENCE_LABELS,
        "needs_addressed": EXTENT_LABELS,
    }
    TEXT_FIELDS = (
        "diagnosis",
        "experience_before",
        "unmet_needs",
        "improvements",
        "additional_comments",
    )

    class Meta:
        ordering = ("-created_at",)

    def __str__(self):
        return f"Survey response {self.pk} ({self.created_at:%Y-%m-%d %H:%M})"

    def likert_label(self, field_name):
        value = getattr(self, field_name)
        if value is None:
            return ""
        return self.LIKERT_FIELDS[field_name][value - 1]


class SurveyThrottleWindow(models.Model):
    """One global request count per fixed window; contains no client identity."""

    window_start = models.DateTimeField(primary_key=True)
    request_count = models.PositiveIntegerField(default=0)
