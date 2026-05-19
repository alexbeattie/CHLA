"""Retrieval + answer guardrails.

These checks run on every answer so we surface citation gaps, safety
violations, and access-class leaks. They are intentionally simple string
checks; treat them as fast unit-test-style guards rather than a substitute
for human review.
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field

from ..rag.vectorstore import VectorHit
from ..sources.models import AccessClass


CITATION_PATTERN = re.compile(r"\[S\d+\]")

DEFAULT_FORBIDDEN_PHRASES = [
    "you should take",
    "you should give your child",
    "i recommend that you",
    "stop taking your medication",
]


@dataclass
class CitationCheck:
    ok: bool
    found_labels: list[str] = field(default_factory=list)
    missing_evidence: bool = False


@dataclass
class SafetyCheck:
    ok: bool
    matched_phrases: list[str] = field(default_factory=list)


@dataclass
class SourceClassCheck:
    ok: bool
    leaked_classes: list[str] = field(default_factory=list)


@dataclass
class GuardrailReport:
    citation: CitationCheck
    safety: SafetyCheck
    source_class: SourceClassCheck
    notes: list[str] = field(default_factory=list)

    @property
    def citations_ok(self) -> bool:
        return self.citation.ok

    @property
    def safety_ok(self) -> bool:
        return self.safety.ok

    @property
    def source_class_ok(self) -> bool:
        return self.source_class.ok

    @property
    def all_ok(self) -> bool:
        return self.citation.ok and self.safety.ok and self.source_class.ok


def run_guardrails(
    *,
    question: str,
    answer: str,
    hits: list[VectorHit],
    forbidden_terms: list[str] | None = None,
    allowed_access_classes: list[AccessClass] | None = None,
) -> GuardrailReport:
    citation = check_citations(answer=answer, hits=hits)
    safety = check_safety(answer=answer, forbidden_terms=forbidden_terms)
    source_class = check_source_classes(
        hits=hits, allowed=allowed_access_classes
    )
    notes: list[str] = []
    if not citation.ok:
        notes.append("citation check failed: claim not backed by any [S#] tag")
    if not safety.ok:
        notes.append(
            f"safety check failed: matched phrases={safety.matched_phrases!r}"
        )
    if not source_class.ok:
        notes.append(
            f"source class leak: retrieved restricted classes={source_class.leaked_classes!r}"
        )
    return GuardrailReport(
        citation=citation,
        safety=safety,
        source_class=source_class,
        notes=notes,
    )


def check_citations(*, answer: str, hits: list[VectorHit]) -> CitationCheck:
    if not hits:
        # An empty answer when no hits are found is acceptable.
        return CitationCheck(ok=True)
    found = CITATION_PATTERN.findall(answer)
    if not found:
        return CitationCheck(ok=False, missing_evidence=True)
    return CitationCheck(ok=True, found_labels=sorted(set(found)))


def check_safety(*, answer: str, forbidden_terms: list[str] | None) -> SafetyCheck:
    lowered = answer.lower()
    matched: list[str] = []
    haystacks = list(DEFAULT_FORBIDDEN_PHRASES)
    if forbidden_terms:
        haystacks.extend(t.lower() for t in forbidden_terms)
    for phrase in haystacks:
        if phrase and phrase.lower() in lowered:
            matched.append(phrase)
    return SafetyCheck(ok=not matched, matched_phrases=matched)


def check_source_classes(
    *,
    hits: list[VectorHit],
    allowed: list[AccessClass] | None,
) -> SourceClassCheck:
    allowed_values = (
        {a.value for a in allowed}
        if allowed
        else {
            AccessClass.PUBLIC_OPEN.value,
            AccessClass.PUBLIC_METADATA_ONLY.value,
            AccessClass.CONTROLLED_METADATA.value,
            AccessClass.APPROVED_CONTROLLED.value,
        }
    )
    leaks: list[str] = []
    for hit in hits:
        access_class = hit.metadata.get("access_class")
        if access_class and access_class not in allowed_values:
            leaks.append(access_class)
    return SourceClassCheck(ok=not leaks, leaked_classes=sorted(set(leaks)))
