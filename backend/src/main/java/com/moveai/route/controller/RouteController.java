package com.moveai.route.controller;

<<<<<<< HEAD
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
=======
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moveai.route.dto.RouteResponse;
import com.moveai.route.service.RouteService;
>>>>>>> 062d1fac691c4e6b28d78b4ea555ea009cd527bf

@RestController
@RequestMapping("/api/routes")
public class RouteController {

<<<<<<< HEAD
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
=======
    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping("/{id}")
    public RouteResponse.Detail detail(@PathVariable Long id) {
        return routeService.findDetail(id);
>>>>>>> 062d1fac691c4e6b28d78b4ea555ea009cd527bf
    }
}
