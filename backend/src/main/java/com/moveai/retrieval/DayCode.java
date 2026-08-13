package com.moveai.retrieval;

import java.time.DayOfWeek;

/** 05A의 MON~SUN 코드와 java.time.DayOfWeek 사이의 명시적 경계. */
final class DayCode {

    private DayCode() {
    }

    static DayOfWeek parse(String value) {
        return switch (value) {
            case "MON" -> DayOfWeek.MONDAY;
            case "TUE" -> DayOfWeek.TUESDAY;
            case "WED" -> DayOfWeek.WEDNESDAY;
            case "THU" -> DayOfWeek.THURSDAY;
            case "FRI" -> DayOfWeek.FRIDAY;
            case "SAT" -> DayOfWeek.SATURDAY;
            case "SUN" -> DayOfWeek.SUNDAY;
            default -> DayOfWeek.valueOf(value);
        };
    }
}
