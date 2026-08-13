package com.moveai.guidance.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.moveai.retrieval.KnowledgeCandidate;
import com.moveai.retrieval.RankingService;

class GuidanceCardAssemblerTest {

    private final GuidanceCardAssembler assembler = new GuidanceCardAssembler();

    @Test
    void actionWithoutActionTextFallsBackToStatementAndMarksRecentUnknown() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 13, 12, 0);
        KnowledgeCandidate candidate = candidate(
                "ROUTE_GUIDANCE", null, now.minusHours(2), "UNKNOWN");

        var card = assembler.assemble(
                new RankingService.RankedCandidate(candidate, 0.8, 0.8), now);

        assertThat(card.kind()).isEqualTo("ACTION");
        assertThat(card.actionText()).isEqualTo("문이 무겁다.");
        assertThat(card.isRecentlyAdded()).isTrue();
        assertThat(card.isUnresolvedTarget()).isTrue();
    }

    @Test
    void warningKeepsNullActionAndOldSeedIsNotRecent() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 13, 12, 0);
        var card = assembler.assemble(
                new RankingService.RankedCandidate(
                        candidate("WARNING_ONLY", null, now.minusDays(30), "NODE"), 0.8, 0.8), now);

        assertThat(card.kind()).isEqualTo("WARNING");
        assertThat(card.actionText()).isNull();
        assertThat(card.isRecentlyAdded()).isFalse();
    }

    private KnowledgeCandidate candidate(
            String scope, String action, LocalDateTime publishedAt, String targetType) {
        return new KnowledgeCandidate(
                1, "K_TEST", 2, targetType, null, null, "PEDESTRIAN", "CART",
                "WARNING", null, scope, "문이 무겁다.", action, "방화문",
                KnowledgeCandidate.Conditions.empty(), publishedAt, new double[] {1, 0});
    }
}
