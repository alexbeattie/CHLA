"""
URL routes for LLM endpoints.
"""

from django.urls import path
from .views import (
    AskKiNDDView,
    EligibilityCheckView,
    SmartSearchView,
    LLMHealthView,
    LLMMonitorView,
    StreamingAskView,
    AgentAskView,
    LangGraphAgentAskView,
    StreamingLangGraphAgentAskView,
    LangGraphSupervisorAskView,
    StreamingAgentAskView,
    ImageAnalysisView,
    DocumentAnalysisView,
    AutismResearchView,
    AssistantResponseReportView,
)

urlpatterns = [
    path(
        "response-reports/",
        AssistantResponseReportView.as_view(),
        name="llm-response-report",
    ),
    path("ask/", AskKiNDDView.as_view(), name="llm-ask"),
    path("stream/", StreamingAskView.as_view(), name="llm-stream"),
    path("agent/", AgentAskView.as_view(), name="llm-agent"),
    path(
        "langgraph-agent/",
        LangGraphAgentAskView.as_view(),
        name="llm-langgraph-agent",
    ),
    path(
        "langgraph-agent-stream/",
        StreamingLangGraphAgentAskView.as_view(),
        name="llm-langgraph-agent-stream",
    ),
    path(
        "langgraph-supervisor/",
        LangGraphSupervisorAskView.as_view(),
        name="llm-langgraph-supervisor",
    ),
    path("agent-stream/", StreamingAgentAskView.as_view(), name="llm-agent-stream"),
    path("eligibility/", EligibilityCheckView.as_view(), name="llm-eligibility"),
    path("search/", SmartSearchView.as_view(), name="llm-search"),
    path("health/", LLMHealthView.as_view(), name="llm-health"),
    path("monitor/", LLMMonitorView.as_view(), name="llm-monitor"),
    path("autism-research/", AutismResearchView.as_view(), name="llm-autism-research"),
    path("analyze-image/", ImageAnalysisView.as_view(), name="llm-analyze-image"),
    path("analyze-document/", DocumentAnalysisView.as_view(), name="llm-analyze-document"),
]
