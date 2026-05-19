"""SFARI Gene adapter.

SFARI publishes curated CSV downloads under their terms. The download URL
may change over time, so we accept either a local CSV path or an explicit
URL via kwargs and emit one document per gene with the curated score,
syndromic flag, and evidence summary.
"""

from __future__ import annotations

import csv
import io
import logging
from collections.abc import Iterable
from pathlib import Path

from tenacity import retry, stop_after_attempt, wait_exponential

from ..models import AccessClass, EvidenceType, SourceDocument
from . import _http
from .base import BaseAdapter

logger = logging.getLogger(__name__)


class SFARIGeneAdapter(BaseAdapter):
    source_key = "sfari_gene"

    def fetch(
        self,
        query: str = "",
        *,
        limit: int = 1000,
        csv_path: str | None = None,
        csv_url: str | None = None,
        **_: object,
    ) -> Iterable[SourceDocument]:
        rows = self._load_rows(csv_path=csv_path, csv_url=csv_url)
        emitted = 0
        for row in rows:
            if emitted >= limit:
                break
            doc = self._row_to_document(row)
            if doc is None:
                continue
            if query and query.lower() not in (doc.title.lower() + doc.text.lower()):
                continue
            emitted += 1
            yield doc

    def _load_rows(self, *, csv_path: str | None, csv_url: str | None) -> Iterable[dict[str, str]]:
        if csv_path:
            return self._read_local_csv(Path(csv_path))
        if csv_url:
            return self._read_remote_csv(csv_url)
        logger.warning(
            "SFARI Gene: no csv_path or csv_url supplied. "
            "Download the latest CSV from %s and pass --csv-path.",
            self.source.homepage,
        )
        return []

    def _read_local_csv(self, path: Path) -> list[dict[str, str]]:
        if not path.exists():
            raise FileNotFoundError(f"SFARI Gene CSV not found at {path}")
        with path.open("r", newline="", encoding="utf-8") as fh:
            return list(csv.DictReader(fh))

    @retry(
        wait=wait_exponential(multiplier=1, min=1, max=10),
        stop=stop_after_attempt(3),
        reraise=True,
    )
    def _read_remote_csv(self, url: str) -> list[dict[str, str]]:
        text = _http.get_text(url, timeout=60.0)
        reader = csv.DictReader(io.StringIO(text))
        return list(reader)

    def _row_to_document(self, row: dict[str, str]) -> SourceDocument | None:
        symbol = (row.get("gene-symbol") or row.get("gene_symbol") or row.get("Gene Symbol") or "").strip()
        name = (row.get("gene-name") or row.get("gene_name") or row.get("Gene Name") or "").strip()
        score = (row.get("gene-score") or row.get("gene_score") or row.get("Gene Score") or "").strip()
        syndromic = (row.get("syndromic") or row.get("Syndromic") or "").strip()
        ensembl = (row.get("ensembl-id") or row.get("ensembl_id") or "").strip()
        chromosome = (row.get("chromosome") or row.get("Chromosome") or "").strip()
        if not symbol:
            return None
        text_parts = [
            f"Gene: {symbol}",
            f"Name: {name}" if name else "",
            f"SFARI gene score: {score}" if score else "",
            f"Syndromic: {syndromic}" if syndromic else "",
            f"Chromosome: {chromosome}" if chromosome else "",
            f"Ensembl ID: {ensembl}" if ensembl else "",
        ]
        text = "\n".join(p for p in text_parts if p)
        return SourceDocument(
            source_key=self.source_key,
            source_id=symbol,
            title=f"SFARI Gene: {symbol}",
            text=text,
            url=f"https://gene.sfari.org/database/human-gene/{symbol}",
            evidence_type=EvidenceType.GENE_EVIDENCE,
            access_class=AccessClass.PUBLIC_OPEN,
            citation_ids={"SFARI_GENE": symbol, "ENSEMBL": ensembl} if ensembl else {"SFARI_GENE": symbol},
            extra={"score": score, "syndromic": syndromic, "chromosome": chromosome},
        )
