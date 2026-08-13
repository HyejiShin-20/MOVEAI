package com.moveai.knowledge.repository;
import com.moveai.knowledge.entity.KnowledgeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface KnowledgeItemRepository extends JpaRepository<KnowledgeItem, Long> {
    List<KnowledgeItem> findByPublicationStatus(String publicationStatus);
}
