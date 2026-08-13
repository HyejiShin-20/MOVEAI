package com.moveai.knowledge.embedding;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmbeddingTextBuilderTest {

    private final EmbeddingTextBuilder builder = new EmbeddingTextBuilder();

    @Test
    void matchesPythonSeedTextForKB001() {
        EmbeddingTextBuilder.Source source = new EmbeddingTextBuilder.Source(
                "K_B_001", "NODE", null, "가온스퀘어 오피스타워", "후문 차량 출입구",
                null, null, "VEHICLE", "DRIVE", null,
                "배송차량은 후문으로 진입하는 것이 가장 빠르다.", "후문으로 진입한다.");

        assertThat(builder.build(source)).isEqualTo(
                "위치: 후문 차량 출입구\n"
                        + "이동: VEHICLE / DRIVE\n"
                        + "내용: 배송차량은 후문으로 진입하는 것이 가장 빠르다.\n"
                        + "행동: 후문으로 진입한다.");
    }

    @Test
    void usesSegmentLabelAndDropsMissingActionLine() {
        EmbeddingTextBuilder.Source source = new EmbeddingTextBuilder.Source(
                "K_X", "SEGMENT", null, "장소", null, "하역장", "방화문",
                "PEDESTRIAN", "CART", null, "카트로 이동한다.", null);

        assertThat(builder.build(source)).isEqualTo(
                "위치: 하역장 → 방화문\n이동: PEDESTRIAN / CART\n내용: 카트로 이동한다.");
    }

    @Test
    void keepsUnknownFreeTextAndCustomTraversal() {
        EmbeddingTextBuilder.Source source = new EmbeddingTextBuilder.Source(
                "K_X", "UNKNOWN", "화단 옆 좁아지는 구간", "장소", null, null, null,
                "VEHICLE", "OTHER", "후진", "폭이 좁다.", null);

        assertThat(builder.build(source)).startsWith(
                "위치: 화단 옆 좁아지는 구간\n이동: VEHICLE / 후진\n");
    }
}
