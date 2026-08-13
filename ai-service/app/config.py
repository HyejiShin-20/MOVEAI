from functools import lru_cache
from pathlib import Path

from pydantic import Field, SecretStr
from pydantic_settings import BaseSettings, SettingsConfigDict


PROJECT_ROOT = Path(__file__).resolve().parents[2]


class Settings(BaseSettings):
    gemini_api_key: SecretStr | None = Field(default=None, alias="GEMINI_API_KEY")
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
