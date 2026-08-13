import pytest
from tenacity import wait_none

from app.errors import EmbeddingProviderError
from app.services.embedding import GeminiEmbeddingService, is_retryable_error


@pytest.fixture(autouse=True)
def no_retry_backoff(monkeypatch: pytest.MonkeyPatch) -> None:
    """재시도 대기 때문에 테스트가 몇 초씩 멈추지 않게 한다."""
    monkeypatch.setattr(
        GeminiEmbeddingService._request_embeddings.retry,
        "wait",
        wait_none(),
    )


class FakeEmbedding:
    def __init__(self, values: list[float]) -> None:
        self.values = values


class FakeResponse:
    def __init__(self, embeddings: list[FakeEmbedding]) -> None:
        self.embeddings = embeddings


class FakeModels:
    def __init__(self, dimension: int = 4, *, vectors_per_call: int | None = None) -> None:
        self.dimension = dimension
        self.vectors_per_call = vectors_per_call
        self.calls: list[list[str]] = []

    def embed_content(self, *, model, contents, config):  # noqa: ANN001, ANN202
        texts = [content.parts[0].text for content in contents]
        self.calls.append(texts)
        count = self.vectors_per_call if self.vectors_per_call is not None else len(texts)
        return FakeResponse(
            [FakeEmbedding([float(index)] * self.dimension) for index in range(count)]
        )


class FakeClient:
    def __init__(self, models: FakeModels) -> None:
        self.models = models


class FlakyModels(FakeModels):
    def __init__(self, *, failures: int, code: int) -> None:
        super().__init__()
        self.failures = failures
        self.code = code
        self.attempts = 0

    def embed_content(self, *, model, contents, config):  # noqa: ANN001, ANN202
        self.attempts += 1
        if self.attempts <= self.failures:
            error = RuntimeError("quota")
            error.code = self.code
            raise error
        return super().embed_content(model=model, contents=contents, config=config)


def make_service(models: FakeModels, *, dimension: int = 4, batch_size: int = 2):  # noqa: ANN201
    return GeminiEmbeddingService(
        api_key="test-key",
        model="gemini-embedding-2",
        dimension=dimension,
        batch_size=batch_size,
        client=FakeClient(models),
    )


def test_embed_sends_each_text_as_its_own_content() -> None:
    models = FakeModels()
    service = make_service(models, batch_size=10)

    vectors = service.embed(["첫 번째", "두 번째"])

    # 텍스트 배열을 그대로 넘기면 Gemini가 한 Content로 묶어 벡터 1개만 돌려준다.
    assert models.calls == [["첫 번째", "두 번째"]]
    assert len(vectors) == 2


def test_embed_splits_requests_by_batch_size_and_keeps_order() -> None:
    models = FakeModels()
    service = make_service(models, batch_size=2)

    vectors = service.embed(["A", "B", "C", "D", "E"])

    assert models.calls == [["A", "B"], ["C", "D"], ["E"]]
    assert len(vectors) == 5


def test_embed_caps_batch_size_at_api_limit() -> None:
    service = make_service(FakeModels(), batch_size=100)

    assert service.batch_size == 100

    service = GeminiEmbeddingService(
        api_key="test-key",
        model="gemini-embedding-2",
        dimension=4,
        batch_size=500,
        client=FakeClient(FakeModels()),
    )

    assert service.batch_size == 100


def test_embed_fails_when_vector_count_differs_from_input() -> None:
    service = make_service(FakeModels(vectors_per_call=1), batch_size=10)

    with pytest.raises(EmbeddingProviderError) as exc_info:
        service.embed(["A", "B", "C"])

    assert exc_info.value.code == "EMBEDDING_PROVIDER_ERROR"
    assert "입력 3건" in exc_info.value.message


def test_embed_fails_when_dimension_differs() -> None:
    service = make_service(FakeModels(dimension=8), dimension=4, batch_size=10)

    with pytest.raises(EmbeddingProviderError) as exc_info:
        service.embed(["A"])

    assert "차원" in exc_info.value.message


def test_embed_wraps_provider_failure() -> None:
    class BrokenModels(FakeModels):
        def embed_content(self, *, model, contents, config):  # noqa: ANN001, ANN202
            raise RuntimeError("boom")

    service = make_service(BrokenModels(), batch_size=10)

    with pytest.raises(EmbeddingProviderError):
        service.embed(["A"])


def test_embed_retries_on_quota_error() -> None:
    models = FlakyModels(failures=1, code=429)
    service = make_service(models, batch_size=10)

    vectors = service.embed(["A", "B"])

    assert models.attempts == 2
    assert len(vectors) == 2


def test_is_retryable_error_only_covers_transient_status_codes() -> None:
    quota = RuntimeError("429")
    quota.code = 429
    bad_request = RuntimeError("400")
    bad_request.code = 400

    assert is_retryable_error(quota) is True
    assert is_retryable_error(bad_request) is False
    assert is_retryable_error(RuntimeError("no code")) is False
