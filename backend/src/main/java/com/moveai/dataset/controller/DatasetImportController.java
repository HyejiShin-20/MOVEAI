package com.moveai.dataset.controller;

import com.moveai.dataset.service.DatasetImportService;
import com.moveai.common.response.ApiResponse;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/datasets")
public class DatasetImportController {
    private final DatasetImportService service;
    public DatasetImportController(DatasetImportService service) { this.service = service; }

    @PostMapping(value="/validate", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<String>> validate(@RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(service.validate(file));
    }

    @PostMapping(value="/import", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> importDataset(@RequestPart("file") MultipartFile file) {
        service.importDataset(file);
        return ApiResponse.ok("imported");
    }
}
