package com.moveai.auth.dto;

import java.util.Map;

public record LoginResponse(
    String status,
    String role,
    String name,
    String token
) {
    public Map<String, Object> toMap() {
        return Map.of(
            "status", status,
            "role", role,
            "name", name,
            "token", token
        );
    }
}
