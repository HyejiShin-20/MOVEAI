package com.moveai.dataset.dto;
public record ExpectedKnowledgeImportDto(
        String knowledgeCode, String reportCode, String statement, String sourceExcerpt,
        TargetImportDto target, String movementMode, String traversalMethod,
        Double minTonnage, Double maxTonnage,
        Boolean minTonnageInclusive, Boolean maxTonnageInclusive,
        Double maxVehicleHeight, boolean resolved
) {
    public record TargetImportDto(String targetType, String targetCode, String targetResolutionStatus, String targetFreeText) {}
}
