package com.moveai.ai;

import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Python ai-service 호출 경계. Phase 1에서는 상태 확인만 한다.
 *
 * <p>STT · 추출 · 임베딩 클라이언트는 이후 Phase에서 인터페이스 뒤에 붙인다 (05C §6).
 */
@Component
public class AiServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AiServiceClient.class);
    private static final ParameterizedTypeReference<Map<String, Object>> BODY_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public AiServiceClient(@Value("${moveai.ai-service.url}") String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    /** ai-service /health 를 호출한다. 실패해도 예외를 던지지 않고 사유를 담아 돌려준다. */
    public Map<String, Object> health() {
        try {
            Map<String, Object> body = restClient.get()
                    .uri("/health")
                    .retrieve()
                    .body(BODY_TYPE);
            return body == null ? Map.of("status", "unknown") : body;
        } catch (Exception exception) {
            log.warn("ai-service health call failed: error_type={}", exception.getClass().getSimpleName());
            return Map.of("status", "down", "reason", exception.getClass().getSimpleName());
        }
    }
}
