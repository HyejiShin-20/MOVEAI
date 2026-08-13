class AppError(Exception):
    def __init__(self, *, code: str, message: str, status_code: int) -> None:
        super().__init__(message)
        self.code = code
        self.message = message
        self.status_code = status_code


class InvalidAudioError(AppError):
    def __init__(self, message: str) -> None:
        super().__init__(
            code="INVALID_AUDIO_FILE",
            message=message,
            status_code=400,
        )


class AudioTooLargeError(AppError):
    def __init__(self, max_file_bytes: int) -> None:
        max_megabytes = max_file_bytes // (1024 * 1024)
        super().__init__(
            code="AUDIO_FILE_TOO_LARGE",
            message=f"오디오 파일은 {max_megabytes}MB 이하여야 합니다.",
            status_code=413,
        )


class SttConfigurationError(AppError):
    def __init__(self) -> None:
        super().__init__(
            code="STT_NOT_CONFIGURED",
            message="STT 서비스 설정이 완료되지 않았습니다.",
            status_code=503,
        )


class SttProviderError(AppError):
    def __init__(self) -> None:
        super().__init__(
            code="STT_PROVIDER_ERROR",
            message="음성 변환에 실패했습니다. 잠시 후 다시 시도해 주세요.",
            status_code=502,
        )


class ExtractionConfigurationError(AppError):
    def __init__(self) -> None:
        super().__init__(
            code="EXTRACTION_NOT_CONFIGURED",
            message="지식 추출 서비스 설정이 완료되지 않았습니다.",
            status_code=503,
        )


class ExtractionProviderError(AppError):
    def __init__(self) -> None:
        super().__init__(
            code="EXTRACTION_PROVIDER_ERROR",
            message="지식 추출 모델 호출에 실패했습니다. 잠시 후 다시 시도해 주세요.",
            status_code=502,
        )


class EmbeddingConfigurationError(AppError):
    def __init__(self) -> None:
        super().__init__(
            code="EMBEDDING_NOT_CONFIGURED",
            message="임베딩 서비스 설정이 완료되지 않았습니다.",
            status_code=503,
        )


class EmbeddingProviderError(AppError):
    def __init__(self, message: str | None = None) -> None:
        super().__init__(
            code="EMBEDDING_PROVIDER_ERROR",
            message=message or "임베딩 생성에 실패했습니다. 잠시 후 다시 시도해 주세요.",
            status_code=502,
        )


class ExtractionFailedError(AppError):
    def __init__(self, details: list[str] | None = None) -> None:
        self.details = details or []
        super().__init__(
            code="EXTRACTION_FAILED",
            message="지식 추출 결과가 검증을 통과하지 못했습니다.",
            status_code=422,
        )
