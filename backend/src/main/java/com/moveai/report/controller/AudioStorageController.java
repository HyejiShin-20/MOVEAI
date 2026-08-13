package com.moveai.report.controller;

import com.moveai.report.storage.AudioStorageService;
import com.moveai.report.storage.StoredAudio;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/driver/reports")
public class AudioStorageController {

    private final AudioStorageService audioStorageService;

    public AudioStorageController(AudioStorageService audioStorageService) {
        this.audioStorageService = audioStorageService;
    }

    @PostMapping("/{reportId}/audio")
    public ResponseEntity<Map<String, Object>> uploadAudio(
            @PathVariable Long reportId,
            @RequestParam("file") MultipartFile file) {
        try {
            StoredAudio stored = audioStorageService.save(file);
            return ResponseEntity.ok(Map.of(
                "reportId", reportId,
                "filePath", stored.filePath(),
                "originalName", stored.originalName(),
                "fileSize", stored.fileSize(),
                "contentType", stored.contentType(),
                "status", "UPLOADED"
            ));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "파일 저장 실패",
                "message", e.getMessage()
            ));
        }
    }
}
