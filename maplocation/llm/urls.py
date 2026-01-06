"""
URL routes for LLM endpoints.
"""

from django.urls import path
from .views import (
    AskKiNDDView,
    EligibilityCheckView,
    SmartSearchView,
    LLMHealthView,
    StreamingAskView,
    AgentAskView,
)

urlpatterns = [
    path("ask/", AskKiNDDView.as_view(), name="llm-ask"),
    path("stream/", StreamingAskView.as_view(), name="llm-stream"),  # SSE streaming
    path("agent/", AgentAskView.as_view(), name="llm-agent"),  # Strands agent
    path("eligibility/", EligibilityCheckView.as_view(), name="llm-eligibility"),
    path("search/", SmartSearchView.as_view(), name="llm-search"),
    path("health/", LLMHealthView.as_view(), name="llm-health"),
]
