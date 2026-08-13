"""datasets/*.json 의 지식 146건을 임베딩해 Spring이 적재할 산출물로 떨어뜨린다.

DB에는 쓰지 않는다. 산출물은 `05A §2-3` knowledge_embeddings 컬럼과 1:1이며
Spring 임포트가 knowledge_code로 조인해 그대로 INSERT 한다.

    python scripts/embed_dataset.py --dry-run     # 텍스트만 확인 (API 호출 없음)
    python scripts/embed_dataset.py               # 실제 임베딩 생성
"""

from __future__ import annotations

import argparse
import json
import logging
import sys
import time
from datetime import datetime
from pathlib import Path


AI_SERVICE_ROOT = Path(__file__).resolve().parents[1]
PROJECT_ROOT = AI_SERVICE_ROOT.parent
sys.path.insert(0, str(AI_SERVICE_ROOT))

from app.config import get_settings  # noqa: E402
from app.errors import AppError  # noqa: E402
from app.services.dataset_embedding import (  # noqa: E402
    build_artifact,
    build_inputs,
    dataset_paths,
    default_dataset_dir,
    load_dataset,
)
from app.services.embedding import GeminiEmbeddingService  # noqa: E402


DEFAULT_OUTPUT = PROJECT_ROOT / "data" / "embeddings" / "knowledge_embeddings.json"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--out", type=Path, default=DEFAULT_OUTPUT, help="산출물 경로")
    parser.add_argument("--limit", type=int, default=None, help="앞의 N건만 처리 (점검용)")
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="임베딩 텍스트만 만들고 Gemini를 호출하지 않는다",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    # 무료 등급 쿼터(분당 100건) 때문에 146건은 재시도 대기가 걸린다.
    # 조용히 멈춰 보이지 않도록 재시도 로그를 띄운다.
    logging.basicConfig(level=logging.INFO, format="%(levelname)s %(message)s", stream=sys.stderr)
    settings = get_settings()

    paths = dataset_paths(default_dataset_dir(PROJECT_ROOT))
    inputs = build_inputs([load_dataset(path) for path in paths])
    if args.limit is not None:
        inputs = inputs[: args.limit]

    if args.dry_run:
        for item in inputs[:3]:
            print(f"--- {item.knowledge_code} ({item.place_code})")
            print(item.embedding_text)
        print(json.dumps({"count": len(inputs), "dryRun": True}, ensure_ascii=True))
        return 0

    if settings.gemini_api_key is None or not settings.gemini_api_key.get_secret_value().strip():
        print("GEMINI_API_KEY가 설정되지 않았습니다.", file=sys.stderr)
        return 2

    service = GeminiEmbeddingService(
        api_key=settings.gemini_api_key.get_secret_value(),
        model=settings.embedding_model,
        dimension=settings.embedding_dimension,
        batch_size=settings.embedding_batch_size,
    )

    started_at = time.perf_counter()
    try:
        vectors = service.embed([item.embedding_text for item in inputs])
    except AppError as exc:
        print(f"error={exc.code}: {exc.message}", file=sys.stderr)
        return 1
    elapsed_ms = round((time.perf_counter() - started_at) * 1000)

    artifact = build_artifact(
        inputs,
        vectors,
        model=service.model,
        dimension=service.dimension,
        generated_at=datetime.now().isoformat(timespec="seconds"),
    )

    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(artifact, ensure_ascii=False, indent=2), encoding="utf-8")

    # ASCII-only JSON keeps the summary readable across Windows console encodings.
    print(
        json.dumps(
            {
                "model": service.model,
                "dimension": service.dimension,
                "count": artifact["count"],
                "batchSize": service.batch_size,
                "elapsedMs": elapsed_ms,
                "out": str(args.out),
            },
            ensure_ascii=True,
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
