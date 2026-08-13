package com.moveai.knowledge;

import com.moveai.knowledge.condition.ConditionEvaluator;
import com.moveai.knowledge.entity.KnowledgeCondition;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConditionEvaluatorTest {
    @Test
    void heightLimit() {
        var evaluator = new ConditionEvaluator();
        var condition = new KnowledgeCondition(null, null, null, true, true, 2.7);
        assertTrue(evaluator.matches(condition, null, 2.5));
        assertFalse(evaluator.matches(condition, null, 3.0));
    }

    @Test
    void tonnageBoundary() {
        var evaluator = new ConditionEvaluator();
        var condition = new KnowledgeCondition(null, null, 1.0, true, true, null);
        assertTrue(evaluator.matches(condition, 1.0, null));
        assertFalse(evaluator.matches(condition, 1.1, null));
    }
}
