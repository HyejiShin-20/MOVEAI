package com.moveai.ai.extraction;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.moveai.common.ApiException;

/**
 * ai-service {@code POST /extract-knowledge} 호출 (05B §5-2).
 *
 * <p>스키마 강제·검증·1회 재요청은 Python 쪽에 이미 있다. Spring 은 문맥을 넘기고 결과를
 * 받아 저장할 뿐, <b>필드를 조용히 지우지 않는다</b>(절대 규칙 9).
 */
@Component
public class ExtractionClient {

    private static final Logger log = LoggerFactory.getLogger(ExtractionClient.class);

    private final RestClient restClient;

    public ExtractionClient(@Value("${moveai.ai-service.url}") String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        // 검증 실패 시 1회 재요청이 붙으므로 넉넉히 준다.
        requestFactory.setReadTimeout(Duration.ofSeconds(180));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public List<JsonNode> extract(
            String placeName,
            String transcript,
            String scopeNodeName,
            List<KnownLocation> knownNodes,
            List<KnownLocation> knownSegments) {

        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("placeName", placeName);
        payload.put("transcript", transcript);
        payload.put("scopeNodeName", scopeNodeName);
        payload.put("knownNodes", knownNodes);
        payload.put("knownSegments", knownSegments);

        JsonNode body;
        try {
            body = restClient.post()
                    .uri("/extract-knowledge")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception exception) {
            log.warn("extraction call failed: error_type={}", exception.getClass().getSimpleName());
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY, "EXTRACTION_FAILED",
                    "지식 추출에 실패했습니다. 원문을 확인하고 다시 시도해 주세요.");
        }

        JsonNode items = body == null ? null : body.get("items");
        if (items == null || !items.isArray()) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY, "EXTRACTION_FAILED", "추출 결과 형식이 올바르지 않습니다.");
        }
        return java.util.stream.StreamSupport.stream(items.spliterator(), false).toList();
    }

    public record KnownLocation(String code, String name) {}
}
