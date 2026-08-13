package com.moveai.report.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

/** 05B §4-5. POST /api/reports/{id}/extract */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExtractResponse(Long reportId, String status, List<Draft> drafts, String reason) {

    public static ExtractResponse extracted(Long reportId, List<Draft> drafts) {
        return new ExtractResponse(reportId, "EXTRACTED", drafts, null);
    }

    public static ExtractResponse failed(Long reportId, String reason) {
        return new ExtractResponse(reportId, "EXTRACTION_FAILED", null, reason);
    }

    /** payload 는 AI 출력 원본 그대로다. 검수 화면이 원문과 대조할 근거다. */
    public record Draft(Long draftId, int draftIndex, JsonNode payload) {}
}
