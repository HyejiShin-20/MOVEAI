package com.moveai.dataset.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * 05A §3-3 의 8건 표를 그대로 고정한다.
 *
 * <p>여기가 틀리면 Phase 4 의 경로 선택이 같은 이유로 틀린다. 문자열 규칙이 어긋나면
 * 이 테스트가 먼저 잡는다.
 */
class TonnageBoundaryRuleTest {

    /** statement 는 데이터셋 원문 그대로다. 원문이 바뀌면 이 테스트가 먼저 깨져야 한다. */
    @ParameterizedTest(name = "{0} min={1} → inclusive={3}")
    @CsvSource(delimiter = '|', value = {
        "K_A_001 | 1.5 | 1.5톤 탑차는 정문 진입이 빡빡하다. | true",
        "K_A_002 | 1.5 | 제보자는 1.5톤 탑차로 후문으로 진입했다. | true",
        "K_A_016 | 1.5 | 101동 오른쪽 출입구 진입 전 왼쪽으로 꺾는 길은 1.5톤 탑차가 회전하기 어렵다. | true",
        "K_B_005 | 1.0 | 1톤을 초과하는 차량은 지하주차장으로 내려갈 수 없다. | false",
        "K_B_006 | 1.0 | 1톤을 초과하는 차량은 정문으로 진입해 로비 앞에 정차한다. | false",
        "K_C_005 | 5.0 | 5톤 이상 트럭은 2번 게이트로 진입한다. | true",
        "K_D_023 | 5.0 | 5톤 이상 화물차는 B 게이트 진입이 금지된다. | true",
        "K_D_025 | 5.0 | 5톤 이상 차량은 D 게이트 지상 하역장을 이용해야 한다. | true",
    })
    @DisplayName("statement 문구로 톤수 경계의 포함/배타를 판정한다 (데이터셋 전량 8건)")
    void derivesInclusivityFromStatement(
            String code, String minTonnage, String statement, boolean expected) {
        Boolean inclusive = TonnageBoundaryRule.inclusive(new BigDecimal(minTonnage), statement);

        assertThat(inclusive).as(code).isEqualTo(expected);
    }

    @Test
    @DisplayName("톤수 조건이 없으면 파생 컬럼도 비운다")
    void returnsNullWhenNoTonnage() {
        assertThat(TonnageBoundaryRule.inclusive(null, "1톤을 초과하는 차량은 …")).isNull();
    }

    @Test
    @DisplayName("'넘' 표현도 배타로 본다")
    void treatsNeumAsExclusive() {
        Boolean inclusive =
                TonnageBoundaryRule.inclusive(new BigDecimal("1.0"), "1톤 넘는 차는 지하로 못 간다.");

        assertThat(inclusive).isFalse();
    }

    @Test
    @DisplayName("표현이 없으면 포함으로 본다")
    void defaultsToInclusive() {
        Boolean inclusive = TonnageBoundaryRule.inclusive(new BigDecimal("1.5"), "1.5톤 탑차가 들어왔다.");

        assertThat(inclusive).isTrue();
    }
}
