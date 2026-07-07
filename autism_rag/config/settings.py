"""Environment-driven settings for the autism RAG system.

Centralizes configuration so adapters, ingestion, retrieval, and the API all
read from a single source of truth instead of scattered ``os.getenv`` calls.
"""

from __future__ import annotations

from functools import lru_cache
from pathlib import Path
from typing import Literal

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


PROJECT_ROOT = Path(__file__).resolve().parent.parent
DATA_DIR = PROJECT_ROOT / "data"
RAW_DIR = DATA_DIR / "raw"
PROCESSED_DIR = DATA_DIR / "processed"


class Settings(BaseSettings):
    """Application settings loaded from environment / .env file."""

    model_config = SettingsConfigDict(
        env_file=str(PROJECT_ROOT / ".env"),
        env_file_encoding="utf-8",
        extra="ignore",
        case_sensitive=False,
    )

    cohere_api_key: str = Field(default="", description="Cohere API key for embeddings + rerank.")
    cohere_embed_model: str = Field(default="embed-v4.0")
    cohere_embed_dim: int = Field(default=1536)
    cohere_embed_batch_size: int = Field(default=12)
    cohere_embed_pause_seconds: float = Field(default=6.0)
    cohere_embed_max_retries: int = Field(default=8)

    rerank_enabled: bool = Field(default=True)
    rerank_model: str = Field(default="rerank-english-v3.0")

    pinecone_api_key: str = Field(default="")
    pinecone_index: str = Field(default="autism-research-rag")
    pinecone_cloud: Literal["aws", "gcp", "azure"] = Field(default="aws")
    pinecone_region: str = Field(default="us-east-1")
    pinecone_metric: Literal["cosine", "dotproduct", "euclidean"] = Field(default="cosine")
    pinecone_embed_dims: int = Field(default=1536)

    firecrawl_api_key: str = Field(default="")

    ncbi_api_key: str = Field(default="")
    ncbi_tool: str = Field(default="autism_research_rag")
    ncbi_email: str = Field(default="")

    openai_api_key: str = Field(default="")
    answer_model: str = Field(default="gpt-4o-mini")
    anthropic_api_key: str = Field(default="")
    anthropic_answer_model: str = Field(default="claude-opus-4-8")
    # Bedrock fallback matches the model the Django backend already has access to
    anthropic_bedrock_enabled: bool = Field(default=True)
    anthropic_bedrock_model: str = Field(default="us.anthropic.claude-sonnet-4-5-20250929-v1:0")
    aws_region: str = Field(default="us-west-2")

    default_top_k: int = Field(default=8)

    project_root: Path = Field(default=PROJECT_ROOT)
    raw_dir: Path = Field(default=RAW_DIR)
    processed_dir: Path = Field(default=PROCESSED_DIR)

    def ensure_dirs(self) -> None:
        self.raw_dir.mkdir(parents=True, exist_ok=True)
        self.processed_dir.mkdir(parents=True, exist_ok=True)


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    settings = Settings()
    settings.ensure_dirs()
    return settings
