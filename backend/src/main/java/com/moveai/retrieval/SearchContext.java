package com.moveai.retrieval;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** 차량·이동·시각 조건. null 값은 알 수 없는 조건이므로 해당 필터를 적용하지 않는다. */
public record SearchContext(
        String vehicleClass,
        Double vehicleTonnage,
        Double vehicleHeightM,
        Double vehicleWidthM,
        String movementMode,
        LocalTime currentTime,
        DayOfWeek currentDay) {
}
