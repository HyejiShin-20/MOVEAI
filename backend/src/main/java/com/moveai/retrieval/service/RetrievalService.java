package com.moveai.retrieval.service;

import com.moveai.retrieval.entity.RetrievalDocument;
import com.moveai.retrieval.repository.RetrievalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RetrievalService {

    private final RetrievalRepository retrievalRepository;

    public RetrievalService(RetrievalRepository retrievalRepository) {
        this.retrievalRepository = retrievalRepository;
    }

    public List<RetrievalDocument> findAll() {
        return retrievalRepository.findAll();
    }

    @Transactional
    public RetrievalDocument create(String title, String content, String source) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("제목은 필수입니다.");
        }
        return retrievalRepository.save(new RetrievalDocument(title, content, source == null ? "LOCAL" : source));
    }
}
