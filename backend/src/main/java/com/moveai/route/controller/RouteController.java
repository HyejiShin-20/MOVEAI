package com.moveai.route.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    @GetMapping("/{placeId}")
    public ResponseEntity<Map<String, Object>> getRoutesByPlace(@PathVariable Long placeId) {
        return ResponseEntity.ok(Map.of(
            "placeId", placeId,
            "routes", new Object[]{},
            "total", 0,
            "status", "SUCCESS"
        ));
    }

    @GetMapping("/{placeId}/{routeId}")
    public ResponseEntity<Map<String, Object>> getRoute(@PathVariable Long placeId, @PathVariable Long routeId) {
        return ResponseEntity.ok(Map.of(
            "placeId", placeId,
            "routeId", routeId,
            "status", "SUCCESS"
        ));
    }

    @PostMapping("/{placeId}")
    public ResponseEntity<Map<String, Object>> createRoute(@PathVariable Long placeId, @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of(
            "routeId", 1L,
            "placeId", placeId,
            "status", "CREATED"
        ));
    }
}
