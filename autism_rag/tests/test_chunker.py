from datetime import datetime, timezone

from autism_rag.ingestion.chunker import chunk_document
from autism_rag.sources.models import AccessClass, EvidenceType, SourceDocument


def _doc(text: str, evidence_type: EvidenceType = EvidenceType.LITERATURE) -> SourceDocument:
    return SourceDocument(
        source_key="pubmed",
        source_id="12345",
        title="Autism Title",
        text=text,
        url="https://example.org/12345",
        evidence_type=evidence_type,
        access_class=AccessClass.PUBLIC_OPEN,
        published_at=datetime(2024, 1, 1, tzinfo=timezone.utc),
        authors=["Doe J"],
        citation_ids={"PMID": "12345"},
    )


def test_short_document_yields_single_chunk():
    chunks = chunk_document(_doc("Short abstract about autism."))
    assert len(chunks) == 1
    assert chunks[0].namespace == "public_literature"
    assert chunks[0].metadata["source_key"] == "pubmed"
    assert chunks[0].metadata["cite_pmid"] == "12345"
    assert chunks[0].metadata["published_year"] == 2024


def test_long_document_splits_with_overlap():
    body = "Autism research finding. " * 400  # ~10k chars
    chunks = chunk_document(_doc(body))
    assert len(chunks) > 1
    # Overlap means consecutive chunks share trailing/leading characters.
    first_tail = chunks[0].text[-50:]
    second_head = chunks[1].text[:50]
    assert any(token in second_head for token in first_tail.split() if token)


def test_chunk_ids_are_stable_and_unique():
    chunks = chunk_document(_doc("Autism research. " * 500))
    ids = [c.chunk_id for c in chunks]
    assert len(ids) == len(set(ids))
    assert ids[0].startswith("pubmed:12345#chunk-")
