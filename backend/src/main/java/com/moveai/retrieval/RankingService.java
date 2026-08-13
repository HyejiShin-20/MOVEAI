package com.moveai.retrieval;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** 04 §6의 cosine + 구조 가산점 랭킹. */
public class RankingService {

    private final CosineCalculator cosineCalculator;

    public RankingService(CosineCalculator cosineCalculator) {
        this.cosineCalculator = cosineCalculator;
    }

    public List<RankedCandidate> rank(
            List<KnowledgeCandidate> candidates,
            double[] queryVector,
            SegmentContext segment,
            LocalDateTime now) {
        return candidates.stream()
                .map(candidate -> score(candidate, queryVector, segment, now))
                .sorted(Comparator.comparingDouble(RankedCandidate::score).reversed()
                        .thenComparing(result -> result.candidate().knowledgeCode()))
                .toList();
    }

    public List<RankedCandidate> rankByCosine(
            List<KnowledgeCandidate> candidates, double[] queryVector) {
        return rank(candidates, queryVector, null, null);
    }

    private RankedCandidate score(
            KnowledgeCandidate candidate,
            double[] queryVector,
            SegmentContext segment,
            LocalDateTime now) {
        double cosine = cosineCalculator.similarity(queryVector, candidate.embedding());
        double score = cosine;
        if (segment != null) {
            if (Long.valueOf(segment.segmentId()).equals(candidate.targetSegmentId())) {
                score += 0.20;
            }
            if (Long.valueOf(segment.toNodeId()).equals(candidate.targetNodeId())) {
                score += 0.12;
            }
            if (candidate.traversalMethod() != null
                    && candidate.traversalMethod().equals(segment.traversalMethod())) {
                score += 0.06;
            }
        }
        if (now != null && candidate.publishedAt() != null
                && !candidate.publishedAt().isAfter(now)
                && Duration.between(candidate.publishedAt(), now).compareTo(Duration.ofHours(24)) <= 0) {
            score += 0.05;
        }
        return new RankedCandidate(candidate, cosine, score);
    }

    public record RankedCandidate(KnowledgeCandidate candidate, double cosine, double score) {
    }
}
