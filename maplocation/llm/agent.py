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
import hashlib
import json
import logging
import os

from locations.models import ProviderV2, RegionalCenter

from .observability import agent_observation


logger = logging.getLogger(__name__)


def _fingerprint(value: str) -> str:
    """Create a stable, non-reversible identifier for sensitive user text."""
    return hashlib.sha256(value.encode("utf-8")).hexdigest()[:12]


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
def find_provider_location(provider_name: str) -> str:
    """
    Find a provider's map location by name.

    Use this when users ask where a named provider is, where it is located,
    whether it is in a Regional Center area, or when they want to see a
    provider on the map.

    Args:
        provider_name: Name of the provider to locate.

    Returns:
        JSON with provider id, name, address, coordinates, and Regional Center.
    """
    providers = ProviderV2.objects.filter(name__icontains=provider_name).order_by("name")[:5]

    if not providers:
        return json.dumps(
            {
                "found": False,
                "message": f"No provider found with name containing '{provider_name}'",
            }
        )

    results = []
    for provider in providers:
        results.append(
            {
                "id": str(provider.id),
                "name": provider.name,
                "address": provider.address,
                "phone": provider.phone,
                "latitude": float(provider.latitude),
                "longitude": float(provider.longitude),
                "regional_center": provider.regional_center,
                "therapy_types": provider.therapy_types or [],
            }
        )

    best_match = results[0]
    return json.dumps(
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


CLINICAL_ALLOWLIST = [
    "chla.org",
    "aap.org",
    "healthychildren.org",
    "cdc.gov",
    "nih.gov",
    "medlineplus.gov",
    "pubmed.ncbi.nlm.nih.gov",
    "cochranelibrary.com",
]

TRUSTED_WEB_DOMAINS = [
    ".edu",
    ".gov",
    ".org",
    "uclahealth.org",
    "medschool.ucla.edu",
    "dgsom.ucla.edu",
    "chla.org",
]

_tavily_client = None


def _get_tavily_client():
    """Create the Tavily client lazily so agent imports do not require a key."""
    global _tavily_client
    if _tavily_client is None:
        api_key = os.environ.get("TAVILY_API_KEY")
        if not api_key:
            logger.warning("Tavily search disabled: TAVILY_API_KEY is not set.")
            raise RuntimeError("TAVILY_API_KEY is not configured")

        from tavily import TavilyClient

        _tavily_client = TavilyClient(api_key=api_key)
    return _tavily_client


def _format_tavily_response(response: dict) -> str:
    """Return the Tavily response in the compact JSON shape agents expect."""
    return json.dumps(
        {
            "results": [
                {
                    "title": result.get("title"),
                    "url": result.get("url"),
                    "content": result.get("content"),
                }
                for result in response.get("results", [])
            ],
            "answer": response.get("answer"),
        }
    )


def _run_tavily_search(
    *,
    query: str,
    max_results: int,
    search_type: str,
    include_domains: Optional[list[str]] = None,
) -> str:
    """Run Tavily with safe logging and consistent error output."""
    requested_results = max_results
    max_results = min(max(max_results, 1), 10)
    query_fingerprint = _fingerprint(query)

    logger.info(
        "Tavily search requested",
        extra={
            "query_fingerprint": query_fingerprint,
            "query_length": len(query),
            "requested_results": requested_results,
            "max_results": max_results,
            "search_type": search_type,
            "domain_count": len(include_domains or []),
        },
    )

    try:
        client = _get_tavily_client()
        search_kwargs = {
            "query": query,
            "search_depth": "advanced",
            "max_results": max_results,
        }
        if include_domains:
            search_kwargs["include_domains"] = include_domains

        response = client.search(**search_kwargs)
    except Exception as exc:
        logger.warning(
            "Tavily search unavailable",
            extra={
                "query_fingerprint": query_fingerprint,
                "search_type": search_type,
                "error_type": type(exc).__name__,
            },
        )
        return json.dumps(
            {
                "error": f"{search_type}_unavailable",
                "message": str(exc),
                "results": [],
            }
        )

    logger.info(
        "Tavily search completed",
        extra={
            "query_fingerprint": query_fingerprint,
            "search_type": search_type,
            "result_count": len(response.get("results", [])),
            "has_answer": bool(response.get("answer")),
        },
    )
    return _format_tavily_response(response)


@tool
def clinical_search(query: str, max_results: int = 5) -> str:
    """
    Search authoritative pediatric clinical sources.

    Use for questions about medical conditions, treatments, developmental
    concerns, or clinical guidelines where authoritative sources are needed.
    Do NOT use for Regional Center service navigation, eligibility, or
    geographic queries - use the local KiNDD tools for those.

    Args:
        query: Clinical question in natural language.
        max_results: Number of results to return, 1-10. Default 5.

    Returns:
        JSON with results containing title, url, content, and Tavily answer.
    """
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
    """
    Search the Autism Research RAG database.

    Use for autism research evidence: PubMed/PMC literature, ClinicalTrials.gov
    studies, NIH grants, SFARI Gene evidence, SPARK/SFARI/NDA/dbGaP dataset
    metadata, or questions asking what the research says.
    Do NOT use for finding local providers, eligibility, or geographic service
    navigation - use local KiNDD tools for those.

    Args:
        question: Autism research question in natural language.
        evidence_type: Optional filter: literature, clinical_trial, grant,
            gene_evidence, dataset_metadata, or web.
        max_results: Number of retrieved passages to use, 1-10. Default 5.

    Returns:
        JSON with answer, citations, model, and retrieved evidence metadata.
    """
    from llm.autism_research import AutismResearchError, ask_autism_research

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
        return json.dumps(
            {
                "answer": result.get("answer", ""),
                "citations": result.get("citations", []),
                "model": result.get("model"),
            }
        )
    except AutismResearchError as exc:
        return json.dumps(
            {
                "error": "autism_research_unavailable",
                "message": str(exc),
                "answer": "",
                "citations": [],
            }
        )


@tool
def web_search(query: str, max_results: int = 5) -> str:
    """
    Search the web for current facts and non-clinical information.

    Use for current facts, leadership roles, institution-specific facts, dates,
    policies, news, named people, or anything likely to be stale if answered
    from model memory. Prefer official sources such as university, hospital,
    government, or organization pages. Cite the returned source URLs.

    Args:
        query: Web search query in natural language.
        max_results: Number of results to return, 1-10. Default 5.

    Returns:
        JSON with results containing title, url, content, and Tavily answer.
    """
    return _run_tavily_search(
        query=query,
        max_results=max_results,
        search_type="web_search",
    )


# ============================================================================
# KINDD AGENT - Main agent with tools and streaming
# ============================================================================

KINDD_SYSTEM_PROMPT_EN = """You are KiNDD, an expert navigator for neurodevelopmental services in Los Angeles County.

## Your Role
You help families and clinicians find developmental therapy services and navigate the Regional Center system. You have access to a database of real providers in LA County.

## Your Tools
You have tools to:
- **search_providers**: Find providers by therapy type, location, and criteria
- **get_regional_center**: Determine which Regional Center serves a ZIP code
- **get_provider_details**: Get full information about a specific provider
- **find_provider_location**: Find a named provider's address, coordinates, and map target
- **check_eligibility**: Assess eligibility for Regional Center services
- **list_therapy_types**: Explain available therapy types
- **clinical_search**: Search authoritative pediatric clinical sources
- **autism_research**: Search the autism research RAG for literature, trials, NIH grants, SFARI Gene evidence, and dataset metadata
- **web_search**: Search the open web for current facts, institution facts, leadership, policies, dates, and named people

## Guidelines
1. **Use your tools** - Always search the database rather than making up provider names
2. **Be specific** - Cite actual provider names and contact info from search results
3. **Acknowledge limitations** - If data may be outdated, say so
4. **Be empathetic** - Families navigating these systems are often stressed
5. **Give next steps** - Always end with actionable recommendations
6. **Use clinical_search for clinical questions** - For medical conditions, treatments, developmental concerns, or clinical guidelines, search authoritative sources. Do not diagnose or replace a clinician.
7. **Use autism_research for autism evidence questions** - For questions about studies, trials, genes, SFARI scores, NIH grants, SPARK/SFARI/NDA/dbGaP metadata, or "what does research say", call `autism_research` and cite its returned sources.
8. **Use web_search for current facts** - For current leadership, named people, institutional facts, policies, dates, or anything likely to change, search the web and cite official sources. Do not answer current facts from memory.

<reasoning_requirements>
- Use private step-by-step reasoning to understand the user's goal, but do not reveal hidden chain-of-thought. Share only a brief rationale when useful.
- Use a ReAct-style loop internally: decide whether a local KiNDD tool, `clinical_search`, `autism_research`, or `web_search` is needed, call the right tool, observe the result, then answer from that evidence.
- Use self-consistency before finalizing: check that provider names, eligibility statements, next steps, and citations agree with tool results and official sources.
- Prompting approach informed by DAIR.AI Prompt Engineering Guide techniques: Chain-of-Thought, ReAct, and Self-Consistency (https://github.com/dair-ai/Prompt-Engineering-Guide).
</reasoning_requirements>

## Response Format
- Do NOT use markdown headers (`#`, `##`, `###`) because the mobile app renders inline Markdown.
- Use bold section labels instead, such as **Quick answer**, **What to watch for**, **What to do next**, **Local help**, and **Sources**.
- Keep paragraphs short. Most paragraphs should be 1-3 sentences.
- Use simple Markdown hyphen bullets for signs, options, and provider details.
- Use numbered lists for steps the family can take.
- Do not use raw HTML, complex tables, footnotes, horizontal rules, or decorative formatting.
- Keep each section to 3 sentences or fewer unless the user asks for detail.
- For important safety caveats, deadlines, or limitations, use a plain bold label:
  `**Note:**`. Do not use blockquotes or a leading `>`.
- If you use `clinical_search`, `autism_research`, or `web_search`, include a **Sources** section with the returned source titles and URLs.
- If you mention CDC, AAP, HealthyChildren.org, NIH, MedlinePlus, CHLA, or another clinical authority, include its URL in **Sources**.
- Do not invent citations. Use URLs returned by `clinical_search` or `web_search` whenever available.
- In **Sources**, write each source as `Source name: URL` on its own line. Do not prefix sources with hyphens.
- Do not add a horizontal rule before **Sources**; the mobile app styles sources separately.
- For general rare-disorder, syndrome, resource-navigation, or family-support questions, keep the tone conversational. Prefer **Quick answer**, **What to know**, **What to do next**, and **Local help**.
- Do not use **Evidence**, **Interpretation**, or **Limitations** as section labels unless the user explicitly asks for research evidence, studies, trials, gene evidence, SFARI scores, NIH grants, or datasets.
- When provider search results are available, show the best 4 when possible. Include each provider's name, city or neighborhood, full address, and phone if present so map and directions actions can target real locations.
- Do a final style pass before answering: tighten prose, keep list items parallel, and remove filler.

Default answer shape:
Direct answer in one short paragraph.

**What this may mean**
- **Point one:** Clear explanation in one or two sentences.
- **Point two:** Clear explanation in one or two sentences.

**What to do next**
- Practical next step.
- Suggest speaking with a clinician when appropriate.

## Regional Centers in LA County
- Westside Regional Center (WRC) - Santa Monica area
- Harbor Regional Center (HRC) - Harbor/South Bay area
- South Central Los Angeles Regional Center (SCLARC) - South LA
- Eastern Los Angeles Regional Center (ELARC) - East LA
- North Los Angeles County Regional Center (NLACRC) - SFV, Santa Clarita
- Frank D. Lanterman Regional Center - Glendale, Pasadena area
- San Gabriel/Pomona Regional Center (SGPRC) - SGV, Pomona

When asked about providers, Regional Centers, service eligibility, or geography, ALWAYS use the local KiNDD tools first."""

KINDD_SYSTEM_PROMPT_ES = """Eres KiNDD, un navegador experto para servicios de neurodesarrollo en el Condado de Los Ángeles.

IMPORTANTE: Responde siempre en español.

## Tu Rol
Ayudas a familias y profesionales a encontrar servicios de terapia del desarrollo y navegar el sistema de Centros Regionales. Tienes acceso a una base de datos de proveedores reales en el Condado de LA.

## Tus Herramientas
Tienes herramientas para:
- **search_providers**: Encontrar proveedores por tipo de terapia, ubicación y criterios
- **get_regional_center**: Determinar qué Centro Regional sirve un código postal
- **get_provider_details**: Obtener información completa sobre un proveedor específico
- **find_provider_location**: Encontrar la dirección, coordenadas y destino del mapa de un proveedor
- **check_eligibility**: Evaluar elegibilidad para servicios del Centro Regional
- **list_therapy_types**: Explicar los tipos de terapia disponibles
- **clinical_search**: Buscar fuentes clínicas pediátricas autorizadas
- **autism_research**: Buscar en la base RAG de investigación sobre autismo literatura, ensayos, subvenciones NIH, evidencia genética de SFARI y metadatos de datasets
- **web_search**: Buscar en la web datos actuales, instituciones, liderazgo, políticas, fechas y personas nombradas

## Pautas
1. **Usa tus herramientas** - Siempre busca en la base de datos en lugar de inventar nombres de proveedores
2. **Sé específico** - Cita nombres reales de proveedores e información de contacto de los resultados de búsqueda
3. **Reconoce limitaciones** - Si los datos pueden estar desactualizados, dilo
4. **Sé empático** - Las familias que navegan estos sistemas a menudo están estresadas
5. **Da los próximos pasos** - Siempre termina con recomendaciones accionables
6. **Usa clinical_search para preguntas clínicas** - Para condiciones médicas, tratamientos, preocupaciones del desarrollo o guías clínicas, busca fuentes autorizadas. No diagnostiques ni reemplaces a un profesional clínico.
7. **Usa autism_research para preguntas de evidencia sobre autismo** - Para estudios, ensayos, genes, puntajes de SFARI, subvenciones NIH, metadatos SPARK/SFARI/NDA/dbGaP o "qué dice la investigación", llama `autism_research` y cita sus fuentes.
8. **Usa web_search para datos actuales** - Para liderazgo actual, personas nombradas, datos institucionales, políticas, fechas o información que pueda cambiar, busca en la web y cita fuentes oficiales. No respondas datos actuales de memoria.

<requisitos_de_razonamiento>
- Usa razonamiento privado paso a paso para entender el objetivo del usuario, pero no reveles la cadena de pensamiento oculta. Comparte solo una razón breve cuando sea útil.
- Usa internamente un ciclo tipo ReAct: decide si hace falta una herramienta local de KiNDD, `clinical_search`, `autism_research` o `web_search`, llama la herramienta correcta, observa el resultado y responde desde esa evidencia.
- Usa una comprobación de consistencia antes de finalizar: confirma que nombres de proveedores, elegibilidad, próximos pasos y citas coincidan con resultados de herramientas y fuentes oficiales.
- Enfoque de prompting informado por las técnicas de DAIR.AI Prompt Engineering Guide: Chain-of-Thought, ReAct y Self-Consistency (https://github.com/dair-ai/Prompt-Engineering-Guide).
</requisitos_de_razonamiento>

## Formato de Respuesta
- NO uses encabezados markdown (`#`, `##`, `###`) porque la app móvil renderiza Markdown en línea.
- Usa etiquetas de sección en negrita, como **Respuesta rápida**, **Señales a observar**, **Qué hacer ahora**, **Ayuda local** y **Fuentes**.
- Mantén los párrafos breves. La mayoría debe tener 1-3 frases.
- Usa el símbolo de viñeta `•` para señales, opciones y detalles de proveedores. No uses guiones como viñetas.
- Usa listas numeradas para pasos que la familia puede seguir.
- Mantén cada sección en 3 frases o menos salvo que el usuario pida más detalle.
- Para advertencias de seguridad, fechas límite o limitaciones importantes, usa una etiqueta simple en negritas:
  `**Nota:**`. No uses citas en bloque ni el símbolo inicial `>`.
- Si usas `clinical_search`, `autism_research` o `web_search`, incluye una sección **Fuentes** con los títulos y URLs devueltos.
- Si mencionas CDC, AAP, HealthyChildren.org, NIH, MedlinePlus, CHLA u otra autoridad clínica, incluye su URL en **Fuentes**.
- No inventes citas. Usa las URLs devueltas por `clinical_search` o `web_search` siempre que estén disponibles.
- En **Fuentes**, escribe cada fuente como `Nombre de la fuente: URL` en su propia línea. No uses guiones antes de las fuentes.
- No agregues una línea horizontal antes de **Fuentes**; la app móvil da estilo a las fuentes por separado.
- Cuando haya resultados de proveedores, muestra los mejores 4 cuando sea posible. Incluye el nombre, ciudad o vecindario, dirección completa y teléfono si existe para que las acciones de mapa e indicaciones apunten a ubicaciones reales.
- Haz una revisión final de estilo antes de responder: ajusta la prosa, mantén las listas paralelas y elimina relleno.

## Centros Regionales en el Condado de LA
- Westside Regional Center (WRC) - área de Santa Monica
- Harbor Regional Center (HRC) - área de Harbor/South Bay
- South Central Los Angeles Regional Center (SCLARC) - South LA
- Eastern Los Angeles Regional Center (ELARC) - East LA
- North Los Angeles County Regional Center (NLACRC) - SFV, Santa Clarita
- Frank D. Lanterman Regional Center - área de Glendale, Pasadena
- San Gabriel/Pomona Regional Center (SGPRC) - SGV, Pomona

Cuando te pregunten sobre proveedores, Centros Regionales, elegibilidad de servicios o geografía, SIEMPRE usa primero las herramientas locales de KiNDD."""

# Default for backwards compatibility
KINDD_SYSTEM_PROMPT = KINDD_SYSTEM_PROMPT_EN


def get_agent_system_prompt_for_locale(locale: str = "en") -> str:
    """Get the agent system prompt for the given locale."""
    if locale.startswith("es"):
        return KINDD_SYSTEM_PROMPT_ES
    return KINDD_SYSTEM_PROMPT_EN


def create_kindd_agent(locale: str = "en") -> Agent:
    """Create the KiNDD agent with Bedrock and tools.
    
    Args:
        locale: Language code (e.g., "en", "es") for response language
    """

    # Use Claude Sonnet 4.5 via Bedrock inference profile
    model = BedrockModel(
        model_id="us.anthropic.claude-sonnet-4-5-20250929-v1:0",
        region_name="us-west-2",
    )

    # Get locale-specific system prompt
    system_prompt = get_agent_system_prompt_for_locale(locale)

    agent = Agent(
        model=model,
        system_prompt=system_prompt,
        tools=[
            search_providers,
            get_regional_center,
            get_provider_details,
            find_provider_location,
            check_eligibility,
            list_therapy_types,
            clinical_search,
            autism_research,
            web_search,
        ],
    )

    return agent


def build_agent_message(user_message: str, user_context: Optional[dict] = None) -> str:
    """Add structured user context to the agent prompt when provided."""
    if not user_context:
        return user_message

    context_parts = []
    if user_context.get("zip_code"):
        context_parts.append(f"User's ZIP: {user_context['zip_code']}")
    if user_context.get("child_age"):
        context_parts.append(f"Child's age: {user_context['child_age']} years")
    if user_context.get("diagnosis"):
        context_parts.append(f"Diagnosis: {user_context['diagnosis']}")
    if user_context.get("insurance"):
        context_parts.append(f"Insurance: {user_context['insurance']}")
    if user_context.get("memory_context"):
        context_parts.append(f"History: {user_context['memory_context']}")

    if not context_parts:
        return user_message

    return f"[User context: {', '.join(context_parts)}]\n\n{user_message}"


def chat_with_agent(
    user_message: str,
    user_context: Optional[dict] = None,
    conversation_history: Optional[list] = None,
    locale: str = "en",
    user_id: Optional[str] = None,
    session_id: Optional[str] = None,
    feature: str = "chla",
) -> dict:
    """
    Non-streaming chat with the KiNDD agent.

    Args:
        user_message: The user's message
        user_context: Optional context (zip_code, child_age, etc.)
        conversation_history: Optional previous messages
        locale: Language code (e.g., "en", "es") for response language
        user_id: Optional stable user identifier for Langfuse traces
        session_id: Optional session/conversation id for Langfuse traces
        feature: Feature tag for Langfuse traces

    Returns full response with tool usage info.
    """
    agent = create_kindd_agent(locale=locale)
    enhanced_message = build_agent_message(user_message, user_context)
    query_fingerprint = _fingerprint(user_message)

    logger.info(
        "KiNDD agent request started",
        extra={
            "query_fingerprint": query_fingerprint,
            "locale": locale,
            "feature": feature,
            "streaming": False,
            "has_user_context": bool(user_context),
            "has_conversation_history": bool(conversation_history),
        },
    )

    with agent_observation(
        query=user_message,
        locale=locale,
        user_id=user_id,
        session_id=session_id,
        feature=feature,
        streaming=False,
        has_conversation_history=bool(conversation_history),
    ) as observation:
        try:
            response = agent(enhanced_message)
            response_text = str(response)
            if observation:
                observation.update(output={"response": response_text})
        except Exception:
            logger.exception(
                "KiNDD agent request failed",
                extra={
                    "query_fingerprint": query_fingerprint,
                    "locale": locale,
                    "feature": feature,
                    "streaming": False,
                },
            )
            raise

    logger.info(
        "KiNDD agent request completed",
        extra={
            "query_fingerprint": query_fingerprint,
            "locale": locale,
            "feature": feature,
            "streaming": False,
            "response_length": len(response_text),
        },
    )

    return {
        "answer": response_text,
        "tools_used": [],  # Strands handles this internally
        "regional_center": (
            user_context.get("regional_center") if user_context else None
        ),
    }


async def stream_chat_with_agent(
    user_message: str,
    user_context: Optional[dict] = None,
    locale: str = "en",
    user_id: Optional[str] = None,
    session_id: Optional[str] = None,
    feature: str = "chla",
):
    """
    Streaming chat with the KiNDD agent.

    Yields text chunks as they're generated.
    """
    agent = create_kindd_agent(locale=locale)
    enhanced_message = build_agent_message(user_message, user_context)
    chunks = []
    query_fingerprint = _fingerprint(user_message)

    logger.info(
        "KiNDD agent request started",
        extra={
            "query_fingerprint": query_fingerprint,
            "locale": locale,
            "feature": feature,
            "streaming": True,
            "has_user_context": bool(user_context),
        },
    )

    # Stream the response
    with agent_observation(
        query=user_message,
        locale=locale,
        user_id=user_id,
        session_id=session_id,
        feature=feature,
        streaming=True,
    ) as observation:
        try:
            async for event in agent.stream_async(enhanced_message):
                if hasattr(event, "data") and event.data:
                    chunks.append(event.data)
                    yield event.data
                elif isinstance(event, str):
                    chunks.append(event)
                    yield event

            if observation:
                observation.update(output={"response": "".join(chunks)})
        except Exception:
            logger.exception(
                "KiNDD agent request failed",
                extra={
                    "query_fingerprint": query_fingerprint,
                    "locale": locale,
                    "feature": feature,
                    "streaming": True,
                },
            )
            raise

    logger.info(
        "KiNDD agent request completed",
        extra={
            "query_fingerprint": query_fingerprint,
            "locale": locale,
            "feature": feature,
            "streaming": True,
            "response_length": len("".join(chunks)),
        },
    )
