package com.example.resumeanalyzer.dto;

import java.time.Instant;

public record AnalysisHistoryItemDto(
        Long id,
        String originalFilename,
        Integer atsScore,
        Instant createdAt
) {
}
