"""Run one golden transcript through Gemini knowledge extraction."""

from __future__ import annotations

import argparse
import json
import sys
import time
from pathlib import Path


AI_SERVICE_ROOT = Path(__file__).resolve().parents[1]
PROJECT_ROOT = AI_SERVICE_ROOT.parent
sys.path.insert(0, str(AI_SERVICE_ROOT))

from app.config import get_settings  # noqa: E402
from app.errors import AppError  # noqa: E402
from app.schemas import KnowledgeExtractionRequest  # noqa: E402
from app.services.extraction import KnowledgeExtractionService  # noqa: E402


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dataset", choices="ABCD", default="B")
    parser.add_argument("--report", default="REPORT_B_01")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    dataset_path = PROJECT_ROOT / "datasets" / f"synthetic_dataset_{args.dataset}.json"
    dataset = json.loads(dataset_path.read_text(encoding="utf-8"))
    report = next(
        (item for item in dataset["field_reports"] if item["report_code"] == args.report),
        None,
    )
    if report is None:
        print(f"report를 찾을 수 없습니다: {args.report}", file=sys.stderr)
        return 2

    node_names = {node["node_code"]: node["name"] for node in dataset["nodes"]}
    request = KnowledgeExtractionRequest.model_validate(
        {
            "placeName": dataset["place"]["name"],
            "transcript": report["transcript"],
            "scopeNodeName": node_names.get(report["selected_scope_node_code"]),
            "knownNodes": [
                {"code": node["node_code"], "name": node["name"]}
                for node in dataset["nodes"]
            ],
            "knownSegments": [
                {
                    "code": segment["segment_code"],
                    "name": (
                        f"{node_names[segment['from_node_code']]} → "
                        f"{node_names[segment['to_node_code']]}"
                    ),
                }
                for segment in dataset["route_segments"]
            ],
        }
    )

    settings = get_settings()
    if settings.gemini_api_key is None or not settings.gemini_api_key.get_secret_value().strip():
        print("GEMINI_API_KEY가 설정되지 않았습니다.", file=sys.stderr)
        return 2

    service = KnowledgeExtractionService(
        api_key=settings.gemini_api_key.get_secret_value(),
        model=settings.llm_model,
        thinking_level=settings.llm_thinking_level,
    )
    started_at = time.perf_counter()
    try:
        result = service.extract(request)
    except AppError as exc:
        print(f"error={exc.code}: {exc.message}", file=sys.stderr)
        details = getattr(exc, "details", None)
        if details:
            print(
                "validation_errors=" + json.dumps(details, ensure_ascii=True),
                file=sys.stderr,
            )
        return 1

    payload = {
        "model": settings.llm_model,
        "report": report["report_code"],
        "elapsedMs": round((time.perf_counter() - started_at) * 1000),
        "expectedItemCount": len(report["expected_knowledge_items"]),
        "actualItemCount": len(result.items),
        "result": result.model_dump(mode="json"),
    }
    print(json.dumps(payload, ensure_ascii=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
