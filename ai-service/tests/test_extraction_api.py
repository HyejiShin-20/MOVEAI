from fastapi.testclient import TestClient

from app.errors import ExtractionFailedError
from app.main import create_app
from app.schemas import KnowledgeExtractionResponse
from tests.test_stt_api import make_settings


REQUEST_BODY = {
    "placeName": "가온스퀘어 오피스타워",
    "transcript": "후문으로 들어가면 제일 빨라요.",
    "scopeNodeName": "후문 차량 출입구",
    "knownNodes": [{"code": "NODE_B_03", "name": "후문 차량 출입구"}],
    "knownSegments": [],
}

VALID_RESPONSE = {
    "items": [
        {
            "target": {
                "target_type": "NODE",
                "target_code": "NODE_B_03",
                "target_resolution_status": "RESOLVED",
                "target_free_text": None,
            },
            "category": "ACCESS",
            "custom_category_label": None,
            "fact_type": "INSTRUCTION",
            "custom_fact_type_label": None,
            "movement_mode": "VEHICLE",
            "traversal_method": "DRIVE",
            "custom_traversal_method": None,
            "access_state": None,
            "statement": "후문으로 진입하는 것이 가장 빠르다.",
            "action_text": "후문으로 진입한다.",
            "source_excerpt": "후문으로 들어가면 제일 빨라요.",
            "conditions": {
                "vehicle_class": None,
                "min_tonnage": None,
                "max_tonnage": None,
                "max_vehicle_height_m": None,
                "max_vehicle_width_m": None,
                "active_time_start": None,
                "active_time_end": None,
                "active_days": None,
                "extra_condition_text": None,
            },
            "usage_scope": "ACTION_GUIDANCE",
        }
    ]
}


class FakeExtractionService:
    def __init__(self) -> None:
        self.requests = []

    def extract(self, request):
        self.requests.append(request)
        return KnowledgeExtractionResponse.model_validate(VALID_RESPONSE)


class FailingExtractionService:
    def extract(self, request):
        raise ExtractionFailedError(["invalid target"])


def test_extract_knowledge_returns_structured_items_without_knowledge_code() -> None:
    service = FakeExtractionService()
    client = TestClient(
        create_app(settings=make_settings(), extraction_service=service)
    )

    response = client.post("/extract-knowledge", json=REQUEST_BODY)

    assert response.status_code == 200
    assert response.json() == VALID_RESPONSE
    assert "knowledge_code" not in response.json()["items"][0]
    assert service.requests[0].transcript == REQUEST_BODY["transcript"]


def test_extract_knowledge_rejects_duplicate_context_codes() -> None:
    body = dict(REQUEST_BODY)
    body["knownNodes"] = [
        {"code": "NODE_B_03", "name": "후문"},
        {"code": "NODE_B_03", "name": "중복 후문"},
    ]
    client = TestClient(
        create_app(settings=make_settings(), extraction_service=FakeExtractionService())
    )

    response = client.post("/extract-knowledge", json=body)

    assert response.status_code == 422


def test_extract_knowledge_maps_semantic_failure() -> None:
    client = TestClient(
        create_app(settings=make_settings(), extraction_service=FailingExtractionService())
    )

    response = client.post("/extract-knowledge", json=REQUEST_BODY)

    assert response.status_code == 422
    assert response.json()["error"]["code"] == "EXTRACTION_FAILED"
