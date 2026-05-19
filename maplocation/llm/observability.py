"""
Langfuse observability helpers for KiNDD agent requests.

The Strands SDK emits OpenTelemetry spans when installed with OTEL support.
This module configures the Langfuse OTLP endpoint at Django startup and wraps
agent calls with trace attributes that make CHLA queries searchable.
"""

import base64
import hashlib
import logging
import os
import time
from collections import Counter, deque
from contextlib import contextmanager
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from typing import Any, Iterator, Optional

logger = logging.getLogger(__name__)

DEFAULT_LANGFUSE_HOST = "https://us.cloud.langfuse.com"
MAX_RECENT_LLM_CALLS = 100


@dataclass
class LLMCallRecord:
    """Sanitized local telemetry for a single LLM/Bedrock call."""

    timestamp: str
    operation: str
    model_id: str
    status: str
    latency_ms: int
    prompt_chars: int = 0
    prompt_fingerprint: str = ""
    output_chars: int = 0
    input_tokens: int | None = None
    output_tokens: int | None = None
    error_type: str | None = None


_recent_llm_calls: deque[LLMCallRecord] = deque(maxlen=MAX_RECENT_LLM_CALLS)


def _langfuse_credentials() -> tuple[Optional[str], Optional[str], str]:
    public_key = os.environ.get("LANGFUSE_PUBLIC_KEY")
    secret_key = os.environ.get("LANGFUSE_SECRET_KEY")
    host = (
        os.environ.get("LANGFUSE_HOST")
        or os.environ.get("LANGFUSE_BASE_URL")
        or DEFAULT_LANGFUSE_HOST
    )
    return public_key, secret_key, host.rstrip("/")


def configure_langfuse_otel() -> bool:
    """Configure OTEL environment variables for Langfuse ingestion."""
    public_key, secret_key, host = _langfuse_credentials()
    if not (public_key and secret_key):
        return False

    auth = base64.b64encode(f"{public_key}:{secret_key}".encode()).decode()
    os.environ.setdefault("LANGFUSE_BASE_URL", host)
    os.environ.setdefault("OTEL_EXPORTER_OTLP_ENDPOINT", f"{host}/api/public/otel")
    os.environ.setdefault(
        "OTEL_EXPORTER_OTLP_HEADERS",
        f"Authorization=Basic {auth},x-langfuse-ingestion-version=4",
    )
    return True


def langfuse_is_configured() -> bool:
    """Return whether Langfuse credentials are present."""
    public_key, secret_key, _host = _langfuse_credentials()
    return bool(public_key and secret_key)


def prompt_fingerprint(value: str) -> str:
    """Create a stable, non-reversible fingerprint for prompt correlation."""
    if not value:
        return ""
    return hashlib.sha256(value.encode("utf-8")).hexdigest()[:12]


def record_llm_call(
    *,
    operation: str,
    model_id: str,
    status: str,
    latency_ms: int,
    prompt: str = "",
    output_text: str = "",
    usage: dict[str, Any] | None = None,
    error: BaseException | None = None,
) -> None:
    """Record sanitized local telemetry for monitor/debug endpoints."""
    usage = usage or {}
    record = LLMCallRecord(
        timestamp=datetime.now(timezone.utc).isoformat(),
        operation=operation,
        model_id=model_id,
        status=status,
        latency_ms=latency_ms,
        prompt_chars=len(prompt or ""),
        prompt_fingerprint=prompt_fingerprint(prompt or ""),
        output_chars=len(output_text or ""),
        input_tokens=_usage_int(usage, "input_tokens"),
        output_tokens=_usage_int(usage, "output_tokens"),
        error_type=type(error).__name__ if error else None,
    )
    _recent_llm_calls.append(record)
    logger.info(
        "LLM call",
        extra={
            "operation": operation,
            "model_id": model_id,
            "status": status,
            "latency_ms": latency_ms,
            "prompt_fingerprint": record.prompt_fingerprint,
            "input_tokens": record.input_tokens,
            "output_tokens": record.output_tokens,
            "error_type": record.error_type,
        },
    )


@contextmanager
def bedrock_call_monitor(
    *,
    operation: str,
    model_id: str,
    prompt: str = "",
) -> Iterator[dict[str, Any]]:
    """Time a Bedrock call and record it without storing raw prompt text."""
    started_at = time.perf_counter()
    state: dict[str, Any] = {"usage": {}, "output_text": ""}
    try:
        yield state
    except Exception as exc:
        record_llm_call(
            operation=operation,
            model_id=model_id,
            status="error",
            latency_ms=_elapsed_ms(started_at),
            prompt=prompt,
            usage=state.get("usage"),
            output_text=state.get("output_text", ""),
            error=exc,
        )
        raise
    else:
        record_llm_call(
            operation=operation,
            model_id=model_id,
            status="ok",
            latency_ms=_elapsed_ms(started_at),
            prompt=prompt,
            usage=state.get("usage"),
            output_text=state.get("output_text", ""),
        )


def llm_monitor_snapshot() -> dict[str, Any]:
    """Return aggregate LLM metrics and recent calls for a lightweight monitor."""
    records = list(_recent_llm_calls)
    status_counts = Counter(record.status for record in records)
    operation_counts = Counter(record.operation for record in records)
    error_counts = Counter(record.error_type for record in records if record.error_type)
    latencies = [record.latency_ms for record in records]
    input_tokens = sum(record.input_tokens or 0 for record in records)
    output_tokens = sum(record.output_tokens or 0 for record in records)

    return {
        "status": "ok",
        "window_size": MAX_RECENT_LLM_CALLS,
        "total_recorded": len(records),
        "langfuse_configured": langfuse_is_configured(),
        "summary": {
            "status_counts": dict(status_counts),
            "operation_counts": dict(operation_counts),
            "error_counts": dict(error_counts),
            "latency_ms": _latency_summary(latencies),
            "input_tokens": input_tokens,
            "output_tokens": output_tokens,
            "total_tokens": input_tokens + output_tokens,
        },
        "recent": [asdict(record) for record in reversed(records[-25:])],
    }


def _elapsed_ms(started_at: float) -> int:
    return int((time.perf_counter() - started_at) * 1000)


def _usage_int(usage: dict[str, Any], key: str) -> int | None:
    value = usage.get(key)
    return value if isinstance(value, int) else None


def _latency_summary(latencies: list[int]) -> dict[str, int | None]:
    if not latencies:
        return {"min": None, "avg": None, "max": None, "p95": None}
    sorted_latencies = sorted(latencies)
    p95_index = min(int(len(sorted_latencies) * 0.95), len(sorted_latencies) - 1)
    return {
        "min": sorted_latencies[0],
        "avg": int(sum(sorted_latencies) / len(sorted_latencies)),
        "max": sorted_latencies[-1],
        "p95": sorted_latencies[p95_index],
    }


@contextmanager
def agent_observation(
    *,
    query: str,
    locale: str,
    user_id: Optional[str] = None,
    session_id: Optional[str] = None,
    feature: str = "chla",
    streaming: bool = False,
    has_conversation_history: bool = False,
) -> Iterator[object | None]:
    """Start a Langfuse observation when configured, otherwise no-op."""
    if not langfuse_is_configured():
        yield None
        return

    try:
        from langfuse import get_client, propagate_attributes
    except ImportError:
        logger.warning("Langfuse credentials are set but langfuse is not installed.")
        yield None
        return

    metadata = {
        "feature": feature,
        "locale": locale,
        "streaming": streaming,
        "has_conversation_history": has_conversation_history,
    }
    tags = [feature, "agent", "streaming" if streaming else "sync"]
    trace_attributes = {
        "tags": tags,
        "metadata": metadata,
    }
    if user_id:
        trace_attributes["user_id"] = user_id
    if session_id:
        trace_attributes["session_id"] = session_id

    langfuse = get_client()
    with langfuse.start_as_current_observation(
        as_type="span",
        name=f"kindd.{feature}.agent",
        input={"query": query},
    ) as observation:
        with propagate_attributes(**trace_attributes):
            yield observation
