from __future__ import annotations

import json
import logging
import re
from pathlib import Path
from typing import Any

from pydantic import ValidationError

from app.errors import ExtractionFailedError, ExtractionProviderError
from app.schemas import (
    ExtractedKnowledgeItem,
    KnowledgeExtractionRequest,
    KnowledgeExtractionResponse,
)
from app.services.gemini import create_gemini_client


logger = logging.getLogger(__name__)
PROMPT_PATH = Path(__file__).resolve().parents[1] / "prompts" / "knowledge_extraction.txt"
BASE_PROMPT = PROMPT_PATH.read_text(encoding="utf-8").strip()
NUMERIC_CONDITION_FIELDS = (
    "min_tonnage",
    "max_tonnage",
    "max_vehicle_height_m",
    "max_vehicle_width_m",
)


class KnowledgeExtractionService:
    def __init__(
        self,
        *,
        api_key: str,
        model: str,
        thinking_level: str,
        client: Any | None = None,
    ) -> None:
        self.model = model
        self.thinking_level = thinking_level
        self._client = client or create_gemini_client(api_key)

    def extract(self, request: KnowledgeExtractionRequest) -> KnowledgeExtractionResponse:
        validation_errors: list[str] = []
        for attempt in range(2):
            prompt = build_extraction_prompt(request, validation_errors)
            try:
                interaction = self._client.interactions.create(
                    model=self.model,
                    input=prompt,
                    response_format={
                        "type": "text",
                        "mime_type": "application/json",
                        "schema": gemini_response_schema(),
                    },
                    generation_config={
                        "thinking_level": self.thinking_level,
                        "max_output_tokens": 8192,
                    },
                    store=False,
                )
            except Exception as exc:
                logger.warning(
                    "Gemini extraction request failed: model=%s error_type=%s",
                    self.model,
                    type(exc).__name__,
                )
                raise ExtractionProviderError() from exc

            output_text = (getattr(interaction, "output_text", None) or "").strip()
            validation_errors = parse_and_validate_output(output_text, request)
            if not validation_errors:
                return KnowledgeExtractionResponse.model_validate_json(output_text)

            logger.info(
                "Extraction validation failed: model=%s attempt=%s error_count=%s",
                self.model,
                attempt + 1,
                len(validation_errors),
            )

        raise ExtractionFailedError(validation_errors)


def build_extraction_prompt(
    request: KnowledgeExtractionRequest,
    previous_errors: list[str] | None = None,
) -> str:
    context = {
        "placeName": request.placeName,
        "scopeNodeName": request.scopeNodeName,
        "knownNodes": [node.model_dump() for node in request.knownNodes],
        "knownSegments": [segment.model_dump() for segment in request.knownSegments],
        "transcript": request.transcript,
    }
    sections = [BASE_PROMPT, "입력 데이터:\n" + json.dumps(context, ensure_ascii=False)]
    if previous_errors:
        error_lines = "\n".join(f"- {error}" for error in previous_errors)
        sections.append(
            "이전 출력은 아래 서버 검증을 통과하지 못했다. 같은 오류를 수정해서 다시 추출하라:\n"
            + error_lines
        )
    return "\n\n".join(sections)


def parse_and_validate_output(
    output_text: str,
    request: KnowledgeExtractionRequest,
) -> list[str]:
    if not output_text:
        return ["모델이 빈 출력을 반환했다."]
    try:
        response = KnowledgeExtractionResponse.model_validate_json(output_text)
    except ValidationError as exc:
        return [f"JSON 스키마 오류: {error['loc']} {error['msg']}" for error in exc.errors()]
    except ValueError as exc:
        return [f"JSON 파싱 오류: {exc}"]
    return validate_extraction_response(response, request)


def gemini_response_schema() -> dict[str, Any]:
    """Return the complete Pydantic schema without the incompatible maxItems."""
    return _without_schema_key(
        KnowledgeExtractionResponse.model_json_schema(),
        "maxItems",
    )


def _without_schema_key(value: Any, key_to_remove: str) -> Any:
    if isinstance(value, list):
        return [_without_schema_key(item, key_to_remove) for item in value]
    if not isinstance(value, dict):
        return value
    return {
        key: _without_schema_key(item, key_to_remove)
        for key, item in value.items()
        if key != key_to_remove
    }


def validate_extraction_response(
    response: KnowledgeExtractionResponse,
    request: KnowledgeExtractionRequest,
) -> list[str]:
    errors: list[str] = []
    node_codes = {node.code for node in request.knownNodes}
    segment_codes = {segment.code for segment in request.knownSegments}

    for index, item in enumerate(response.items):
        prefix = f"items[{index}]"
        errors.extend(_validate_target(item, node_codes, segment_codes, prefix))
        errors.extend(_validate_custom_labels(item, prefix))
        errors.extend(_validate_usage_scope(item, prefix))

        if normalize_text(item.source_excerpt) not in normalize_text(request.transcript):
            errors.append(f"{prefix}.source_excerpt가 transcript의 연속 구절이 아니다.")

        conditions = item.conditions.model_dump()
        for field in NUMERIC_CONDITION_FIELDS:
            value = conditions[field]
            if value is not None and not transcript_contains_number(request.transcript, value):
                errors.append(f"{prefix}.conditions.{field}={value}가 transcript에 없다.")

    return errors


def _validate_target(
    item: ExtractedKnowledgeItem,
    node_codes: set[str],
    segment_codes: set[str],
    prefix: str,
) -> list[str]:
    target = item.target
    if target.target_type == "NODE":
        if target.target_code not in node_codes:
            return [f"{prefix}.target.target_code가 knownNodes에 없다."]
        if target.target_resolution_status != "RESOLVED" or target.target_free_text is not None:
            return [f"{prefix}.target NODE는 RESOLVED이고 free_text가 null이어야 한다."]
    elif target.target_type == "SEGMENT":
        if target.target_code not in segment_codes:
            return [f"{prefix}.target.target_code가 knownSegments에 없다."]
        if target.target_resolution_status != "RESOLVED" or target.target_free_text is not None:
            return [f"{prefix}.target SEGMENT는 RESOLVED이고 free_text가 null이어야 한다."]
    elif target.target_type == "UNKNOWN":
        if target.target_code is not None:
            return [f"{prefix}.target UNKNOWN은 target_code가 null이어야 한다."]
        if target.target_resolution_status != "UNRESOLVED" or not _has_text(target.target_free_text):
            return [f"{prefix}.target UNKNOWN은 UNRESOLVED이고 free_text가 필요하다."]
    elif target.target_type == "PLACE":
        if target.target_code is not None or target.target_resolution_status != "RESOLVED":
            return [f"{prefix}.target PLACE는 code 없이 RESOLVED여야 한다."]
    return []


def _validate_custom_labels(item: ExtractedKnowledgeItem, prefix: str) -> list[str]:
    errors: list[str] = []
    required_for_other = (
        ("category", item.category, item.custom_category_label),
        ("fact_type", item.fact_type, item.custom_fact_type_label),
        ("traversal_method", item.traversal_method, item.custom_traversal_method),
    )
    for field, enum_value, custom_value in required_for_other:
        if enum_value == "OTHER" and not _has_text(custom_value):
            errors.append(f"{prefix}.{field}=OTHER이면 custom 값이 필요하다.")
    if item.category != "OTHER" and item.custom_category_label is not None:
        errors.append(f"{prefix}.category가 OTHER가 아니면 custom 값은 null이어야 한다.")
    return errors


def _validate_usage_scope(item: ExtractedKnowledgeItem, prefix: str) -> list[str]:
    if item.usage_scope == "WARNING_ONLY" and item.action_text is not None:
        return [f"{prefix}.usage_scope=WARNING_ONLY이면 action_text는 null이어야 한다."]
    if item.usage_scope in {"ACTION_GUIDANCE", "ROUTE_GUIDANCE"} and not _has_text(item.action_text):
        return [f"{prefix}.usage_scope={item.usage_scope}이면 action_text가 필요하다."]
    return []


def normalize_text(value: str) -> str:
    return " ".join(value.split())


def transcript_contains_number(transcript: str, value: float) -> bool:
    forms = {str(value)}
    if float(value).is_integer():
        forms.add(str(int(value)))
    normalized = transcript.replace(",", "")
    return any(re.search(rf"(?<!\d){re.escape(form)}(?!\d)", normalized) for form in forms)


def _has_text(value: str | None) -> bool:
    return value is not None and bool(value.strip())
