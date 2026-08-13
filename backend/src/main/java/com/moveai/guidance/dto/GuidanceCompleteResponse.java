package com.moveai.guidance.dto;

import java.time.LocalDateTime;

public record GuidanceCompleteResponse(long sessionId, String status, LocalDateTime completedAt) {
}
