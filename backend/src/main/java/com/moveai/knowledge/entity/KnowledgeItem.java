package com.moveai.knowledge.entity;

import com.moveai.report.entity.FieldReport;
import jakarta.persistence.*;

@Entity
@Table(name = "knowledge_items", uniqueConstraints = @UniqueConstraint(name = "uk_knowledge_code", columnNames = "knowledge_code"))
public class KnowledgeItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_report_id", nullable = false)
    private FieldReport sourceReport;

    @Column(name = "knowledge_code", nullable = false, length = 100)
    private String knowledgeCode;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String statement;

    @Column(name = "publication_status", nullable = false, length = 30)
    private String publicationStatus;

    protected KnowledgeItem() {}
    public KnowledgeItem(FieldReport report, String code, String statement, String status) {
        this.sourceReport = report; this.knowledgeCode = code; this.statement = statement; this.publicationStatus = status;
    }
    public Long getId() { return id; }
    public String getKnowledgeCode() { return knowledgeCode; }
    public String getStatement() { return statement; }
    public String getPublicationStatus() { return publicationStatus; }
}
