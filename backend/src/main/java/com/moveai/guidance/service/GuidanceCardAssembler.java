package com.moveai.guidance.service;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.moveai.guidance.dto.GuidanceSessionResponse.GuidanceCardResponse;
import com.moveai.retrieval.KnowledgeCandidate;
import com.moveai.retrieval.RankingService;

/** 검색 결과를 05B §4-4 카드 계약으로 바꾸는 순수 조립기. */
@Component
public class GuidanceCardAssembler {

    public GuidanceCardResponse assemble(
            RankingService.RankedCandidate ranked, LocalDateTime now) {
        KnowledgeCandidate candidate = ranked.candidate();
        String kind = switch (candidate.usageScope()) {
            case "WARNING_ONLY" -> "WARNING";
            case "ACTION_GUIDANCE", "ROUTE_GUIDANCE" -> "ACTION";
            case "REFERENCE_ONLY" -> "REFERENCE";
            default -> throw new IllegalStateException("지원하지 않는 usage_scope: " + candidate.usageScope());
        };
        String actionText = candidate.actionText();
        if ("ACTION".equals(kind) && (actionText == null || actionText.isBlank())) {
            actionText = candidate.statement();
        }
        boolean recent = candidate.publishedAt() != null
                && !candidate.publishedAt().isAfter(now)
                && Duration.between(candidate.publishedAt(), now).compareTo(Duration.ofHours(24)) <= 0;
        return new GuidanceCardResponse(
                candidate.id(), kind, candidate.statement(), actionText,
                candidate.conditions().extraConditionText(), recent, candidate.targetName(),
                "UNKNOWN".equals(candidate.targetType()));
    }
}
