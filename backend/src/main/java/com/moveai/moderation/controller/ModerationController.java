package com.moveai.moderation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/moderation")
public class ModerationController {

    @PostMapping("/check")
    public ResponseEntity<Map<String, Object>> checkContent(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of(
            "content", request.get("content"),
            "isSafe", true,
            "categories", new Object[]{},
            "status", "SUCCESS"
        ));
    }

    @PostMapping("/report/{reportId}/check")
    public ResponseEntity<Map<String, Object>> checkReport(@PathVariable Long reportId) {
        return ResponseEntity.ok(Map.of(
            "reportId", reportId,
            "isSafe", true,
            "status", "CHECKED"
        ));
    }
}
