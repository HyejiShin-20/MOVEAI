package com.moveai.dataset.dto;

import java.util.List;

public record DatasetImportDto(
        PlaceImportDto place,
        List<NodeImportDto> nodes,
        List<RouteImportDto> routes,
        List<FieldReportImportDto> reports,
        List<ExpectedKnowledgeImportDto> knowledge
) {}
