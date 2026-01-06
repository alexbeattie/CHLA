"""
Embedding pipeline for providers.

Run this to generate/update embeddings for all providers.
"""

from django.db import connection
from locations.models import ProviderV2
from .bedrock import generate_embedding


def build_provider_text(provider: ProviderV2) -> str:
    """
    Build a rich text representation of a provider for embedding.

    The quality of this text directly affects search quality.
    Include everything relevant for semantic matching.
    """

    parts = [
        f"Provider: {provider.name}",
    ]

    if provider.therapy_types:
        parts.append(f"Services: {', '.join(provider.therapy_types)}")

    if provider.age_groups:
        parts.append(f"Age groups: {', '.join(provider.age_groups)}")

    if provider.diagnoses_treated:
        parts.append(f"Specialties: {provider.diagnoses_treated}")

    if provider.description:
        parts.append(f"Description: {provider.description}")

    if provider.address:
        parts.append(f"Location: {provider.address}")

    if hasattr(provider, "regional_center") and provider.regional_center:
        parts.append(f"Regional Center: {provider.regional_center}")

    if hasattr(provider, "insurance_networks") and provider.insurance_networks:
        parts.append(f"Insurance accepted: {', '.join(provider.insurance_networks)}")

    return "\n".join(parts)


def embed_provider(provider: ProviderV2) -> list[float]:
    """Generate embedding for a single provider."""
    text = build_provider_text(provider)
    return generate_embedding(text)


def embed_all_providers(batch_size: int = 50, force: bool = False):
    """
    Generate embeddings for all providers.

    Args:
        batch_size: Process this many before progress update
        force: If True, re-embed even if already exists
    """

    if force:
        queryset = ProviderV2.objects.all()
    else:
        queryset = ProviderV2.objects.filter(embedding__isnull=True)

    total = queryset.count()
    print(f"📊 Embedding {total} providers...")

    if total == 0:
        print("✅ All providers already have embeddings!")
        return

    processed = 0
    errors = 0

    for provider in queryset.iterator():
        try:
            embedding = embed_provider(provider)

            # Update using raw SQL since Django's vector field support varies
            with connection.cursor() as cursor:
                cursor.execute(
                    """
                    UPDATE locations_providerv2 
                    SET embedding = %s::vector 
                    WHERE id = %s
                    """,
                    [embedding, provider.id],
                )

            processed += 1

            if processed % batch_size == 0:
                print(f"  Progress: {processed}/{total} ({processed*100//total}%)")

        except Exception as e:
            errors += 1
            print(f"  ❌ Error embedding {provider.name}: {e}")

    print(f"✅ Done! Embedded {processed} providers, {errors} errors")


def test_embedding_search(query: str):
    """
    Test semantic search with a query.
    Shows top 5 results with similarity scores.
    """

    query_embedding = generate_embedding(query)

    with connection.cursor() as cursor:
        cursor.execute(
            """
            SELECT id, name, 1 - (embedding <=> %s::vector) as similarity
            FROM locations_providerv2
            WHERE embedding IS NOT NULL
            ORDER BY embedding <=> %s::vector
            LIMIT 5
        """,
            [query_embedding, query_embedding],
        )

        results = cursor.fetchall()

    print(f"\n🔍 Query: {query}")
    print("-" * 50)

    for id, name, similarity in results:
        print(f"  {similarity:.3f} | {name}")

    return results


# Management command wrapper
def run():
    """Entry point for `python manage.py runscript embeddings`"""
    embed_all_providers()
