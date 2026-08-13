package com.moveai.security.config;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, TokenAuthenticationFilter filter) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.requestMatchers("/api/auth/**").permitAll().anyRequest().authenticated())
                .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    TokenAuthenticationFilter tokenAuthenticationFilter(
            @Value("${move-ai.security.admin-token:}") String adminToken,
            @Value("${move-ai.security.driver-token:}") String driverToken) {
        return new TokenAuthenticationFilter(adminToken, driverToken);
    }

    static class TokenAuthenticationFilter extends OncePerRequestFilter {
        private final String adminToken, driverToken;
        TokenAuthenticationFilter(String adminToken, String driverToken) { this.adminToken=adminToken; this.driverToken=driverToken; }

        @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) token = token.substring(7);
            String role = null;
            if (token != null && !adminToken.isBlank() && token.equals(adminToken)) role = "ROLE_ADMIN";
            else if (token != null && !driverToken.isBlank() && token.equals(driverToken)) role = "ROLE_DRIVER";
            if (role != null) {
                var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        role, null, java.util.List.of(new SimpleGrantedAuthority(role)));
                org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
            }
            chain.doFilter(request, response);
        }
    }
}
