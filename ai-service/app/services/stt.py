import base64
import logging
from typing import Any

from app.errors import SttProviderError
from app.services.gemini import create_gemini_client


logger = logging.getLogger(__name__)
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
        self._client = client or create_gemini_client(api_key)

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
