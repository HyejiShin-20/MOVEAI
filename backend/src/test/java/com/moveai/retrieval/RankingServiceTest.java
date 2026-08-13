package com.moveai.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class RankingServiceTest {

    private final RankingService ranking = new RankingService(new CosineCalculator());

    @Test
    void structuralBonusCanOutrankSlightlyHigherCosine() {
        KnowledgeCandidate direct = RetrievalFixtures.candidate(
                1, "DIRECT", "SEGMENT", null, 100L, 0.8, 0.6);
        KnowledgeCandidate semantic = RetrievalFixtures.candidate(
                2, "SEMANTIC", "PLACE", null, null, 1, 0);
        SegmentContext segment = new SegmentContext(
                100, 10, 20, "도착", "VEHICLE", "WALK", null, "이동", false);

        assertThat(ranking.rank(
                        List.of(semantic, direct), new double[] {1, 0}, segment,
                        LocalDateTime.of(2026, 8, 13, 0, 0)))
                .extracting(result -> result.candidate().knowledgeCode())
                .containsExactly("DIRECT", "SEMANTIC");
    }
}
