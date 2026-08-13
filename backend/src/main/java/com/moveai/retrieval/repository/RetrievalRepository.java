package com.moveai.retrieval.repository;

import com.moveai.retrieval.entity.RetrievalDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RetrievalRepository extends JpaRepository<RetrievalDocument, Long> {
}
