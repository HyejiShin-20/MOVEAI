package com.moveai.auth.controller;
import com.moveai.auth.dto.LoginRequest;
import com.moveai.auth.service.JwtTokenService;
import com.moveai.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api/auth")
public class AuthController {
 private final UserRepository users;
 private final PasswordEncoder encoder;
 private final JwtTokenService tokenService;
 
 public AuthController(UserRepository u, PasswordEncoder e, JwtTokenService t) {
  users = u;
  encoder = e;
  tokenService = t;
 }
 
 @GetMapping("/login-id/duplicate")
 public Map<String, Object> duplicate(@RequestParam String loginId) {
  return Map.of("loginId", loginId, "duplicated", users.existsByLoginId(loginId));
 }
 
 @PostMapping("/login")
 public Map<String, Object> login(@RequestBody LoginRequest r) {
  var u = users.findByLoginId(r.loginId()).orElseThrow(() -> new IllegalArgumentException("로그인 정보가 올바르지 않습니다."));
  if (!encoder.matches(r.password(), u.getPasswordHash())) {
   throw new IllegalArgumentException("로그인 정보가 올바르지 않습니다.");
  }
  String token = tokenService.generateToken(u.getId(), u.getLoginId(), u.getRole().name());
  return Map.of("status", "LOGIN_OK", "role", u.getRole().name(), "name", u.getName(), "token", token);
 }
}
