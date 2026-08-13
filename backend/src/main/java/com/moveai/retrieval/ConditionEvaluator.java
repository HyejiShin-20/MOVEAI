package com.moveai.retrieval;

import java.time.LocalTime;

/** 04 §2~3의 movement·차량·시간·요일 hard filter. */
public class ConditionEvaluator {

    public boolean matches(KnowledgeCandidate candidate, SearchContext context) {
        if (!movementMatches(candidate.movementMode(), context.movementMode())) {
            return false;
        }
        KnowledgeCandidate.Conditions condition = candidate.conditions() == null
                ? KnowledgeCandidate.Conditions.empty() : candidate.conditions();

        if (condition.vehicleClass() != null && context.vehicleClass() != null
                && !condition.vehicleClass().equals(context.vehicleClass())) {
            return false;
        }
        if (!minimumMatches(
                context.vehicleTonnage(), condition.minTonnage(), condition.minTonnageInclusive())) {
            return false;
        }
        if (!maximumMatches(
                context.vehicleTonnage(), condition.maxTonnage(), condition.maxTonnageInclusive())) {
            return false;
        }
        if (!heightMatches(candidate, context.vehicleHeightM(), condition.maxVehicleHeightM())) {
            return false;
        }
        if (!widthMatches(candidate, context.vehicleWidthM(), condition.maxVehicleWidthM())) {
            return false;
        }
        if (context.currentDay() != null && condition.activeDays() != null
                && !condition.activeDays().isEmpty()
                && !condition.activeDays().contains(context.currentDay())) {
            return false;
        }
        return timeMatches(context.currentTime(), condition.activeTimeStart(), condition.activeTimeEnd());
    }

    public boolean movementMatches(String knowledgeMode, String contextMode) {
        return contextMode == null || "GENERAL".equals(knowledgeMode) || contextMode.equals(knowledgeMode);
    }

    private boolean minimumMatches(Double actual, Double limit, Boolean inclusive) {
        if (actual == null || limit == null) {
            return true;
        }
        return Boolean.FALSE.equals(inclusive) ? actual > limit : actual >= limit;
    }

    private boolean maximumMatches(Double actual, Double limit, Boolean inclusive) {
        if (actual == null || limit == null) {
            return true;
        }
        return Boolean.FALSE.equals(inclusive) ? actual < limit : actual <= limit;
    }

    private boolean heightMatches(KnowledgeCandidate candidate, Double actual, Double limit) {
        if (actual == null || limit == null || actual <= 0) {
            return true;
        }
        return isRestriction(candidate) ? actual > limit : actual <= limit;
    }

    private boolean widthMatches(KnowledgeCandidate candidate, Double actual, Double limit) {
        if (actual == null || limit == null || actual <= 0) {
            return true;
        }
        return isRestriction(candidate) ? actual > limit : actual <= limit;
    }

    private boolean isRestriction(KnowledgeCandidate candidate) {
        return "PROHIBITED".equals(candidate.accessState())
                || "RESTRICTION".equals(candidate.factType());
    }

    private boolean timeMatches(LocalTime actual, LocalTime start, LocalTime end) {
        if (actual == null || (start == null && end == null)) {
            return true;
        }
        if (start == null) {
            return !actual.isAfter(end);
        }
        if (end == null) {
            return !actual.isBefore(start);
        }
        if (!end.isBefore(start)) {
            return !actual.isBefore(start) && !actual.isAfter(end);
        }
        // 22:00~02:00처럼 자정을 넘는 범위.
        return !actual.isBefore(start) || !actual.isAfter(end);
    }
}
