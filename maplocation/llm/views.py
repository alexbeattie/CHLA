"""
API views for KiNDD LLM endpoints.
"""

from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import IsAuthenticated, AllowAny
from django.views.decorators.cache import cache_page
from django.utils.decorators import method_decorator

from .query import answer_query, explain_eligibility, find_providers_by_criteria
from .bedrock import test_connection


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
