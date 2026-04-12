package com.example.resumeanalyzer.service;

import com.example.resumeanalyzer.dto.AnalysisHistoryItemDto;
import com.example.resumeanalyzer.dto.AnalysisResponse;
import com.example.resumeanalyzer.entity.ResumeAnalysis;
import com.example.resumeanalyzer.entity.User;
import com.example.resumeanalyzer.exception.ApiException;
import com.example.resumeanalyzer.repository.ResumeAnalysisRepository;
import com.example.resumeanalyzer.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResumeService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx");
    private static final int EXCERPT_MAX = 4000;

    private final TextExtractionService textExtractionService;
    private final OpenAiResumeAnalysisService openAiResumeAnalysisService;
    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final UserRepository userRepository;

    private final int maxCharsForAi;

    public ResumeService(
            TextExtractionService textExtractionService,
            OpenAiResumeAnalysisService openAiResumeAnalysisService,
            ResumeAnalysisRepository resumeAnalysisRepository,
            UserRepository userRepository,
            @Value("${app.resume.max-chars-for-ai:15000}") int maxCharsForAi
    ) {
        this.textExtractionService = textExtractionService;
        this.openAiResumeAnalysisService = openAiResumeAnalysisService;
        this.resumeAnalysisRepository = resumeAnalysisRepository;
        this.userRepository = userRepository;
        this.maxCharsForAi = maxCharsForAi;
    }

    @Transactional
    public AnalysisResponse analyze(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File is required");
        }
        validateExtension(file.getOriginalFilename());

        String fullText = textExtractionService.extractText(file);
        String forModel = truncate(fullText, maxCharsForAi);

        OpenAiResumeAnalysisService.ParsedAnalysis parsed = openAiResumeAnalysisService.analyze(forModel);

        User user = currentUser();
        String excerpt = truncate(fullText, EXCERPT_MAX);

        ResumeAnalysis saved = resumeAnalysisRepository.save(new ResumeAnalysis(
                user,
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "resume",
                file.getContentType(),
                excerpt,
                parsed.rawJson(),
                parsed.atsScore()
        ));

        return new AnalysisResponse(
                saved.getId(),
                saved.getCreatedAt(),
                parsed.skills(),
                parsed.improvements(),
                parsed.atsScore(),
                parsed.summary()
        );
    }

    @Transactional(readOnly = true)
    public List<AnalysisHistoryItemDto> history(int limit) {
        User user = currentUser();
        int size = Math.min(Math.max(limit, 1), 100);
        return resumeAnalysisRepository.findByUserOrderByCreatedAtDesc(user, PageRequest.of(0, size))
                .stream()
                .map(a -> new AnalysisHistoryItemDto(
                        a.getId(),
                        a.getOriginalFilename(),
                        a.getAtsScore(),
                        a.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private static void validateExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File must have an extension (.pdf, .doc, or .docx)");
        }
        String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only PDF and Word files (.pdf, .doc, .docx) are supported");
        }
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max);
    }
}
