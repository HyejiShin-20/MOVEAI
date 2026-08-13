from pathlib import Path

import pytest

from app.config import PROJECT_ROOT
from app.services.dataset_embedding import (
    build_artifact,
    build_inputs,
    dataset_paths,
    default_dataset_dir,
    load_dataset,
)


EXPECTED_KNOWLEDGE_COUNT = 146


@pytest.fixture(scope="module")
def inputs():  # noqa: ANN201
    paths = dataset_paths(default_dataset_dir(Path(PROJECT_ROOT)))
    return build_inputs([load_dataset(path) for path in paths])


def test_every_dataset_knowledge_item_produces_one_input(inputs) -> None:  # noqa: ANN001
    assert len(inputs) == EXPECTED_KNOWLEDGE_COUNT
    assert len({item.knowledge_code for item in inputs}) == EXPECTED_KNOWLEDGE_COUNT


def test_all_texts_start_with_a_resolved_location(inputs) -> None:  # noqa: ANN001
    for item in inputs:
        first_line = item.embedding_text.splitlines()[0]
        assert first_line.startswith("위치: ")
        assert first_line != "위치: 위치 미상"


def test_texts_have_three_or_four_lines(inputs) -> None:  # noqa: ANN001
    for item in inputs:
        lines = item.embedding_text.splitlines()
        assert len(lines) in (3, 4)
        assert lines[1].startswith("이동: ")
        assert lines[2].startswith("내용: ")


def test_demo_knowledge_text_is_fixed(inputs) -> None:  # noqa: ANN001
    """시연 장소 B의 대표 지식. Spring EmbeddingTextBuilder가 맞춰야 할 기준 문자열이다."""
    by_code = {item.knowledge_code: item for item in inputs}

    assert by_code["K_B_001"].place_code == "PLACE_B"
    assert by_code["K_B_001"].embedding_text == (
        "위치: 후문 차량 출입구\n"
        "이동: VEHICLE / DRIVE\n"
        "내용: 배송차량은 후문으로 진입하는 것이 가장 빠르다.\n"
        "행동: 후문으로 진입한다."
    )


def test_build_artifact_matches_knowledge_embeddings_columns(inputs) -> None:  # noqa: ANN001
    sample = inputs[:2]
    vectors = [[0.1, 0.2], [0.3, 0.4]]

    artifact = build_artifact(
        sample,
        vectors,
        model="gemini-embedding-2",
        dimension=2,
        generated_at="2026-08-13T00:00:00",
    )

    assert artifact["count"] == 2
    assert artifact["items"][0]["knowledgeCode"] == sample[0].knowledge_code
    assert artifact["items"][0]["embeddingJson"] == "[0.1, 0.2]"
    assert artifact["items"][1]["embeddingDimension"] == 2


def test_build_artifact_rejects_count_mismatch(inputs) -> None:  # noqa: ANN001
    with pytest.raises(ValueError):
        build_artifact(
            inputs[:2],
            [[0.1]],
            model="gemini-embedding-2",
            dimension=1,
            generated_at="2026-08-13T00:00:00",
        )
