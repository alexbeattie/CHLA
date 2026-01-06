"""
KiNDD Strands Agent with custom tools for neurodevelopmental service navigation.

Uses AWS Strands SDK with Bedrock for:
- Streaming responses
- Tool use (search providers, check eligibility, etc.)
- Multi-turn conversation
"""

from strands import Agent, tool
from strands.models import BedrockModel
from typing import Optional
import json

from django.db.models import Q
from locations.models import ProviderV2, RegionalCenter


# ============================================================================
# KINDD TOOLS - These give the agent access to our database
# ============================================================================


@tool
def search_providers(
    query: str,
    therapy_type: Optional[str] = None,
    zip_code: Optional[str] = None,
    max_results: int = 10,
) -> str:
    """
    Search for neurodevelopmental service providers in LA County.

    Use this tool when the user asks about finding providers, therapists,
    or services. Combine with filters for better results.

    Args:
        query: Natural language search (e.g., "ABA therapy for toddlers")
        therapy_type: Filter by service type (ABA therapy, Speech therapy,
                      Occupational therapy, Physical therapy)
        zip_code: Filter by location/regional center area
        max_results: Maximum providers to return (default 10)

    Returns:
        JSON list of matching providers with contact info
    """
    from llm.query import semantic_search, keyword_search

    # Try semantic search first, fallback to keyword
    try:
        providers = semantic_search(query, limit=max_results)
    except Exception:
        providers = keyword_search(query, limit=max_results)

    # Apply additional filters
    if therapy_type:
        providers = [
            p for p in providers if therapy_type.lower() in str(p.therapy_types).lower()
        ]

    if zip_code:
        rc = RegionalCenter.find_by_zip_code(zip_code)
        if rc:
            providers = [
                p
                for p in providers
                if rc.regional_center.lower() in str(p.regional_center).lower()
            ]

    # Format results
    results = []
    for p in providers[:max_results]:
        results.append(
            {
                "id": str(p.id),
                "name": p.name,
                "therapy_types": p.therapy_types or [],
                "age_groups": p.age_groups or [],
                "address": p.address,
                "phone": p.phone,
                "website": p.website,
                "regional_center": p.regional_center,
            }
        )

    if not results:
        return json.dumps(
            {"message": "No providers found matching your criteria.", "providers": []}
        )

    return json.dumps({"count": len(results), "providers": results})


@tool
def get_regional_center(zip_code: str) -> str:
    """
    Find which Regional Center serves a given ZIP code in LA County.

    Use this when users ask which Regional Center serves their area,
    or when you need to determine eligibility based on location.

    Args:
        zip_code: 5-digit ZIP code in Los Angeles County

    Returns:
        Regional Center name and contact information
    """
    rc = RegionalCenter.find_by_zip_code(zip_code)

    if not rc:
        return json.dumps(
            {
                "found": False,
                "message": f"Could not find a Regional Center for ZIP code {zip_code}. "
                "This may be outside LA County coverage area.",
            }
        )

    return json.dumps(
        {
            "found": True,
            "name": rc.regional_center,
            "abbreviation": rc.abbreviation if hasattr(rc, "abbreviation") else None,
            "phone": rc.phone if hasattr(rc, "phone") else None,
            "website": rc.website if hasattr(rc, "website") else None,
            "address": rc.address if hasattr(rc, "address") else None,
            "zip_code": zip_code,
        }
    )


@tool
def get_provider_details(provider_name: str) -> str:
    """
    Get full details about a specific provider by name.

    Use this when a user asks for more information about a
    provider mentioned in search results.

    Args:
        provider_name: Name of the provider (partial match supported)

    Returns:
        Complete provider information including services, contact, hours
    """
    providers = ProviderV2.objects.filter(name__icontains=provider_name)[:5]

    if not providers:
        return json.dumps(
            {
                "found": False,
                "message": f"No provider found with name containing '{provider_name}'",
            }
        )

    results = []
    for p in providers:
        results.append(
            {
                "id": str(p.id),
                "name": p.name,
                "therapy_types": p.therapy_types or [],
                "diagnoses_treated": p.diagnoses_treated,
                "age_groups": p.age_groups or [],
                "address": p.address,
                "phone": p.phone,
                "email": p.email if hasattr(p, "email") else None,
                "website": p.website,
                "description": p.description,
                "regional_center": p.regional_center,
                "insurance_accepted": (
                    p.insurance_networks if hasattr(p, "insurance_networks") else None
                ),
            }
        )

    return json.dumps({"count": len(results), "providers": results})


@tool
def check_eligibility(
    age_years: int,
    diagnosis: str,
    insurance: Optional[str] = None,
    zip_code: Optional[str] = None,
) -> str:
    """
    Check eligibility for Regional Center and therapy services.

    Use this when families ask about whether they qualify for services,
    what their child is eligible for, or how to get started.

    Args:
        age_years: Child's age in years
        diagnosis: Primary diagnosis (autism, developmental delay, etc.)
        insurance: Insurance type if known (Medi-Cal, Kaiser, commercial)
        zip_code: Location for Regional Center determination

    Returns:
        Eligibility information and recommended next steps
    """
    eligibility = {
        "age_years": age_years,
        "diagnosis": diagnosis,
        "insurance": insurance,
    }

    # Determine age-based program
    if age_years < 3:
        eligibility["program"] = "Early Start"
        eligibility["program_description"] = (
            "California's Early Start program serves children 0-3 with "
            "developmental delays or established risk conditions. "
            "Services include developmental evaluations, therapy, and family support."
        )
    elif age_years < 22:
        eligibility["program"] = "Lanterman Act Services"
        eligibility["program_description"] = (
            "The Lanterman Developmental Disabilities Services Act provides "
            "services to Californians with developmental disabilities including "
            "intellectual disability, cerebral palsy, epilepsy, autism, and "
            "conditions requiring similar treatment."
        )
    else:
        eligibility["program"] = "Adult Services"
        eligibility["program_description"] = (
            "Adults with developmental disabilities may qualify for ongoing "
            "Regional Center services, supported living, day programs, and "
            "employment support."
        )

    # Check diagnosis eligibility
    diagnosis_lower = diagnosis.lower()
    if any(dx in diagnosis_lower for dx in ["autism", "asd", "autistic"]):
        eligibility["likely_eligible"] = True
        eligibility["diagnosis_notes"] = (
            "Autism Spectrum Disorder is a qualifying condition under the "
            "Lanterman Act. SB 946 also requires commercial insurance to "
            "cover ABA therapy for autism."
        )
    elif any(dx in diagnosis_lower for dx in ["delay", "developmental", "global"]):
        eligibility["likely_eligible"] = True
        eligibility["diagnosis_notes"] = (
            "Developmental delays may qualify for Early Start (under 3) or "
            "Regional Center services if delays are significant. Evaluation needed."
        )
    else:
        eligibility["likely_eligible"] = "Evaluation needed"
        eligibility["diagnosis_notes"] = (
            "Eligibility depends on evaluation findings. Contact your "
            "Regional Center to request an intake assessment."
        )

    # Add Regional Center info if ZIP provided
    if zip_code:
        rc = RegionalCenter.find_by_zip_code(zip_code)
        if rc:
            eligibility["regional_center"] = rc.regional_center

    # Insurance notes
    if insurance:
        insurance_lower = insurance.lower()
        if "medi-cal" in insurance_lower or "medicaid" in insurance_lower:
            eligibility["insurance_notes"] = (
                "Medi-Cal covers many therapy services. Regional Center can "
                "provide services not covered by insurance."
            )
        elif "kaiser" in insurance_lower:
            eligibility["insurance_notes"] = (
                "Kaiser has an in-network ABA provider network. Contact Kaiser "
                "Behavioral Health for referrals. Wait times can be significant."
            )
        else:
            eligibility["insurance_notes"] = (
                "Commercial plans in California must cover autism diagnosis and "
                "ABA therapy under SB 946. Contact your plan for network providers."
            )

    eligibility["next_steps"] = [
        "Call your Regional Center to request an intake assessment",
        "Gather any existing medical/developmental evaluations",
        "Contact your insurance about covered therapy services",
        "Consider private evaluations if wait times are long",
    ]

    return json.dumps(eligibility)


@tool
def list_therapy_types() -> str:
    """
    List all therapy types available in the KiNDD database.

    Use this when users ask what types of services are available
    or need clarification on therapy options.

    Returns:
        List of therapy types with descriptions
    """
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

    return json.dumps({"therapy_types": therapy_info})


# ============================================================================
# KINDD AGENT - Main agent with tools and streaming
# ============================================================================

KINDD_SYSTEM_PROMPT = """You are KiNDD, an expert navigator for neurodevelopmental services in Los Angeles County.

## Your Role
You help families and clinicians find developmental therapy services and navigate the Regional Center system. You have access to a database of real providers in LA County.

## Your Tools
You have tools to:
- **search_providers**: Find providers by therapy type, location, and criteria
- **get_regional_center**: Determine which Regional Center serves a ZIP code
- **get_provider_details**: Get full information about a specific provider
- **check_eligibility**: Assess eligibility for Regional Center services
- **list_therapy_types**: Explain available therapy types

## Guidelines
1. **Use your tools** - Always search the database rather than making up provider names
2. **Be specific** - Cite actual provider names and contact info from search results
3. **Acknowledge limitations** - If data may be outdated, say so
4. **Be empathetic** - Families navigating these systems are often stressed
5. **Give next steps** - Always end with actionable recommendations

## Regional Centers in LA County
- Westside Regional Center (WRC) - Santa Monica area
- Harbor Regional Center (HRC) - Harbor/South Bay area
- South Central Los Angeles Regional Center (SCLARC) - South LA
- Eastern Los Angeles Regional Center (ELARC) - East LA
- North Los Angeles County Regional Center (NLACRC) - SFV, Santa Clarita
- Frank D. Lanterman Regional Center - Glendale, Pasadena area
- San Gabriel/Pomona Regional Center (SGPRC) - SGV, Pomona

When asked about providers, ALWAYS use the search_providers tool first."""


def create_kindd_agent() -> Agent:
    """Create the KiNDD agent with Bedrock and tools."""

    # Use Claude 3.5 Sonnet via Bedrock
    model = BedrockModel(
        model_id="anthropic.claude-3-5-sonnet-20241022-v2:0",
        region_name="us-west-2",
    )

    agent = Agent(
        model=model,
        system_prompt=KINDD_SYSTEM_PROMPT,
        tools=[
            search_providers,
            get_regional_center,
            get_provider_details,
            check_eligibility,
            list_therapy_types,
        ],
    )

    return agent


def chat_with_agent(
    user_message: str,
    user_context: Optional[dict] = None,
    conversation_history: Optional[list] = None,
) -> dict:
    """
    Non-streaming chat with the KiNDD agent.

    Returns full response with tool usage info.
    """
    agent = create_kindd_agent()

    # Enhance message with user context
    if user_context:
        context_parts = []
        if user_context.get("zip_code"):
            context_parts.append(f"User's ZIP: {user_context['zip_code']}")
        if user_context.get("child_age"):
            context_parts.append(f"Child's age: {user_context['child_age']} years")
        if user_context.get("diagnosis"):
            context_parts.append(f"Diagnosis: {user_context['diagnosis']}")
        if user_context.get("insurance"):
            context_parts.append(f"Insurance: {user_context['insurance']}")

        if context_parts:
            user_message = (
                f"[User context: {', '.join(context_parts)}]\n\n{user_message}"
            )

    response = agent(user_message)

    return {
        "answer": str(response),
        "tools_used": [],  # Strands handles this internally
        "regional_center": (
            user_context.get("regional_center") if user_context else None
        ),
    }


async def stream_chat_with_agent(
    user_message: str,
    user_context: Optional[dict] = None,
):
    """
    Streaming chat with the KiNDD agent.

    Yields text chunks as they're generated.
    """
    agent = create_kindd_agent()

    # Enhance message with user context
    if user_context:
        context_parts = []
        if user_context.get("zip_code"):
            context_parts.append(f"User's ZIP: {user_context['zip_code']}")
        if user_context.get("child_age"):
            context_parts.append(f"Child's age: {user_context['child_age']} years")
        if user_context.get("diagnosis"):
            context_parts.append(f"Diagnosis: {user_context['diagnosis']}")
        if user_context.get("insurance"):
            context_parts.append(f"Insurance: {user_context['insurance']}")

        if context_parts:
            user_message = (
                f"[User context: {', '.join(context_parts)}]\n\n{user_message}"
            )

    # Stream the response
    async for event in agent.stream_async(user_message):
        if hasattr(event, "data") and event.data:
            yield event.data
        elif isinstance(event, str):
            yield event
