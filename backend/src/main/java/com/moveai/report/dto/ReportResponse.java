package com.moveai.report.dto;

/** 05B §4-5. */
public final class ReportResponse {

    private ReportResponse() {}

    /** POST /api/reports */
    public record Created(Long reportId, String rawSttText) {}

    /** PATCH /api/reports/{id}/transcript */
    public record Transcript(Long reportId, String correctedSttText) {}
}
