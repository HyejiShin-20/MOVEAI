import time
from collections.abc import Callable
from typing import Annotated, Any

from anyio import to_thread
from fastapi import FastAPI, File, Request, UploadFile
from fastapi.responses import JSONResponse

from app.config import Settings, get_settings
from app.errors import (
    AppError,
    AudioTooLargeError,
    EmbeddingConfigurationError,
    ExtractionConfigurationError,
    InvalidAudioError,
    SttConfigurationError,
)
from app.schemas import (
    EmbedRequest,
    EmbedResponse,
    HealthResponse,
    KnowledgeExtractionRequest,
    KnowledgeExtractionResponse,
    SttResponse,
)
from app.services.embedding import GeminiEmbeddingService
from app.services.extraction import KnowledgeExtractionService
from app.services.stt import GeminiSttService


SUPPORTED_AUDIO_TYPES = {
    "audio/aac",
    "audio/aiff",
    "audio/flac",
    "audio/mp3",
    "audio/mpeg",
    "audio/mp4",
    "audio/ogg",
    "audio/wav",
    "audio/x-aiff",
    "audio/x-flac",
    "audio/x-wav",
}
MIME_ALIASES = {
    "audio/mpeg": "audio/mp3",
    "audio/x-aiff": "audio/aiff",
    "audio/x-flac": "audio/flac",
    "audio/x-wav": "audio/wav",
}
READ_CHUNK_BYTES = 1024 * 1024


async def read_audio(upload: UploadFile, max_file_bytes: int) -> bytes:
    audio = bytearray()
    while chunk := await upload.read(READ_CHUNK_BYTES):
        audio.extend(chunk)
        if len(audio) > max_file_bytes:
            raise AudioTooLargeError(max_file_bytes)
    if not audio:
        raise InvalidAudioError("빈 오디오 파일은 처리할 수 없습니다.")
    return bytes(audio)


def normalize_mime_type(content_type: str | None) -> str:
    mime_type = (content_type or "").split(";", maxsplit=1)[0].strip().lower()
    if mime_type not in SUPPORTED_AUDIO_TYPES:
        supported = "WAV, MP3, M4A, AIFF, AAC, OGG, FLAC"
        raise InvalidAudioError(f"지원하지 않는 오디오 형식입니다. 지원 형식: {supported}")
    return MIME_ALIASES.get(mime_type, mime_type)


def create_app(
    *,
    settings: Settings | None = None,
    stt_service: Any | None = None,
    service_factory: Callable[[Settings], Any] | None = None,
    extraction_service: Any | None = None,
    extraction_service_factory: Callable[[Settings], Any] | None = None,
    embedding_service: Any | None = None,
    embedding_service_factory: Callable[[Settings], Any] | None = None,
) -> FastAPI:
    app = FastAPI(title="MOVE-AI AI Service", version="0.1.0")
    app.state.settings = settings or get_settings()
    app.state.stt_service = stt_service
    app.state.service_factory = service_factory or _create_stt_service
    app.state.extraction_service = extraction_service
    app.state.extraction_service_factory = extraction_service_factory or _create_extraction_service
    app.state.embedding_service = embedding_service
    app.state.embedding_service_factory = embedding_service_factory or _create_embedding_service

    @app.exception_handler(AppError)
    async def handle_app_error(_request: Request, exc: AppError) -> JSONResponse:
        return JSONResponse(
            status_code=exc.status_code,
            content={"error": {"code": exc.code, "message": exc.message}},
        )

    @app.get("/health", response_model=HealthResponse)
    async def health(request: Request) -> HealthResponse:
        current_settings: Settings = request.app.state.settings
        return HealthResponse(
            status="ok",
            provider="gemini",
            model=current_settings.stt_model,
        )

    @app.post("/stt", response_model=SttResponse)
    async def transcribe_audio(
        request: Request,
        audio: Annotated[UploadFile, File(...)],
    ) -> SttResponse:
        started_at = time.perf_counter()
        current_settings: Settings = request.app.state.settings
        mime_type = normalize_mime_type(audio.content_type)
        try:
            audio_bytes = await read_audio(audio, current_settings.stt_max_file_bytes)
        finally:
            await audio.close()

        service = _get_stt_service(request.app)
        transcript = await to_thread.run_sync(
            lambda: service.transcribe(audio_bytes=audio_bytes, mime_type=mime_type)
        )
        duration_ms = round((time.perf_counter() - started_at) * 1000)
        return SttResponse(text=transcript, durationMs=duration_ms)

    @app.post("/extract-knowledge", response_model=KnowledgeExtractionResponse)
    async def extract_knowledge(
        request: Request,
        payload: KnowledgeExtractionRequest,
    ) -> KnowledgeExtractionResponse:
        service = _get_extraction_service(request.app)
        return await to_thread.run_sync(lambda: service.extract(payload))

    @app.post("/embed", response_model=EmbedResponse)
    async def embed_texts(request: Request, payload: EmbedRequest) -> EmbedResponse:
        service = _get_embedding_service(request.app)
        vectors = await to_thread.run_sync(lambda: service.embed(payload.texts))
        return EmbedResponse(
            model=service.model,
            dimension=service.dimension,
            vectors=vectors,
        )

    return app


def _create_stt_service(settings: Settings) -> GeminiSttService:
    if settings.gemini_api_key is None:
        raise SttConfigurationError()
    api_key = settings.gemini_api_key.get_secret_value().strip()
    if not api_key:
        raise SttConfigurationError()
    return GeminiSttService(api_key=api_key, model=settings.stt_model)


def _get_stt_service(app: FastAPI) -> Any:
    if app.state.stt_service is None:
        app.state.stt_service = app.state.service_factory(app.state.settings)
    return app.state.stt_service


def _create_extraction_service(settings: Settings) -> KnowledgeExtractionService:
    if settings.gemini_api_key is None:
        raise ExtractionConfigurationError()
    api_key = settings.gemini_api_key.get_secret_value().strip()
    if not api_key:
        raise ExtractionConfigurationError()
    return KnowledgeExtractionService(
        api_key=api_key,
        model=settings.llm_model,
        thinking_level=settings.llm_thinking_level,
    )


def _get_extraction_service(app: FastAPI) -> Any:
    if app.state.extraction_service is None:
        app.state.extraction_service = app.state.extraction_service_factory(app.state.settings)
    return app.state.extraction_service


def _create_embedding_service(settings: Settings) -> GeminiEmbeddingService:
    if settings.gemini_api_key is None:
        raise EmbeddingConfigurationError()
    api_key = settings.gemini_api_key.get_secret_value().strip()
    if not api_key:
        raise EmbeddingConfigurationError()
    return GeminiEmbeddingService(
        api_key=api_key,
        model=settings.embedding_model,
        dimension=settings.embedding_dimension,
        batch_size=settings.embedding_batch_size,
    )


def _get_embedding_service(app: FastAPI) -> Any:
    if app.state.embedding_service is None:
        app.state.embedding_service = app.state.embedding_service_factory(app.state.settings)
    return app.state.embedding_service


app = create_app()
