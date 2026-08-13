package com.moveai.knowledge.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "knowledge_items")
public class KnowledgeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "publication_status", nullable = false)
    private String publicationStatus = "DRAFT";

    protected KnowledgeItem() {}

    public KnowledgeItem(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getPublicationStatus() { return publicationStatus; }
    public void setPublicationStatus(String publicationStatus) { this.publicationStatus = publicationStatus; }
}
