package com.moveai.knowledge.condition;

import com.moveai.knowledge.entity.KnowledgeCondition;
import org.springframework.stereotype.Component;

@Component
public class ConditionEvaluator {
    public boolean matches(KnowledgeCondition c, Double vehicleTonnage, Double vehicleHeight) {
        if (vehicleTonnage != null) {
            if (c.getMinTonnage() != null &&
                (c.isMinTonnageInclusive() ? vehicleTonnage < c.getMinTonnage() : vehicleTonnage <= c.getMinTonnage())) return false;
            if (c.getMaxTonnage() != null &&
                (c.isMaxTonnageInclusive() ? vehicleTonnage > c.getMaxTonnage() : vehicleTonnage >= c.getMaxTonnage())) return false;
        }
        if (vehicleHeight != null && c.getMaxVehicleHeight() != null && vehicleHeight > c.getMaxVehicleHeight()) return false;
        return true;
    }
}
