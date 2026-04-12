package com.example.resumeanalyzer.dto;

public record AuthResponse(
        String token,
        String email,
        long expiresInMs
) {
}
