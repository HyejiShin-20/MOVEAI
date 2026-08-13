package com.moveai.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class HybridSearchServiceTest {

    @Test
    void reservesFourStructuralSlotsAndOneUnresolvedSlot() {
        List<KnowledgeCandidate> candidates = new ArrayList<>();
        for (int index = 1; index <= 6; index++) {
            candidates.add(RetrievalFixtures.candidate(
                    index, "S" + index, "NODE", 20L, null, 1, index / 10.0));
        }
        candidates.add(RetrievalFixtures.candidate(20, "U1", "UNKNOWN", null, null, 1, 0));
        candidates.add(RetrievalFixtures.candidate(21, "U2", "UNKNOWN", null, null, 0, 1));

        HybridSearchService service = new HybridSearchService(
                new CandidateCollector(), new ConditionEvaluator(),
                new RankingService(new CosineCalculator()));
        SegmentContext segment = new SegmentContext(
                100, 10, 20, "도착", "VEHICLE", "DRIVE", null, "이동", false);
        SearchContext context = new SearchContext(
                null, null, null, null, "VEHICLE", null, null);

        List<RankingService.RankedCandidate> results = service.search(
                candidates, segment, context, new double[] {1, 0},
                LocalDateTime.of(2026, 8, 13, 0, 0));

        assertThat(results).hasSize(5);
        assertThat(results.subList(0, 4))
                .allMatch(result -> !"UNKNOWN".equals(result.candidate().targetType()));
        assertThat(results.get(4).candidate().knowledgeCode()).isEqualTo("U1");
    }
}
