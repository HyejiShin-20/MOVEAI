package com.moveai.dataset.validation;

import java.util.Set;

/**
 * 05A §2-6 의 허용목록. <b>이 목록이 전부다.</b>
 *
 * <p>여기에 없는 값이 들어오면 임포트를 실패시킨다. 조용히 통과시키면 검색·조건 평가가
 * 어긋난 채로 굴러가고, 원인을 찾는 데 시연 시간을 쓰게 된다.
 */
public final class EnumAllowList {

    public static final Set<String> PLACE_TYPE =
            Set.of("APARTMENT", "OFFICE", "LOGISTICS_CENTER", "COMPLEX_FACILITY", "OTHER");

    public static final Set<String> NODE_TYPE = Set.of(
            "SITE", "ENTRANCE", "SECURITY_GATE", "PARKING_POINT", "LOADING_POINT",
            "BUILDING", "BUILDING_ENTRANCE", "ELEVATOR", "STAIRS", "CORRIDOR",
            "DELIVERY_POINT", "EXIT_POINT", "OTHER");

    public static final Set<String> MOVEMENT_MODE = Set.of("VEHICLE", "PEDESTRIAN", "GENERAL");

    public static final Set<String> TRAVERSAL_METHOD =
            Set.of("DRIVE", "WALK", "STAIRS", "ELEVATOR", "ESCALATOR", "CART", "OTHER");

    public static final Set<String> ACCESS_STATE =
            Set.of("ALLOWED", "CONDITIONAL", "PROHIBITED", "UNKNOWN");

    public static final Set<String> CATEGORY = Set.of(
            "ACCESS", "PARKING_STOPPING", "LOADING", "BUILDING_ENTRANCE",
            "INTERNAL_ROUTE", "ELEVATOR_STAIRS", "CONGESTION_WAIT",
            "DELIVERY_POINT", "OTHER");

    public static final Set<String> FACT_TYPE = Set.of(
            "RESTRICTION", "ALLOWANCE", "LOCATION", "INSTRUCTION",
            "WARNING", "CONDITION", "OTHER");

    public static final Set<String> USAGE_SCOPE =
            Set.of("WARNING_ONLY", "ACTION_GUIDANCE", "ROUTE_GUIDANCE", "REFERENCE_ONLY");

    public static final Set<String> TARGET_TYPE = Set.of("PLACE", "NODE", "SEGMENT", "UNKNOWN");

    public static final Set<String> TARGET_RESOLUTION_STATUS =
            Set.of("RESOLVED", "UNRESOLVED", "NEEDS_REVIEW");

    public static final Set<String> VEHICLE_CLASS = Set.of("TRUCK");

    public static final Set<String> SOURCE_TYPE = Set.of("SYNTHETIC", "VOICE", "TEXT");

    private EnumAllowList() {}

    /** null 은 통과시킨다(선택 필드). 값이 있으면 반드시 허용목록 안이어야 한다. */
    public static String require(String value, Set<String> allowed, String field, String code) {
        if (value == null) {
            return null;
        }
        if (!allowed.contains(value)) {
            throw new IllegalArgumentException(
                    "%s: %s 에 허용되지 않은 값 '%s'. 허용목록(05A §2-6)을 확인하라."
                            .formatted(code, field, value));
        }
        return value;
    }
}
