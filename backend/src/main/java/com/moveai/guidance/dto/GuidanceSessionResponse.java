package com.moveai.guidance.dto;

import java.util.List;

public record GuidanceSessionResponse(
        long sessionId,
        RouteSummary route,
        GuidanceStepResponse currentStep) {

    public record RouteSummary(long id, String name, int totalSteps) {
    }

    public record GuidanceStepResponse(
            int sequenceNo,
            int totalSteps,
            String fromNodeName,
            String toNodeName,
            String movementMode,
            String traversalMethod,
            String instruction,
            boolean isLastStep,
            List<GuidanceCardResponse> cards) {
    }

    public record GuidanceCardResponse(
            long knowledgeId,
            String kind,
            String statement,
            String actionText,
            String conditionLabel,
            boolean isRecentlyAdded,
            String targetName,
            boolean isUnresolvedTarget) {
    }
}
