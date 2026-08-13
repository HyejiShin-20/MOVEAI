package com.moveai.report.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 기사가 남긴 현장 제보. 승인 전에는 검색에 쓰이지 않는다 (절대 규칙 3). */
@Entity
@Table(name = "field_reports")
public class FieldReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_code")
    private String reportCode;

    @Column(name = "place_id", nullable = false)
    private Long placeId;

    @Column(name = "selected_scope_node_id")
    private Long selectedScopeNodeId;

    @Column(name = "source_type", nullable = false)
    private String sourceType;

    @Column(name = "raw_stt_text")
    private String rawSttText;

    @Column(name = "corrected_stt_text", nullable = false)
    private String correctedSttText;

    @Column(nullable = false)
    private String status;

    @Column(name = "audio_recording_candidate", nullable = false)
    private boolean audioRecordingCandidate;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected FieldReport() {}

    public FieldReport(Long placeId, Long selectedScopeNodeId, String sourceType, String sttText) {
        this.placeId = placeId;
        this.selectedScopeNodeId = selectedScopeNodeId;
        this.sourceType = sourceType;
        this.rawSttText = sttText;
        // 기사가 고치기 전에도 추출 입력이 비어 있으면 안 되므로 원문으로 채워 둔다.
        this.correctedSttText = sttText;
        this.status = "SUBMITTED";
        this.audioRecordingCandidate = false;
    }

    /** 기사가 화면에서 고친 문장. 추출은 항상 이 값을 입력으로 쓴다. */
    public void correctTranscript(String correctedText) {
        this.correctedSttText = correctedText;
    }

    public void markStatus(String status) {
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getReportCode() {
        return reportCode;
    }

    public Long getPlaceId() {
        return placeId;
    }

    public Long getSelectedScopeNodeId() {
        return selectedScopeNodeId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getRawSttText() {
        return rawSttText;
    }

    public String getCorrectedSttText() {
        return correctedSttText;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
