"""Answer generation with strict citation requirements.

We prompt the model to:
  - cite every supporting claim with the bracketed source id we hand it
  - separate evidence from interpretation
  - disclose clinical / data limitations
  - decline diagnosis or treatment recommendations

The model is configurable. When no answer model is reachable (e.g., no
``OPENAI_API_KEY``), the answerer falls back to a deterministic extractive
summary so retrieval can still be inspected end-to-end.
"""

from __future__ import annotations

import logging
from dataclasses import dataclass, field
from typing import Any

from ...config import Settings, get_settings
from ..vectorstore import VectorHit

logger = logging.getLogger(__name__)


SYSTEM_PROMPT = """You are an autism research assistant.
You answer questions using ONLY the provided sources. Rules:
- Cite every claim with the bracketed source id given in the context, e.g. [S1].
- If the sources do not support a claim, say so explicitly. Do not speculate.
- Distinguish evidence (what the sources say) from interpretation (what it implies).
- Flag clinical limitations and uncertainty.
- Do NOT provide diagnostic conclusions, treatment instructions, or medical advice for individuals.
- Preserve study type when relevant (RCT, observational, review, animal model, in vitro, etc.).
"""


@dataclass
class Citation:
    label: str
    title: str
    url: str
    source_key: str
    citation_ids: dict[str, str] = field(default_factory=dict)
    evidence_type: str = ""
    published_year: int | None = None

    def to_dict(self) -> dict[str, Any]:
        return {
            "label": self.label,
            "title": self.title,
            "url": self.url,
            "source": self.source_key,
            "evidence_type": self.evidence_type,
            "published_year": self.published_year,
            "ids": self.citation_ids,
        }


@dataclass
class AnswerResponse:
    answer: str
    citations: list[Citation]
    used_model: str
    retrieval: list[VectorHit]

    def to_dict(self) -> dict[str, Any]:
        return {
            "answer": self.answer,
            "citations": [c.to_dict() for c in self.citations],
            "model": self.used_model,
            "retrieval": [
                {
                    "id": hit.id,
                    "score": hit.score,
                    "metadata": hit.metadata,
                }
                for hit in self.retrieval
            ],
        }


def build_context(hits: list[VectorHit]) -> tuple[str, list[Citation]]:
    """Render retrieval hits as numbered context blocks for the prompt."""

    blocks: list[str] = []
    citations: list[Citation] = []
    for index, hit in enumerate(hits, start=1):
        label = f"S{index}"
        metadata = hit.metadata or {}
        title = metadata.get("title") or hit.id
        url = metadata.get("url", "")
        text = hit.text or metadata.get("text", "")
        if not text:
            continue
        published_year = metadata.get("published_year")
        evidence_type = metadata.get("evidence_type", "")
        access_class = metadata.get("access_class", "")
        citation_ids = {
            k.removeprefix("cite_").upper(): v
            for k, v in metadata.items()
            if isinstance(k, str) and k.startswith("cite_") and isinstance(v, str)
        }
        header = f"[{label}] {title}"
        details = []
        if published_year:
            details.append(str(published_year))
        if evidence_type:
            details.append(evidence_type)
        if access_class and access_class != "public_open":
            details.append(f"access:{access_class}")
        if details:
            header += f" ({', '.join(details)})"
        if url:
            header += f"\nURL: {url}"
        blocks.append(f"{header}\n{text}")
        citations.append(
            Citation(
                label=label,
                title=str(title),
                url=str(url),
                source_key=str(metadata.get("source_key", "")),
                citation_ids=citation_ids,
                evidence_type=str(evidence_type),
                published_year=int(published_year) if isinstance(published_year, int) else None,
            )
        )
    return "\n\n---\n\n".join(blocks), citations


class Answerer:
    def __init__(self, settings: Settings | None = None) -> None:
        self.settings = settings or get_settings()

    def answer(self, question: str, hits: list[VectorHit]) -> AnswerResponse:
        context, citations = build_context(hits)
        if not citations:
            return AnswerResponse(
                answer=(
                    "No supporting passages were retrieved. Try a different query "
                    "or widen the source/access filters."
                ),
                citations=[],
                used_model="none",
                retrieval=hits,
            )
        if self.settings.openai_api_key:
            return self._answer_with_openai(question, context, citations, hits)
        return self._extractive_fallback(question, citations, hits)

    def _answer_with_openai(
        self,
        question: str,
        context: str,
        citations: list[Citation],
        hits: list[VectorHit],
    ) -> AnswerResponse:
        try:
            from openai import OpenAI  # noqa: WPS433

            client = OpenAI(api_key=self.settings.openai_api_key)
            user_prompt = (
                "Question:\n"
                f"{question}\n\n"
                "Sources (cite by bracketed label):\n"
                f"{context}\n\n"
                "Write a concise answer that:\n"
                "1. Cites every claim with [S#].\n"
                "2. Separates 'Evidence' from 'Interpretation' sections.\n"
                "3. Notes limitations (e.g., study type, sample size, recency).\n"
                "4. Refuses any individual diagnostic or treatment advice."
            )
            response = client.chat.completions.create(
                model=self.settings.answer_model,
                messages=[
                    {"role": "system", "content": SYSTEM_PROMPT},
                    {"role": "user", "content": user_prompt},
                ],
                temperature=0.1,
            )
            text = response.choices[0].message.content or ""
            return AnswerResponse(
                answer=text.strip(),
                citations=citations,
                used_model=self.settings.answer_model,
                retrieval=hits,
            )
        except Exception as exc:  # pragma: no cover - external service guard
            logger.warning("OpenAI generation failed (%s); falling back to extractive.", exc)
            return self._extractive_fallback(question, citations, hits)

    def _extractive_fallback(
        self,
        question: str,
        citations: list[Citation],
        hits: list[VectorHit],
    ) -> AnswerResponse:
        bullets: list[str] = []
        for citation, hit in zip(citations, hits, strict=False):
            snippet = (hit.text or hit.metadata.get("text", ""))[:400].strip().replace("\n", " ")
            if not snippet:
                continue
            bullets.append(f"- [{citation.label}] {snippet}")
        answer = (
            "No generation model is configured (set OPENAI_API_KEY to enable). "
            "Returning extractive snippets from the top retrieved passages:\n\n"
            + "\n".join(bullets)
        )
        return AnswerResponse(
            answer=answer,
            citations=citations,
            used_model="extractive-fallback",
            retrieval=hits,
        )
