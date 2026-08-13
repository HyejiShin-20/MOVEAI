package com.moveai.report.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 원본 음성. 로컬 디렉터리 저장으로 충분하다 (05C §7 Phase 5). */
@Entity
@Table(name = "report_audio_files")
public class ReportAudioFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "duration_ms")
    private Integer durationMs;

    protected ReportAudioFile() {}

    public ReportAudioFile(Long reportId, String filePath, String mimeType, Integer durationMs) {
        this.reportId = reportId;
        this.filePath = filePath;
        this.mimeType = mimeType;
        this.durationMs = durationMs;
    }

    public Long getId() {
        return id;
    }

    public String getFilePath() {
        return filePath;
    }
}
