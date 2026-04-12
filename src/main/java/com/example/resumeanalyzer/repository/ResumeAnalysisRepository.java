package com.example.resumeanalyzer.repository;

import com.example.resumeanalyzer.entity.ResumeAnalysis;
import com.example.resumeanalyzer.entity.User;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeAnalysisRepository extends JpaRepository<ResumeAnalysis, Long> {

    List<ResumeAnalysis> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
}
