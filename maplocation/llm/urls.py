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
    StreamingAgentAskView,
    ImageAnalysisView,
    DocumentAnalysisView,
)

urlpatterns = [
    path("ask/", AskKiNDDView.as_view(), name="llm-ask"),
    path("stream/", StreamingAskView.as_view(), name="llm-stream"),
    path("agent/", AgentAskView.as_view(), name="llm-agent"),
    path("agent-stream/", StreamingAgentAskView.as_view(), name="llm-agent-stream"),
    path("eligibility/", EligibilityCheckView.as_view(), name="llm-eligibility"),
    path("search/", SmartSearchView.as_view(), name="llm-search"),
    path("health/", LLMHealthView.as_view(), name="llm-health"),
    path("analyze-image/", ImageAnalysisView.as_view(), name="llm-analyze-image"),
    path("analyze-document/", DocumentAnalysisView.as_view(), name="llm-analyze-document"),
]
