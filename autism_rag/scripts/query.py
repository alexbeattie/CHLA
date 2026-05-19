"""Query the RAG from the command line."""

from __future__ import annotations

import argparse
import json
import logging
import sys

from ..rag.generation import Answerer
from ..rag.retrieval import RetrievalFilters, Retriever
from ..sources.models import AccessClass, EvidenceType


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Query the autism research RAG.")
    parser.add_argument("question", help="Natural-language question.")
    parser.add_argument("--top-k", type=int, default=None)
    parser.add_argument(
        "--evidence-types",
        nargs="*",
        choices=[e.value for e in EvidenceType],
        default=None,
    )
    parser.add_argument(
        "--access-classes",
        nargs="*",
        choices=[a.value for a in AccessClass],
        default=[AccessClass.PUBLIC_OPEN.value],
    )
    parser.add_argument(
        "--namespaces",
        nargs="*",
        default=None,
        help="Override Pinecone namespaces to search.",
    )
    parser.add_argument(
        "--corpus",
        default=None,
        help=(
            "Search a separated corpus by prefix, for example 'rare_ndd' searches "
            "namespaces like rare_ndd_public_literature."
        ),
    )
    parser.add_argument("--min-year", type=int, default=None)
    parser.add_argument(
        "--no-rerank",
        action="store_true",
        help="Disable Cohere rerank for this query.",
    )
    parser.add_argument(
        "--json",
        action="store_true",
        help="Emit the full structured response as JSON.",
    )
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    logging.basicConfig(level=logging.WARNING, format="%(levelname)s %(name)s: %(message)s")
    args = parse_args(argv)
    retriever = Retriever()
    filters = RetrievalFilters(
        evidence_types=[EvidenceType(v) for v in args.evidence_types] if args.evidence_types else None,
        access_classes=[AccessClass(v) for v in args.access_classes],
        namespaces=args.namespaces,
        namespace_prefix=args.corpus,
        min_year=args.min_year,
    )
    hits = retriever.search(
        args.question,
        top_k=args.top_k,
        filters=filters,
        rerank=not args.no_rerank,
    )
    answerer = Answerer()
    response = answerer.answer(args.question, hits)
    if args.json:
        print(json.dumps(response.to_dict(), indent=2))
    else:
        print(response.answer)
        print("\nCitations:")
        for citation in response.citations:
            ids = ", ".join(f"{k}:{v}" for k, v in citation.citation_ids.items())
            extras = f" ({ids})" if ids else ""
            print(f"  [{citation.label}] {citation.title}{extras} -> {citation.url}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
