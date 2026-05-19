"""Run the retrieval/guardrail evaluation suite."""

from __future__ import annotations

import argparse
import json
import logging
import sys

from ..evaluation import RetrievalEvaluator


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Evaluate the autism RAG.")
    parser.add_argument("--top-k", type=int, default=8)
    parser.add_argument("--json", action="store_true")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    logging.basicConfig(level=logging.WARNING, format="%(levelname)s %(name)s: %(message)s")
    args = parse_args(argv)
    evaluator = RetrievalEvaluator()
    reports = evaluator.evaluate(top_k=args.top_k)
    if args.json:
        print(json.dumps([r.to_dict() for r in reports], indent=2))
        return 0
    total = len(reports)
    evidence_ok = sum(1 for r in reports if r.evidence_match)
    cite_ok = sum(1 for r in reports if r.citation_check)
    safety_ok = sum(1 for r in reports if r.safety_check)
    source_ok = sum(1 for r in reports if r.source_class_check)
    print(f"Questions evaluated: {total}")
    print(f"Evidence match     : {evidence_ok}/{total}")
    print(f"Citation check     : {cite_ok}/{total}")
    print(f"Safety check       : {safety_ok}/{total}")
    print(f"Source class check : {source_ok}/{total}")
    print()
    for r in reports:
        flags = []
        if not r.evidence_match:
            flags.append("EVIDENCE")
        if not r.citation_check:
            flags.append("CITATION")
        if not r.safety_check:
            flags.append("SAFETY")
        if not r.source_class_check:
            flags.append("ACCESS")
        flag_str = ",".join(flags) or "ok"
        print(f"- [{flag_str}] {r.question}")
        if r.notes:
            for note in r.notes:
                print(f"    note: {note}")
        if r.answer_preview:
            print(f"    answer: {r.answer_preview}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
