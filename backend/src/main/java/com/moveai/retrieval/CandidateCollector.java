package com.moveai.retrieval;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 04 §1의 도착 예고 규칙으로 구조 후보와 UNRESOLVED 후보를 분리한다. */
public class CandidateCollector {

    public CandidatePool collect(List<KnowledgeCandidate> candidates, SegmentContext segment) {
        Map<Long, KnowledgeCandidate> structural = new LinkedHashMap<>();
        Map<Long, KnowledgeCandidate> unresolved = new LinkedHashMap<>();

        for (KnowledgeCandidate candidate : candidates) {
            if ("UNKNOWN".equals(candidate.targetType())) {
                unresolved.putIfAbsent(candidate.id(), candidate);
                continue;
            }
            if (isStructurallyConnected(candidate, segment)) {
                structural.putIfAbsent(candidate.id(), candidate);
            }
        }
        return new CandidatePool(List.copyOf(structural.values()), List.copyOf(unresolved.values()));
    }

    private boolean isStructurallyConnected(KnowledgeCandidate candidate, SegmentContext segment) {
        if ("PLACE".equals(candidate.targetType())) {
            return true;
        }
        if ("SEGMENT".equals(candidate.targetType())) {
            return Long.valueOf(segment.segmentId()).equals(candidate.targetSegmentId());
        }
        if (!"NODE".equals(candidate.targetType())) {
            return false;
        }
        if (Long.valueOf(segment.toNodeId()).equals(candidate.targetNodeId())) {
            return true;
        }
        return segment.firstSegment()
                && Long.valueOf(segment.fromNodeId()).equals(candidate.targetNodeId());
    }

    public record CandidatePool(
            List<KnowledgeCandidate> structural,
            List<KnowledgeCandidate> unresolved) {
    }
}
