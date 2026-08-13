import copy
import json
from pathlib import Path
from types import SimpleNamespace

import pytest

from app.errors import ExtractionFailedError
from app.schemas import KnowledgeExtractionRequest, KnowledgeExtractionResponse
from app.services.extraction import (
    KnowledgeExtractionService,
    gemini_response_schema,
    parse_and_validate_output,
    validate_extraction_response,
)
from tests.test_extraction_api import REQUEST_BODY, VALID_RESPONSE


PROJECT_ROOT = Path(__file__).resolve().parents[2]
NULLABLE_ITEM_FIELDS = (
    "custom_category_label",
    "custom_fact_type_label",
    "traversal_method",
    "custom_traversal_method",
    "access_state",
    "action_text",
)
CONDITION_FIELDS = (
    "vehicle_class",
    "min_tonnage",
    "max_tonnage",
    "max_vehicle_height_m",
    "max_vehicle_width_m",
    "active_time_start",
    "active_time_end",
    "active_days",
    "extra_condition_text",
)


class FakeInteractions:
    def __init__(self, outputs: list[str]) -> None:
        self.outputs = iter(outputs)
        self.calls = []

    def create(self, **kwargs):
        self.calls.append(kwargs)
        return SimpleNamespace(output_text=next(self.outputs))


def extraction_request() -> KnowledgeExtractionRequest:
    return KnowledgeExtractionRequest.model_validate(REQUEST_BODY)


def test_service_uses_structured_output_and_disables_storage() -> None:
    output = json.dumps(VALID_RESPONSE, ensure_ascii=False)
    interactions = FakeInteractions([output])
    service = KnowledgeExtractionService(
        api_key="unused",
        model="gemini-3.5-flash-lite",
        thinking_level="minimal",
        client=SimpleNamespace(interactions=interactions),
    )

    result = service.extract(extraction_request())

    assert result.items[0].statement == "후문으로 진입하는 것이 가장 빠르다."
    call = interactions.calls[0]
    assert call["model"] == "gemini-3.5-flash-lite"
    assert call["generation_config"]["thinking_level"] == "minimal"
    assert call["response_format"]["mime_type"] == "application/json"
    assert call["store"] is False


def test_gemini_schema_keeps_pydantic_constraints_except_max_items() -> None:
    pydantic_schema = KnowledgeExtractionResponse.model_json_schema()
    schema = gemini_response_schema()
    serialized = json.dumps(schema)

    expected_schema = copy.deepcopy(pydantic_schema)
    _remove_schema_key(expected_schema, "maxItems")
    assert schema == expected_schema
    assert '"maxItems"' not in serialized
    assert '"additionalProperties": false' in serialized
    assert '"pattern"' in serialized
    assert '"minLength"' in serialized
    assert '"enum"' in serialized
    assert '"$ref"' in serialized


def _remove_schema_key(value, key_to_remove: str) -> None:
    if isinstance(value, dict):
        value.pop(key_to_remove, None)
        for child in value.values():
            _remove_schema_key(child, key_to_remove)
    elif isinstance(value, list):
        for child in value:
            _remove_schema_key(child, key_to_remove)


def test_service_retries_once_after_semantic_validation_failure() -> None:
    invalid = copy.deepcopy(VALID_RESPONSE)
    invalid["items"][0]["target"]["target_code"] = "NODE_NOT_ALLOWED"
    interactions = FakeInteractions(
        [
            json.dumps(invalid, ensure_ascii=False),
            json.dumps(VALID_RESPONSE, ensure_ascii=False),
        ]
    )
    service = KnowledgeExtractionService(
        api_key="unused",
        model="gemini-3.5-flash-lite",
        thinking_level="minimal",
        client=SimpleNamespace(interactions=interactions),
    )

    result = service.extract(extraction_request())

    assert len(interactions.calls) == 2
    assert "knownNodes에 없다" in interactions.calls[1]["input"]
    assert result.items


def test_service_fails_after_exactly_one_retry() -> None:
    invalid = copy.deepcopy(VALID_RESPONSE)
    invalid["items"][0]["source_excerpt"] = "원문에 없는 구절"
    output = json.dumps(invalid, ensure_ascii=False)
    interactions = FakeInteractions([output, output])
    service = KnowledgeExtractionService(
        api_key="unused",
        model="gemini-3.5-flash-lite",
        thinking_level="minimal",
        client=SimpleNamespace(interactions=interactions),
    )

    with pytest.raises(ExtractionFailedError) as error:
        service.extract(extraction_request())

    assert len(interactions.calls) == 2
    assert "source_excerpt" in error.value.details[0]


def test_numeric_condition_must_appear_in_transcript() -> None:
    invalid = copy.deepcopy(VALID_RESPONSE)
    invalid["items"][0]["conditions"]["max_vehicle_height_m"] = 2.3

    errors = parse_and_validate_output(
        json.dumps(invalid, ensure_ascii=False), extraction_request()
    )

    assert any("max_vehicle_height_m=2.3" in error for error in errors)


def test_all_146_golden_items_match_runtime_schema_and_semantic_rules() -> None:
    total_items = 0
    for path in sorted((PROJECT_ROOT / "datasets").glob("synthetic_dataset_*.json")):
        dataset = json.loads(path.read_text(encoding="utf-8"))
        node_names = {node["node_code"]: node["name"] for node in dataset["nodes"]}
        known_nodes = [
            {"code": node["node_code"], "name": node["name"]}
            for node in dataset["nodes"]
        ]
        known_segments = [
            {
                "code": segment["segment_code"],
                "name": (
                    f"{node_names[segment['from_node_code']]} → "
                    f"{node_names[segment['to_node_code']]}"
                ),
            }
            for segment in dataset["route_segments"]
        ]

        for report in dataset["field_reports"]:
            items = [_runtime_item(item) for item in report["expected_knowledge_items"]]
            response = KnowledgeExtractionResponse.model_validate({"items": items})
            request = KnowledgeExtractionRequest.model_validate(
                {
                    "placeName": dataset["place"]["name"],
                    "transcript": report["transcript"],
                    "scopeNodeName": node_names.get(report["selected_scope_node_code"]),
                    "knownNodes": known_nodes,
                    "knownSegments": known_segments,
                }
            )

            assert validate_extraction_response(response, request) == [], report["report_code"]
            total_items += len(items)

    assert total_items == 146


def _runtime_item(golden_item: dict) -> dict:
    item = {key: value for key, value in golden_item.items() if key != "knowledge_code"}
    for field in NULLABLE_ITEM_FIELDS:
        item.setdefault(field, None)
    item["conditions"] = dict(item["conditions"])
    for field in CONDITION_FIELDS:
        item["conditions"].setdefault(field, None)
    return item
