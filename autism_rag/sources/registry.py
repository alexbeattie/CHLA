"""Authoritative source registry.

This is the single source of truth for which autism research sources we
ingest, under what license, by what method, and into which Pinecone
namespace. Adapters and ingestion scripts look up everything here.
"""

from __future__ import annotations

from .models import AccessClass, EvidenceType, SourceRecord


SOURCE_REGISTRY: dict[str, SourceRecord] = {
    "pubmed": SourceRecord(
        key="pubmed",
        name="PubMed",
        homepage="https://pubmed.ncbi.nlm.nih.gov/",
        access_class=AccessClass.PUBLIC_OPEN,
        evidence_type=EvidenceType.LITERATURE,
        license="NLM terms; metadata reusable; full text only via PMC OA",
        access_method="NCBI E-utilities (esearch + efetch)",
        refresh_cadence="daily",
        pinecone_namespace="public_literature",
        notes="Use abstracts here. Full text comes from PMC OA when available.",
    ),
    "pmc_oa": SourceRecord(
        key="pmc_oa",
        name="PubMed Central Open Access Subset",
        homepage="https://pmc.ncbi.nlm.nih.gov/tools/openftlist/",
        access_class=AccessClass.PUBLIC_OPEN,
        evidence_type=EvidenceType.LITERATURE,
        license="Per-article CC/OA license; check each record",
        access_method="BioC API + E-utilities",
        refresh_cadence="weekly",
        pinecone_namespace="public_literature",
        notes="Only ingest articles whose license permits redistribution/reuse.",
    ),
    "clinicaltrials": SourceRecord(
        key="clinicaltrials",
        name="ClinicalTrials.gov",
        homepage="https://clinicaltrials.gov/",
        access_class=AccessClass.PUBLIC_OPEN,
        evidence_type=EvidenceType.CLINICAL_TRIAL,
        license="Public domain (U.S. government work)",
        access_method="REST API v2 (clinicaltrials.gov/api/v2/studies)",
        refresh_cadence="weekly",
        pinecone_namespace="clinical_trials",
    ),
    "nih_reporter": SourceRecord(
        key="nih_reporter",
        name="NIH RePORTER",
        homepage="https://reporter.nih.gov/",
        access_class=AccessClass.PUBLIC_OPEN,
        evidence_type=EvidenceType.GRANT,
        license="Public domain (U.S. government work)",
        access_method="REST API v2 (api.reporter.nih.gov/v2/projects/search)",
        refresh_cadence="monthly",
        pinecone_namespace="nih_grants",
    ),
    "sfari_gene": SourceRecord(
        key="sfari_gene",
        name="SFARI Gene",
        homepage="https://gene.sfari.org/",
        access_class=AccessClass.PUBLIC_OPEN,
        evidence_type=EvidenceType.GENE_EVIDENCE,
        license="SFARI Gene terms; attribution required; non-commercial use guidance applies",
        access_method="Data download (CSV/Excel) under SFARI terms",
        refresh_cadence="quarterly",
        pinecone_namespace="sfari_gene",
        notes="Curated ASD gene scores, CNVs, animal models. Verify current SFARI usage terms before redistribution.",
    ),
    "openalex": SourceRecord(
        key="openalex",
        name="OpenAlex",
        homepage="https://openalex.org/",
        access_class=AccessClass.PUBLIC_OPEN,
        evidence_type=EvidenceType.LITERATURE,
        license="CC0",
        access_method="REST API (api.openalex.org/works)",
        refresh_cadence="weekly",
        pinecone_namespace="public_literature",
        notes="Use for citation graph enrichment and dedup against PubMed.",
    ),
    "firecrawl_web": SourceRecord(
        key="firecrawl_web",
        name="Permitted web pages (Firecrawl)",
        homepage="https://firecrawl.dev/",
        access_class=AccessClass.PUBLIC_OPEN,
        evidence_type=EvidenceType.WEB,
        license="Per-site terms; respect robots.txt and site terms",
        access_method="Firecrawl CLI / API for explicitly approved URLs",
        refresh_cadence="ad hoc",
        pinecone_namespace="public_web",
        notes="Only crawl pages we are allowed to fetch under the site's terms.",
    ),
    "nda_metadata": SourceRecord(
        key="nda_metadata",
        name="NIMH Data Archive (NDA) - metadata only",
        homepage="https://nda.nih.gov/",
        access_class=AccessClass.CONTROLLED_METADATA,
        evidence_type=EvidenceType.DATASET_METADATA,
        license="NDA terms; participant-level data is controlled",
        access_method="Public study/collection metadata pages only",
        refresh_cadence="quarterly",
        pinecone_namespace="controlled_metadata",
        notes="Participant-level data requires DAR/IRB and goes to a separate namespace.",
    ),
    "sfari_base_metadata": SourceRecord(
        key="sfari_base_metadata",
        name="SFARI Base - metadata only",
        homepage="https://www.sfari.org/resource/sfari-base/",
        access_class=AccessClass.CONTROLLED_METADATA,
        evidence_type=EvidenceType.DATASET_METADATA,
        license="SFARI terms; participant-level data requires RDA",
        access_method="Public cohort descriptions only",
        refresh_cadence="quarterly",
        pinecone_namespace="controlled_metadata",
    ),
    "dbgap_metadata": SourceRecord(
        key="dbgap_metadata",
        name="dbGaP - study metadata",
        homepage="https://www.ncbi.nlm.nih.gov/gap/",
        access_class=AccessClass.CONTROLLED_METADATA,
        evidence_type=EvidenceType.DATASET_METADATA,
        license="Public study summaries; genotypes require DAR",
        access_method="Public study pages / E-utilities (gap database)",
        refresh_cadence="quarterly",
        pinecone_namespace="controlled_metadata",
    ),
    "approved_controlled_export": SourceRecord(
        key="approved_controlled_export",
        name="Approved controlled-access exports",
        homepage="",
        access_class=AccessClass.APPROVED_CONTROLLED,
        evidence_type=EvidenceType.DATASET_METADATA,
        license="Per executed DUA/IRB",
        access_method="Manual upload of approved exports",
        refresh_cadence="ad hoc",
        pinecone_namespace="approved_controlled_exports",
        notes="Reserved for future use. Requires written approvals before any ingestion.",
    ),
}


def get_source(key: str) -> SourceRecord:
    try:
        return SOURCE_REGISTRY[key]
    except KeyError as exc:
        raise KeyError(f"Unknown source key: {key!r}") from exc
