"""LangGraph sidecar agent for KiNDD chat.

This module intentionally mirrors the existing Strands agent in `llm.agent`,
but uses LangGraph's explicit graph primitives so the control flow is easier to
study:

- State: the shared conversation object passed between graph nodes.
- Nodes: Python callables that read state and return state updates.
- Edges: the routes that decide which node runs next.
- ToolNode: LangGraph's equivalent of letting an agent execute registered tools.
"""

import json
import logging
from typing import Annotated, Any, Optional, TypedDict

from django.conf import settings
from langchain_aws import ChatBedrockConverse
from langchain_core.messages import (
    AIMessage,
    BaseMessage,
    HumanMessage,
    SystemMessage,
    ToolMessage,
)
from langchain_core.runnables import RunnableConfig
from langchain_core.tools import tool
from langgraph.graph import END, START, StateGraph
from langgraph.graph.message import add_messages
from langgraph.prebuilt import ToolNode, tools_condition

from locations.models import ProviderV2, RegionalCenter

from .agent import (
    CLINICAL_ALLOWLIST,
    get_agent_system_prompt_for_locale,
    _run_tavily_search,
)
from .autism_research import AutismResearchError, ask_autism_research
from .bedrock import CHAT_MODEL_ID
from .query import keyword_search, semantic_search

logger = logging.getLogger(__name__)


class KiNDDGraphState(TypedDict):
    """State is the object every LangGraph node receives and updates."""

    messages: Annotated[list[BaseMessage], add_messages]


class KiNDDSupervisorState(TypedDict):
    """State for the first multi-agent supervisor graph."""

    messages: Annotated[list[BaseMessage], add_messages]
    specialist: str


def _json_response(payload: dict[str, Any]) -> str:
    """Return compact JSON because tool outputs become model-readable messages."""
    return json.dumps(payload, default=str)


@tool
def search_providers(
    query: str,
    therapy_type: Optional[str] = None,
    zip_code: Optional[str] = None,
    max_results: int = 10,
) -> str:
    """Search KiNDD provider records for therapy providers and services."""
    try:
        providers = semantic_search(query, limit=max_results)
    except Exception:
        logger.exception("LangGraph provider semantic search failed; using keywords")
        providers = keyword_search(query, limit=max_results)

    if therapy_type:
        providers = [
            provider
            for provider in providers
            if therapy_type.lower() in str(provider.therapy_types).lower()
        ]

    if zip_code:
        regional_center = RegionalCenter.find_by_zip_code(zip_code)
        if regional_center:
            providers = [
                provider
                for provider in providers
                if regional_center.regional_center.lower()
                in str(_provider_regional_center(provider)).lower()
            ]

    results = [_provider_summary(provider) for provider in providers[:max_results]]
    return _json_response({"count": len(results), "providers": results})


@tool
def get_regional_center(zip_code: str) -> str:
    """Find the Regional Center that serves a ZIP code."""
    regional_center = RegionalCenter.find_by_zip_code(zip_code)
    if not regional_center:
        return _json_response(
            {
                "found": False,
                "message": f"No Regional Center found for ZIP code {zip_code}.",
            }
        )

    return _json_response(
        {
            "found": True,
            "name": regional_center.regional_center,
            "phone": getattr(regional_center, "telephone", None),
            "website": getattr(regional_center, "website", None),
            "zip_code": zip_code,
        }
    )


@tool
def get_provider_details(provider_name: str) -> str:
    """Get full details about a specific provider by name."""
    providers = ProviderV2.objects.filter(name__icontains=provider_name)[:5]
    if not providers:
        return _json_response(
            {
                "found": False,
                "message": f"No provider found with name containing '{provider_name}'",
            }
        )

    return _json_response(
        {
            "count": len(providers),
            "providers": [
                {
                    "id": str(provider.id),
                    "name": provider.name,
                    "therapy_types": provider.therapy_types or [],
                    "diagnoses_treated": provider.diagnoses_treated,
                    "age_groups": provider.age_groups or [],
                    "address": provider.address,
                    "phone": provider.phone,
                    "email": provider.email,
                    "website": provider.website,
                    "description": provider.description,
                    "regional_center": _provider_regional_center(provider),
                    "insurance_accepted": getattr(
                        provider,
                        "insurance_networks",
                        None,
                    ),
                }
                for provider in providers
            ],
        }
    )


@tool
def find_provider_location(provider_name: str) -> str:
    """Find a provider's map location by name."""
    providers = ProviderV2.objects.filter(name__icontains=provider_name).order_by("name")[
        :5
    ]
    if not providers:
        return _json_response(
            {
                "found": False,
                "message": f"No provider found with name containing '{provider_name}'",
            }
        )

    results = [
        {
            "id": str(provider.id),
            "name": provider.name,
            "address": provider.address,
            "phone": provider.phone,
            "latitude": float(provider.latitude),
            "longitude": float(provider.longitude),
            "regional_center": _provider_regional_center(provider),
            "therapy_types": provider.therapy_types or [],
        }
        for provider in providers
    ]
    best_match = results[0]
    return _json_response(
        {
            "found": True,
            "provider": best_match,
            "matches": results,
            "map_action": {
                "type": "show_provider_on_map",
                "provider_id": best_match["id"],
                "provider_name": best_match["name"],
            },
        }
    )


@tool
def check_eligibility(
    age_years: int,
    diagnosis: str,
    insurance: Optional[str] = None,
    zip_code: Optional[str] = None,
) -> str:
    """Explain likely service eligibility and practical next steps."""
    if age_years < 3:
        program = "Early Start"
        program_description = (
            "California's Early Start program serves children 0-3 with "
            "developmental delays or established risk conditions. Services "
            "include developmental evaluations, therapy, and family support."
        )
    elif age_years < 22:
        program = "Lanterman Act Services"
        program_description = (
            "The Lanterman Developmental Disabilities Services Act provides "
            "services to Californians with developmental disabilities including "
            "intellectual disability, cerebral palsy, epilepsy, autism, and "
            "conditions requiring similar treatment."
        )
    else:
        program = "Adult Services"
        program_description = (
            "Adults with developmental disabilities may qualify for ongoing "
            "Regional Center services, supported living, day programs, and "
            "employment support."
        )

    diagnosis_lower = diagnosis.lower()
    likely_eligible: bool | str = "Evaluation needed"
    diagnosis_notes = (
        "Eligibility depends on evaluation findings. Contact your Regional "
        "Center to request an intake assessment."
    )

    if any(term in diagnosis_lower for term in ("autism", "asd", "autistic")):
        likely_eligible = True
        diagnosis_notes = (
            "Autism Spectrum Disorder is a qualifying condition under the "
            "Lanterman Act. SB 946 also requires commercial insurance to cover "
            "ABA therapy for autism."
        )
    elif any(term in diagnosis_lower for term in ("delay", "developmental", "global")):
        likely_eligible = True
        diagnosis_notes = (
            "Developmental delays may qualify for Early Start (under 3) or "
            "Regional Center services if delays are significant. Evaluation is "
            "needed."
        )

    payload: dict[str, Any] = {
        "age_years": age_years,
        "diagnosis": diagnosis,
        "insurance": insurance,
        "program": program,
        "program_description": program_description,
        "likely_eligible": likely_eligible,
        "diagnosis_notes": diagnosis_notes,
        "next_steps": [
            "Call your Regional Center to request an intake assessment",
            "Gather any existing medical/developmental evaluations",
            "Contact your insurance about covered therapy services",
            "Consider private evaluations if wait times are long",
        ],
    }

    if insurance:
        insurance_lower = insurance.lower()
        if "medi-cal" in insurance_lower or "medicaid" in insurance_lower:
            payload["insurance_notes"] = (
                "Medi-Cal covers many therapy services. Regional Center can "
                "provide services not covered by insurance."
            )
        elif "kaiser" in insurance_lower:
            payload["insurance_notes"] = (
                "Kaiser has an in-network ABA provider network. Contact Kaiser "
                "Behavioral Health for referrals. Wait times can be significant."
            )
        else:
            payload["insurance_notes"] = (
                "Commercial plans in California must cover autism diagnosis and "
                "ABA therapy under SB 946. Contact your plan for network providers."
            )

    if zip_code:
        regional_center = RegionalCenter.find_by_zip_code(zip_code)
        if regional_center:
            payload["regional_center"] = regional_center.regional_center

    return _json_response(payload)


@tool
def list_therapy_types() -> str:
    """List therapy types available in the KiNDD database."""
    therapy_info = [
        {
            "name": "ABA therapy",
            "full_name": "Applied Behavior Analysis",
            "description": "Evidence-based therapy for autism that focuses on improving social behaviors, communication, and learning skills through positive reinforcement.",
            "typical_ages": "2-21 years",
        },
        {
            "name": "Speech therapy",
            "full_name": "Speech-Language Pathology",
            "description": "Addresses communication disorders including speech articulation, language development, and social communication.",
            "typical_ages": "All ages",
        },
        {
            "name": "Occupational therapy",
            "full_name": "Occupational Therapy (OT)",
            "description": "Helps develop fine motor skills, sensory processing, daily living skills, and self-regulation.",
            "typical_ages": "All ages",
        },
        {
            "name": "Physical therapy",
            "full_name": "Physical Therapy (PT)",
            "description": "Addresses gross motor development, balance, coordination, and mobility challenges.",
            "typical_ages": "All ages",
        },
        {
            "name": "Feeding therapy",
            "full_name": "Pediatric Feeding Therapy",
            "description": "Helps children with food aversions, swallowing difficulties, or limited diets expand their eating.",
            "typical_ages": "Infants through adolescents",
        },
        {
            "name": "Parent training",
            "full_name": "Parent-Child Interaction Therapy / Behavior Management",
            "description": "Teaches parents strategies to support their child's development and manage challenging behaviors.",
            "typical_ages": "Parents of children 2-12",
        },
    ]
    return _json_response({"therapy_types": therapy_info})


@tool
def clinical_search(query: str, max_results: int = 5) -> str:
    """Search authoritative pediatric clinical sources."""
    return _run_tavily_search(
        query=query,
        max_results=max_results,
        search_type="clinical_search",
        include_domains=CLINICAL_ALLOWLIST,
    )


@tool
def autism_research(
    question: str,
    evidence_type: Optional[str] = None,
    max_results: int = 5,
) -> str:
    """Search the Autism Research RAG database."""
    evidence_types = [evidence_type] if evidence_type else None
    access_classes = (
        ["controlled_metadata"]
        if evidence_type == "dataset_metadata"
        else ["public_open", "public_metadata_only"]
    )

    try:
        result = ask_autism_research(
            question,
            top_k=min(max(max_results, 1), 10),
            evidence_types=evidence_types,
            access_classes=access_classes,
        )
        return _json_response(
            {
                "answer": result.get("answer", ""),
                "citations": result.get("citations", []),
                "model": result.get("model"),
            }
        )
    except AutismResearchError as exc:
        return _json_response(
            {
                "error": "autism_research_unavailable",
                "message": str(exc),
                "answer": "",
                "citations": [],
            }
        )


@tool
def web_search(query: str, max_results: int = 5) -> str:
    """Search the web for current facts and non-clinical information."""
    return _run_tavily_search(
        query=query,
        max_results=max_results,
        search_type="web_search",
    )


LANGGRAPH_TOOLS = [
    search_providers,
    get_regional_center,
    get_provider_details,
    find_provider_location,
    check_eligibility,
    list_therapy_types,
    clinical_search,
    autism_research,
    web_search,
]


def create_bedrock_langchain_model():
    """Create the LangChain chat model that LangGraph will call.

    In Strands, `BedrockModel` is passed directly to `Agent`. In LangGraph, the
    model is a LangChain chat model, then tools are bound to it before the graph
    invokes it.
    """
    return ChatBedrockConverse(
        model=CHAT_MODEL_ID,
        region_name=getattr(settings, "AWS_REGION", "us-west-2"),
        temperature=0.3,
        max_tokens=1500,
    )


def build_langgraph_messages(
    user_message: str,
    user_context: Optional[dict[str, Any]] = None,
    conversation_history: Optional[list[dict[str, str]]] = None,
    locale: str = "en",
) -> list[BaseMessage]:
    """Convert API input into LangChain messages for the graph.

    Strands accepts one prompt string plus optional internal memory. LangGraph
    works best when the conversation is explicit: system, previous messages,
    then the current human turn.
    """
    messages: list[BaseMessage] = [
        SystemMessage(content=get_agent_system_prompt_for_locale(locale))
    ]

    for history_item in conversation_history or []:
        role = history_item.get("role")
        content = history_item.get("content")
        if not content:
            continue
        if role == "assistant":
            messages.append(AIMessage(content=content))
        elif role == "system":
            # Never promote client-supplied history to a system message: these
            # endpoints are anonymous, and a real SystemMessage here would let
            # callers override the KiNDD safety prompt.
            continue
        else:
            messages.append(HumanMessage(content=content))

    messages.append(HumanMessage(content=_message_with_context(user_message, user_context)))
    return messages


def create_kindd_langgraph(model=None):
    """Compile the LangGraph agent.

    The graph is deliberately small:

    START -> agent -> either END or tools -> agent -> ...

    That loop is the LangGraph version of a ReAct/tool-using agent. The model
    decides whether to answer directly or request a tool call. ToolNode executes
    the requested tool and appends a ToolMessage, then the agent sees the result.
    """
    chat_model = model or create_bedrock_langchain_model()
    model_with_tools = chat_model.bind_tools(LANGGRAPH_TOOLS)

    def call_model(state: KiNDDGraphState, config: RunnableConfig):
        response = model_with_tools.invoke(state["messages"], config=config)
        return {"messages": [response]}

    graph = StateGraph(KiNDDGraphState)
    graph.add_node("agent", call_model)
    graph.add_node("tools", ToolNode(LANGGRAPH_TOOLS))
    graph.add_edge(START, "agent")
    graph.add_conditional_edges(
        "agent",
        tools_condition,
        {"tools": "tools", END: END},
    )
    graph.add_edge("tools", "agent")
    return graph.compile()


def route_supervisor_topic(user_message: str) -> str:
    """Choose the specialist node for a chat turn.

    This is deliberately deterministic for now, which keeps the first
    supervisor graph easy to inspect in tests and LangSmith traces. A later
    iteration can replace this with an LLM router once evals justify it.
    """
    query = user_message.lower()
    if any(
        term in query
        for term in (
            "research",
            "study",
            "studies",
            "trial",
            "gene",
            "sfari",
            "evidence",
            "pubmed",
            "dataset",
        )
    ):
        return "research_agent"
    if any(
        term in query
        for term in (
            "current",
            "latest",
            "today",
            "who is",
            "director",
            "chief",
            "leader",
            "policy",
            "news",
            "website",
        )
    ):
        return "web_agent"
    if any(
        term in query
        for term in (
            "symptom",
            "urgent",
            "seizure",
            "breathing",
            "swallowing",
            "diagnosis",
            "treatment",
            "medication",
            "medical",
            "clinician",
        )
    ):
        return "clinical_agent"
    return "provider_agent"


def create_kindd_supervisor_graph(model=None):
    """Compile a small supervisor graph with specialist agent nodes.

    The graph shape is:

    START -> supervisor -> specialist -> either END or tools -> specialist -> ...

    The specialist nodes share the same model/tool surface for now, but each
    adds a short role instruction and emits `specialist` metadata to LangSmith.
    Like the single-agent graph, each specialist can loop through ToolNode:
    without that loop, a specialist that requests a tool would end the run with
    an empty answer.
    """
    chat_model = model or create_bedrock_langchain_model()
    model_with_tools = chat_model.bind_tools(LANGGRAPH_TOOLS)

    def supervisor(state: KiNDDSupervisorState):
        return {"specialist": route_supervisor_topic(_last_human_text(state["messages"]))}

    def specialist_node(name: str, role_instruction: str):
        def run_specialist(state: KiNDDSupervisorState, config: RunnableConfig):
            messages = [
                SystemMessage(content=role_instruction),
                *state["messages"],
            ]
            node_config = _config_with_specialist(config, name)
            response = model_with_tools.invoke(messages, config=node_config)
            return {"messages": [response]}

        return run_specialist

    def route_after_specialist(state: KiNDDSupervisorState):
        return tools_condition(state)

    def route_after_tools(state: KiNDDSupervisorState):
        return state["specialist"]

    graph = StateGraph(KiNDDSupervisorState)
    graph.add_node("supervisor", supervisor)
    graph.add_node(
        "provider_agent",
        specialist_node(
            "provider_agent",
            "You are the provider navigation specialist. Prefer local KiNDD provider and Regional Center tools.",
        ),
    )
    graph.add_node(
        "clinical_agent",
        specialist_node(
            "clinical_agent",
            "You are the clinical safety specialist. Use cautious medical wording and escalate urgent symptoms.",
        ),
    )
    graph.add_node(
        "research_agent",
        specialist_node(
            "research_agent",
            "You are the autism research specialist. Prefer autism_research and cite returned evidence.",
        ),
    )
    graph.add_node(
        "web_agent",
        specialist_node(
            "web_agent",
            "You are the current-facts specialist. Prefer web_search and cite official returned sources.",
        ),
    )
    graph.add_node("tools", ToolNode(LANGGRAPH_TOOLS))
    graph.add_edge(START, "supervisor")
    graph.add_conditional_edges(
        "supervisor",
        lambda state: state["specialist"],
        {
            "provider_agent": "provider_agent",
            "clinical_agent": "clinical_agent",
            "research_agent": "research_agent",
            "web_agent": "web_agent",
        },
    )
    specialist_names = ("provider_agent", "clinical_agent", "research_agent", "web_agent")
    for specialist_name in specialist_names:
        graph.add_conditional_edges(
            specialist_name,
            route_after_specialist,
            {"tools": "tools", END: END},
        )
    graph.add_conditional_edges(
        "tools",
        route_after_tools,
        {name: name for name in specialist_names},
    )
    return graph.compile()


def chat_with_langgraph_agent(
    user_message: str,
    user_context: Optional[dict[str, Any]] = None,
    conversation_history: Optional[list[dict[str, str]]] = None,
    locale: str = "en",
    user_id: Optional[str] = None,
    session_id: Optional[str] = None,
    model=None,
) -> dict[str, Any]:
    """Run one non-streaming LangGraph chat turn.

    LangSmith does not need custom API calls here. When `LANGSMITH_TRACING=true`
    and `LANGSMITH_API_KEY` are set, LangChain/LangGraph automatically picks up
    this run config and sends tags/metadata to LangSmith.
    """
    graph = create_kindd_langgraph(model=model)
    messages = build_langgraph_messages(
        user_message=user_message,
        user_context=user_context,
        conversation_history=conversation_history,
        locale=locale,
    )
    config = {
        "run_name": "kindd_langgraph_agent",
        "tags": ["kindd-langgraph", "agent", "sync"],
        "metadata": {
            "user_id": user_id,
            "session_id": session_id,
            "locale": locale,
            "has_user_context": bool(user_context),
            "has_conversation_history": bool(conversation_history),
        },
    }

    result = graph.invoke({"messages": messages}, config=config)
    final_message = result["messages"][-1]

    return {
        "answer": _message_content_as_text(final_message),
        "tools_used": _tools_used(result["messages"]),
        "regional_center": user_context.get("regional_center") if user_context else None,
        "runtime": "langgraph",
    }


def stream_chat_with_langgraph_agent(
    user_message: str,
    user_context: Optional[dict[str, Any]] = None,
    conversation_history: Optional[list[dict[str, str]]] = None,
    locale: str = "en",
    user_id: Optional[str] = None,
    session_id: Optional[str] = None,
    model=None,
):
    """Yield LangGraph answer chunks using the existing SSE-compatible shape.

    The current Strands streaming endpoint emits the completed answer as one
    chunk, so this provides endpoint parity while preserving a future path to
    token-level graph streaming.
    """
    result = chat_with_langgraph_agent(
        user_message=user_message,
        user_context=user_context,
        conversation_history=conversation_history,
        locale=locale,
        user_id=user_id,
        session_id=session_id,
        model=model,
    )
    yield result["answer"]


def chat_with_langgraph_supervisor(
    user_message: str,
    user_context: Optional[dict[str, Any]] = None,
    conversation_history: Optional[list[dict[str, str]]] = None,
    locale: str = "en",
    user_id: Optional[str] = None,
    session_id: Optional[str] = None,
    model=None,
) -> dict[str, Any]:
    """Run one non-streaming turn through the supervisor graph."""
    graph = create_kindd_supervisor_graph(model=model)
    messages = build_langgraph_messages(
        user_message=user_message,
        user_context=user_context,
        conversation_history=conversation_history,
        locale=locale,
    )
    specialist = route_supervisor_topic(user_message)
    config = {
        "run_name": "kindd_langgraph_supervisor",
        "tags": ["kindd-langgraph", "supervisor", specialist],
        "metadata": {
            "user_id": user_id,
            "session_id": session_id,
            "locale": locale,
            "specialist": specialist,
            "has_user_context": bool(user_context),
            "has_conversation_history": bool(conversation_history),
        },
    }
    result = graph.invoke(
        {"messages": messages, "specialist": specialist},
        config=config,
    )
    final_message = result["messages"][-1]
    return {
        "answer": _message_content_as_text(final_message),
        "tools_used": _tools_used(result["messages"]),
        "regional_center": user_context.get("regional_center") if user_context else None,
        "runtime": "langgraph-supervisor",
        "specialist": result["specialist"],
    }


def _message_with_context(
    user_message: str,
    user_context: Optional[dict[str, Any]] = None,
) -> str:
    if not user_context:
        return user_message

    context_parts = []
    if user_context.get("zip_code"):
        context_parts.append(f"ZIP: {user_context['zip_code']}")
    if user_context.get("child_age"):
        context_parts.append(f"child age: {user_context['child_age']}")
    if user_context.get("diagnosis"):
        context_parts.append(f"diagnosis: {user_context['diagnosis']}")
    if user_context.get("insurance"):
        context_parts.append(f"insurance: {user_context['insurance']}")
    if user_context.get("memory_context"):
        context_parts.append(f"history: {user_context['memory_context']}")

    if not context_parts:
        return user_message

    return f"User context: {', '.join(context_parts)}\n\n{user_message}"


def _last_human_text(messages: list[BaseMessage]) -> str:
    for message in reversed(messages):
        if isinstance(message, HumanMessage):
            return _message_content_as_text(message)
    return _message_content_as_text(messages[-1]) if messages else ""


def _config_with_specialist(config: RunnableConfig, specialist: str) -> RunnableConfig:
    config = dict(config or {})
    metadata = dict(config.get("metadata") or {})
    metadata["specialist"] = specialist
    tags = list(config.get("tags") or [])
    if specialist not in tags:
        tags.append(specialist)
    config["metadata"] = metadata
    config["tags"] = tags
    return config


def _message_content_as_text(message: BaseMessage) -> str:
    if isinstance(message.content, str):
        return message.content
    return json.dumps(message.content, default=str)


def _tools_used(messages: list[BaseMessage]) -> list[str]:
    tool_names = []
    for message in messages:
        if isinstance(message, ToolMessage) and message.name:
            tool_names.append(message.name)
    return tool_names


def _provider_summary(provider: ProviderV2) -> dict[str, Any]:
    return {
        "id": str(provider.id),
        "name": provider.name,
        "therapy_types": provider.therapy_types or [],
        "age_groups": provider.age_groups or [],
        "address": provider.address,
        "phone": provider.phone,
        "website": provider.website,
        "regional_center": _provider_regional_center(provider),
    }


def _provider_regional_center(provider: ProviderV2) -> str | None:
    legacy_value = getattr(provider, "regional_center", None)
    if legacy_value:
        return legacy_value

    relationship = provider.regional_centers.select_related("regional_center").first()
    if relationship:
        return relationship.regional_center.regional_center

    return None
