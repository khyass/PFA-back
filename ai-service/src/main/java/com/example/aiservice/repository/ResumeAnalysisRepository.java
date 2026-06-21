package com.example.aiservice.repository;

import com.example.aiservice.entity.ResumeAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ResumeAnalysis entity.
 */
@Repository
public interface ResumeAnalysisRepository extends JpaRepository<ResumeAnalysis, UUID> {

    /**
     * Finds the resume analysis for a specific candidate.
     */
    Optional<ResumeAnalysis> findByCandidateId(String candidateId);

    /**
     * Checks if a resume analysis exists for a candidate.
     */
    boolean existsByCandidateId(String candidateId);
}
