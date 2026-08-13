package com.moveai.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CosineCalculatorTest {

    private final CosineCalculator calculator = new CosineCalculator();

    @Test
    void calculatesNormalizedSimilarity() {
        assertThat(calculator.similarity(new double[] {1, 0}, new double[] {2, 0})).isEqualTo(1.0);
        assertThat(calculator.similarity(new double[] {1, 0}, new double[] {0, 1})).isEqualTo(0.0);
    }

    @Test
    void returnsZeroForZeroVectorAndRejectsDimensionMismatch() {
        assertThat(calculator.similarity(new double[] {0, 0}, new double[] {1, 1})).isZero();
        assertThatThrownBy(() -> calculator.similarity(new double[] {1}, new double[] {1, 2}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
