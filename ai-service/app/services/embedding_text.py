"""Knowledge embedding_text 조립 규칙 (04 §5-1).

시드 146건의 벡터를 만들 때 쓴다. 승인 시점에 신규 지식을 임베딩하는
Spring `EmbeddingTextBuilder`는 **여기와 완전히 같은 문자열**을 만들어야 한다.
포맷이 갈리면 시드 벡터와 신규 벡터가 다른 공간에 놓여 랭킹이 흔들린다.

    위치: {target 이름 or target_free_text}
    이동: {movement_mode} / {traversal_method}
    내용: {statement}
    행동: {action_text}

`장소:`와 `조건:` 줄은 넣지 않는다. Place는 이미 SQL hard filter라 변별력이 없고,
조건은 statement 문장 안에 이미 들어 있어 두 번 반영되면 가중치만 왜곡된다.
"""

from __future__ import annotations

from typing import Any


UNKNOWN_LOCATION_LABEL = "위치 미상"


def build_embedding_text(
    item: dict[str, Any],
    *,
    node_names: dict[str, str],
    segment_labels: dict[str, str],
) -> str:
    lines = [
        f"위치: {resolve_location_label(item['target'], node_names, segment_labels)}",
        f"이동: {resolve_movement_label(item)}",
        f"내용: {item['statement'].strip()}",
    ]
    action_text = (item.get("action_text") or "").strip()
    if action_text:
        # action_text가 없는 지식이 146건 중 86건이다. 빈 줄을 남기지 않는다.
        lines.append(f"행동: {action_text}")
    return "\n".join(lines)


def resolve_location_label(
    target: dict[str, Any],
    node_names: dict[str, str],
    segment_labels: dict[str, str],
) -> str:
    target_type = target["target_type"]
    target_code = target.get("target_code")

    if target_type == "NODE" and target_code:
        return node_names[target_code]
    if target_type == "SEGMENT" and target_code:
        # 데이터셋 구간에는 name이 없다. 05B §5-2 knownSegments와 같은 "출발 → 도착" 형식으로 맞춘다.
        return segment_labels[target_code]

    # UNKNOWN은 억지로 노드에 붙이지 않고 기사가 말한 표현을 그대로 쓴다(절대 규칙 8).
    free_text = (target.get("target_free_text") or "").strip()
    return free_text or UNKNOWN_LOCATION_LABEL


def resolve_movement_label(item: dict[str, Any]) -> str:
    movement_mode = item["movement_mode"]
    traversal = item.get("traversal_method")
    if traversal == "OTHER":
        # OTHER 7건은 전부 custom_traversal_method가 채워져 있다. 라벨이 원문에 더 가깝다.
        traversal = (item.get("custom_traversal_method") or "").strip() or None
    if not traversal:
        return movement_mode
    return f"{movement_mode} / {traversal}"


def build_segment_label(segment: dict[str, Any], node_names: dict[str, str]) -> str:
    from_name = node_names[segment["from_node_code"]]
    to_name = node_names[segment["to_node_code"]]
    return f"{from_name} → {to_name}"
