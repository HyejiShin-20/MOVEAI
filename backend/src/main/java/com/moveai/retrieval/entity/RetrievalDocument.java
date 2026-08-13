package com.moveai.retrieval.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "retrieval_documents")
public class RetrievalDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private String source;

    protected RetrievalDocument() {}

    public RetrievalDocument(String title, String content, String source) {
        this.title = title;
        this.content = content;
        this.source = source;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
