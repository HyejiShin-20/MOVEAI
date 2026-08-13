import base64
import os
from types import SimpleNamespace

import pytest

from app.errors import SttProviderError
from app.services import stt
from app.services.stt import GeminiSttService


class FakeInteractions:
    def __init__(self, output_text: str | None = "전사 결과") -> None:
        self.output_text = output_text
        self.kwargs = None

    def create(self, **kwargs):
        self.kwargs = kwargs
        return SimpleNamespace(output_text=self.output_text)


def test_gemini_service_sends_inline_audio() -> None:
    interactions = FakeInteractions(output_text="  2.5톤 차량입니다.  ")
    client = SimpleNamespace(interactions=interactions)
    service = GeminiSttService(
        api_key="unused-test-key",
        model="gemini-3.6-flash",
        client=client,
    )

    result = service.transcribe(audio_bytes=b"audio", mime_type="audio/wav")

    assert result == "2.5톤 차량입니다."
    assert interactions.kwargs["model"] == "gemini-3.6-flash"
    audio_input = interactions.kwargs["input"][1]
    assert audio_input == {
        "type": "audio",
        "data": base64.b64encode(b"audio").decode("ascii"),
        "mime_type": "audio/wav",
    }


def test_gemini_service_rejects_empty_provider_output() -> None:
    client = SimpleNamespace(interactions=FakeInteractions(output_text="  "))
    service = GeminiSttService(api_key="unused", model="model", client=client)

    with pytest.raises(SttProviderError):
        service.transcribe(audio_bytes=b"audio", mime_type="audio/wav")


def test_gemini_service_hides_provider_exception() -> None:
    class FailingInteractions:
        def create(self, **kwargs):
            raise RuntimeError("sensitive upstream details")

    client = SimpleNamespace(interactions=FailingInteractions())
    service = GeminiSttService(api_key="unused", model="model", client=client)

    with pytest.raises(SttProviderError) as error:
        service.transcribe(audio_bytes=b"audio", mime_type="audio/wav")

    assert "sensitive upstream details" not in error.value.message


def test_client_creation_prioritizes_explicit_gemini_key(monkeypatch) -> None:
    captured = {}

    def fake_client(*, api_key):
        captured["api_key"] = api_key
        captured["google_key_during_creation"] = os.environ.get("GOOGLE_API_KEY")
        return object()

    monkeypatch.setenv("GOOGLE_API_KEY", "must-not-be-used")
    monkeypatch.setattr(stt.genai, "Client", fake_client)

    client = stt._create_gemini_client("selected-gemini-key")

    assert client is not None
    assert captured == {
        "api_key": "selected-gemini-key",
        "google_key_during_creation": None,
    }
    assert os.environ["GOOGLE_API_KEY"] == "must-not-be-used"
