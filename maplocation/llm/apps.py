"""Django app configuration for LLM features."""

from django.apps import AppConfig


class LLMConfig(AppConfig):
    default_auto_field = "django.db.models.BigAutoField"
    name = "llm"

    def ready(self):
        from .observability import configure_langfuse_otel

        configure_langfuse_otel()
