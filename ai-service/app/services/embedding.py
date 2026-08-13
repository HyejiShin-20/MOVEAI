from __future__ import annotations

import logging
from typing import Any

from google.genai import types
from tenacity import (
    RetryCallState,
    retry,
    retry_if_exception,
    stop_after_attempt,
    wait_exponential,
)

from app.errors import EmbeddingProviderError
from app.services.gemini import create_gemini_client


logger = logging.getLogger(__name__)

# Gemini는 임베딩 요청 하나에 100개까지만 받는다(BatchEmbedContentsRequest 400).
API_MAX_BATCH_SIZE = 100
RETRYABLE_STATUS_CODES = frozenset({429, 500, 502, 503, 504})
# 무료 등급 쿼터는 EmbedContentRequestsPerMinutePerProjectPerModel = 100 이고,
# 배치 안의 텍스트 1건이 요청 1건으로 계산된다. 시드 146건은 1분에 못 끝내므로
# 분 단위 윈도우가 열릴 때까지 기다릴 수 있도록 대기를 길게 잡는다.
MAX_ATTEMPTS = 5
RETRY_WAIT_MIN_SECONDS = 4
RETRY_WAIT_MAX_SECONDS = 60


class GeminiEmbeddingService:
    """텍스트 배열을 같은 순서의 벡터 배열로 바꾼다.

    검색 품질은 지식 벡터와 질의 벡터가 같은 공간에 있느냐에 달려 있다.
    04 §5-1과 §5-2가 두 텍스트를 같은 포맷으로 맞춰 두었으므로,
    task_type도 방향성 없는 SEMANTIC_SIMILARITY로 통일한다.
    """

    def __init__(
        self,
        *,
        api_key: str,
        model: str,
        dimension: int,
        batch_size: int,
        client: Any | None = None,
    ) -> None:
        self.model = model
        self.dimension = dimension
        self.batch_size = min(batch_size, API_MAX_BATCH_SIZE)
        self._client = client or create_gemini_client(api_key)

    def embed(self, texts: list[str]) -> list[list[float]]:
        vectors: list[list[float]] = []
        for start in range(0, len(texts), self.batch_size):
            chunk = texts[start : start + self.batch_size]
            vectors.extend(self._embed_chunk(chunk, offset=start))
        return vectors

    def _embed_chunk(self, chunk: list[str], *, offset: int) -> list[list[float]]:
        try:
            response = self._request_embeddings(chunk)
        except Exception as exc:
            logger.warning(
                "Gemini embedding request failed: model=%s offset=%s size=%s "
                "error_type=%s status=%s",
                self.model,
                offset,
                len(chunk),
                type(exc).__name__,
                getattr(exc, "code", None),
            )
            raise EmbeddingProviderError() from exc

        embeddings = getattr(response, "embeddings", None) or []
        vectors = [list(embedding.values) for embedding in embeddings]

        # 텍스트 배열을 그대로 넘기면 SDK가 전체를 한 Content로 묶어 벡터 1개만 돌려준다.
        # 조용히 어긋나면 지식과 벡터의 짝이 통째로 밀리므로 여기서 끊는다.
        if len(vectors) != len(chunk):
            raise EmbeddingProviderError(
                f"임베딩 개수가 입력과 다릅니다. 입력 {len(chunk)}건, 응답 {len(vectors)}건."
            )
        for index, vector in enumerate(vectors):
            if len(vector) != self.dimension:
                raise EmbeddingProviderError(
                    f"임베딩 차원이 {self.dimension}이 아닙니다. "
                    f"texts[{offset + index}] 차원 {len(vector)}."
                )
        return vectors

    @retry(
        stop=stop_after_attempt(MAX_ATTEMPTS),
        wait=wait_exponential(
            multiplier=RETRY_WAIT_MIN_SECONDS,
            min=RETRY_WAIT_MIN_SECONDS,
            max=RETRY_WAIT_MAX_SECONDS,
        ),
        retry=retry_if_exception(lambda exc: is_retryable_error(exc)),
        before_sleep=lambda state: _log_retry(state),
        reraise=True,
    )
    def _request_embeddings(self, chunk: list[str]) -> Any:
        return self._client.models.embed_content(
            model=self.model,
            contents=[to_content(text) for text in chunk],
            config=types.EmbedContentConfig(
                output_dimensionality=self.dimension,
                task_type="SEMANTIC_SIMILARITY",
            ),
        )


def to_content(text: str) -> types.Content:
    return types.Content(role="user", parts=[types.Part(text=text)])


def is_retryable_error(exc: BaseException) -> bool:
    return getattr(exc, "code", None) in RETRYABLE_STATUS_CODES


def _log_retry(state: RetryCallState) -> None:
    exc = state.outcome.exception() if state.outcome else None
    logger.info(
        "Retrying embedding request: attempt=%s status=%s sleep=%ss",
        state.attempt_number,
        getattr(exc, "code", None),
        round(state.upcoming_sleep),
    )
