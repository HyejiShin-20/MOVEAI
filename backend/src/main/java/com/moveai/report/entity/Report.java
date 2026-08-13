package com.moveai.report.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "field_reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "place_id", nullable = false)
    private Long placeId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private String status = "CREATED";

    protected Report() {}

    public Report(Long placeId, String title, String content) {
        this.placeId = placeId;
        this.title = title;
        this.content = content;
    }

    public Long getId() { return id; }
    public Long getPlaceId() { return placeId; }
    public void setPlaceId(Long placeId) { this.placeId = placeId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
