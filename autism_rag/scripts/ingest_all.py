"""Run a broad ingestion across multiple public sources."""

from __future__ import annotations

import argparse
import json
import logging
import sys

from ..ingestion import IngestionPipeline
from ..sources.adapters import get_adapter


DEFAULT_TARGETS = [
    ("pubmed", "autism spectrum disorder", 50),
    ("clinicaltrials", "autism", 50),
    ("nih_reporter", "autism", 50),
    ("openalex", "autism spectrum disorder", 50),
]


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Ingest a broad starter corpus.")
    parser.add_argument("--limit", type=int, default=50, help="Per-source limit.")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    logging.basicConfig(level=logging.INFO, format="%(levelname)s %(name)s: %(message)s")
    args = parse_args(argv)
    pipeline = IngestionPipeline()
    summary = []
    for source_key, query, default_limit in DEFAULT_TARGETS:
        adapter = get_adapter(source_key)()
        result = pipeline.run(adapter, query=query, limit=min(args.limit, default_limit))
        summary.append(
            {
                "source": source_key,
                "query": query,
                "documents": result.documents,
                "chunks": result.chunks,
                "upserts_by_namespace": result.upserts_by_namespace,
            }
        )
    print(json.dumps(summary, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
