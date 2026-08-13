"""Call Gemini STT with one local audio file without printing API credentials."""

from __future__ import annotations

import argparse
import json
import mimetypes
import sys
import time
from pathlib import Path


AI_SERVICE_ROOT = Path(__file__).resolve().parents[1]
PROJECT_ROOT = AI_SERVICE_ROOT.parent
sys.path.insert(0, str(AI_SERVICE_ROOT))

from app.config import get_settings  # noqa: E402
from app.errors import AppError  # noqa: E402
from app.services.stt import GeminiSttService  # noqa: E402


MIME_BY_SUFFIX = {
    ".aac": "audio/aac",
    ".aif": "audio/aiff",
    ".aiff": "audio/aiff",
    ".flac": "audio/flac",
    ".m4a": "audio/mp4",
    ".mp3": "audio/mp3",
    ".ogg": "audio/ogg",
    ".wav": "audio/wav",
}


def default_sample() -> Path:
    samples = sorted((PROJECT_ROOT / "datasets" / "voice").glob("*"))
    audio_samples = [path for path in samples if path.is_file() and path.suffix.lower() in MIME_BY_SUFFIX]
    if not audio_samples:
        raise FileNotFoundError("datasets/voice에 지원되는 샘플 오디오가 없습니다.")
    return audio_samples[0]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("audio", type=Path, nargs="?", help="검증할 오디오 파일")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    audio_path = (args.audio or default_sample()).resolve()
    if not audio_path.is_file():
        raise FileNotFoundError(audio_path)

    settings = get_settings()
    if settings.gemini_api_key is None or not settings.gemini_api_key.get_secret_value().strip():
        print("GEMINI_API_KEY가 설정되지 않았습니다.", file=sys.stderr)
        return 2

    mime_type = MIME_BY_SUFFIX.get(audio_path.suffix.lower())
    if mime_type is None:
        mime_type = mimetypes.guess_type(audio_path.name)[0]
    if not mime_type:
        print("오디오 MIME 유형을 확인할 수 없습니다.", file=sys.stderr)
        return 2

    service = GeminiSttService(
        api_key=settings.gemini_api_key.get_secret_value(),
        model=settings.stt_model,
    )
    started_at = time.perf_counter()
    try:
        transcript = service.transcribe(
            audio_bytes=audio_path.read_bytes(),
            mime_type=mime_type,
        )
    except AppError as exc:
        print(f"error={exc.code}: {exc.message}", file=sys.stderr)
        return 1

    elapsed_ms = round((time.perf_counter() - started_at) * 1000)
    # ASCII-only JSON keeps Korean text readable after capture even when the
    # Windows parent shell and Python use different console encodings.
    print(
        json.dumps(
            {
                "model": settings.stt_model,
                "file": audio_path.name,
                "elapsedMs": elapsed_ms,
                "text": transcript,
            },
            ensure_ascii=True,
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
