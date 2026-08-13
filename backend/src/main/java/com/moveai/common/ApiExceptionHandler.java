package com.moveai.common;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 05B 공통 오류 형식.
 *
 * <pre>
 * { "error": { "code": "NO_ROUTE_AVAILABLE", "message": "…" } }
 * </pre>
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApiException(ApiException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(body(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception exception) {
        // 내부 메시지를 그대로 내보내지 않는다. 원인은 로그에만 남긴다.
        log.error("unhandled error: error_type={}", exception.getClass().getSimpleName(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body("INTERNAL_ERROR", "처리 중 오류가 발생했습니다."));
    }

    private static Map<String, Object> body(String code, String message) {
        Map<String, String> error = new LinkedHashMap<>();
        error.put("code", code);
        error.put("message", message);
        return Map.of("error", error);
    }
}
