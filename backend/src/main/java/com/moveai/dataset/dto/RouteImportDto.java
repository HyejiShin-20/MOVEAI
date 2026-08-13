package com.moveai.dataset.dto;
import java.util.List;
public record RouteImportDto(String routeCode, String startNodeCode, String destinationNodeCode, List<RouteSegmentImportDto> segments) {}
