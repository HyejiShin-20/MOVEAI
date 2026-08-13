package com.moveai.ai.moderation.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class ModerationService {

    @Value("${move-ai.ai.moderation-api-key:}")
    private String moderationApiKey;

    @Value("${move-ai.ai.moderation-api-url:}")
    private String moderationApiUrl;

    public Map<String, Object> moderateContent(String content) {
        // Moderation API 호출
        // 실제 구현은 API 키가 제공될 때 완성
        return Map.of(
            "status", "PENDING",
            "message", "Moderation API 키가 필요합니다",
            "isSafe", true,
            "categories", new Object[]{}
        );
    }

    public boolean validateApiKey() {
        return moderationApiKey != null && !moderationApiKey.isEmpty();
    }
}
