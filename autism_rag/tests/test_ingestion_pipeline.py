from collections.abc import Iterable
from datetime import datetime, timezone

from autism_rag.config import Settings
from autism_rag.ingestion import IngestionPipeline
from autism_rag.rag.embeddings import EmbeddingProvider, InputType
from autism_rag.rag.vectorstore import VectorHit, VectorRecord, VectorStore
from autism_rag.sources.adapters import BaseAdapter
from autism_rag.sources.models import AccessClass, EvidenceType, SourceDocument


class FakeAdapter(BaseAdapter):
    source_key = "pubmed"

    def fetch(self, query: str, *, limit: int = 25, **kwargs) -> Iterable[SourceDocument]:
        return [
            SourceDocument(
                source_key=self.source_key,
                source_id="123",
                title="Rare NDD finding",
                text="Rare diseases and neurodevelopmental disorders overlap.",
                url="https://example.org/rare-ndd",
                evidence_type=EvidenceType.LITERATURE,
                access_class=AccessClass.PUBLIC_OPEN,
                published_at=datetime(2024, 1, 1, tzinfo=timezone.utc),
                citation_ids={"PMID": "123"},
            )
        ]


class FakeEmbedder(EmbeddingProvider):
    @property
    def model_name(self) -> str:
        return "fake-embedder"

    @property
    def dimension(self) -> int:
        return 3

    def embed(self, texts: list[str], *, input_type: InputType) -> list[list[float]]:
        return [[1.0, 0.0, 0.0] for _ in texts]


class FakeVectorStore(VectorStore):
    def __init__(self) -> None:
        self.upserts: dict[str, list[VectorRecord]] = {}
        self.deletes: list[tuple[str, dict]] = []
        self.dimension: int | None = None
        self.metric: str | None = None

    def ensure_index(self, *, dimension: int, metric: str = "cosine") -> None:
        self.dimension = dimension
        self.metric = metric

    def upsert(self, records: list[VectorRecord], *, namespace: str) -> None:
        self.upserts.setdefault(namespace, []).extend(records)

    def delete(self, *, namespace: str, metadata_filter: dict) -> None:
        self.deletes.append((namespace, metadata_filter))

    def query(
        self,
        *,
        vector: list[float],
        namespace: str | None,
        top_k: int,
        metadata_filter: dict | None = None,
        include_namespaces: list[str] | None = None,
    ) -> list[VectorHit]:
        return []

    def describe(self) -> dict:
        return {}


def test_pipeline_can_prefix_namespace_for_separate_corpus():
    store = FakeVectorStore()
    pipeline = IngestionPipeline(
        embedder=FakeEmbedder(),
        vector_store=store,
        settings=Settings(),
    )

    result = pipeline.run(
        FakeAdapter(),
        query="rare disease neurodevelopmental disorder",
        limit=1,
        namespace_prefix="rare_ndd",
        metadata_tags={"corpus": "rare_ndd", "search_terms": ["rare diseases", "rare disorders"]},
        save_raw=False,
    )

    assert result.upserts_by_namespace == {"rare_ndd_public_literature": 1}
    record = store.upserts["rare_ndd_public_literature"][0]
    assert record.metadata["corpus"] == "rare_ndd"
    assert record.metadata["base_namespace"] == "public_literature"
    assert record.metadata["namespace_prefix"] == "rare_ndd"
    assert record.metadata["search_terms"] == ["rare diseases", "rare disorders"]


def test_pipeline_can_replace_existing_source_records():
    store = FakeVectorStore()
    pipeline = IngestionPipeline(
        embedder=FakeEmbedder(),
        vector_store=store,
        settings=Settings(),
    )

    pipeline.run(
        FakeAdapter(),
        query="rare disease neurodevelopmental disorder",
        limit=1,
        namespace_prefix="rare_ndd",
        replace_source=True,
        save_raw=False,
    )

    assert store.deletes == [
        ("rare_ndd_public_literature", {"source_key": {"$eq": "pubmed"}})
    ]
