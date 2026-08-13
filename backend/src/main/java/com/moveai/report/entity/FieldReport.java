package com.moveai.report.entity;

import com.moveai.place.entity.Place;
import jakarta.persistence.*;

@Entity
@Table(name = "field_reports", uniqueConstraints = @UniqueConstraint(name = "uk_reports_code", columnNames = "report_code"))
public class FieldReport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(name = "report_code", nullable = false, length = 100)
    private String reportCode;

    @Column(columnDefinition = "TEXT")
    private String transcript;

    @Column(name = "corrected_stt_text", columnDefinition = "TEXT")
    private String correctedSttText;

    @Column(name = "audio_recording_candidate", nullable = false)
    private boolean audioRecordingCandidate;

    protected FieldReport() {}
    public FieldReport(Place place, String reportCode, String transcript, boolean audioRecordingCandidate) {
        this.place = place; this.reportCode = reportCode; this.transcript = transcript;
        this.audioRecordingCandidate = audioRecordingCandidate;
    }
    public Long getId() { return id; }
    public String getReportCode() { return reportCode; }
    public String getTranscript() { return transcript; }
}
