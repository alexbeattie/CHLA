from collections.abc import Mapping

from rest_framework import serializers

from .models import AssistantResponseReport
from .response_fingerprints import (
    InvalidResponseFingerprint,
    validate_response_fingerprint,
)


class AssistantResponseReportSerializer(serializers.ModelSerializer):
    response_fingerprint = serializers.CharField(
        max_length=512,
        trim_whitespace=False,
        write_only=True,
    )

    class Meta:
        model = AssistantResponseReport
        fields = (
            "reason",
            "reported_response",
            "locale",
            "platform",
            "app_version",
            "response_fingerprint",
        )
        extra_kwargs = {
            "reported_response": {"allow_blank": False, "trim_whitespace": False},
            "locale": {"allow_blank": False, "trim_whitespace": True},
            "app_version": {"allow_blank": False, "trim_whitespace": True},
        }

    def to_internal_value(self, data):
        if not isinstance(data, Mapping):
            raise serializers.ValidationError(
                {"non_field_errors": ["Expected a JSON object."]}
            )
        unsupported_fields = set(data.keys()) - set(self.fields)
        if unsupported_fields:
            raise serializers.ValidationError(
                {"non_field_errors": ["Request contains unsupported fields."]}
            )
        return super().to_internal_value(data)

    def validate_reported_response(self, value):
        if not value.strip():
            raise serializers.ValidationError("This field may not be blank.")
        return value

    def validate(self, attrs):
        token = attrs.pop("response_fingerprint")
        try:
            attrs["response_fingerprint_digest"] = validate_response_fingerprint(
                token,
                attrs["reported_response"],
            )
        except InvalidResponseFingerprint:
            raise serializers.ValidationError(
                {"response_fingerprint": ["Invalid or expired response fingerprint."]}
            )
        return attrs
