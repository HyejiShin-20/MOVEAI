package com.moveai.dataset.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * datasets/synthetic_dataset_*.json 의 읽기 전용 표현.
 *
 * <p>필드명은 스네이크케이스 JSON과 1:1로 대응한다(ObjectMapper의 SNAKE_CASE 전략).
 * <b>데이터셋은 수정하지 않는다.</b> 값 보정이 필요하면 임포트 코드에서 흡수한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DatasetPayload(
        Place place,
        List<Node> nodes,
        List<Route> routes,
        List<Segment> routeSegments,
        List<Report> fieldReports,
        List<RagQuery> ragTestQueries) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Place(
            String placeCode,
            String name,
            String placeType,
            String customPlaceType,
            String description,
            Boolean synthetic) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Node(
            String nodeCode,
            String parentNodeCode,
            String nodeType,
            String customNodeType,
            String name,
            String floorLabel,
            Boolean isIndoor,
            String description) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Route(
            String routeCode,
            String name,
            String startNodeCode,
            String destinationNodeCode,
            String vehicleClass,
            BigDecimal minTonnage,
            BigDecimal maxTonnage,
            BigDecimal maxVehicleHeightM,
            BigDecimal maxVehicleWidthM,
            Boolean isDefault) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Segment(
            String segmentCode,
            String routeCode,
            Integer sequenceNo,
            String fromNodeCode,
            String toNodeCode,
            String movementMode,
            String traversalMethod,
            String customTraversalMethod,
            String instruction,
            Boolean isIndoor) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Report(
            String reportCode,
            String placeCode,
            String selectedScopeNodeCode,
            String sourceType,
            String transcript,
            Boolean audioRecordingCandidate,
            List<Knowledge> expectedKnowledgeItems) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Knowledge(
            String knowledgeCode,
            Target target,
            String category,
            String customCategoryLabel,
            String factType,
            String customFactTypeLabel,
            String movementMode,
            String traversalMethod,
            String customTraversalMethod,
            String accessState,
            String statement,
            String actionText,
            String sourceExcerpt,
            Conditions conditions,
            String usageScope) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Target(
            String targetType,
            String targetCode,
            String targetResolutionStatus,
            String targetFreeText) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Conditions(
            String vehicleClass,
            BigDecimal minTonnage,
            BigDecimal maxTonnage,
            BigDecimal maxVehicleHeightM,
            BigDecimal maxVehicleWidthM,
            String activeTimeStart,
            String activeTimeEnd,
            List<String> activeDays,
            String extraConditionText) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RagQuery(
            String queryCode,
            String placeCode,
            String question,
            Map<String, Object> context,
            List<String> expectedKnowledgeCodes,
            List<String> mustNotReturnCodes,
            String reason) {}
}
