import pytest

from app.services.embedding_text import (
    build_embedding_text,
    build_segment_label,
    resolve_movement_label,
)


NODE_NAMES = {
    "NODE_B_03": "후문 차량 출입구",
    "NODE_B_04": "지하주차장 진입 램프",
}
SEGMENT_LABELS = {"SEG_B_01": "후문 차량 출입구 → 지하주차장 진입 램프"}


def make_item(**overrides):  # noqa: ANN201
    item = {
        "target": {
            "target_type": "NODE",
            "target_code": "NODE_B_03",
            "target_resolution_status": "RESOLVED",
            "target_free_text": None,
        },
        "movement_mode": "VEHICLE",
        "traversal_method": "DRIVE",
        "custom_traversal_method": None,
        "statement": "배송차량은 후문으로 진입하는 것이 가장 빠르다.",
        "action_text": "후문으로 진입한다.",
    }
    item.update(overrides)
    return item


def build(item) -> str:  # noqa: ANN001
    return build_embedding_text(item, node_names=NODE_NAMES, segment_labels=SEGMENT_LABELS)


def test_node_target_uses_node_name_and_four_lines() -> None:
    assert build(make_item()) == (
        "위치: 후문 차량 출입구\n"
        "이동: VEHICLE / DRIVE\n"
        "내용: 배송차량은 후문으로 진입하는 것이 가장 빠르다.\n"
        "행동: 후문으로 진입한다."
    )


def test_place_line_and_condition_line_are_never_emitted() -> None:
    # 04 §5-1. Place는 이미 SQL hard filter이고 조건은 statement에 이미 들어 있다.
    text = build(make_item())

    assert "장소:" not in text
    assert "조건:" not in text


def test_action_line_is_dropped_when_action_text_is_missing() -> None:
    text = build(make_item(action_text=None))

    assert text.splitlines() == [
        "위치: 후문 차량 출입구",
        "이동: VEHICLE / DRIVE",
        "내용: 배송차량은 후문으로 진입하는 것이 가장 빠르다.",
    ]


def test_segment_target_uses_from_to_label() -> None:
    item = make_item(
        target={
            "target_type": "SEGMENT",
            "target_code": "SEG_B_01",
            "target_resolution_status": "RESOLVED",
            "target_free_text": None,
        }
    )

    assert build(item).startswith("위치: 후문 차량 출입구 → 지하주차장 진입 램프")


def test_unknown_target_keeps_driver_wording() -> None:
    item = make_item(
        target={
            "target_type": "UNKNOWN",
            "target_code": None,
            "target_resolution_status": "UNRESOLVED",
            "target_free_text": "하역장 옆 카트 대기 공간",
        }
    )

    assert build(item).startswith("위치: 하역장 옆 카트 대기 공간")


def test_other_traversal_method_uses_custom_label() -> None:
    item = make_item(traversal_method="OTHER", custom_traversal_method="지게차")

    assert resolve_movement_label(item) == "VEHICLE / 지게차"
    assert "이동: VEHICLE / 지게차" in build(item)


def test_movement_line_omits_traversal_when_missing() -> None:
    item = make_item(traversal_method=None)

    assert "이동: VEHICLE\n" in build(item)


def test_segment_label_is_built_from_node_names() -> None:
    segment = {"from_node_code": "NODE_B_03", "to_node_code": "NODE_B_04"}

    assert build_segment_label(segment, NODE_NAMES) == "후문 차량 출입구 → 지하주차장 진입 램프"


def test_unknown_node_code_fails_loudly() -> None:
    item = make_item(
        target={
            "target_type": "NODE",
            "target_code": "NODE_ZZ_99",
            "target_resolution_status": "RESOLVED",
            "target_free_text": None,
        }
    )

    with pytest.raises(KeyError):
        build(item)
