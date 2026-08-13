package com.moveai.report.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.moveai.report.dto.ExtractResponse;
import com.moveai.report.dto.ReportResponse;
import com.moveai.report.dto.TranscriptUpdateRequest;
import com.moveai.report.service.ReportExtractionService;
import com.moveai.report.service.ReportService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final ReportExtractionService reportExtractionService;

    public ReportController(
            ReportService reportService, ReportExtractionService reportExtractionService) {
        this.reportService = reportService;
        this.reportExtractionService = reportExtractionService;
    }

    /** 05B §4-5. corrected_stt_text 를 입력으로 Draft 를 만든다. */
    @PostMapping("/{id}/extract")
    public ExtractResponse extract(@PathVariable Long id) {
        return reportExtractionService.extract(id);
    }

    /** 05B §4-5. multipart: placeId, selectedScopeNodeId?, audio */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse.Created create(
            @RequestParam Long placeId,
            @RequestParam(required = false) Long selectedScopeNodeId,
            @RequestParam MultipartFile audio) {
        return reportService.createFromAudio(placeId, selectedScopeNodeId, audio);
    }

    /**
     * 녹음이 막혔을 때의 축소 경로. 텍스트만 받아 같은 파이프라인으로 넣는다.
     *
     * <p>"음성으로 남긴다"가 시연에서 약해지지만 이후 추출·검수·발행은 그대로 살아난다.
     */
    @PostMapping(path = "/text", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse.Created createFromText(
            @RequestParam Long placeId,
            @RequestParam(required = false) Long selectedScopeNodeId,
            @Valid @RequestBody TranscriptUpdateRequest request) {
        return reportService.createFromText(placeId, selectedScopeNodeId, request.correctedText());
    }

    @PatchMapping("/{id}/transcript")
    public ReportResponse.Transcript updateTranscript(
            @PathVariable Long id, @Valid @RequestBody TranscriptUpdateRequest request) {
        return reportService.updateTranscript(id, request.correctedText());
    }
}
