package com.moveai.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class CandidateCollectorTest {

    private final CandidateCollector collector = new CandidateCollector();

    @Test
    void includesFromNodeOnlyOnFirstSegmentAndSeparatesUnknown() {
        List<KnowledgeCandidate> candidates = List.of(
                RetrievalFixtures.candidate(1, "FROM", "NODE", 10L, null, 1, 0),
                RetrievalFixtures.candidate(2, "TO", "NODE", 20L, null, 1, 0),
                RetrievalFixtures.candidate(3, "SEG", "SEGMENT", null, 100L, 1, 0),
                RetrievalFixtures.candidate(4, "PLACE", "PLACE", null, null, 1, 0),
                RetrievalFixtures.candidate(5, "UNKNOWN", "UNKNOWN", null, null, 1, 0),
                RetrievalFixtures.candidate(6, "OTHER_NODE", "NODE", 30L, null, 1, 0));

        SegmentContext first = new SegmentContext(
                100, 10, 20, "도착", "VEHICLE", "DRIVE", null, "이동", true);
        SegmentContext later = new SegmentContext(
                100, 10, 20, "도착", "VEHICLE", "DRIVE", null, "이동", false);

        assertThat(collector.collect(candidates, first).structural())
                .extracting(KnowledgeCandidate::knowledgeCode)
                .containsExactly("FROM", "TO", "SEG", "PLACE");
        assertThat(collector.collect(candidates, later).structural())
                .extracting(KnowledgeCandidate::knowledgeCode)
                .containsExactly("TO", "SEG", "PLACE");
        assertThat(collector.collect(candidates, first).unresolved())
                .extracting(KnowledgeCandidate::knowledgeCode)
                .containsExactly("UNKNOWN");
    }
}
