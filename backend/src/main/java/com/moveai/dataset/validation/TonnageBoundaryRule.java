package com.moveai.dataset.validation;

/**
 * 05A §3-3. 데이터셋 작성자마다 "초과"와 "이상"을 같은 필드에 넣어서, 값만으로는
 * 경계 포함 여부를 알 수 없다.
 *
 * <pre>
 * K_B_005  min_tonnage=1.0  "1톤을 초과하는 차량은 …"  → 배타(&gt;)
 * K_C_005  min_tonnage=5.0  "5톤 이상 트럭은 …"       → 포함(≥)
 * </pre>
 *
 * <p>정답 질문이 정확히 이 경계를 찌른다. {@code >=} 로 통일하면 QUERY_B_02 가 깨지고,
 * {@code >} 로 통일하면 QUERY_C_01 이 깨진다. 그래서 임포트 시점에 statement 문구로
 * 판정해 파생 컬럼에 굳혀 둔다. 대상은 전량 8건뿐이라 단위 테스트로 고정할 수 있다.
 */
public final class TonnageBoundaryRule {

    private TonnageBoundaryRule() {}

    /**
     * @return 경계값을 포함하면 true(≥), 배타면 false(&gt;). 톤수 조건이 없으면 null.
     */
    public static Boolean inclusive(java.math.BigDecimal tonnage, String statement) {
        if (tonnage == null) {
            return null;
        }
        String text = statement == null ? "" : statement;
        // "이상" 또는 표현 없음 → 포함, "초과"/"넘" → 배타
        return !(text.contains("초과") || text.contains("넘"));
    }
}
