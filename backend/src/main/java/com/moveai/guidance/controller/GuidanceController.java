package com.moveai.guidance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/driver/guidance")
public class GuidanceController {

    @GetMapping("/{placeId}/routes")
    public ResponseEntity<Map<String, Object>> getRoutes(@PathVariable Long placeId) {
        return ResponseEntity.ok(Map.of(
            "placeId", placeId,
            "routes", new Object[]{},
            "status", "SUCCESS"
        ));
    }

    @GetMapping("/{placeId}/routes/{routeId}/nodes")
    public ResponseEntity<Map<String, Object>> getRouteNodes(@PathVariable Long placeId, @PathVariable Long routeId) {
        return ResponseEntity.ok(Map.of(
            "placeId", placeId,
            "routeId", routeId,
            "nodes", new Object[]{},
            "status", "SUCCESS"
        ));
    }

    @PostMapping("/{placeId}/guidance/start")
    public ResponseEntity<Map<String, Object>> startGuidance(@PathVariable Long placeId, @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of(
            "guidanceId", 1L,
            "placeId", placeId,
            "status", "STARTED"
        ));
    }

    @PostMapping("/{placeId}/guidance/{guidanceId}/stop")
    public ResponseEntity<Map<String, Object>> stopGuidance(@PathVariable Long placeId, @PathVariable Long guidanceId) {
        return ResponseEntity.ok(Map.of(
            "guidanceId", guidanceId,
            "status", "STOPPED"
        ));
    }
}
