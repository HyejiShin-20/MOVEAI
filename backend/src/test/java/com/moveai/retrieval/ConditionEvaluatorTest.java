package com.moveai.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ConditionEvaluatorTest {

    private final ConditionEvaluator evaluator = new ConditionEvaluator();

    @Test
    void oneTonDoesNotMatchExclusiveMinimumButFiveTonMatchesInclusiveMinimum() {
        KnowledgeCandidate b005 = withConditions("K_B_005", conditions(1.0, false, null),
                "RESTRICTION", "PROHIBITED");
        KnowledgeCandidate c005 = withConditions("K_C_005", conditions(5.0, true, null),
                "INSTRUCTION", "ALLOWED");

        assertThat(evaluator.matches(b005, context(1.0, null, "VEHICLE"))).isFalse();
        assertThat(evaluator.matches(b005, context(1.01, null, "VEHICLE"))).isTrue();
        assertThat(evaluator.matches(c005, context(5.0, null, "VEHICLE"))).isTrue();
        assertThat(evaluator.matches(c005, context(4.99, null, "VEHICLE"))).isFalse();
    }

    @Test
    void restrictionHeightAppliesAboveLimitAndAllowanceAppliesAtOrBelowLimit() {
        KnowledgeCandidate b002 = withConditions("K_B_002", conditions(null, null, 2.3),
                "RESTRICTION", "PROHIBITED");
        KnowledgeCandidate c006 = withConditions("K_C_006", conditions(null, null, 3.6),
                "ALLOWANCE", "ALLOWED");

        assertThat(evaluator.matches(b002, context(null, 2.31, "VEHICLE"))).isTrue();
        assertThat(evaluator.matches(b002, context(null, 2.30, "VEHICLE"))).isFalse();
        assertThat(evaluator.matches(c006, context(null, 3.60, "VEHICLE"))).isTrue();
        assertThat(evaluator.matches(c006, context(null, 3.61, "VEHICLE"))).isFalse();
    }

    @Test
    void movementIsHardFilterButGeneralAlwaysPasses() {
        KnowledgeCandidate pedestrian = RetrievalFixtures.candidate(
                1, "WALK", "NODE", 1L, null, "PEDESTRIAN", "CART",
                KnowledgeCandidate.Conditions.empty(), "WARNING", null, 1, 0);
        KnowledgeCandidate general = RetrievalFixtures.candidate(
                2, "GENERAL", "NODE", 1L, null, "GENERAL", null,
                KnowledgeCandidate.Conditions.empty(), "WARNING", null, 1, 0);

        assertThat(evaluator.matches(pedestrian, context(null, null, "VEHICLE"))).isFalse();
        assertThat(evaluator.matches(general, context(null, null, "VEHICLE"))).isTrue();
    }

    @Test
    void handlesDayAndMidnightCrossingTimeWindow() {
        KnowledgeCandidate.Conditions conditions = new KnowledgeCandidate.Conditions(
                null, null, null, null, null, null, null,
                LocalTime.of(22, 0), LocalTime.of(2, 0), Set.of(DayOfWeek.SATURDAY), null);
        KnowledgeCandidate candidate = withConditions("NIGHT", conditions, "WARNING", null);

        assertThat(evaluator.matches(candidate, timed(LocalTime.of(23, 0), DayOfWeek.SATURDAY))).isTrue();
        assertThat(evaluator.matches(candidate, timed(LocalTime.of(1, 0), DayOfWeek.SATURDAY))).isTrue();
        assertThat(evaluator.matches(candidate, timed(LocalTime.of(12, 0), DayOfWeek.SATURDAY))).isFalse();
        assertThat(evaluator.matches(candidate, timed(LocalTime.of(23, 0), DayOfWeek.MONDAY))).isFalse();
    }

    private KnowledgeCandidate withConditions(
            String code, KnowledgeCandidate.Conditions conditions, String factType, String accessState) {
        return RetrievalFixtures.candidate(
                1, code, "NODE", 1L, null, "VEHICLE", "DRIVE",
                conditions, factType, accessState, 1, 0);
    }

    private KnowledgeCandidate.Conditions conditions(
            Double min, Boolean inclusive, Double maxHeight) {
        return new KnowledgeCandidate.Conditions(
                "TRUCK", min, inclusive, null, null, maxHeight, null,
                null, null, Set.of(), null);
    }

    private SearchContext context(Double tonnage, Double height, String movement) {
        return new SearchContext("TRUCK", tonnage, height, null, movement, null, null);
    }

    private SearchContext timed(LocalTime time, DayOfWeek day) {
        return new SearchContext(null, null, null, null, "VEHICLE", time, day);
    }
}
