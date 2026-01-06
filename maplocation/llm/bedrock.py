"""
AWS Bedrock integration for KiNDD Resource Navigator.

Uses Claude 3.5 Sonnet for chat/reasoning and Amazon Titan for embeddings.
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
# CHAT/REASONING - Claude 3.5 Sonnet
# ============================================================================

KINDD_SYSTEM_PROMPT = """You are KiNDD, an expert navigator for neurodevelopmental services in Los Angeles County.

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


def chat_completion(
    user_message: str,
    system_prompt: str = KINDD_SYSTEM_PROMPT,
    context: Optional[str] = None,
    conversation_history: Optional[list] = None,
    max_tokens: int = 1500,
    temperature: float = 0.3,
) -> str:
    """
    Generate a response using Claude 3.5 Sonnet via Bedrock.

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
        modelId="anthropic.claude-sonnet-4-20250514-v1:0",  # Claude Sonnet 4 (latest)
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
    Stream a response from Claude 3.5 Sonnet.

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
        modelId="anthropic.claude-sonnet-4-20250514-v1:0",  # Claude Sonnet 4
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
        print(f"✅ Claude 3.5 Sonnet working - Response: {response[:100]}...")

        return True
    except Exception as e:
        print(f"❌ Error: {e}")
        return False
