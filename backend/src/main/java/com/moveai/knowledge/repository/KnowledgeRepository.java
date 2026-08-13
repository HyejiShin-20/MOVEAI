package com.moveai.knowledge.repository;

import com.moveai.knowledge.entity.KnowledgeItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeRepository extends JpaRepository<KnowledgeItem, Long> {
}
