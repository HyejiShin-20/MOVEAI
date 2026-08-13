from fastapi.testclient import TestClient
from pydantic import SecretStr

from app.config import Settings
from app.errors import EmbeddingProviderError
from app.main import create_app


class FakeEmbeddingService:
    def __init__(self, *, model: str = "gemini-embedding-2", dimension: int = 3) -> None:
        self.model = model
        self.dimension = dimension
        self.calls: list[list[str]] = []

    def embed(self, texts: list[str]) -> list[list[float]]:
        self.calls.append(texts)
        return [[0.1, 0.2, 0.3] for _ in texts]


class FailingEmbeddingService:
    model = "gemini-embedding-2"
    dimension = 1536

    def embed(self, texts: list[str]) -> list[list[float]]:
        raise EmbeddingProviderError()


def make_settings() -> Settings:
    return Settings(
        GEMINI_API_KEY=SecretStr("test-key"),
        EMBEDDING_MODEL="gemini-embedding-2",
        EMBEDDING_DIMENSION=3,
    )


def test_embed_returns_one_vector_per_text() -> None:
    service = FakeEmbeddingService()
    client = TestClient(create_app(settings=make_settings(), embedding_service=service))

    response = client.post("/embed", json={"texts": ["위치: 후문", "위치: 하역장"]})

    assert response.status_code == 200
    assert response.json() == {
        "model": "gemini-embedding-2",
        "dimension": 3,
        "vectors": [[0.1, 0.2, 0.3], [0.1, 0.2, 0.3]],
    }
    assert service.calls == [["위치: 후문", "위치: 하역장"]]


def test_embed_rejects_empty_text_list() -> None:
    client = TestClient(
        create_app(settings=make_settings(), embedding_service=FakeEmbeddingService())
    )

    response = client.post("/embed", json={"texts": []})

    assert response.status_code == 422


def test_embed_rejects_blank_text() -> None:
    client = TestClient(
        create_app(settings=make_settings(), embedding_service=FakeEmbeddingService())
    )

    response = client.post("/embed", json={"texts": ["정상 문장", "   "]})

    assert response.status_code == 422


def test_embed_rejects_unknown_fields() -> None:
    client = TestClient(
        create_app(settings=make_settings(), embedding_service=FakeEmbeddingService())
    )

    response = client.post("/embed", json={"texts": ["문장"], "taskType": "RETRIEVAL_QUERY"})

    assert response.status_code == 422


def test_embed_maps_provider_failure_to_bad_gateway() -> None:
    client = TestClient(
        create_app(settings=make_settings(), embedding_service=FailingEmbeddingService())
    )

    response = client.post("/embed", json={"texts": ["문장"]})

    assert response.status_code == 502
    assert response.json()["error"]["code"] == "EMBEDDING_PROVIDER_ERROR"
