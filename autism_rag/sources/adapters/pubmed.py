"""PubMed adapter via NCBI E-utilities.

We fetch PMIDs via esearch, then abstracts and metadata via efetch with
``retmode=xml``. Only abstracts are stored here. Full-text retrieval is the
job of :class:`PMCOpenAccessAdapter` once we extend the pipeline to PMC OA.
"""

from __future__ import annotations

import logging
from collections.abc import Iterable
from datetime import datetime, timezone

from tenacity import retry, stop_after_attempt, wait_exponential

from ..models import AccessClass, EvidenceType, SourceDocument
from . import _http
from .base import BaseAdapter

logger = logging.getLogger(__name__)

EUTILS_BASE = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils"


class PubMedAdapter(BaseAdapter):
    source_key = "pubmed"

    def fetch(self, query: str, *, limit: int = 25, **_: object) -> Iterable[SourceDocument]:
        pmids = self._esearch(query, limit=limit)
        if not pmids:
            logger.info("PubMed: no PMIDs returned for query %r", query)
            return []
        return self._efetch(pmids)

    @retry(
        wait=wait_exponential(multiplier=1, min=1, max=10),
        stop=stop_after_attempt(3),
        reraise=True,
    )
    def _esearch(self, query: str, *, limit: int) -> list[str]:
        params: dict[str, str | int] = {
            "db": "pubmed",
            "term": query,
            "retmode": "json",
            "retmax": min(limit, 200),
            "sort": "pub_date",
            "tool": self.settings.ncbi_tool,
        }
        if self.settings.ncbi_email:
            params["email"] = self.settings.ncbi_email
        if self.settings.ncbi_api_key:
            params["api_key"] = self.settings.ncbi_api_key
        url = f"{EUTILS_BASE}/esearch.fcgi"
        data = _http.get_json(url, params=params, timeout=20.0)
        return data.get("esearchresult", {}).get("idlist", [])

    @retry(
        wait=wait_exponential(multiplier=1, min=1, max=10),
        stop=stop_after_attempt(3),
        reraise=True,
    )
    def _efetch(self, pmids: list[str]) -> list[SourceDocument]:
        params: dict[str, str | int] = {
            "db": "pubmed",
            "id": ",".join(pmids),
            "rettype": "abstract",
            "retmode": "xml",
            "tool": self.settings.ncbi_tool,
        }
        if self.settings.ncbi_email:
            params["email"] = self.settings.ncbi_email
        if self.settings.ncbi_api_key:
            params["api_key"] = self.settings.ncbi_api_key
        url = f"{EUTILS_BASE}/efetch.fcgi"
        xml = _http.get_text(url, params=params, timeout=30.0)
        return list(self._parse_pubmed_xml(xml))

    def _parse_pubmed_xml(self, xml: str) -> Iterable[SourceDocument]:
        # Use lxml only when actually parsing. Keeps cold-import surface small.
        from lxml import etree

        try:
            root = etree.fromstring(xml.encode("utf-8"))
        except etree.XMLSyntaxError as exc:
            logger.warning("PubMed: failed to parse XML: %s", exc)
            return

        for article in root.findall(".//PubmedArticle"):
            pmid_el = article.find(".//PMID")
            if pmid_el is None or not pmid_el.text:
                continue
            pmid = pmid_el.text.strip()

            title = _join_text(article.find(".//ArticleTitle"))
            abstract = _join_abstract(article.findall(".//Abstract/AbstractText"))
            if not abstract:
                continue

            journal = _join_text(article.find(".//Journal/Title"))
            year_el = article.find(".//JournalIssue/PubDate/Year")
            published_at: datetime | None = None
            if year_el is not None and year_el.text and year_el.text.isdigit():
                published_at = datetime(int(year_el.text), 1, 1, tzinfo=timezone.utc)

            authors: list[str] = []
            for author in article.findall(".//AuthorList/Author"):
                last = _join_text(author.find("LastName"))
                first = _join_text(author.find("ForeName"))
                if last and first:
                    authors.append(f"{first} {last}")
                elif last:
                    authors.append(last)

            doi = ""
            for id_el in article.findall(".//ArticleIdList/ArticleId"):
                if id_el.get("IdType") == "doi" and id_el.text:
                    doi = id_el.text.strip()
                    break

            citation_ids = {"PMID": pmid}
            if doi:
                citation_ids["DOI"] = doi

            url = f"https://pubmed.ncbi.nlm.nih.gov/{pmid}/"
            yield SourceDocument(
                source_key=self.source_key,
                source_id=pmid,
                title=title or f"PubMed {pmid}",
                text=abstract,
                url=url,
                evidence_type=EvidenceType.LITERATURE,
                access_class=AccessClass.PUBLIC_OPEN,
                published_at=published_at,
                authors=authors,
                citation_ids=citation_ids,
                extra={"journal": journal, "doi": doi},
            )


def _join_text(node) -> str:
    if node is None:
        return ""
    # lxml ``itertext`` walks children too, which matters for mixed-content
    # nodes like <ArticleTitle>Foo <i>bar</i></ArticleTitle>.
    return " ".join(t.strip() for t in node.itertext() if t and t.strip())


def _join_abstract(nodes) -> str:
    parts: list[str] = []
    for node in nodes:
        label = node.get("Label")
        text = _join_text(node)
        if not text:
            continue
        parts.append(f"{label}: {text}" if label else text)
    return "\n\n".join(parts)
