package com.example.resumeanalyzer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.openai")
public record OpenAiProperties(
        String apiKey,
        String baseUrl,
        String model,
        long connectTimeoutMs,
        long readTimeoutMs
) {
    public OpenAiProperties {
        if (apiKey == null) {
            apiKey = "";
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.openai.com";
        }
        if (model == null || model.isBlank()) {
            model = "gpt-4o-mini";
        }
        if (connectTimeoutMs <= 0) {
            connectTimeoutMs = 10_000;
        }
        if (readTimeoutMs <= 0) {
            readTimeoutMs = 120_000;
        }
    }
}
