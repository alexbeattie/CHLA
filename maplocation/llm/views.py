"""
API views for KiNDD LLM endpoints.

Includes both regular and streaming (SSE) endpoints.
"""

import json
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import IsAuthenticated, AllowAny
from django.views.decorators.cache import cache_page
from django.utils.decorators import method_decorator
from django.http import StreamingHttpResponse

from .query import answer_query, explain_eligibility, find_providers_by_criteria
from .bedrock import test_connection, chat_completion_streaming


class AskKiNDDView(APIView):
    """
    POST /api/llm/ask/

    Natural language query endpoint.

    Request body:
    {
        "query": "What ABA providers near 90210 accept Medi-Cal?",
        "context": {
            "zip_code": "90210",
            "child_age": 4,
            "diagnosis": "autism",
            "insurance": "Medi-Cal"
        }
    }

    Response:
    {
        "query": "...",
        "answer": "...",
        "providers_referenced": [1, 2, 3],
        "regional_center": "Westside Regional Center"
    }
    """

    permission_classes = [AllowAny]  # Change to IsAuthenticated for production

    def post(self, request):
        query = request.data.get("query")
        user_context = request.data.get("context", {})

        if not query:
            return Response(
                {"error": "Query is required"}, status=status.HTTP_400_BAD_REQUEST
            )

        if len(query) > 1000:
            return Response(
                {"error": "Query too long (max 1000 characters)"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        try:
            result = answer_query(query, user_context)
            return Response(
                {
                    "query": query,
                    "answer": result["answer"],
                    "providers_referenced": result["providers_referenced"],
                    "regional_center": result["regional_center"],
                }
            )

        except Exception as e:
            return Response(
                {"error": f"LLM error: {str(e)}"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )


class EligibilityCheckView(APIView):
    """
    POST /api/llm/eligibility/

    Check eligibility for Regional Center and insurance coverage.

    Request body:
    {
        "age": 4,
        "diagnosis": "autism",
        "insurance": "Kaiser",
        "zip_code": "90210"
    }
    """

    permission_classes = [AllowAny]

    def post(self, request):
        age = request.data.get("age")
        diagnosis = request.data.get("diagnosis")
        insurance = request.data.get("insurance")
        zip_code = request.data.get("zip_code")

        if not all([age, diagnosis, insurance]):
            return Response(
                {"error": "age, diagnosis, and insurance are required"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        try:
            explanation = explain_eligibility(
                age=int(age),
                diagnosis=diagnosis,
                insurance=insurance,
                zip_code=zip_code,
            )

            return Response(
                {
                    "age": age,
                    "diagnosis": diagnosis,
                    "insurance": insurance,
                    "zip_code": zip_code,
                    "eligibility_explanation": explanation,
                }
            )

        except Exception as e:
            return Response(
                {"error": f"Error: {str(e)}"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )


class SmartSearchView(APIView):
    """
    GET /api/llm/search/

    Structured search with semantic understanding.

    Query params:
    - therapy_type: ABA, Speech Therapy, OT, PT
    - insurance: Medi-Cal, Kaiser, etc.
    - age: Child's age in years
    - zip_code: Location
    - diagnosis: autism, ADHD, etc.
    """

    permission_classes = [AllowAny]

    @method_decorator(cache_page(60 * 5))  # Cache 5 minutes
    def get(self, request):
        therapy_type = request.query_params.get("therapy_type")
        insurance = request.query_params.get("insurance")
        age = request.query_params.get("age")
        zip_code = request.query_params.get("zip_code")
        diagnosis = request.query_params.get("diagnosis")
        limit = int(request.query_params.get("limit", 20))

        providers = find_providers_by_criteria(
            therapy_type=therapy_type,
            insurance=insurance,
            age=int(age) if age else None,
            zip_code=zip_code,
            diagnosis=diagnosis,
            limit=min(limit, 50),
        )

        return Response(
            {
                "count": len(providers),
                "filters_applied": {
                    "therapy_type": therapy_type,
                    "insurance": insurance,
                    "age": age,
                    "zip_code": zip_code,
                    "diagnosis": diagnosis,
                },
                "providers": [
                    {
                        "id": p.id,
                        "name": p.name,
                        "therapy_types": p.therapy_types,
                        "age_groups": p.age_groups,
                        "address": p.address,
                        "phone": p.phone,
                        "website": p.website,
                    }
                    for p in providers
                ],
            }
        )


class LLMHealthView(APIView):
    """
    GET /api/llm/health/

    Check Bedrock connectivity.
    """

    permission_classes = [AllowAny]

    def get(self, request):
        try:
            working = test_connection()
            return Response(
                {
                    "status": "healthy" if working else "unhealthy",
                    "bedrock": working,
                    "models": {
                        "embeddings": "amazon.titan-embed-text-v2:0",
                        "chat": "anthropic.claude-3-5-sonnet-20241022-v2:0",
                    },
                }
            )
        except Exception as e:
            return Response(
                {"status": "unhealthy", "error": str(e)},
                status=status.HTTP_503_SERVICE_UNAVAILABLE,
            )


class StreamingAskView(APIView):
    """
    POST /api/llm/stream/

    Streaming chat endpoint using Server-Sent Events (SSE).
    Returns text chunks as they're generated for real-time UX.

    Request body:
    {
        "query": "What ABA providers near 90210 accept Medi-Cal?",
        "context": {
            "zip_code": "90210",
            "child_age": 4,
            "diagnosis": "autism"
        }
    }

    Response: SSE stream with chunks like:
    data: {"type": "chunk", "content": "Based on"}
    data: {"type": "chunk", "content": " your location"}
    data: {"type": "done", "providers_referenced": [...]}
    """

    permission_classes = [AllowAny]

    def post(self, request):
        query = request.data.get("query")
        user_context = request.data.get("context", {})

        if not query:
            return Response(
                {"error": "Query is required"}, status=status.HTTP_400_BAD_REQUEST
            )

        if len(query) > 1000:
            return Response(
                {"error": "Query too long (max 1000 characters)"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        def event_stream():
            """Generate SSE events from streaming response."""
            try:
                # Build context for RAG
                from .query import (
                    semantic_search,
                    format_provider_context,
                    format_user_context,
                )
                from locations.models import RegionalCenter

                # Find regional center
                regional_center = None
                if user_context.get("zip_code"):
                    regional_center = RegionalCenter.find_by_zip_code(
                        user_context["zip_code"]
                    )

                # Get relevant providers
                enhanced_query = query
                if user_context.get("diagnosis"):
                    enhanced_query += f" {user_context['diagnosis']}"
                if regional_center:
                    enhanced_query += f" {regional_center.regional_center}"

                try:
                    relevant_providers = semantic_search(enhanced_query, limit=15)
                except Exception:
                    from .query import keyword_search

                    relevant_providers = keyword_search(enhanced_query, limit=15)

                # Build context
                provider_context = format_provider_context(relevant_providers)
                user_context_str = format_user_context(user_context, regional_center)

                full_context = f"""PROVIDERS IN DATABASE:
{provider_context}

{user_context_str}"""

                # Stream from Claude
                for chunk in chat_completion_streaming(
                    user_message=query,
                    context=full_context,
                ):
                    # Send each chunk as SSE event
                    yield f"data: {json.dumps({'type': 'chunk', 'content': chunk})}\n\n"

                # Send completion event with metadata
                yield f"data: {json.dumps({'type': 'done', 'providers_referenced': [str(p.id) for p in relevant_providers], 'regional_center': regional_center.regional_center if regional_center else None})}\n\n"

            except Exception as e:
                yield f"data: {json.dumps({'type': 'error', 'message': str(e)})}\n\n"

        response = StreamingHttpResponse(
            event_stream(), content_type="text/event-stream"
        )
        response["Cache-Control"] = "no-cache"
        response["X-Accel-Buffering"] = "no"  # Disable nginx buffering
        return response


class AgentAskView(APIView):
    """
    POST /api/llm/agent/

    Agent-based chat using Strands SDK with tool use.
    The agent can search providers, check eligibility, etc.

    Request body:
    {
        "query": "Find ABA providers in 90210 that take Medi-Cal",
        "context": {
            "zip_code": "90210",
            "child_age": 4,
            "diagnosis": "autism"
        }
    }
    """

    permission_classes = [AllowAny]

    def post(self, request):
        query = request.data.get("query")
        user_context = request.data.get("context", {})

        if not query:
            return Response(
                {"error": "Query is required"}, status=status.HTTP_400_BAD_REQUEST
            )

        try:
            from .agent import chat_with_agent

            result = chat_with_agent(query, user_context)
            return Response(
                {
                    "query": query,
                    "answer": result["answer"],
                    "tools_used": result.get("tools_used", []),
                    "regional_center": result.get("regional_center"),
                }
            )

        except ImportError as e:
            # Strands not installed - fall back to regular query
            result = answer_query(query, user_context)
            return Response(
                {
                    "query": query,
                    "answer": result["answer"],
                    "providers_referenced": result["providers_referenced"],
                    "regional_center": result["regional_center"],
                    "note": "Agent mode not available, using RAG fallback",
                }
            )

        except Exception as e:
            return Response(
                {"error": f"Agent error: {str(e)}"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )
