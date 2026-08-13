package com.moveai.knowledge.service;

import com.moveai.knowledge.entity.KnowledgeItem;
import com.moveai.knowledge.repository.KnowledgeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class KnowledgeService {

    private final KnowledgeRepository knowledgeRepository;

    public KnowledgeService(KnowledgeRepository knowledgeRepository) {
        this.knowledgeRepository = knowledgeRepository;
    }

    public List<KnowledgeItem> findAll() {
        return knowledgeRepository.findAll();
    }

    public KnowledgeItem findById(Long id) {
        return knowledgeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("지식 문서를 찾을 수 없습니다: " + id));
    }

    @Transactional
    public KnowledgeItem create(String title, String content) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("제목은 필수입니다.");
        }
        return knowledgeRepository.save(new KnowledgeItem(title, content));
    }

    @Transactional
    public KnowledgeItem update(Long id, String title, String content, String status) {
        KnowledgeItem item = findById(id);
        if (title != null && !title.isBlank()) item.setTitle(title);
        if (content != null) item.setContent(content);
        if (status != null && !status.isBlank()) item.setPublicationStatus(status);
        return knowledgeRepository.save(item);
    }
}
