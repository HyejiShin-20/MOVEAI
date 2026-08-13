package com.moveai.ai.stt.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class SttService {

    @Value("${move-ai.ai.stt-api-key:}")
    private String sttApiKey;

    @Value("${move-ai.ai.stt-api-url:}")
    private String sttApiUrl;

    public Map<String, Object> transcribeAudio(byte[] audioData, String language) {
        // STT API 호출
        // 실제 구현은 API 키가 제공될 때 완성
        return Map.of(
            "status", "PENDING",
            "message", "STT API 키가 필요합니다",
            "transcript", ""
        );
    }

    public boolean validateApiKey() {
        return sttApiKey != null && !sttApiKey.isEmpty();
    }
}
