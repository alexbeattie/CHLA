"""
API views for KiNDD LLM endpoints.

Includes both regular and streaming (SSE) endpoints.
"""

import json
import logging
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import AllowAny
from rest_framework.throttling import AnonRateThrottle
from django.views.decorators.cache import cache_page
from django.utils.decorators import method_decorator
from django.http import StreamingHttpResponse

from .query import answer_query, explain_eligibility, find_providers_by_criteria
from .autism_research import (
    AutismResearchError,
    ask_autism_research,
    check_autism_research_health,
)
from .bedrock import (
    test_connection,
    chat_completion,
    chat_completion_streaming,
    get_system_prompt_for_locale,
)
from .langgraph_agent import (
    chat_with_langgraph_agent,
    chat_with_langgraph_supervisor,
    stream_chat_with_langgraph_agent,
)
from .observability import llm_monitor_snapshot

logger = logging.getLogger(__name__)


class LLMBurstThrottle(AnonRateThrottle):
    rate = "30/minute"


class LLMSensitiveThrottle(AnonRateThrottle):
    """Stricter limit for endpoints that handle uploaded images/documents."""
    rate = "10/minute"


def _request_trace_ids(request):
    """Extract stable IDs for observability without changing API responses."""
    user = getattr(request, "user", None)
    user_id = None
    if getattr(user, "is_authenticated", False) and getattr(user, "pk", None):
        user_id = str(user.pk)

    session = getattr(request, "session", None)
    session_id = getattr(session, "session_key", None)
    return user_id, session_id


def _research_question_with_context(question, user_context):
    """Attach app location context to research questions without changing user text."""
    if not isinstance(user_context, dict) or not user_context:
        return question

    context_parts = []
    zip_code = user_context.get("zip_code")
    if zip_code:
        context_parts.append(f"user ZIP code: {zip_code}")
        try:
            from locations.models import RegionalCenter

            regional_center = RegionalCenter.find_by_zip_code(zip_code)
            if regional_center:
                context_parts.append(
                    f"user Regional Center: {regional_center.regional_center}"
                )
        except Exception:
            logger.exception("Could not resolve Regional Center for research context")

    if user_context.get("diagnosis"):
        context_parts.append(f"diagnosis/context: {user_context['diagnosis']}")
    if user_context.get("memory_context"):
        context_parts.append(f"remembered context: {user_context['memory_context']}")

    if not context_parts:
        return question

    return (
        f"{question}\n\n"
        "App context for this user. Use this when the user asks for nearby, local, "
        f"or geographically relevant research/trials: {'; '.join(context_parts)}."
    )


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

    permission_classes = [AllowAny]
    throttle_classes = [LLMBurstThrottle]

    def post(self, request):
        query = request.data.get("query")
        user_context = request.data.get("context", {})
        locale = request.data.get("locale", "en")
        conversation_history = request.data.get("conversation_history")

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
            result = answer_query(
                query,
                user_context,
                conversation_history=conversation_history,
                locale=locale,
            )
            return Response(
                {
                    "query": query,
                    "answer": result["answer"],
                    "providers_referenced": result["providers_referenced"],
                    "regional_center": result["regional_center"],
                }
            )

        except Exception as e:
            logger.exception("LLM ask error")
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
    throttle_classes = [LLMBurstThrottle]

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
    throttle_classes = [LLMBurstThrottle]

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
            try:
                autism_rag = check_autism_research_health()
            except AutismResearchError as exc:
                autism_rag = {"status": "unavailable", "error": str(exc)}
            return Response(
                {
                    "status": "healthy" if working else "unhealthy",
                    "bedrock": working,
                    "autism_research_rag": autism_rag,
                    "models": {
                        "embeddings": "amazon.titan-embed-text-v2:0",
                        "chat": "us.anthropic.claude-sonnet-4-5-20250929-v1:0",
                    },
                }
            )
        except Exception as e:
            return Response(
                {"status": "unhealthy", "error": str(e)},
                status=status.HTTP_503_SERVICE_UNAVAILABLE,
            )


class LLMMonitorView(APIView):
    """
    GET /api/llm/monitor/

    Lightweight local monitor for recent LLM/Bedrock calls.
    Does not expose raw prompts or responses.
    """

    permission_classes = [AllowAny]

    def get(self, request):
        return Response(llm_monitor_snapshot())


class AutismResearchView(APIView):
    """
    POST /api/llm/autism-research/

    Proxy endpoint for the Autism Research RAG service.

    Request body:
    {
        "question": "Which genes have strongest SFARI evidence?",
        "top_k": 5,
        "evidence_types": ["gene_evidence"],
        "access_classes": ["public_open"],
        "context": {"zip_code": "90210"}
    }
    """

    permission_classes = [AllowAny]
    throttle_classes = [LLMBurstThrottle]

    def post(self, request):
        question = request.data.get("question") or request.data.get("query")
        if not question:
            return Response(
                {"error": "question is required"}, status=status.HTTP_400_BAD_REQUEST
            )

        if len(question) > 1000:
            return Response(
                {"error": "Question too long (max 1000 characters)"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        user_context = request.data.get("context", {})
        research_question = _research_question_with_context(question, user_context)

        try:
            result = ask_autism_research(
                research_question,
                top_k=request.data.get("top_k"),
                evidence_types=request.data.get("evidence_types"),
                access_classes=request.data.get("access_classes"),
                min_year=request.data.get("min_year"),
                rerank=request.data.get("rerank"),
            )
            return Response(
                {
                    "query": question,
                    "answer": result.get("answer", ""),
                    "citations": result.get("citations", []),
                    "model": result.get("model"),
                    "retrieval": result.get("retrieval", []),
                }
            )
        except AutismResearchError as exc:
            return Response(
                {"error": f"Autism Research RAG unavailable: {str(exc)}"},
                status=status.HTTP_503_SERVICE_UNAVAILABLE,
            )
        except Exception as exc:
            logger.exception("Autism Research RAG proxy error")
            return Response(
                {"error": f"Autism Research RAG error: {str(exc)}"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
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
    throttle_classes = [LLMBurstThrottle]

    def post(self, request):
        query = request.data.get("query")
        user_context = request.data.get("context", {})
        locale = request.data.get("locale", "en")
        conversation_history = request.data.get("conversation_history")

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
                from .query import (
                    semantic_search,
                    format_provider_context,
                    format_user_context,
                    should_retrieve_providers,
                    get_web_context,
                )
                from locations.models import RegionalCenter

                regional_center = None
                if user_context.get("zip_code"):
                    regional_center = RegionalCenter.find_by_zip_code(
                        user_context["zip_code"]
                    )

                enhanced_query = query
                if user_context.get("diagnosis"):
                    enhanced_query += f" {user_context['diagnosis']}"
                if regional_center:
                    enhanced_query += f" {regional_center.regional_center}"

                if should_retrieve_providers(query, user_context):
                    try:
                        relevant_providers = semantic_search(enhanced_query, limit=8)
                    except Exception:
                        from .query import keyword_search

                        relevant_providers = keyword_search(enhanced_query, limit=8)
                else:
                    relevant_providers = []

                provider_context = format_provider_context(relevant_providers)
                user_context_str = format_user_context(user_context, regional_center)
                web_context = get_web_context(query)

                full_context = f"""PROVIDERS IN DATABASE:
{provider_context}

{user_context_str}

{web_context}"""

                system_prompt = get_system_prompt_for_locale(locale)

                emitted_chunks = False
                try:
                    for chunk in chat_completion_streaming(
                        user_message=query,
                        context=full_context,
                        system_prompt=system_prompt,
                        conversation_history=conversation_history,
                    ):
                        emitted_chunks = True
                        yield f"data: {json.dumps({'type': 'chunk', 'content': chunk})}\n\n"
                except Exception:
                    logger.exception("Bedrock stream interrupted")
                    if not emitted_chunks:
                        fallback_answer = chat_completion(
                            user_message=query,
                            context=full_context,
                            system_prompt=system_prompt,
                            conversation_history=conversation_history,
                        )
                        yield (
                            "data: "
                            f"{json.dumps({'type': 'chunk', 'content': fallback_answer})}"
                            "\n\n"
                        )
                    else:
                        yield (
                            "data: "
                            f"{json.dumps({'type': 'chunk', 'content': '\n\n**Note:** The streaming connection was interrupted, so this answer may be incomplete.'})}"
                            "\n\n"
                        )

                yield f"data: {json.dumps({'type': 'done', 'providers_referenced': [str(p.id) for p in relevant_providers], 'regional_center': regional_center.regional_center if regional_center else None})}\n\n"

            except Exception as e:
                logger.exception("Streaming ask error")
                yield f"data: {json.dumps({'type': 'error', 'message': str(e)})}\n\n"

        response = StreamingHttpResponse(
            event_stream(), content_type="text/event-stream"
        )
        response["Cache-Control"] = "no-cache"
        response["X-Accel-Buffering"] = "no"
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
    throttle_classes = [LLMBurstThrottle]

    def post(self, request):
        query = request.data.get("query")
        user_context = request.data.get("context", {})
        locale = request.data.get("locale", "en")
        conversation_history = request.data.get("conversation_history")

        if not query:
            return Response(
                {"error": "Query is required"}, status=status.HTTP_400_BAD_REQUEST
            )

        try:
            from .agent import chat_with_agent

            user_id, session_id = _request_trace_ids(request)
            result = chat_with_agent(
                query,
                user_context,
                conversation_history=conversation_history,
                locale=locale,
                user_id=user_id,
                session_id=session_id,
                feature="chla",
            )
            return Response(
                {
                    "query": query,
                    "answer": result["answer"],
                    "tools_used": result.get("tools_used", []),
                    "regional_center": result.get("regional_center"),
                }
            )

        except ImportError:
            result = answer_query(
                query, user_context, conversation_history=conversation_history, locale=locale
            )
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
            logger.exception("Agent ask error")
            return Response(
                {"error": f"Agent error: {str(e)}"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )


class LangGraphAgentAskView(APIView):
    """
    POST /api/llm/langgraph-agent/

    Parallel LangGraph agent endpoint for learning and eval comparisons.
    It intentionally returns the same basic shape as the Strands agent endpoint
    so callers can compare the two runtimes without changing UI code.
    """

    permission_classes = [AllowAny]
    throttle_classes = [LLMBurstThrottle]

    def post(self, request):
        query = request.data.get("query")
        user_context = request.data.get("context", {})
        locale = request.data.get("locale", "en")
        conversation_history = request.data.get("conversation_history")

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
            user_id, session_id = _request_trace_ids(request)
            result = chat_with_langgraph_agent(
                query,
                user_context=user_context,
                conversation_history=conversation_history,
                locale=locale,
                user_id=user_id,
                session_id=session_id,
            )
            return Response(
                {
                    "query": query,
                    "answer": result["answer"],
                    "tools_used": result.get("tools_used", []),
                    "regional_center": result.get("regional_center"),
                    "runtime": result.get("runtime", "langgraph"),
                }
            )

        except Exception as e:
            logger.exception("LangGraph agent ask error")
            return Response(
                {"error": f"LangGraph agent error: {str(e)}"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )


class StreamingLangGraphAgentAskView(APIView):
    """
    POST /api/llm/langgraph-agent-stream/

    Streaming-compatible LangGraph endpoint. It mirrors the existing Strands
    streaming endpoint by emitting the completed answer as one SSE chunk.
    """

    permission_classes = [AllowAny]
    throttle_classes = [LLMBurstThrottle]

    def post(self, request):
        query = request.data.get("query")
        user_context = request.data.get("context", {})
        locale = request.data.get("locale", "en")
        conversation_history = request.data.get("conversation_history")

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
            try:
                user_id, session_id = _request_trace_ids(request)
                for chunk in stream_chat_with_langgraph_agent(
                    query,
                    user_context=user_context,
                    conversation_history=conversation_history,
                    locale=locale,
                    user_id=user_id,
                    session_id=session_id,
                ):
                    yield (
                        "data: "
                        f"{json.dumps({'type': 'chunk', 'content': chunk})}"
                        "\n\n"
                    )

                yield (
                    "data: "
                    f"{json.dumps({'type': 'done', 'runtime': 'langgraph'})}"
                    "\n\n"
                )
            except Exception as e:
                logger.exception("LangGraph streaming error")
                yield f"data: {json.dumps({'type': 'error', 'message': str(e)})}\n\n"

        response = StreamingHttpResponse(
            event_stream(), content_type="text/event-stream"
        )
        response["Cache-Control"] = "no-cache"
        response["X-Accel-Buffering"] = "no"
        return response


class LangGraphSupervisorAskView(APIView):
    """
    POST /api/llm/langgraph-supervisor/

    First multi-agent LangGraph endpoint. A deterministic supervisor routes the
    turn to provider, clinical, research, or current-facts specialist nodes.
    """

    permission_classes = [AllowAny]
    throttle_classes = [LLMBurstThrottle]

    def post(self, request):
        query = request.data.get("query")
        user_context = request.data.get("context", {})
        locale = request.data.get("locale", "en")
        conversation_history = request.data.get("conversation_history")

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
            user_id, session_id = _request_trace_ids(request)
            result = chat_with_langgraph_supervisor(
                query,
                user_context=user_context,
                conversation_history=conversation_history,
                locale=locale,
                user_id=user_id,
                session_id=session_id,
            )
            return Response(
                {
                    "query": query,
                    "answer": result["answer"],
                    "tools_used": result.get("tools_used", []),
                    "regional_center": result.get("regional_center"),
                    "runtime": result.get("runtime", "langgraph-supervisor"),
                    "specialist": result.get("specialist"),
                }
            )

        except Exception as e:
            logger.exception("LangGraph supervisor ask error")
            return Response(
                {"error": f"LangGraph supervisor error: {str(e)}"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )


class ImageAnalysisView(APIView):
    """
    POST /api/llm/analyze-image/
    
    Analyze an uploaded image using Claude's vision capabilities.
    
    Request body:
    {
        "image": "<base64-encoded image data>",
        "type": "insurance_card" | "document" | "general",
        "prompt": "Optional custom prompt for 'general' type"
    }
    
    Supported image formats: JPEG, PNG, GIF, WebP
    Max image size: ~20MB (after base64 encoding)
    
    Response:
    {
        "analysis": "...",
        "type": "insurance_card"
    }
    """
    
    permission_classes = [AllowAny]
    throttle_classes = [LLMSensitiveThrottle]
    
    def post(self, request):
        image_data = request.data.get("image")
        analysis_type = request.data.get("type", "general")
        custom_prompt = request.data.get("prompt")
        
        if not image_data:
            return Response(
                {"error": "Image data is required. Send base64-encoded image in 'image' field."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        
        # Detect media type from base64 header or default to JPEG
        media_type = "image/jpeg"
        if image_data.startswith("data:"):
            # Handle data URL format: data:image/png;base64,xxxxx
            header, image_data = image_data.split(",", 1)
            if "image/png" in header:
                media_type = "image/png"
            elif "image/gif" in header:
                media_type = "image/gif"
            elif "image/webp" in header:
                media_type = "image/webp"
        
        try:
            from .bedrock import analyze_insurance_card, analyze_document, analyze_image
            
            if analysis_type == "insurance_card":
                result = analyze_insurance_card(image_data, media_type)
            elif analysis_type == "document":
                result = analyze_document(image_data, media_type)
            else:
                # General analysis with optional custom prompt
                prompt = custom_prompt or "Describe what you see in this image and how it might be relevant to someone seeking neurodevelopmental services."
                result = analyze_image(image_data, prompt, media_type)
            
            return Response({
                "analysis": result,
                "type": analysis_type,
            })
            
        except Exception as e:
            return Response(
                {"error": f"Image analysis failed: {str(e)}"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )


class DocumentAnalysisView(APIView):
    """
    POST /api/llm/analyze-document/
    
    Analyze an uploaded document (Word, PDF, text) by extracting text and analyzing.
    
    Request body:
    {
        "document": "<base64-encoded document data>",
        "file_type": "docx" | "pdf" | "txt" | "rtf",
        "prompt": "Optional analysis prompt"
    }
    
    Response:
    {
        "analysis": "...",
        "file_type": "docx",
        "text_extracted": true
    }
    """
    
    permission_classes = [AllowAny]
    throttle_classes = [LLMSensitiveThrottle]
    
    def post(self, request):
        document_data = request.data.get("document")
        file_type = request.data.get("file_type", "").lower()
        custom_prompt = request.data.get("prompt", "")
        
        if not document_data:
            return Response(
                {"error": "Document data is required. Send base64-encoded document in 'document' field."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        
        try:
            import base64
            import io
            
            # Decode base64 document
            doc_bytes = base64.b64decode(document_data)
            
            # Extract text based on file type
            extracted_text = ""
            
            if file_type in ["docx", "doc"]:
                try:
                    from docx import Document
                    doc = Document(io.BytesIO(doc_bytes))
                    extracted_text = "\n".join([para.text for para in doc.paragraphs if para.text.strip()])
                except Exception as e:
                    return Response(
                        {"error": f"Failed to parse Word document: {str(e)}"},
                        status=status.HTTP_400_BAD_REQUEST,
                    )
                    
            elif file_type == "pdf":
                try:
                    from PyPDF2 import PdfReader
                    reader = PdfReader(io.BytesIO(doc_bytes))
                    extracted_text = "\n".join([page.extract_text() or "" for page in reader.pages])
                except Exception as e:
                    return Response(
                        {"error": f"Failed to parse PDF: {str(e)}"},
                        status=status.HTTP_400_BAD_REQUEST,
                    )
                    
            elif file_type in ["txt", "rtf", "text"]:
                try:
                    extracted_text = doc_bytes.decode("utf-8")
                except UnicodeDecodeError:
                    extracted_text = doc_bytes.decode("latin-1")
                    
            else:
                return Response(
                    {"error": f"Unsupported file type: {file_type}. Supported: docx, pdf, txt"},
                    status=status.HTTP_400_BAD_REQUEST,
                )
            
            if not extracted_text.strip():
                return Response(
                    {"error": "No text could be extracted from the document."},
                    status=status.HTTP_400_BAD_REQUEST,
                )
            
            # Truncate if too long
            max_chars = 50000
            if len(extracted_text) > max_chars:
                extracted_text = extracted_text[:max_chars] + "\n\n[Document truncated due to length...]"
            
            # Analyze with LLM
            from .bedrock import chat_completion
            
            system_prompt = """You are KiNDD, a helpful assistant for families navigating neurodevelopmental disability services.
Analyze the following document and provide a helpful summary. Focus on:
- What type of document this is (IEP, assessment, insurance document, etc.)
- Key information relevant to the family
- Any action items or important dates
- How this relates to services and support available

Be warm, supportive, and use clear language."""
            
            user_message = f"{custom_prompt}\n\nDocument content:\n\n{extracted_text}" if custom_prompt else f"Please analyze this document:\n\n{extracted_text}"
            
            result = chat_completion(user_message, system_prompt=system_prompt)
            
            return Response({
                "analysis": result,
                "file_type": file_type,
                "text_extracted": True,
                "text_length": len(extracted_text),
            })
            
        except Exception as e:
            logger.exception("Document analysis error")
            return Response(
                {"error": f"Document analysis failed: {str(e)}"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )


class StreamingAgentAskView(APIView):
    """
    POST /api/llm/agent-stream/

    Agent endpoint using Server-Sent Events (SSE).
    The agent has tool access (search providers, check eligibility, etc.)
    and returns the completed agent answer as a single SSE chunk.

    Request body:
    {
        "query": "Find ABA providers near 90210",
        "context": { "zip_code": "90210" },
        "locale": "en"
    }
    """

    permission_classes = [AllowAny]
    throttle_classes = [LLMBurstThrottle]

    def post(self, request):
        query = request.data.get("query")
        user_context = request.data.get("context", {})
        locale = request.data.get("locale", "en")

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
            try:
                from .agent import chat_with_agent

                user_id, session_id = _request_trace_ids(request)
                result = chat_with_agent(
                    query,
                    user_context,
                    locale=locale,
                    user_id=user_id,
                    session_id=session_id,
                    feature="chla",
                )

                yield (
                    f"data: "
                    f"{json.dumps({'type': 'chunk', 'content': result['answer']})}\n\n"
                )

                yield (
                    f"data: "
                    f"{json.dumps({'type': 'done', 'regional_center': result.get('regional_center')})}\n\n"
                )

            except ImportError:
                yield f"data: {json.dumps({'type': 'error', 'message': 'Agent streaming not available; Strands SDK not installed.'})}\n\n"
            except Exception as e:
                logger.exception("Agent streaming error")
                yield f"data: {json.dumps({'type': 'error', 'message': str(e)})}\n\n"

        response = StreamingHttpResponse(
            event_stream(), content_type="text/event-stream"
        )
        response["Cache-Control"] = "no-cache"
        response["X-Accel-Buffering"] = "no"
        return response
