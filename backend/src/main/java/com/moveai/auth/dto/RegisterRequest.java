package com.moveai.auth.dto;

public record RegisterRequest(
    String loginId,
    String password,
    String name,
    String phone,
    String role,
    String companyName,
    String position,
    String address,
    String career,
    String affiliation
) {}
