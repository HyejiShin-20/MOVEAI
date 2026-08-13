package com.moveai.ai.embedding.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class EmbeddingService {

    @Value("${move-ai.ai.embedding-api-key:}")
    private String embeddingApiKey;

    @Value("${move-ai.ai.embedding-api-url:}")
    private String embeddingApiUrl;

    public double[] generateEmbedding(String text) {
        // Embedding API 호출
        // 실제 구현은 API 키가 제공될 때 완성
        return new double[]{};
    }

    public Map<String, Object> searchSimilar(String query, int limit) {
        return Map.of(
            "status", "PENDING",
            "message", "Embedding API 키가 필요합니다",
            "results", new Object[]{}
        );
    }

    public boolean validateApiKey() {
        return embeddingApiKey != null && !embeddingApiKey.isEmpty();
    }
}
