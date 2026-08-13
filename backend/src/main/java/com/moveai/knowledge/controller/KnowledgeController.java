package com.moveai.knowledge.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> getKnowledge() {
        return ResponseEntity.ok(Map.of(
            "status", "SUCCESS",
            "data", new Object[]{},
            "total", 0
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getKnowledgeById(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
            "id", id,
            "status", "SUCCESS"
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createKnowledge(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of(
            "knowledgeId", 1L,
            "status", "CREATED"
        ));
    }
}
