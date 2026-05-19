from .base import BaseAdapter
from .pubmed import PubMedAdapter
from .clinicaltrials import ClinicalTrialsAdapter
from .nih_reporter import NIHReporterAdapter
from .openalex import OpenAlexAdapter
from .sfari_gene import SFARIGeneAdapter
from .firecrawl_web import (
    DbGaPMetadataAdapter,
    FirecrawlWebAdapter,
    NDAMetadataAdapter,
    SFARIBaseMetadataAdapter,
)

ADAPTERS: dict[str, type[BaseAdapter]] = {
    "pubmed": PubMedAdapter,
    "clinicaltrials": ClinicalTrialsAdapter,
    "nih_reporter": NIHReporterAdapter,
    "openalex": OpenAlexAdapter,
    "sfari_gene": SFARIGeneAdapter,
    "firecrawl_web": FirecrawlWebAdapter,
    "nda_metadata": NDAMetadataAdapter,
    "sfari_base_metadata": SFARIBaseMetadataAdapter,
    "dbgap_metadata": DbGaPMetadataAdapter,
}


def get_adapter(key: str) -> type[BaseAdapter]:
    try:
        return ADAPTERS[key]
    except KeyError as exc:
        raise KeyError(
            f"No adapter registered for source {key!r}. "
            f"Known adapters: {sorted(ADAPTERS)}"
        ) from exc


__all__ = [
    "ADAPTERS",
    "BaseAdapter",
    "ClinicalTrialsAdapter",
    "DbGaPMetadataAdapter",
    "FirecrawlWebAdapter",
    "NIHReporterAdapter",
    "NDAMetadataAdapter",
    "OpenAlexAdapter",
    "PubMedAdapter",
    "SFARIBaseMetadataAdapter",
    "SFARIGeneAdapter",
    "get_adapter",
]
