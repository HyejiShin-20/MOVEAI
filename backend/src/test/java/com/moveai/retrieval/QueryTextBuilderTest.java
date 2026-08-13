package com.moveai.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class QueryTextBuilderTest {

    private final QueryTextBuilder builder = new QueryTextBuilder();

    @Test
    void buildsSegmentQueryInKnowledgeFormat() {
        SegmentContext segment = new SegmentContext(
                1, 10, 20, "하역장 방화문", "PEDESTRIAN", "CART", null,
                "카트에 물품을 적재하여 방화문까지 이동한다", false);

        assertThat(builder.build(segment)).isEqualTo(
                "위치: 하역장 방화문\n"
                        + "이동: PEDESTRIAN / CART\n"
                        + "내용: 카트에 물품을 적재하여 방화문까지 이동한다");
    }
}
