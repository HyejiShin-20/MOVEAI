package com.moveai.retrieval;

import java.time.LocalDateTime;

final class RetrievalFixtures {

    private RetrievalFixtures() {
    }

    static KnowledgeCandidate candidate(
            long id, String code, String targetType, Long nodeId, Long segmentId,
            String movement, String traversal, KnowledgeCandidate.Conditions conditions,
            String factType, String accessState, double... vector) {
        return new KnowledgeCandidate(
                id, code, 2L, targetType, nodeId, segmentId, movement, traversal,
                factType, accessState, "WARNING_ONLY", code + " 내용", null, code + " 위치",
                conditions, LocalDateTime.of(2026, 7, 1, 0, 0), vector);
    }

    static KnowledgeCandidate candidate(
            long id, String code, String targetType, Long nodeId, Long segmentId, double... vector) {
        return candidate(id, code, targetType, nodeId, segmentId, "VEHICLE", "DRIVE",
                KnowledgeCandidate.Conditions.empty(), "WARNING", "CONDITIONAL", vector);
    }
}
