package com.moveai.retrieval;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

/** 검색에 필요한 PUBLISHED 지식의 불변 스냅샷. */
public record KnowledgeCandidate(
        long id,
        String knowledgeCode,
        long placeId,
        String targetType,
        Long targetNodeId,
        Long targetSegmentId,
        String movementMode,
        String traversalMethod,
        String factType,
        String accessState,
        String usageScope,
        String statement,
        String actionText,
        String targetName,
        Conditions conditions,
        LocalDateTime publishedAt,
        double[] embedding) {

    public record Conditions(
            String vehicleClass,
            Double minTonnage,
            Boolean minTonnageInclusive,
            Double maxTonnage,
            Boolean maxTonnageInclusive,
            Double maxVehicleHeightM,
            Double maxVehicleWidthM,
            LocalTime activeTimeStart,
            LocalTime activeTimeEnd,
            Set<DayOfWeek> activeDays,
            String extraConditionText) {

        public static Conditions empty() {
            return new Conditions(null, null, null, null, null, null, null, null, null, Set.of(), null);
        }
    }
}
