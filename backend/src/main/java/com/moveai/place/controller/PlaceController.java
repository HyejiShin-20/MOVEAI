package com.moveai.place.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/places")
public class PlaceController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> getPlaces() {
        return ResponseEntity.ok(Map.of(
            "status", "SUCCESS",
            "places", new Object[]{},
            "total", 0
        ));
    }

    @GetMapping("/{placeId}")
    public ResponseEntity<Map<String, Object>> getPlace(@PathVariable Long placeId) {
        return ResponseEntity.ok(Map.of(
            "placeId", placeId,
            "status", "SUCCESS"
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createPlace(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of(
            "placeId", 1L,
            "status", "CREATED"
        ));
    }
}
