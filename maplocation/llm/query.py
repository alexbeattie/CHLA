"""
RAG Query Engine for KiNDD.

Combines semantic search with LLM reasoning to answer
questions about neurodevelopmental services.
"""

from django.db import connection
from typing import Optional
from locations.models import ProviderV2, RegionalCenter
from .bedrock import generate_embedding, chat_completion


def semantic_search(query: str, limit: int = 15) -> list[ProviderV2]:
    """
    Find providers semantically similar to the query.

    Uses pgvector for cosine similarity search.
    Falls back to keyword search if embeddings not available.
    """

    # Check if we have embeddings (field may not exist yet)
    try:
        has_embeddings = ProviderV2.objects.filter(embedding__isnull=False).exists()
    except Exception:
        # Embedding field doesn't exist yet - use keyword search
        has_embeddings = False

    if has_embeddings:
        # Vector similarity search
        query_embedding = generate_embedding(query)

        with connection.cursor() as cursor:
            cursor.execute(
                """
                SELECT id, 1 - (embedding <=> %s::vector) as similarity
                FROM providers_v2
                WHERE embedding IS NOT NULL
                ORDER BY embedding <=> %s::vector
                LIMIT %s
            """,
                [query_embedding, query_embedding, limit],
            )

            results = cursor.fetchall()

        provider_ids = [r[0] for r in results]
        # Preserve order from similarity search
        providers = list(ProviderV2.objects.filter(id__in=provider_ids))
        providers.sort(key=lambda p: provider_ids.index(p.id))
        return providers

    else:
        # Fallback: keyword search
        return keyword_search(query, limit)


def keyword_search(query: str, limit: int = 15) -> list[ProviderV2]:
    """
    Basic keyword search fallback.
    Searches name, therapy types, and diagnoses.
    """
    from django.db.models import Q

    terms = query.lower().split()

    q = Q()
    for term in terms:
        q |= Q(name__icontains=term)
        q |= Q(therapy_types__icontains=term)
        q |= Q(diagnoses_treated__icontains=term)
        q |= Q(description__icontains=term)

    return list(ProviderV2.objects.filter(q)[:limit])


def format_provider_context(providers: list[ProviderV2]) -> str:
    """Format providers for LLM context window."""

    if not providers:
        return "No matching providers found in database."

    lines = []
    for i, p in enumerate(providers, 1):
        therapy_types = (
            ", ".join(p.therapy_types) if p.therapy_types else "Not specified"
        )
        age_groups = ", ".join(p.age_groups) if p.age_groups else "All ages"
        insurance = (
            ", ".join(p.insurance_networks)
            if hasattr(p, "insurance_networks") and p.insurance_networks
            else "Contact for info"
        )

        lines.append(
            f"""
**{i}. {p.name}**
- Services: {therapy_types}
- Ages Served: {age_groups}
- Address: {p.address or 'Not listed'}
- Phone: {p.phone or 'Not listed'}
- Insurance: {insurance}
- Website: {p.website or 'Not listed'}
"""
        )

    return "\n".join(lines)


def format_user_context(
    user_context: dict, regional_center: Optional[RegionalCenter] = None
) -> str:
    """Format user context for LLM."""

    if not user_context and not regional_center:
        return ""

    lines = ["USER'S SITUATION:"]

    if user_context.get("zip_code"):
        lines.append(f"- Location: {user_context['zip_code']}")

    if regional_center:
        lines.append(f"- Regional Center: {regional_center.name}")

    if user_context.get("child_age"):
        lines.append(f"- Child's Age: {user_context['child_age']}")

    if user_context.get("diagnosis"):
        lines.append(f"- Diagnosis: {user_context['diagnosis']}")

    if user_context.get("insurance"):
        lines.append(f"- Insurance: {user_context['insurance']}")

    if user_context.get("current_services"):
        services = ", ".join(user_context["current_services"])
        lines.append(f"- Current Services: {services}")

    return "\n".join(lines)


def answer_query(
    user_query: str,
    user_context: Optional[dict] = None,
    conversation_history: Optional[list] = None,
) -> dict:
    """
    Main RAG pipeline: retrieve relevant providers, then answer with LLM.

    Args:
        user_query: Natural language question
        user_context: Dict with zip_code, child_age, diagnosis, insurance, etc.
        conversation_history: Previous messages for multi-turn chat

    Returns:
        {
            "answer": str,
            "providers_referenced": list[int],
            "regional_center": str or None
        }
    """

    # Step 1: Find user's regional center if we have ZIP
    regional_center = None
    if user_context and user_context.get("zip_code"):
        regional_center = RegionalCenter.find_by_zip_code(user_context["zip_code"])

    # Step 2: Retrieve relevant providers
    # Enhance query with user context for better retrieval
    enhanced_query = user_query
    if user_context:
        if user_context.get("diagnosis"):
            enhanced_query += f" {user_context['diagnosis']}"
        if user_context.get("insurance"):
            enhanced_query += f" {user_context['insurance']}"
        if regional_center:
            enhanced_query += f" {regional_center.name}"

    relevant_providers = semantic_search(enhanced_query, limit=15)

    # Step 3: Build context for LLM
    provider_context = format_provider_context(relevant_providers)
    user_context_str = format_user_context(user_context or {}, regional_center)

    full_context = f"""PROVIDERS IN DATABASE:
{provider_context}

{user_context_str}"""

    # Step 4: Get LLM response
    answer = chat_completion(
        user_message=user_query,
        context=full_context,
        conversation_history=conversation_history,
    )

    return {
        "answer": answer,
        "providers_referenced": [p.id for p in relevant_providers],
        "regional_center": regional_center.name if regional_center else None,
    }


# ============================================================================
# SPECIALIZED QUERY FUNCTIONS
# ============================================================================


def find_providers_by_criteria(
    therapy_type: Optional[str] = None,
    insurance: Optional[str] = None,
    age: Optional[int] = None,
    zip_code: Optional[str] = None,
    diagnosis: Optional[str] = None,
    limit: int = 20,
) -> list[ProviderV2]:
    """
    Structured search with specific filters.
    Use this for programmatic queries from the UI.
    """
    from django.db.models import Q

    queryset = ProviderV2.objects.all()

    if therapy_type:
        queryset = queryset.filter(therapy_types__icontains=therapy_type)

    if diagnosis:
        queryset = queryset.filter(
            Q(diagnoses_treated__icontains=diagnosis)
            | Q(description__icontains=diagnosis)
        )

    if age is not None:
        # Map age to age group
        if age < 3:
            age_group = "early intervention"
        elif age < 6:
            age_group = "children"
        elif age < 13:
            age_group = "school-age"
        elif age < 18:
            age_group = "adolescent"
        else:
            age_group = "adult"

        queryset = queryset.filter(age_groups__icontains=age_group)

    if zip_code:
        # Filter by regional center
        rc = RegionalCenter.find_by_zip_code(zip_code)
        if rc:
            queryset = queryset.filter(regional_center__icontains=rc.name)

    # TODO: Add insurance filtering once we have that data

    return list(queryset[:limit])


def explain_eligibility(
    age: int, diagnosis: str, insurance: str, zip_code: Optional[str] = None
) -> str:
    """
    Generate an eligibility explanation using the LLM.
    """

    rc = None
    if zip_code:
        rc = RegionalCenter.find_by_zip_code(zip_code)

    context = f"""
California Regional Center Eligibility Rules:
- Developmental Disabilities (Lanterman Act): Autism, intellectual disability, cerebral palsy, epilepsy
- Early Start (0-3): Developmental delay 33%+ in one area or 25%+ in two areas
- Transitions: Early Start → Lanterman at age 3, Adult transition starts at 16

Insurance Coverage:
- Medi-Cal: Covers most services, may require managed care authorization
- Kaiser: In-network only, limited ABA network in some areas
- Commercial plans: SB 946 autism mandate requires ABA coverage

User's Regional Center: {rc.name if rc else 'Unknown'}
"""

    question = f"""
A family has a {age}-year-old child with {diagnosis}. 
They have {insurance} insurance{f' and live in the {rc.name} area' if rc else ''}.

1. What Regional Center services might they be eligible for?
2. What does their insurance likely cover for therapy?
3. What are the immediate next steps they should take?
4. Are there any upcoming age-related transitions to be aware of?
"""

    return chat_completion(question, context=context)
