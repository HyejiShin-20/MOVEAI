package com.moveai.route.dto;

import java.util.List;

/** 05B §4-1. GET /api/routes/{id} */
public final class RouteResponse {

    private RouteResponse() {}

    public record Detail(
            Long id,
            String name,
            int totalSteps,
            List<Segment> segments) {}

    public record Segment(
            Long id,
            int sequenceNo,
            String fromNodeName,
            String toNodeName,
            String movementMode,
            String traversalMethod,
            String instruction,
            boolean isIndoor) {}
}
