package com.moveai.retrieval;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 구조 연결 4자리 + UNRESOLVED 의미검색 1자리의 하이브리드 검색. */
public class HybridSearchService {

    private static final int STRUCTURAL_LIMIT = 4;
    private static final int UNRESOLVED_LIMIT = 1;

    private final CandidateCollector candidateCollector;
    private final ConditionEvaluator conditionEvaluator;
    private final RankingService rankingService;

    public HybridSearchService(
            CandidateCollector candidateCollector,
            ConditionEvaluator conditionEvaluator,
            RankingService rankingService) {
        this.candidateCollector = candidateCollector;
        this.conditionEvaluator = conditionEvaluator;
        this.rankingService = rankingService;
    }

    public List<RankingService.RankedCandidate> search(
            List<KnowledgeCandidate> placeCandidates,
            SegmentContext segment,
            SearchContext context,
            double[] queryVector,
            LocalDateTime now) {
        CandidateCollector.CandidatePool pool = candidateCollector.collect(placeCandidates, segment);
        List<RankingService.RankedCandidate> result = new ArrayList<>();
        result.addAll(rankMatching(pool.structural(), context, queryVector, segment, now)
                .stream().limit(STRUCTURAL_LIMIT).toList());
        result.addAll(rankMatching(pool.unresolved(), context, queryVector, segment, now)
                .stream().limit(UNRESOLVED_LIMIT).toList());
        return List.copyOf(result);
    }

    private List<RankingService.RankedCandidate> rankMatching(
            List<KnowledgeCandidate> candidates,
            SearchContext context,
            double[] queryVector,
            SegmentContext segment,
            LocalDateTime now) {
        List<KnowledgeCandidate> matching = candidates.stream()
                .filter(candidate -> conditionEvaluator.matches(candidate, context))
                .toList();
        return rankingService.rank(matching, queryVector, segment, now);
    }
}
