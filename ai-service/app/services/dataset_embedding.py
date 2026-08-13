"""datasets/*.json 의 지식 146건에서 임베딩 입력을 만든다.

산출물은 `05A §2-3` `knowledge_embeddings` 컬럼과 1:1로 맞춰 둔다.
Spring 임포트가 `knowledge_code`로 조인해 그대로 INSERT 할 수 있어야 한다.
임포트 자체(DB 쓰기)는 Spring 담당이며 여기서는 하지 않는다.
"""

from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from app.services.embedding_text import build_embedding_text, build_segment_label


DATASET_FILENAMES = (
    "synthetic_dataset_A.json",
    "synthetic_dataset_B.json",
    "synthetic_dataset_C.json",
    "synthetic_dataset_D.json",
)


@dataclass(frozen=True)
class KnowledgeEmbeddingInput:
    knowledge_code: str
    place_code: str
    embedding_text: str


def default_dataset_dir(project_root: Path) -> Path:
    return project_root / "datasets"


def dataset_paths(dataset_dir: Path) -> list[Path]:
    paths = [dataset_dir / name for name in DATASET_FILENAMES]
    missing = [path.name for path in paths if not path.is_file()]
    if missing:
        raise FileNotFoundError(f"데이터셋 파일이 없습니다: {', '.join(missing)}")
    return paths


def load_dataset(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def build_inputs(datasets: list[dict[str, Any]]) -> list[KnowledgeEmbeddingInput]:
    """장소 4개를 합쳐 지식 코드 순서를 유지한 채 임베딩 입력을 만든다."""
    node_names: dict[str, str] = {}
    segment_labels: dict[str, str] = {}
    for dataset in datasets:
        for node in dataset["nodes"]:
            node_names[node["node_code"]] = node["name"]
    for dataset in datasets:
        for segment in dataset["route_segments"]:
            segment_labels[segment["segment_code"]] = build_segment_label(segment, node_names)

    inputs: list[KnowledgeEmbeddingInput] = []
    seen: set[str] = set()
    for dataset in datasets:
        place_code = dataset["place"]["place_code"]
        for report in dataset["field_reports"]:
            for item in report["expected_knowledge_items"]:
                knowledge_code = item["knowledge_code"]
                if knowledge_code in seen:
                    raise ValueError(f"지식 코드가 중복됩니다: {knowledge_code}")
                seen.add(knowledge_code)
                inputs.append(
                    KnowledgeEmbeddingInput(
                        knowledge_code=knowledge_code,
                        place_code=place_code,
                        embedding_text=build_embedding_text(
                            item,
                            node_names=node_names,
                            segment_labels=segment_labels,
                        ),
                    )
                )
    return inputs


def build_artifact(
    inputs: list[KnowledgeEmbeddingInput],
    vectors: list[list[float]],
    *,
    model: str,
    dimension: int,
    generated_at: str,
) -> dict[str, Any]:
    if len(inputs) != len(vectors):
        raise ValueError(f"지식 {len(inputs)}건과 벡터 {len(vectors)}건이 맞지 않습니다.")
    return {
        "generatedAt": generated_at,
        "embeddingModel": model,
        "embeddingDimension": dimension,
        "count": len(inputs),
        "items": [
            {
                "knowledgeCode": item.knowledge_code,
                "placeCode": item.place_code,
                "embeddingModel": model,
                "embeddingDimension": dimension,
                "embeddingText": item.embedding_text,
                # LONGTEXT 컬럼에 그대로 들어가도록 문자열로 굳혀 둔다.
                "embeddingJson": json.dumps(vector, ensure_ascii=True),
            }
            for item, vector in zip(inputs, vectors)
        ],
    }
