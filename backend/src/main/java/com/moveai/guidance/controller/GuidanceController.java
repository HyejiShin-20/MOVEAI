package com.moveai.guidance.controller;

<<<<<<< HEAD
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
=======
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.moveai.guidance.dto.GuidanceCompleteResponse;
import com.moveai.guidance.dto.GuidanceCreateRequest;
import com.moveai.guidance.dto.GuidanceSessionResponse;
import com.moveai.guidance.service.GuidanceService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/guidance")
public class GuidanceController {

    private final GuidanceService guidanceService;

    public GuidanceController(GuidanceService guidanceService) {
        this.guidanceService = guidanceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GuidanceSessionResponse create(@Valid @RequestBody GuidanceCreateRequest request) {
        return guidanceService.create(request);
    }

    @GetMapping("/{id}")
    public GuidanceSessionResponse get(@PathVariable long id) {
        return guidanceService.get(id);
    }

    @PostMapping("/{id}/next")
    public GuidanceSessionResponse next(@PathVariable long id) {
        return guidanceService.next(id);
    }

    @PostMapping("/{id}/complete")
    public GuidanceCompleteResponse complete(@PathVariable long id) {
        return guidanceService.complete(id);
>>>>>>> 062d1fac691c4e6b28d78b4ea555ea009cd527bf
    }
}
