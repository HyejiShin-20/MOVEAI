package com.moveai.security.config;
import org.springframework.context.annotation.*;
import org.springframework.web.cors.*;
import java.util.List;
@Configuration
public class CorsConfig {
 @Bean CorsConfigurationSource corsConfigurationSource(){
  CorsConfiguration c=new CorsConfiguration();
  c.setAllowedOrigins(List.of("http://localhost:5173","http://127.0.0.1:5173"));
  c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
  c.setAllowedHeaders(List.of("*")); c.setAllowCredentials(true);
  UrlBasedCorsConfigurationSource s=new UrlBasedCorsConfigurationSource();
  s.registerCorsConfiguration("/**",c); return s;
 }
}
