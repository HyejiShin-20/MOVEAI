from functools import lru_cache
from pathlib import Path

from pydantic import Field, SecretStr
from pydantic_settings import BaseSettings, SettingsConfigDict


PROJECT_ROOT = Path(__file__).resolve().parents[2]


class Settings(BaseSettings):
    gemini_api_key: SecretStr | None = Field(default=None, alias="GEMINI_API_KEY")
    llm_model: str = Field(default="gemini-3.5-flash-lite", alias="LLM_MODEL")
    llm_thinking_level: str = Field(default="minimal", alias="LLM_THINKING_LEVEL")
    embedding_model: str = Field(default="gemini-embedding-2", alias="EMBEDDING_MODEL")
    embedding_dimension: int = Field(default=1536, alias="EMBEDDING_DIMENSION", ge=1)
    # Gemini rejects batches over 100 contents (BatchEmbedContentsRequest 400).
    # 100건은 무료 등급 쿼터에서 429가 나서 기본값을 50으로 둔다.
    embedding_batch_size: int = Field(
        default=50,
        alias="EMBEDDING_BATCH_SIZE",
        ge=1,
        le=100,
    )
    stt_model: str = Field(default="gemini-3.6-flash", alias="STT_MODEL")
    stt_max_file_bytes: int = Field(
        default=10 * 1024 * 1024,
        alias="STT_MAX_FILE_BYTES",
        ge=1,
        le=14 * 1024 * 1024,
    )

    model_config = SettingsConfigDict(
        env_file=PROJECT_ROOT / ".env",
        env_file_encoding="utf-8",
        case_sensitive=True,
        extra="ignore",
    )


@lru_cache
def get_settings() -> Settings:
    return Settings()
