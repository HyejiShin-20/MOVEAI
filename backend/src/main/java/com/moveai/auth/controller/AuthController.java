package com.moveai.auth.controller;

import com.moveai.auth.dto.LoginRequest;
import com.moveai.auth.dto.RegisterRequest;
import com.moveai.auth.service.AuthService;
import com.moveai.auth.service.JwtTokenService;
import com.moveai.user.entity.User;
import com.moveai.user.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtTokenService tokenService;
    private final AuthService authService;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtTokenService tokenService, AuthService authService) {
        this.users = users;
        this.encoder = encoder;
        this.tokenService = tokenService;
        this.authService = authService;
    }

    @GetMapping("/login-id/duplicate")
    public Map<String, Object> duplicate(@RequestParam String loginId) {
        return Map.of("loginId", loginId, "duplicated", users.existsByLoginId(loginId));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        try {
            User user = authService.register(request);
            return ResponseEntity.ok(Map.of(
                    "status", "REGISTER_OK",
                    "userId", user.getId(),
                    "loginId", user.getLoginId(),
                    "role", user.getRole().name(),
                    "name", user.getName()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "REGISTER_FAIL",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest r) {
        var u = users.findByLoginId(r.loginId())
                .orElseThrow(() -> new IllegalArgumentException("로그인 정보가 올바르지 않습니다."));
        if (!encoder.matches(r.password(), u.getPasswordHash())) {
            throw new IllegalArgumentException("로그인 정보가 올바르지 않습니다.");
        }

        String token = tokenService.generateToken(u.getId(), u.getLoginId(), u.getRole().name());
        return Map.of(
                "status", "LOGIN_OK",
                "role", u.getRole().name(),
                "name", u.getName(),
                "userId", u.getId(),
                "token", token
        );
    }
}

