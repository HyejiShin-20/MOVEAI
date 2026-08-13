package com.moveai.moderation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moveai.moderation.entity.KnowledgeDraft;

public interface KnowledgeDraftRepository extends JpaRepository<KnowledgeDraft, Long> {

    List<KnowledgeDraft> findByStatusOrderByIdAsc(String status);

    List<KnowledgeDraft> findByReportIdOrderByDraftIndexAsc(Long reportId);

    void deleteByReportId(Long reportId);
}
