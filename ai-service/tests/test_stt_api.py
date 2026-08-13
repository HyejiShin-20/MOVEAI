from fastapi.testclient import TestClient
from pydantic import SecretStr

from app.config import Settings
from app.errors import SttProviderError
from app.main import create_app


class FakeSttService:
    def __init__(self, text: str = "1톤 탑차는 후문으로 진입합니다.") -> None:
        self.text = text
        self.calls: list[tuple[bytes, str]] = []

    def transcribe(self, *, audio_bytes: bytes, mime_type: str) -> str:
        self.calls.append((audio_bytes, mime_type))
        return self.text


class FailingSttService:
    def transcribe(self, *, audio_bytes: bytes, mime_type: str) -> str:
        raise SttProviderError()


def make_settings(*, max_bytes: int = 1024) -> Settings:
    return Settings(
        GEMINI_API_KEY=SecretStr("test-key"),
        STT_MODEL="gemini-3.6-flash",
        STT_MAX_FILE_BYTES=max_bytes,
    )


def test_health_reports_gemini_model() -> None:
    client = TestClient(create_app(settings=make_settings(), stt_service=FakeSttService()))

    response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {
        "status": "ok",
        "provider": "gemini",
        "model": "gemini-3.6-flash",
    }


def test_stt_returns_transcript_and_normalizes_mp3_mime() -> None:
    service = FakeSttService()
    client = TestClient(create_app(settings=make_settings(), stt_service=service))

    response = client.post(
        "/stt",
        files={"audio": ("report.mp3", b"fake-audio", "audio/mpeg")},
    )

    assert response.status_code == 200
    assert response.json()["text"] == "1톤 탑차는 후문으로 진입합니다."
    assert response.json()["durationMs"] >= 0
    assert service.calls == [(b"fake-audio", "audio/mp3")]


def test_stt_accepts_m4a_audio() -> None:
    service = FakeSttService()
    client = TestClient(create_app(settings=make_settings(), stt_service=service))

    response = client.post(
        "/stt",
        files={"audio": ("report.m4a", b"fake-audio", "audio/mp4")},
    )

    assert response.status_code == 200
    assert service.calls == [(b"fake-audio", "audio/mp4")]


def test_stt_rejects_empty_audio() -> None:
    client = TestClient(create_app(settings=make_settings(), stt_service=FakeSttService()))

    response = client.post(
        "/stt",
        files={"audio": ("empty.wav", b"", "audio/wav")},
    )

    assert response.status_code == 400
    assert response.json()["error"]["code"] == "INVALID_AUDIO_FILE"


def test_stt_rejects_unsupported_audio_type() -> None:
    client = TestClient(create_app(settings=make_settings(), stt_service=FakeSttService()))

    response = client.post(
        "/stt",
        files={"audio": ("report.webm", b"fake-audio", "audio/webm")},
    )

    assert response.status_code == 400
    assert response.json()["error"]["code"] == "INVALID_AUDIO_FILE"


def test_stt_rejects_audio_over_limit() -> None:
    client = TestClient(
        create_app(settings=make_settings(max_bytes=4), stt_service=FakeSttService())
    )

    response = client.post(
        "/stt",
        files={"audio": ("large.wav", b"12345", "audio/wav")},
    )

    assert response.status_code == 413
    assert response.json()["error"]["code"] == "AUDIO_FILE_TOO_LARGE"


def test_stt_maps_provider_failure_to_bad_gateway() -> None:
    client = TestClient(
        create_app(settings=make_settings(), stt_service=FailingSttService())
    )

    response = client.post(
        "/stt",
        files={"audio": ("report.wav", b"fake-audio", "audio/wav")},
    )

    assert response.status_code == 502
    assert response.json() == {
        "error": {
            "code": "STT_PROVIDER_ERROR",
            "message": "음성 변환에 실패했습니다. 잠시 후 다시 시도해 주세요.",
        }
    }
