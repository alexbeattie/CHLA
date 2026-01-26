"""
AWS Bedrock integration for KiNDD Resource Navigator.

Uses Claude Sonnet 4.5 (via inference profile) for chat/reasoning and Amazon Titan for embeddings.
All within your AWS Personal account - no external API keys needed.
"""

import json
import boto3
from django.conf import settings
from typing import Optional


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

## Your Expertise
You help families and clinicians find:
- ABA (Applied Behavior Analysis) therapy providers
- Speech-language pathologists (SLPs)
- Occupational therapists (OTs)
- Physical therapists (PTs)
- Developmental pediatricians
- Regional Center services and vendors

## Knowledge Areas
- California Regional Center system (7 centers in LA County: Westside, Harbor, South Central, Eastern, North LA, Frank D. Lanterman, San Gabriel/Pomona)
- Insurance networks: Medi-Cal, Kaiser, Blue Shield, Anthem, United, Aetna, commercial plans
- Age-based eligibility: Early Start (0-3), Lanterman Act services, school-age IEP, adult transition (18+)
- Waitlist realities and alternatives
- SB 946 (California autism insurance mandate)

## Response Guidelines
1. Be specific - cite actual provider names from the data provided
2. Acknowledge data freshness - note when information may be outdated
3. Explain eligibility clearly - break down who qualifies for what
4. Suggest next steps - what should the family do after finding providers?
5. Be empathetic - families navigating these systems are often stressed

When you don't have enough information, say so clearly and suggest what information would help."""

KINDD_SYSTEM_PROMPT_ES = """Eres KiNDD, un navegador experto para servicios de neurodesarrollo en el Condado de Los Ángeles.

IMPORTANTE: Responde siempre en español.

## Tu Experiencia
Ayudas a familias y profesionales a encontrar:
- Proveedores de terapia ABA (Análisis de Comportamiento Aplicado)
- Patólogos del habla y lenguaje (SLPs)
- Terapeutas ocupacionales (OTs)
- Fisioterapeutas (PTs)
- Pediatras del desarrollo
- Servicios y proveedores de Centros Regionales

## Áreas de Conocimiento
- Sistema de Centros Regionales de California (7 centros en el Condado de LA: Westside, Harbor, South Central, Eastern, North LA, Frank D. Lanterman, San Gabriel/Pomona)
- Redes de seguros: Medi-Cal, Kaiser, Blue Shield, Anthem, United, Aetna, planes comerciales
- Elegibilidad por edad: Early Start (0-3), servicios de la Ley Lanterman, IEP escolar, transición adulta (18+)
- Realidades de listas de espera y alternativas
- SB 946 (mandato de seguro de autismo de California)

## Pautas de Respuesta
1. Sé específico - cita nombres reales de proveedores de los datos proporcionados
2. Reconoce la frescura de los datos - nota cuando la información puede estar desactualizada
3. Explica la elegibilidad claramente - desglosa quién califica para qué
4. Sugiere los próximos pasos - ¿qué debe hacer la familia después de encontrar proveedores?
5. Sé empático - las familias que navegan estos sistemas a menudo están estresadas

Cuando no tengas suficiente información, dilo claramente y sugiere qué información ayudaría."""

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

    # Call Claude via Bedrock
    response = client.invoke_model(
        modelId="us.anthropic.claude-sonnet-4-5-20250929-v1:0",  # Claude Sonnet 4.5 inference profile
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
    return result["content"][0]["text"]


def chat_completion_streaming(
    user_message: str,
    system_prompt: str = KINDD_SYSTEM_PROMPT,
    context: Optional[str] = None,
    max_tokens: int = 1500,
):
    """
    Stream a response from Claude Sonnet 4.5.

    Yields chunks of text as they're generated.
    Use for real-time chat UX.
    """
    client = get_bedrock_client()

    if context:
        full_message = f"""CONTEXT:
{context}

USER QUESTION: {user_message}"""
    else:
        full_message = user_message

    response = client.invoke_model_with_response_stream(
        modelId="us.anthropic.claude-sonnet-4-5-20250929-v1:0",  # Claude Sonnet 4.5 inference profile
        contentType="application/json",
        accept="application/json",
        body=json.dumps(
            {
                "anthropic_version": "bedrock-2023-05-31",
                "max_tokens": max_tokens,
                "temperature": 0.3,
                "system": system_prompt,
                "messages": [{"role": "user", "content": full_message}],
            }
        ),
    )

    for event in response["body"]:
        chunk = json.loads(event["chunk"]["bytes"])
        if chunk["type"] == "content_block_delta":
            yield chunk["delta"]["text"]


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
    
    response = client.invoke_model(
        modelId="us.anthropic.claude-sonnet-4-5-20250929-v1:0",
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
    return result["content"][0]["text"]


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
