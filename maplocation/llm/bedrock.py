"""
AWS Bedrock integration for KiNDD Resource Navigator.

Uses Claude Sonnet 4.5 (via inference profile) for chat/reasoning and Amazon Titan for embeddings.
All within your AWS Personal account - no external API keys needed.
"""

import json
import boto3
from django.conf import settings
from typing import Optional

from .observability import bedrock_call_monitor


CHAT_MODEL_ID = "us.anthropic.claude-sonnet-4-5-20250929-v1:0"


# Initialize Bedrock client
def get_bedrock_client():
    """Get Bedrock runtime client using AWS credentials."""
    return boto3.client(
        "bedrock-runtime",
        region_name=getattr(settings, "AWS_REGION", "us-west-2"),
    )


def get_bedrock_agent_client():
    """Get Bedrock agent client for knowledge bases."""
    return boto3.client(
        "bedrock-agent-runtime",
        region_name=getattr(settings, "AWS_REGION", "us-west-2"),
    )


# ============================================================================
# EMBEDDINGS - Amazon Titan
# ============================================================================


def generate_embedding(text: str) -> list[float]:
    """
    Generate embedding using Amazon Titan Embeddings V2.

    Titan Embeddings G1 - Text: 1536 dimensions
    Titan Embeddings V2: 1024 dimensions (default), configurable

    Cost: ~$0.00002 per 1K tokens (very cheap)
    """
    client = get_bedrock_client()

    response = client.invoke_model(
        modelId="amazon.titan-embed-text-v2:0",
        contentType="application/json",
        accept="application/json",
        body=json.dumps(
            {
                "inputText": text,
                "dimensions": 1024,  # Can be 256, 512, or 1024
                "normalize": True,
            }
        ),
    )

    result = json.loads(response["body"].read())
    return result["embedding"]


def generate_embeddings_batch(texts: list[str]) -> list[list[float]]:
    """Generate embeddings for multiple texts."""
    return [generate_embedding(text) for text in texts]


# ============================================================================
# CHAT/REASONING - Claude Sonnet 4.5 (via inference profile)
# ============================================================================

KINDD_SYSTEM_PROMPT_EN = """You are KiNDD, an expert navigator for neurodevelopmental services in Los Angeles County.

Your Expertise:
You help families and clinicians find ABA therapy providers, speech-language pathologists, occupational therapists, physical therapists, developmental pediatricians, and Regional Center services.

Knowledge Areas:
- California Regional Center system (7 centers in LA County: Westside, Harbor, South Central, Eastern, North LA, Frank D. Lanterman, San Gabriel/Pomona)
- Insurance networks: Medi-Cal, Kaiser, Blue Shield, Anthem, United, Aetna, commercial plans
- Age-based eligibility: Early Start (0-3), Lanterman Act services, school-age IEP, adult transition (18+)
- SB 946 (California autism insurance mandate)

<reasoning_requirements>
- Use private step-by-step reasoning to understand the family's need, but do not reveal hidden chain-of-thought. Share only a brief rationale when it helps the user.
- Use a ReAct-style pattern internally: decide what facts are needed, use available context, then answer grounded in those facts.
- Before finalizing, do a self-consistency check: make sure the answer, next steps, and citations do not conflict with the provided context or official sources.
- Prompting approach informed by DAIR.AI Prompt Engineering Guide techniques: Chain-of-Thought, ReAct, and Self-Consistency (https://github.com/dair-ai/Prompt-Engineering-Guide).
</reasoning_requirements>

Response Guidelines:
1. Be specific - cite actual provider names when available
2. Explain eligibility clearly
3. Suggest concrete next steps
4. Be warm and empathetic
5. Keep answers practical and easy to scan on a phone
6. If the context includes WEB SEARCH RESULTS, use them to answer current or institution-specific facts and include a **Sources** section with the URLs. Do not refuse as out of scope when web results answer the question.

<format_requirements>
- Do NOT use hashtags (#) or markdown headers (##, ###)
- Use **bold labels** for section labels and key terms
- Use numbered lists (1. 2. 3.) for steps
- Use simple Markdown hyphen bullets for lists
- Keep responses conversational and easy to read
- Do not use raw HTML, complex tables, footnotes, or decorative formatting
- Do not use horizontal rules
- Keep each section to 3 sentences or fewer unless the user asks for detail.
- For important safety caveats, deadlines, or limitations, use a plain bold label:
  `**Note:**` in English or `**Nota:**` in Spanish. Do not use blockquotes or a leading `>`.
- If you reference clinical authorities like the CDC, AAP, HealthyChildren.org,
  NIH, MedlinePlus, or CHLA, include a **Sources** section with clickable URLs.
- Only include source URLs that are official and high-confidence. Do not invent
  article-specific URLs.
- Useful official source links:
  - CDC Autism: https://www.cdc.gov/autism/
  - CDC Learn the Signs. Act Early.: https://www.cdc.gov/ncbddd/actearly/
  - HealthyChildren.org Autism: https://www.healthychildren.org/English/health-issues/conditions/Autism/
  - MedlinePlus Autism Spectrum Disorder: https://medlineplus.gov/autismspectrumdisorder.html
  - CHLA: https://www.chla.org/
- Prefer this structure:
  Direct answer in one short paragraph.

  **What this may mean**
  - **Point one:** Clear explanation in one or two sentences.
  - **Point two:** Clear explanation in one or two sentences.
  - **Point three:** Clear explanation in one or two sentences.

  **What to do next**
  - Concrete next step.
  - Suggest speaking with a clinician when appropriate.

  **Local help**
  Mention the relevant Regional Center or provider options only when helpful.

  **Sources**
  Source name: https://official-url.example
  Source name: https://official-url.example
- For general rare-disorder, syndrome, resource-navigation, or family-support questions, keep the tone conversational. Prefer **Quick answer**, **What to know**, **What to do next**, and **Local help**.
- Do not use **Evidence**, **Interpretation**, or **Limitations** as section labels unless the user explicitly asks for research evidence, studies, trials, gene evidence, SFARI scores, NIH grants, or datasets.
- Avoid long paragraphs. Keep most paragraphs to 1-3 sentences.
- For provider recommendations, share the best 4 matches when available. Include each provider's name, city or neighborhood, full address, and phone if present so map and directions actions can target real locations.
- Do a final style pass before answering: tighten prose, keep list items parallel, and remove filler.
</format_requirements>

When you don't have enough information, say so clearly and ask follow-up questions."""

KINDD_SYSTEM_PROMPT_ES = """Eres KiNDD, un navegador experto para servicios de neurodesarrollo en el Condado de Los Ángeles.

IMPORTANTE: Responde siempre en español.

Tu Experiencia:
Ayudas a familias y profesionales a encontrar proveedores de terapia ABA, patólogos del habla, terapeutas ocupacionales, fisioterapeutas, pediatras del desarrollo y servicios de Centros Regionales.

Áreas de Conocimiento:
- Sistema de Centros Regionales de California (7 centros en el Condado de LA)
- Redes de seguros: Medi-Cal, Kaiser, Blue Shield, Anthem, United, Aetna
- Elegibilidad por edad: Early Start (0-3), Ley Lanterman, IEP escolar, transición adulta (18+)
- SB 946 (mandato de seguro de autismo de California)

<requisitos_de_razonamiento>
- Usa razonamiento privado paso a paso para entender la necesidad de la familia, pero no reveles la cadena de pensamiento oculta. Comparte solo una razón breve cuando ayude al usuario.
- Usa internamente un patrón tipo ReAct: decide qué datos hacen falta, usa el contexto disponible y responde con base en esos datos.
- Antes de finalizar, haz una comprobación de consistencia: confirma que la respuesta, los próximos pasos y las citas no contradigan el contexto ni fuentes oficiales.
- Enfoque de prompting informado por las técnicas de DAIR.AI Prompt Engineering Guide: Chain-of-Thought, ReAct y Self-Consistency (https://github.com/dair-ai/Prompt-Engineering-Guide).
</requisitos_de_razonamiento>

Pautas de Respuesta:
1. Sé específico - cita nombres reales de proveedores cuando estén disponibles
2. Explica la elegibilidad claramente
3. Sugiere los próximos pasos concretos
4. Sé cálido y empático
5. Mantén las respuestas prácticas y fáciles de leer en un teléfono
6. Si el contexto incluye WEB SEARCH RESULTS, úsalos para responder datos actuales o institucionales e incluye una sección **Fuentes** con las URLs. No rechaces la pregunta como fuera de alcance cuando los resultados web la respondan.

<requisitos_de_formato>
- NO uses hashtags (#) ni encabezados markdown (##, ###)
- Usa **negritas** para enfatizar términos clave
- Usa listas numeradas (1. 2. 3.) para pasos
- Usa el símbolo de viñeta `•` para listas de elementos. No uses guiones como viñetas.
- Mantén las respuestas conversacionales y fáciles de leer
- Mantén cada sección en 3 frases o menos salvo que el usuario pida más detalle.
- Para advertencias de seguridad, fechas límite o limitaciones importantes, usa una etiqueta simple en negritas:
  `**Nota:**`. No uses citas en bloque ni el símbolo inicial `>`.
- Si mencionas autoridades clínicas como CDC, AAP, HealthyChildren.org, NIH,
  MedlinePlus o CHLA, incluye una sección **Fuentes** con URLs clicables.
- Incluye solo URLs oficiales y de alta confianza. No inventes URLs específicas
  de artículos.
- Enlaces oficiales útiles:
  - CDC Autism: https://www.cdc.gov/autism/
  - CDC Learn the Signs. Act Early.: https://www.cdc.gov/ncbddd/actearly/
  - HealthyChildren.org Autism: https://www.healthychildren.org/English/health-issues/conditions/Autism/
  - MedlinePlus Autism Spectrum Disorder: https://medlineplus.gov/autismspectrumdisorder.html
  - CHLA: https://www.chla.org/
- Prefiere esta estructura:
  **Respuesta rápida**
  Una o dos frases en lenguaje sencillo.

  **Señales a observar**
  • Viñeta breve
  • Viñeta breve

  **Qué hacer ahora**
  1. Próximo paso concreto
  2. Próximo paso concreto

  **Ayuda local**
  Menciona el Centro Regional o proveedores relevantes solo cuando ayude.

  **Fuentes**
  Nombre de la fuente: https://url-oficial.example
  Nombre de la fuente: https://url-oficial.example
- No agregues una línea horizontal antes de **Fuentes**; la app móvil da estilo a las fuentes por separado.
- Evita párrafos largos. Mantén la mayoría de los párrafos en 1-3 frases.
- Para recomendaciones de proveedores, comparte los mejores 4 cuando estén disponibles. Incluye el nombre, ciudad o vecindario, dirección completa y teléfono si existe para que las acciones de mapa e indicaciones apunten a ubicaciones reales.
- Haz una revisión final de estilo antes de responder: ajusta la prosa, mantén las listas paralelas y elimina relleno.
</requisitos_de_formato>

Cuando no tengas suficiente información, dilo claramente y haz preguntas de seguimiento."""

# Default to English for backwards compatibility
KINDD_SYSTEM_PROMPT = KINDD_SYSTEM_PROMPT_EN


def get_system_prompt_for_locale(locale: str = "en") -> str:
    """Get the system prompt for the given locale."""
    if locale.startswith("es"):
        return KINDD_SYSTEM_PROMPT_ES
    return KINDD_SYSTEM_PROMPT_EN


def chat_completion(
    user_message: str,
    system_prompt: str = KINDD_SYSTEM_PROMPT,
    context: Optional[str] = None,
    conversation_history: Optional[list] = None,
    max_tokens: int = 1500,
    temperature: float = 0.3,
) -> str:
    """
    Generate a response using Claude Sonnet 4.5 via Bedrock inference profile.

    Args:
        user_message: The user's question
        system_prompt: System instructions (defaults to KiNDD prompt)
        context: Additional context (e.g., provider data, user info)
        conversation_history: Previous messages for multi-turn chat
        max_tokens: Maximum response length
        temperature: Creativity (0=focused, 1=creative)

    Returns:
        The assistant's response text
    """
    client = get_bedrock_client()

    # Build messages array
    messages = []

    # Add conversation history if provided
    if conversation_history:
        messages.extend(conversation_history)

    # Build the current user message with context
    if context:
        full_message = f"""CONTEXT (use this to answer):
{context}

USER QUESTION: {user_message}"""
    else:
        full_message = user_message

    messages.append({"role": "user", "content": full_message})

    with bedrock_call_monitor(
        operation="chat_completion",
        model_id=CHAT_MODEL_ID,
        prompt=full_message,
    ) as monitor:
        # Call Claude via Bedrock
        response = client.invoke_model(
            modelId=CHAT_MODEL_ID,  # Claude Sonnet 4.5 inference profile
            contentType="application/json",
            accept="application/json",
            body=json.dumps(
                {
                    "anthropic_version": "bedrock-2023-05-31",
                    "max_tokens": max_tokens,
                    "temperature": temperature,
                    "system": system_prompt,
                    "messages": messages,
                }
            ),
        )

        result = json.loads(response["body"].read())
        text = result["content"][0]["text"]
        monitor["usage"] = result.get("usage", {})
        monitor["output_text"] = text
        return text


def chat_completion_streaming(
    user_message: str,
    system_prompt: str = KINDD_SYSTEM_PROMPT,
    context: Optional[str] = None,
    conversation_history: Optional[list] = None,
    max_tokens: int = 1500,
):
    """
    Stream a response from Claude Sonnet 4.5.

    Yields chunks of text as they're generated.
    Use for real-time chat UX.
    """
    client = get_bedrock_client()

    messages = []

    if conversation_history:
        messages.extend(conversation_history)

    if context:
        full_message = f"""CONTEXT:
{context}

USER QUESTION: {user_message}"""
    else:
        full_message = user_message

    messages.append({"role": "user", "content": full_message})

    with bedrock_call_monitor(
        operation="chat_completion_streaming",
        model_id=CHAT_MODEL_ID,
        prompt=full_message,
    ) as monitor:
        response = client.invoke_model_with_response_stream(
            modelId=CHAT_MODEL_ID,
            contentType="application/json",
            accept="application/json",
            body=json.dumps(
                {
                    "anthropic_version": "bedrock-2023-05-31",
                    "max_tokens": max_tokens,
                    "temperature": 0.3,
                    "system": system_prompt,
                    "messages": messages,
                }
            ),
        )

        output_chunks = []
        usage = {}
        for event in response["body"]:
            chunk = json.loads(event["chunk"]["bytes"])
            chunk_type = chunk.get("type")
            if chunk_type == "message_start":
                usage.update(chunk.get("message", {}).get("usage", {}))
            elif chunk_type == "message_delta":
                usage.update(chunk.get("usage", {}))
            elif chunk_type == "content_block_delta":
                text = chunk["delta"]["text"]
                output_chunks.append(text)
                monitor["output_text"] = "".join(output_chunks)
                monitor["usage"] = usage
                yield text
        monitor["output_text"] = "".join(output_chunks)
        monitor["usage"] = usage


# ============================================================================
# VISION - Image Analysis with Claude Sonnet 4.5
# ============================================================================


INSURANCE_CARD_PROMPT = """Analyze this insurance card image and extract the following information:

1. **Insurance Company/Plan Name**
2. **Member ID / Subscriber ID**
3. **Group Number** (if visible)
4. **Plan Type** (HMO, PPO, Medi-Cal, etc.)
5. **Effective Date** (if visible)
6. **Any relevant phone numbers** (member services, claims)

If any information is not visible or unclear, indicate that.

After extracting the info, briefly explain:
- Whether this insurance typically covers ABA therapy, speech therapy, or OT
- Any special considerations for neurodevelopmental services in California
"""

DOCUMENT_ANALYSIS_PROMPT = """Analyze this document image. 

Identify what type of document it is (IEP, Regional Center letter, medical report, etc.) and summarize the key information relevant to a family seeking neurodevelopmental services.

Focus on:
- Services mentioned or approved
- Eligibility determinations
- Important dates or deadlines
- Action items for the family
"""


def analyze_image(
    image_base64: str,
    prompt: str = "What do you see in this image?",
    media_type: str = "image/jpeg",
    max_tokens: int = 1500,
) -> str:
    """
    Analyze an image using Claude Sonnet 4.5's vision capabilities.
    
    Args:
        image_base64: Base64-encoded image data
        prompt: Question or instruction about the image
        media_type: MIME type (image/jpeg, image/png, image/gif, image/webp)
        max_tokens: Maximum response length
    
    Returns:
        Claude's analysis of the image
    """
    client = get_bedrock_client()
    
    messages = [{
        "role": "user",
        "content": [
            {
                "type": "image",
                "source": {
                    "type": "base64",
                    "media_type": media_type,
                    "data": image_base64,
                }
            },
            {
                "type": "text",
                "text": prompt
            }
        ]
    }]
    
    with bedrock_call_monitor(
        operation="image_analysis",
        model_id=CHAT_MODEL_ID,
        prompt=prompt,
    ) as monitor:
        response = client.invoke_model(
            modelId=CHAT_MODEL_ID,
            contentType="application/json",
            accept="application/json",
            body=json.dumps({
                "anthropic_version": "bedrock-2023-05-31",
                "max_tokens": max_tokens,
                "temperature": 0.2,  # Low temp for accurate extraction
                "system": KINDD_SYSTEM_PROMPT,
                "messages": messages,
            }),
        )

        result = json.loads(response["body"].read())
        text = result["content"][0]["text"]
        monitor["usage"] = result.get("usage", {})
        monitor["output_text"] = text
        return text


def analyze_insurance_card(image_base64: str, media_type: str = "image/jpeg") -> str:
    """Analyze an insurance card image and extract relevant information."""
    return analyze_image(image_base64, INSURANCE_CARD_PROMPT, media_type)


def analyze_document(image_base64: str, media_type: str = "image/jpeg") -> str:
    """Analyze a document image (IEP, Regional Center letter, etc.)."""
    return analyze_image(image_base64, DOCUMENT_ANALYSIS_PROMPT, media_type)


# ============================================================================
# UTILITY FUNCTIONS
# ============================================================================


def list_available_models():
    """List foundation models available in your Bedrock account."""
    client = boto3.client("bedrock", region_name="us-west-2")

    response = client.list_foundation_models(
        byProvider="anthropic"  # or "amazon", "meta", "cohere"
    )

    return [
        {"id": m["modelId"], "name": m["modelName"], "provider": m["providerName"]}
        for m in response["modelSummaries"]
    ]


def test_connection():
    """Test Bedrock connectivity."""
    try:
        # Test embedding
        embedding = generate_embedding("Hello, world!")
        print(f"✅ Titan Embeddings working - {len(embedding)} dimensions")

        # Test Claude
        response = chat_completion("Say 'Hello from KiNDD!' in one sentence.")
        print(f"✅ Claude Sonnet 4.5 working - Response: {response[:100]}...")

        return True
    except Exception as e:
        print(f"❌ Error: {e}")
        return False
