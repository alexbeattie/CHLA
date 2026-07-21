from rest_framework import serializers

from .models import AssistantResponseReport


class AssistantResponseReportSerializer(serializers.ModelSerializer):
    class Meta:
        model = AssistantResponseReport
        fields = (
            "reason",
            "reported_response",
            "locale",
            "platform",
            "app_version",
        )
        extra_kwargs = {
            "reported_response": {"allow_blank": False, "trim_whitespace": True},
            "locale": {"allow_blank": False, "trim_whitespace": True},
            "app_version": {"allow_blank": False, "trim_whitespace": True},
        }

    def to_internal_value(self, data):
        unsupported_fields = set(data.keys()) - set(self.fields)
        if unsupported_fields:
            raise serializers.ValidationError(
                {"non_field_errors": ["Request contains unsupported fields."]}
            )
        return super().to_internal_value(data)
