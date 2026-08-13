package com.moveai.common.exception;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<?> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(ex.getErrorCode().status())
                .body(Map.of("success", false, "error", ex.getErrorCode().name(), "message", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("success", false, "error", "VALIDATION", "message", ex.getMessage()));
    }
}
