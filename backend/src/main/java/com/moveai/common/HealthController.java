package com.moveai.common;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moveai.ai.AiServiceClient;

/**
 * Phase 1 확인용. 서버·DB·ai-service 세 경계가 한 번에 보인다.
 *
 * <p>어느 하나가 죽어도 200을 돌려주고 상태만 다르게 표시한다. 여기서 500이 나면
 * 무엇이 끊겼는지 확인할 수단 자체가 사라진다.
 */
@RestController
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final DataSource dataSource;
    private final AiServiceClient aiServiceClient;

    public HealthController(DataSource dataSource, AiServiceClient aiServiceClient) {
        this.dataSource = dataSource;
        this.aiServiceClient = aiServiceClient;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "ok");
        body.put("database", databaseStatus());
        body.put("aiService", aiServiceClient.health());
        return body;
    }

    private Map<String, Object> databaseStatus() {
        try (Connection connection = dataSource.getConnection()) {
            return Map.of(
                    "status", connection.isValid(2) ? "up" : "down",
                    "product", connection.getMetaData().getDatabaseProductVersion());
        } catch (Exception exception) {
            log.warn("database health check failed: error_type={}", exception.getClass().getSimpleName());
            return Map.of("status", "down", "reason", exception.getClass().getSimpleName());
        }
    }
}
