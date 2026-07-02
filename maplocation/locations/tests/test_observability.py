"""Tests for Langfuse observability helpers."""

import os
from contextlib import contextmanager


class _FakeObservation:
    def __init__(self):
        self.updates = []

    def update(self, **kwargs):
        self.updates.append(kwargs)


class _FakeObservationContext:
    def __init__(self, observation):
        self.observation = observation

    def __enter__(self):
        return self.observation

    def __exit__(self, exc_type, exc, tb):
        return False


class _FakeLangfuseClient:
    def __init__(self):
        self.calls = []
        self.observations = []

    def start_as_current_observation(self, **kwargs):
        observation = _FakeObservation()
        self.calls.append(kwargs)
        self.observations.append(observation)
        return _FakeObservationContext(observation)


@contextmanager
def _fake_propagate_attributes(**kwargs):
    yield


def test_configure_langfuse_otel_refreshes_exporter_environment(monkeypatch):
    from llm.observability import configure_langfuse_otel

    monkeypatch.setenv("LANGFUSE_PUBLIC_KEY", "public")
    monkeypatch.setenv("LANGFUSE_SECRET_KEY", "secret")
    monkeypatch.setenv("LANGFUSE_HOST", "https://us.cloud.langfuse.com/")
    monkeypatch.setenv("LANGFUSE_BASE_URL", "https://old.example")
    monkeypatch.setenv("OTEL_EXPORTER_OTLP_ENDPOINT", "https://old.example/otel")
    monkeypatch.setenv("OTEL_EXPORTER_OTLP_HEADERS", "Authorization=Basic old")

    assert configure_langfuse_otel() is True

    assert os.environ["OTEL_EXPORTER_OTLP_ENDPOINT"] == (
        "https://us.cloud.langfuse.com/api/public/otel"
    )
    assert os.environ["LANGFUSE_BASE_URL"] == "https://us.cloud.langfuse.com"
    assert "old" not in os.environ["OTEL_EXPORTER_OTLP_HEADERS"]
    assert "x-langfuse-ingestion-version=4" in os.environ["OTEL_EXPORTER_OTLP_HEADERS"]


def test_bedrock_call_monitor_creates_langfuse_generation(monkeypatch):
    from llm import observability

    fake_client = _FakeLangfuseClient()
    monkeypatch.setenv("LANGFUSE_PUBLIC_KEY", "public")
    monkeypatch.setenv("LANGFUSE_SECRET_KEY", "secret")
    monkeypatch.setattr(observability, "get_client", lambda: fake_client)

    with observability.bedrock_call_monitor(
        operation="chat_completion",
        model_id="anthropic.test-model",
        prompt="private prompt",
    ) as state:
        state["usage"] = {"input_tokens": 12, "output_tokens": 7}
        state["output_text"] = "model answer"

    assert fake_client.calls[0]["as_type"] == "generation"
    assert fake_client.calls[0]["name"] == "bedrock.chat_completion"
    assert fake_client.calls[0]["model"] == "anthropic.test-model"
    assert fake_client.calls[0]["input"]["prompt_chars"] == len("private prompt")
    assert fake_client.calls[0]["input"]["prompt_fingerprint"]
    assert fake_client.observations[0].updates[-1]["usage_details"] == {
        "input": 12,
        "output": 7,
        "total": 19,
    }
    assert fake_client.observations[0].updates[-1]["output"] == "model answer"

