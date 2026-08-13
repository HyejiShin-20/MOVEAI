import base64
import logging
import os
import threading
from typing import Any

from google import genai

from app.errors import SttProviderError


logger = logging.getLogger(__name__)
_CLIENT_ENV_LOCK = threading.Lock()

TRANSCRIPTION_PROMPT = """
다음 오디오의 한국어 음성을 정확히 전사하라.

규칙:
1. 설명, 요약, 제목, 화자 라벨 없이 전사문만 출력한다.
2. 숫자, 소수점, 시간, 차량 톤수와 높이 단위를 원문대로 보존한다.
3. 탑차, 하역장, 방화문, 화물 엘리베이터 등 물류·현장 용어를 임의로 바꾸지 않는다.
4. 들리지 않는 내용을 추측하거나 새로 만들지 않는다.
5. 한국어 문장부호와 띄어쓰기만 자연스럽게 정리한다.
""".strip()


class GeminiSttService:
    def __init__(self, *, api_key: str, model: str, client: Any | None = None) -> None:
        self.model = model
        self._client = client or _create_gemini_client(api_key)

    def transcribe(self, *, audio_bytes: bytes, mime_type: str) -> str:
        encoded_audio = base64.b64encode(audio_bytes).decode("ascii")

        try:
            interaction = self._client.interactions.create(
                model=self.model,
                input=[
                    {"type": "text", "text": TRANSCRIPTION_PROMPT},
                    {
                        "type": "audio",
                        "data": encoded_audio,
                        "mime_type": mime_type,
                    },
                ],
            )
        except Exception as exc:
            logger.warning(
                "Gemini STT request failed: provider=%s model=%s error_type=%s",
                "gemini",
                self.model,
                type(exc).__name__,
            )
            raise SttProviderError() from exc

        transcript = (getattr(interaction, "output_text", None) or "").strip()
        if not transcript:
            logger.warning("Gemini STT returned an empty transcript: model=%s", self.model)
            raise SttProviderError()

        return transcript


def _create_gemini_client(api_key: str) -> Any:
    """Build a client from GEMINI_API_KEY even if GOOGLE_API_KEY exists globally.

    google-genai resolves both environment variables while constructing a client and
    logs that GOOGLE_API_KEY wins. The application passes the selected Gemini key
    explicitly; temporarily hiding the unrelated variable also prevents ambiguity.
    """
    with _CLIENT_ENV_LOCK:
        google_api_key = os.environ.pop("GOOGLE_API_KEY", None)
        try:
            return genai.Client(api_key=api_key)
        finally:
            if google_api_key is not None:
                os.environ["GOOGLE_API_KEY"] = google_api_key
