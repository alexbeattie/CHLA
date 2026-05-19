"""Ingest a separated rare disease / NDD corpus.

The API sources run curated queries around rare diseases, rare disorders, and
neurodevelopmental disorders. Web pages are intentionally opt-in: pass explicit
URLs that are permitted to scrape with ``--web-url``.
"""

from __future__ import annotations

import argparse
import json
import logging
import sys
from pathlib import Path
from typing import Any

from ..ingestion import IngestionPipeline
from ..ingestion.chunker import chunk_document
from ..sources.adapters import get_adapter

RARE_NDD_CORPUS = "rare_ndd"
RARE_NDD_TOPIC = "rare_disease_neurodevelopmental_disorders"
RARE_NDD_TERMS = [
    "rare diseases",
    "rare disorders",
    "neurodevelopmental disorders",
    "NDD",
]

DEFAULT_TARGETS: list[tuple[str, str, int]] = [
    (
        "pubmed",
        '("rare diseases" OR "rare disorders") AND ("neurodevelopmental disorders" OR NDD OR autism)',
        50,
    ),
    (
        "openalex",
        "rare diseases rare disorders neurodevelopmental disorders NDD autism",
        50,
    ),
    (
        "nih_reporter",
        "rare diseases neurodevelopmental disorders",
        50,
    ),
    (
        "clinicaltrials",
        "rare disease neurodevelopmental disorder",
        50,
    ),
]

SOURCE_KEYS = [source_key for source_key, _, _ in DEFAULT_TARGETS]


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Ingest rare disease / rare disorder NDD material into a separated corpus."
    )
    parser.add_argument("--limit", type=int, default=50, help="Per-source API limit.")
    parser.add_argument(
        "--corpus",
        default=RARE_NDD_CORPUS,
        help="Corpus namespace prefix for upserts, for example rare_ndd or nord_rare_diseases.",
    )
    parser.add_argument(
        "--topic",
        default=RARE_NDD_TOPIC,
        help="Flat topic metadata tag applied to ingested chunks.",
    )
    parser.add_argument(
        "--web-url",
        action="append",
        default=[],
        help=(
            "Explicit permitted URL to scrape into the rare NDD web namespace. "
            "Repeat for multiple URLs."
        ),
    )
    parser.add_argument(
        "--url-file",
        default=None,
        help="File containing one explicit permitted web URL per line.",
    )
    parser.add_argument(
        "--checkpoint-file",
        default=None,
        help=(
            "Optional file of completed URLs. When provided with --url-file, "
            "successful URLs are appended so interrupted runs can resume."
        ),
    )
    parser.add_argument(
        "--max-urls",
        type=int,
        default=None,
        help="Maximum number of pending URLs to ingest from --url-file in this run.",
    )
    parser.add_argument(
        "--skip-apis",
        action="store_true",
        help="Only ingest the explicit web URLs.",
    )
    parser.add_argument(
        "--source",
        action="append",
        choices=SOURCE_KEYS,
        default=[],
        help="Restrict API ingestion to one source. Repeat for multiple sources.",
    )
    parser.add_argument(
        "--clinicaltrials-location",
        default=None,
        help="Optional ClinicalTrials.gov location query, for example 'Los Angeles, CA'.",
    )
    parser.add_argument(
        "--replace-source",
        action="store_true",
        help="Delete existing vectors for each ingested source in the target namespace before upserting.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Fetch and chunk without embedding or writing to Pinecone.",
    )
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    logging.basicConfig(level=logging.INFO, format="%(levelname)s %(name)s: %(message)s")
    args = parse_args(argv)
    pipeline = None if args.dry_run else IngestionPipeline()
    metadata_tags = {
        "corpus": args.corpus,
        "topic": args.topic,
        "search_terms": RARE_NDD_TERMS,
    }
    summary: list[dict[str, Any]] = []

    if not args.skip_apis:
        selected_sources = set(args.source)
        for source_key, query, default_limit in DEFAULT_TARGETS:
            if selected_sources and source_key not in selected_sources:
                continue
            target_kwargs: dict[str, Any] = {}
            if source_key == "clinicaltrials" and args.clinicaltrials_location:
                target_kwargs["location"] = args.clinicaltrials_location
            summary.append(
                _run_target(
                    pipeline=pipeline,
                    source_key=source_key,
                    query=query,
                    limit=min(args.limit, default_limit),
                    namespace_prefix=args.corpus,
                    metadata_tags=metadata_tags,
                    dry_run=args.dry_run,
                    replace_source=args.replace_source,
                    **target_kwargs,
                )
            )

    urls = _load_urls(args.web_url, args.url_file)
    if urls:
        if args.url_file and args.checkpoint_file:
            summary.extend(
                _run_checkpointed_urls(
                    pipeline=pipeline,
                    urls=urls,
                    checkpoint_file=Path(args.checkpoint_file),
                    max_urls=args.max_urls,
                    namespace_prefix=args.corpus,
                    metadata_tags=metadata_tags,
                    dry_run=args.dry_run,
                    replace_source=args.replace_source,
                )
            )
        else:
            summary.append(
                _run_target(
                    pipeline=pipeline,
                    source_key="firecrawl_web",
                    query="rare disease neurodevelopmental disorder",
                    limit=len(urls),
                    namespace_prefix=args.corpus,
                    metadata_tags=metadata_tags,
                    dry_run=args.dry_run,
                    replace_source=args.replace_source,
                    urls=urls,
                )
            )

    print(json.dumps(summary, indent=2))
    return 0


def _run_target(
    *,
    pipeline: IngestionPipeline | None,
    source_key: str,
    query: str,
    limit: int,
    namespace_prefix: str,
    metadata_tags: dict[str, Any],
    dry_run: bool,
    replace_source: bool,
    **adapter_kwargs: Any,
) -> dict[str, Any]:
    adapter = get_adapter(source_key)()
    if dry_run:
        docs = list(adapter.fetch(query=query, limit=limit, **adapter_kwargs))
        chunks = [chunk for doc in docs for chunk in chunk_document(doc)]
        return {
            "source": source_key,
            "query": query,
            "documents": len(docs),
            "chunks": len(chunks),
            "dry_run": True,
        }

    if pipeline is None:
        raise RuntimeError("Pipeline is required when dry_run is false.")

    result = pipeline.run(
        adapter,
        query=query,
        limit=limit,
        namespace_prefix=namespace_prefix,
        metadata_tags=metadata_tags,
        replace_source=replace_source,
        **adapter_kwargs,
    )
    return {
        "source": source_key,
        "query": query,
        "documents": result.documents,
        "chunks": result.chunks,
        "upserts_by_namespace": result.upserts_by_namespace,
    }


def _load_urls(cli_urls: list[str], url_file: str | None) -> list[str]:
    urls = [url.strip() for url in cli_urls if url.strip()]
    if url_file:
        urls.extend(
            line.strip()
            for line in Path(url_file).read_text(encoding="utf-8").splitlines()
            if line.strip() and not line.strip().startswith("#")
        )
    return list(dict.fromkeys(urls))


def _run_checkpointed_urls(
    *,
    pipeline: IngestionPipeline | None,
    urls: list[str],
    checkpoint_file: Path,
    max_urls: int | None,
    namespace_prefix: str,
    metadata_tags: dict[str, Any],
    dry_run: bool,
    replace_source: bool,
) -> list[dict[str, Any]]:
    completed = _read_checkpoint(checkpoint_file)
    pending = [url for url in urls if url not in completed]
    if max_urls is not None:
        pending = pending[:max_urls]

    summary: list[dict[str, Any]] = []
    for index, url in enumerate(pending, start=1):
        print(f"Ingesting URL {index}/{len(pending)}: {url}", flush=True)
        item = _run_target(
            pipeline=pipeline,
            source_key="firecrawl_web",
            query="rare disease neurodevelopmental disorder",
            limit=1,
            namespace_prefix=namespace_prefix,
            metadata_tags=metadata_tags,
            dry_run=dry_run,
            replace_source=replace_source,
            urls=[url],
        )
        item["url"] = url
        summary.append(item)
        if not dry_run and item["documents"] > 0 and item["chunks"] > 0:
            _append_checkpoint(checkpoint_file, url)
    return summary


def _read_checkpoint(path: Path) -> set[str]:
    if not path.exists():
        return set()
    return {
        line.strip()
        for line in path.read_text(encoding="utf-8").splitlines()
        if line.strip() and not line.strip().startswith("#")
    }


def _append_checkpoint(path: Path, url: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("a", encoding="utf-8") as handle:
        handle.write(f"{url}\n")


if __name__ == "__main__":
    sys.exit(main())
