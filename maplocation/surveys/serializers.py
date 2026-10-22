from collections.abc import Mapping

from rest_framework import serializers

from .models import SurveyResponse


class SurveyResponseSerializer(serializers.ModelSerializer):
    class Meta:
        model = SurveyResponse
        fields = (
            "diagnosis",
            "diagnosis_status",
            *SurveyResponse.LIKERT_FIELDS,
            "experience_before",
            "unmet_needs",
            "improvements",
            "additional_comments",
            "locale",
            "platform",
            "app_version",
        )
        extra_kwargs = {
            "locale": {"allow_blank": False},
        }

    def to_internal_value(self, data):
        if not isinstance(data, Mapping):
            raise serializers.ValidationError(
                {"non_field_errors": ["Expected a JSON object."]}
            )
        if set(data.keys()) - set(self.fields):
            raise serializers.ValidationError(
                {"non_field_errors": ["Request contains unsupported fields."]}
            )
        return super().to_internal_value(data)

    def validate(self, attrs):
        answered = any(
            attrs.get(name) is not None for name in SurveyResponse.LIKERT_FIELDS
        ) or any(attrs.get(name, "").strip() for name in SurveyResponse.TEXT_FIELDS)
        if not answered and not attrs.get("diagnosis_status"):
            raise serializers.ValidationError(
                {"non_field_errors": ["Answer at least one question."]}
            )
        return attrs
