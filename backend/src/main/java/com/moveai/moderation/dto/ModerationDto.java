package com.moveai.moderation.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.NotBlank;

/** 05B §4-6. */
public final class ModerationDto {

    private ModerationDto() {}

    /** GET /api/moderation/drafts */
    public record DraftSummary(
            Long draftId,
            Long reportId,
            String placeName,
            LocalDateTime createdAt,
            String summary) {}

    /** GET /api/moderation/drafts/{id} */
    public record DraftDetail(
            Long draftId,
            String status,
            Report report,
            JsonNode payload,
            String resolvedTargetName) {}

    public record Report(
            Long id,
            String audioUrl,
            String rawSttText,
            String correctedSttText,
            String placeName,
            String scopeNodeName) {}

    /** 무수정 승인이면 editedPayload 를 생략한다. */
    public record ApproveRequest(JsonNode editedPayload) {}

    public record ApproveResult(Long draftId, Long knowledgeId, boolean embeddingCreated) {}

    public record RejectRequest(@NotBlank String reason) {}

    public record RejectResult(Long draftId, String status) {}
}
