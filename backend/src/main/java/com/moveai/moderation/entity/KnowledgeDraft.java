package com.moveai.moderation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * AI 추출 결과. <b>승인 전에는 절대 검색 대상이 아니다</b>(절대 규칙 3).
 *
 * <p>{@code payloadJson} 을 통째로 보관하는 이유는, 필드별로 쪼개 저장하면 관리자 화면에서
 * "AI가 원래 뭐라고 했는지"를 복원할 수 없기 때문이다. 검수의 핵심은 원본 대조다 (05A §2-2).
 */
@Entity
@Table(name = "knowledge_drafts")
public class KnowledgeDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Column(name = "draft_index", nullable = false)
    private int draftIndex;

    @Column(name = "payload_json", nullable = false, columnDefinition = "json")
    private String payloadJson;

    @Column(nullable = false)
    private String status;

    protected KnowledgeDraft() {}

    public KnowledgeDraft(Long reportId, int draftIndex, String payloadJson) {
        this.reportId = reportId;
        this.draftIndex = draftIndex;
        this.payloadJson = payloadJson;
        this.status = "PENDING";
    }

    public Long getId() {
        return id;
    }

    public Long getReportId() {
        return reportId;
    }

    public int getDraftIndex() {
        return draftIndex;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public String getStatus() {
        return status;
    }

    public boolean isPending() {
        return "PENDING".equals(status);
    }

    public void markApproved() {
        status = "APPROVED";
    }

    public void markRejected() {
        status = "REJECTED";
    }
}
