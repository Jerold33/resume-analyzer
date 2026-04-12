package com.example.resumeanalyzer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "resume_analyses")
public class ResumeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "original_filename", nullable = false, length = 512)
    private String originalFilename;

    @Column(name = "mime_type", length = 255)
    private String mimeType;

    /** Truncated excerpt of extracted text for history/debugging (not full resume). */
    @Column(name = "extracted_text_excerpt", columnDefinition = "TEXT")
    private String extractedTextExcerpt;

    @Column(name = "ai_raw_response", nullable = false, columnDefinition = "LONGTEXT")
    private String aiRawResponse;

    @Column(name = "ats_score")
    private Integer atsScore;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected ResumeAnalysis() {
    }

    public ResumeAnalysis(User user, String originalFilename, String mimeType, String extractedTextExcerpt,
                          String aiRawResponse, Integer atsScore) {
        this.user = user;
        this.originalFilename = originalFilename;
        this.mimeType = mimeType;
        this.extractedTextExcerpt = extractedTextExcerpt;
        this.aiRawResponse = aiRawResponse;
        this.atsScore = atsScore;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getExtractedTextExcerpt() {
        return extractedTextExcerpt;
    }

    public String getAiRawResponse() {
        return aiRawResponse;
    }

    public Integer getAtsScore() {
        return atsScore;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
