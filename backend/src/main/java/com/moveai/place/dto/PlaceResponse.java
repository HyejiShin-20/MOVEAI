package com.moveai.place.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 05B §4-1. <b>필드 이름을 임의로 바꾸지 않는다.</b> 바꾸면 프론트가 멈춘다.
 */
public final class PlaceResponse {

    private PlaceResponse() {}

    /** GET /api/places */
    public record Summary(
            Long id,
            String placeCode,
            String name,
            String placeType,
            String description) {}

    /** GET /api/places/{id} */
    public record Detail(
            Long id,
            String name,
            String placeType,
            String description,
            List<Node> nodes,
            List<Route> routes) {}

    public record Node(
            Long id,
            String nodeCode,
            String nodeType,
            String name,
            String floorLabel,
            boolean isIndoor) {}

    public record Route(
            Long id,
            String routeCode,
            String name,
            boolean isDefault,
            Long destinationNodeId,
            Constraints constraints) {}

    public record Constraints(
            String vehicleClass,
            BigDecimal minTonnage,
            BigDecimal maxTonnage,
            BigDecimal maxVehicleHeightM,
            BigDecimal maxVehicleWidthM) {}
}
