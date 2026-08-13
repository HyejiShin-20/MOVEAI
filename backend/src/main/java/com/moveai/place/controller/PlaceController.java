package com.moveai.place.controller;

<<<<<<< HEAD
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
=======
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moveai.place.dto.PlaceResponse;
import com.moveai.place.service.PlaceService;
>>>>>>> 062d1fac691c4e6b28d78b4ea555ea009cd527bf

@RestController
@RequestMapping("/api/places")
public class PlaceController {

<<<<<<< HEAD
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
=======
    private final PlaceService placeService;

    public PlaceController(PlaceService placeService) {
        this.placeService = placeService;
    }

    @GetMapping
    public List<PlaceResponse.Summary> list() {
        return placeService.findAll();
    }

    @GetMapping("/{id}")
    public PlaceResponse.Detail detail(@PathVariable Long id) {
        return placeService.findDetail(id);
>>>>>>> 062d1fac691c4e6b28d78b4ea555ea009cd527bf
    }
}
