package com.example.resumeanalyzer.dto;

import java.time.Instant;
import java.util.List;

public record AnalysisResponse(
        Long analysisId,
        Instant createdAt,
        List<String> skills,
        List<ImprovementDto> improvements,
        Integer atsScore,
        String summary
) {
}
