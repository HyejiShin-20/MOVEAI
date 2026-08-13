package com.moveai.job.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> getJobs() {
        return ResponseEntity.ok(Map.of(
            "status", "SUCCESS",
            "jobs", new Object[]{},
            "total", 0
        ));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<Map<String, Object>> getJob(@PathVariable Long jobId) {
        return ResponseEntity.ok(Map.of(
            "jobId", jobId,
            "status", "SUCCESS",
            "progress", 0
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createJob(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of(
            "jobId", 1L,
            "status", "CREATED",
            "progress", 0
        ));
    }

    @PostMapping("/{jobId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelJob(@PathVariable Long jobId) {
        return ResponseEntity.ok(Map.of(
            "jobId", jobId,
            "status", "CANCELLED"
        ));
    }
}
