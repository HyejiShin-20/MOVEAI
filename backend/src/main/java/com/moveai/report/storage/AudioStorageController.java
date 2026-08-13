package com.moveai.report.storage;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/driver/reports")
public class AudioStorageController {
    private final AudioStorageService storage;
    public AudioStorageController(AudioStorageService storage) { this.storage = storage; }

    @PostMapping("/{reportId}/audio")
    public ResponseEntity<Map<String,Object>> upload(@PathVariable Long reportId, @RequestParam("file") MultipartFile file) throws IOException {
        var saved = storage.save(file);
        return ResponseEntity.ok(Map.of(
            "reportId", reportId,
            "status", "STORED",
            "fileName", saved.storedName(),
            "contentType", saved.contentType() == null ? "application/octet-stream" : saved.contentType(),
            "size", saved.size(),
            "path", saved.absolutePath()
        ));
    }
}
