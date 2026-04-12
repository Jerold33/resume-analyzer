package com.example.resumeanalyzer.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.resumeanalyzer.config.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;

class JwtServiceTest {

    @Test
    void generatesAndValidatesToken() {
        JwtProperties props = new JwtProperties("test-jwt-secret-at-least-32-characters-long", 60_000L);
        JwtService jwtService = new JwtService(props);

        var userDetails = User.withUsername("alice@example.com")
                .password("pw")
                .roles("USER")
                .build();

        String token = jwtService.generateToken(userDetails);
        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("alice@example.com");
        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }
}
