package com.moveai.knowledge.retrieval;

import com.moveai.knowledge.entity.KnowledgeItem;
import com.moveai.knowledge.repository.KnowledgeItemRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeRetrievalService {
    private final KnowledgeItemRepository repository;
    public KnowledgeRetrievalService(KnowledgeItemRepository repository) { this.repository = repository; }

    public List<KnowledgeItem> findPublished() {
        return repository.findByPublicationStatus("PUBLISHED");
    }
}
