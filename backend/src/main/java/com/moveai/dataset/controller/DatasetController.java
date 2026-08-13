package com.moveai.dataset.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;

@RestController
@RequestMapping("/api/dataset")
public class DatasetController {
    private final Path datasetPath;

    public DatasetController(@Value("${move-ai.dataset.path:../datasets/move-ai-data.json}") String datasetPath) {
        this.datasetPath = Path.of(datasetPath).toAbsolutePath().normalize();
    }

    @GetMapping(value = "/data", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FileSystemResource> data() {
        FileSystemResource resource = new FileSystemResource(datasetPath);
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(resource);
    }
}
