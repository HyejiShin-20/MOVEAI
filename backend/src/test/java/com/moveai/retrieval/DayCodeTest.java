package com.moveai.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;

import org.junit.jupiter.api.Test;

class DayCodeTest {

    @Test
    void mapsDatasetAbbreviations() {
        assertThat(DayCode.parse("MON")).isEqualTo(DayOfWeek.MONDAY);
        assertThat(DayCode.parse("SAT")).isEqualTo(DayOfWeek.SATURDAY);
        assertThat(DayCode.parse("SUN")).isEqualTo(DayOfWeek.SUNDAY);
    }
}
