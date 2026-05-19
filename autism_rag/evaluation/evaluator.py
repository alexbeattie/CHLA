"""Retrieval evaluation harness.

Runs each :class:`EvalQuestion` against the retriever and scores whether
the retrieved evidence types match what we expect, and whether the answer
respects citation and safety rules.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any

from ..rag.generation import Answerer
from ..rag.retrieval import RetrievalFilters, Retriever
from ..sources.models import AccessClass
from .dataset import EVAL_QUESTIONS, EvalQuestion
from .guardrails import GuardrailReport, run_guardrails


@dataclass
class RetrievalReport:
    question: str
    hits_returned: int
    evidence_types_seen: list[str] = field(default_factory=list)
    evidence_match: bool = False
    citation_check: bool = False
    safety_check: bool = True
    source_class_check: bool = True
    notes: list[str] = field(default_factory=list)
    answer_preview: str = ""

    def to_dict(self) -> dict[str, Any]:
        return self.__dict__


class RetrievalEvaluator:
    def __init__(
        self,
        *,
        retriever: Retriever | None = None,
        answerer: Answerer | None = None,
    ) -> None:
        self.retriever = retriever or Retriever()
        self.answerer = answerer or Answerer()

    def evaluate(
        self,
        questions: list[EvalQuestion] | None = None,
        *,
        top_k: int = 8,
    ) -> list[RetrievalReport]:
        questions = questions or EVAL_QUESTIONS
        reports: list[RetrievalReport] = []
        for question in questions:
            report = self._evaluate_one(question, top_k=top_k)
            reports.append(report)
        return reports

    def _evaluate_one(self, q: EvalQuestion, *, top_k: int) -> RetrievalReport:
        filters = RetrievalFilters(
            evidence_types=q.expected_evidence_types or None,
            access_classes=[AccessClass.PUBLIC_OPEN, AccessClass.CONTROLLED_METADATA],
        )
        hits = self.retriever.search(q.question, top_k=top_k, filters=filters)
        evidence_types_seen = sorted(
            {hit.metadata.get("evidence_type", "") for hit in hits if hit.metadata.get("evidence_type")}
        )
        evidence_match = (
            not q.expected_evidence_types
            or any(et.value in evidence_types_seen for et in q.expected_evidence_types)
        )
        response = self.answerer.answer(q.question, hits)
        guardrail: GuardrailReport = run_guardrails(
            question=q.question,
            answer=response.answer,
            hits=hits,
            forbidden_terms=q.forbidden_terms,
        )
        return RetrievalReport(
            question=q.question,
            hits_returned=len(hits),
            evidence_types_seen=evidence_types_seen,
            evidence_match=evidence_match,
            citation_check=guardrail.citations_ok,
            safety_check=guardrail.safety_ok,
            source_class_check=guardrail.source_class_ok,
            notes=guardrail.notes,
            answer_preview=response.answer[:280],
        )
