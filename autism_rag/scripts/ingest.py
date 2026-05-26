r"""Run ingestion for a single source.

Usage:
    python3 -m autism_rag.scripts.ingest --source pubmed \\
        --query "autism spectrum disorder early screening" --limit 50
"""

from __future__ import annotations

import argparse
import json
import logging
import sys

from ..ingestion import IngestionPipeline
from ..ingestion.chunker import chunk_document
from ..sources.adapters import ADAPTERS, get_adapter

logger = logging.getLogger(__name__)


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Ingest from a single source.")
    parser.add_argument(
        "--source",
        required=True,
        choices=sorted(ADAPTERS),
        help="Source key to ingest from.",
    )
    parser.add_argument("--query", default="autism spectrum disorder", help="Search query.")
    parser.add_argument("--limit", type=int, default=25, help="Max documents to fetch.")
    parser.add_argument(
        "--csv-path",
        default=None,
        help="Local CSV path (used by sfari_gene).",
    )
    parser.add_argument(
        "--csv-url",
        default=None,
        help="Remote CSV URL (used by sfari_gene).",
    )
    parser.add_argument(
        "--urls",
        nargs="*",
        default=None,
        help="Explicit URLs (used by firecrawl_web).",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Fetch and chunk documents without embedding or upserting.",
    )
    parser.add_argument(
        "--corpus",
        default=None,
        help=(
            "Optional corpus label. When set, chunks are upserted into prefixed "
            "namespaces such as '<corpus>_public_literature' and tagged with corpus metadata."
        ),
    )
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    logging.basicConfig(level=logging.INFO, format="%(levelname)s %(name)s: %(message)s")
    args = parse_args(argv)
    adapter_cls = get_adapter(args.source)
    adapter = adapter_cls()
    extra: dict = {}
    if args.csv_path:
        extra["csv_path"] = args.csv_path
    if args.csv_url:
        extra["csv_url"] = args.csv_url
    if args.urls:
        extra["urls"] = args.urls

    if args.dry_run:
        docs = list(adapter.fetch(query=args.query, limit=args.limit, **extra))
        print(f"Fetched {len(docs)} documents")
        for doc in docs[:3]:
            chunks = chunk_document(doc)
            print(f"- {doc.source_id}: {doc.title[:80]} ({len(chunks)} chunks)")
        return 0

    pipeline = IngestionPipeline()
    metadata_tags = {"corpus": args.corpus} if args.corpus else None
    result = pipeline.run(
        adapter,
        query=args.query,
        limit=args.limit,
        namespace_prefix=args.corpus,
        metadata_tags=metadata_tags,
        **extra,
    )
    print(
        json.dumps(
            {
                "source": result.source_key,
                "documents": result.documents,
                "chunks": result.chunks,
                "upserts_by_namespace": result.upserts_by_namespace,
                "sample_ids": result.sample_ids,
            },
            indent=2,
        )
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
