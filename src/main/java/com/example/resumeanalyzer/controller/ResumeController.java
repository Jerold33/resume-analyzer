package com.example.resumeanalyzer.controller;

import com.example.resumeanalyzer.dto.AnalysisHistoryItemDto;
import com.example.resumeanalyzer.dto.AnalysisResponse;
import com.example.resumeanalyzer.service.ResumeService;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AnalysisResponse analyze(@RequestParam("file") MultipartFile file) {
        return resumeService.analyze(file);
    }

    @GetMapping("/history")
    public List<AnalysisHistoryItemDto> history(@RequestParam(name = "limit", defaultValue = "20") int limit) {
        return resumeService.history(limit);
    }
}
