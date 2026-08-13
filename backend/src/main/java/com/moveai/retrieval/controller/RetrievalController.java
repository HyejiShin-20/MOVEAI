package com.moveai.retrieval.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/retrieval")
public class RetrievalController {

    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> search(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of(
            "query", request.get("query"),
            "results", new Object[]{},
            "total", 0,
            "status", "SUCCESS"
        ));
    }

    @PostMapping("/search/similarity")
    public ResponseEntity<Map<String, Object>> searchSimilarity(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of(
            "query", request.get("query"),
            "similarity", new Object[]{},
            "status", "SUCCESS"
        ));
    }
}
