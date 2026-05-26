"""Document chunker.

We use a deliberately simple character-based chunker with overlap. It's
good enough for abstracts, trial records, grant abstracts, and gene
evidence rows, and it's predictable to debug. Switch to a sentence-aware
chunker only if eval surfaces broken spans on long full-text docs.
"""

from __future__ import annotations

from ..sources.models import DocumentChunk, EvidenceType, SourceDocument
from ..sources.registry import get_source


DEFAULT_CHUNK_CHARS = 1800
DEFAULT_OVERLAP_CHARS = 200

CHUNK_PROFILES: dict[EvidenceType, tuple[int, int]] = {
    EvidenceType.LITERATURE: (1800, 200),
    EvidenceType.CLINICAL_TRIAL: (2400, 200),
    EvidenceType.GRANT: (1600, 150),
    EvidenceType.GENE_EVIDENCE: (1200, 100),
    EvidenceType.DATASET_METADATA: (1600, 150),
    EvidenceType.WEB: (1800, 200),
}


def chunk_document(doc: SourceDocument) -> list[DocumentChunk]:
    """Split ``doc`` into chunks ready for embedding/upsert."""

    source = get_source(doc.source_key)
    chunk_chars, overlap = CHUNK_PROFILES.get(
        doc.evidence_type, (DEFAULT_CHUNK_CHARS, DEFAULT_OVERLAP_CHARS)
    )

    full_text = f"{doc.title}\n\n{doc.text}".strip() if doc.title else doc.text
    pieces = _split_with_overlap(full_text, chunk_chars=chunk_chars, overlap=overlap)
    chunks: list[DocumentChunk] = []
    for position, piece in enumerate(pieces):
        metadata = _build_metadata(doc, position=position, total=len(pieces))
        chunk_id = f"{doc.stable_id()}#chunk-{position}"
        chunks.append(
            DocumentChunk(
                chunk_id=chunk_id,
                source_key=doc.source_key,
                source_id=doc.source_id,
                namespace=source.pinecone_namespace,
                text=piece,
                position=position,
                metadata=metadata,
            )
        )
    return chunks


def _split_with_overlap(text: str, *, chunk_chars: int, overlap: int) -> list[str]:
    text = text.strip()
    if not text:
        return []
    if len(text) <= chunk_chars:
        return [text]
    pieces: list[str] = []
    start = 0
    while start < len(text):
        end = min(start + chunk_chars, len(text))
        pieces.append(text[start:end].strip())
        if end >= len(text):
            break
        start = end - overlap
        if start < 0:
            start = 0
    return [p for p in pieces if p]


def _build_metadata(doc: SourceDocument, *, position: int, total: int) -> dict:
    metadata: dict = {
        "source_key": doc.source_key,
        "source_id": doc.source_id,
        "title": doc.title[:400],
        "url": doc.url,
        "evidence_type": doc.evidence_type.value,
        "access_class": doc.access_class.value,
        "position": position,
        "total_chunks": total,
        "retrieved_at": doc.retrieved_at.isoformat(),
    }
    if doc.published_at is not None:
        metadata["published_year"] = doc.published_at.year
        metadata["published_at"] = doc.published_at.isoformat()
    if doc.authors:
        # Pinecone metadata has a per-key size limit, so cap aggressively.
        metadata["authors"] = doc.authors[:10]
    for key, value in doc.citation_ids.items():
        if value:
            metadata[f"cite_{key.lower()}"] = value
    # Carry select adapter-specific fields if they are scalar/list of scalars.
    for key, value in doc.extra.items():
        if isinstance(value, (str, int, float, bool)):
            metadata[f"extra_{key}"] = value
        elif isinstance(value, list) and all(isinstance(v, (str, int, float, bool)) for v in value):
            metadata[f"extra_{key}"] = value[:10]
    return metadata
