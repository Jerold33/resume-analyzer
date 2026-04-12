package com.example.resumeanalyzer.service;

import com.example.resumeanalyzer.config.OpenAiProperties;
import com.example.resumeanalyzer.dto.ImprovementDto;
import com.example.resumeanalyzer.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class OpenAiResumeAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiResumeAnalysisService.class);

    private static final String SYSTEM_PROMPT = """
            You are an expert resume reviewer and ATS (Applicant Tracking System) specialist.
            Analyze the resume text provided by the user.
            Respond with a single JSON object only (no markdown fences) using exactly these keys:
            - "skills": array of strings (distinct professional skills inferred from the resume)
            - "improvements": array of objects, each with "area" and "suggestion" strings (concrete, actionable improvements)
            - "atsScore": integer from 0 to 100 estimating ATS friendliness (formatting, keywords, structure—not factual verification)
            - "summary": short string summarizing overall fit and tone for hiring managers
            Be honest, concise, and professional. If the text is not a resume, lower the score and explain in summary.
            """;

    private final WebClient openAiWebClient;
    private final OpenAiProperties openAiProperties;
    private final ObjectMapper objectMapper;

    public OpenAiResumeAnalysisService(WebClient openAiWebClient, OpenAiProperties openAiProperties, ObjectMapper objectMapper) {
        this.openAiWebClient = openAiWebClient;
        this.openAiProperties = openAiProperties;
        this.objectMapper = objectMapper;
    }

    public ParsedAnalysis analyze(String resumeText) {
        if (openAiProperties.apiKey() == null || openAiProperties.apiKey().isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "OpenAI API key is not configured");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", openAiProperties.model());
        body.put("response_format", Map.of("type", "json_object"));
        body.put("messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", "Resume text:\n\n" + resumeText)
        ));

        try {
            JsonNode root = openAiWebClient.post()
                    .uri("/v1/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiProperties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (root == null) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "Empty response from OpenAI");
            }
            String content = root.path("choices").path(0).path("message").path("content").asText(null);
            if (content == null || content.isBlank()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "OpenAI returned no content");
            }
            return parseModelJson(content);
        } catch (WebClientResponseException e) {
            log.warn("OpenAI HTTP error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "OpenAI request failed: " + e.getStatusCode()
            );
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("OpenAI call failed", e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "OpenAI request failed");
        }
    }

    private ParsedAnalysis parseModelJson(String content) {
        try {
            JsonNode node = objectMapper.readTree(content);
            List<String> skills = new ArrayList<>();
            if (node.path("skills").isArray()) {
                node.path("skills").forEach(s -> {
                    if (s.isTextual()) {
                        skills.add(s.asText());
                    }
                });
            }
            List<ImprovementDto> improvements = new ArrayList<>();
            if (node.path("improvements").isArray()) {
                node.path("improvements").forEach(o -> {
                    if (o.isObject()) {
                        improvements.add(new ImprovementDto(
                                o.path("area").asText("General"),
                                o.path("suggestion").asText("")
                        ));
                    }
                });
            }
            int ats = node.path("atsScore").asInt(0);
            ats = Math.max(0, Math.min(100, ats));
            String summary = node.path("summary").asText("");
            return new ParsedAnalysis(skills, improvements, ats, summary, content);
        } catch (Exception e) {
            log.warn("Failed to parse model JSON, returning degraded result", e);
            return new ParsedAnalysis(
                    List.of(),
                    List.of(),
                    null,
                    "AI returned a response that could not be parsed as JSON.",
                    content
            );
        }
    }

    public record ParsedAnalysis(
            List<String> skills,
            List<ImprovementDto> improvements,
            Integer atsScore,
            String summary,
            String rawJson
    ) {
    }
}
