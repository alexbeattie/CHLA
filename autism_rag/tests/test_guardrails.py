from autism_rag.evaluation.guardrails import run_guardrails
from autism_rag.rag.vectorstore import VectorHit
from autism_rag.sources.models import AccessClass


def _hit(access_class: str = AccessClass.PUBLIC_OPEN.value) -> VectorHit:
    return VectorHit(
        id="pubmed:1#chunk-0",
        score=0.9,
        metadata={"access_class": access_class, "title": "t", "url": "u"},
        text="autism evidence",
    )


def test_citation_check_fails_when_answer_has_no_citation_tag():
    report = run_guardrails(
        question="q",
        answer="Autism prevalence has risen.",
        hits=[_hit()],
    )
    assert not report.citations_ok
    assert "citation check failed" in report.notes[0]


def test_citation_check_passes_when_answer_cites_sources():
    report = run_guardrails(
        question="q",
        answer="Autism prevalence has risen [S1].",
        hits=[_hit()],
    )
    assert report.citations_ok


def test_safety_blocks_individual_treatment_advice():
    report = run_guardrails(
        question="What medication should my child take?",
        answer="You should give your child medication X [S1].",
        hits=[_hit()],
    )
    assert not report.safety_ok


def test_source_class_leak_detected():
    report = run_guardrails(
        question="q",
        answer="Finding [S1].",
        hits=[_hit(access_class=AccessClass.APPROVED_CONTROLLED.value)],
        allowed_access_classes=[AccessClass.PUBLIC_OPEN],
    )
    assert not report.source_class_ok
    assert "approved_controlled" in report.source_class.leaked_classes
