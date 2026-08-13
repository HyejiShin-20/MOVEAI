package com.moveai.report.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/driver/reports")
public class ReportController {

    @PostMapping
    public ResponseEntity<Map<String, Object>> createReport(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of(
            "reportId", 1L,
            "placeId", request.get("placeId"),
            "status", "CREATED"
        ));
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<Map<String, Object>> getReport(@PathVariable Long reportId) {
        return ResponseEntity.ok(Map.of(
            "reportId", reportId,
            "status", "SUCCESS",
            "data", new Object[]{}
        ));
    }

    @PutMapping("/{reportId}")
    public ResponseEntity<Map<String, Object>> updateReport(@PathVariable Long reportId, @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of(
            "reportId", reportId,
            "status", "UPDATED"
        ));
    }

    @PostMapping("/{reportId}/submit")
    public ResponseEntity<Map<String, Object>> submitReport(@PathVariable Long reportId) {
        return ResponseEntity.ok(Map.of(
            "reportId", reportId,
            "status", "SUBMITTED"
        ));
    }
}
