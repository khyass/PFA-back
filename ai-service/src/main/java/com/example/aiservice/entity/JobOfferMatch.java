package com.example.aiservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity caching the AI-computed match between a candidate's resume and a job offer.
 * Unique constraint on (candidate_id, job_offer_id) — one cached result per pair.
 */
@Entity
@Table(name = "job_offer_matches", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"candidate_id", "job_offer_id"}, name = "uk_match_candidate_joboffer")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobOfferMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * The user ID (from Keycloak) of the candidate.
     */
    @Column(name = "candidate_id", nullable = false)
    private String candidateId;

    /**
     * The job offer ID from job-offer-service.
     */
    @Column(name = "job_offer_id", nullable = false)
    private UUID jobOfferId;

    /**
     * Cached job offer title for display purposes.
     */
    @Column(name = "job_title", nullable = false)
    private String jobTitle;

    /**
     * Cached company name for display purposes.
     */
    @Column(name = "company_name", nullable = false)
    private String companyName;

    /**
     * Match score from 0 to 100.
     */
    @Column(name = "match_score", nullable = false)
    private Integer matchScore;

    /**
     * Missing skills stored as JSON array string.
     * Example: ["Docker", "Kubernetes"]
     */
    @Column(name = "missing_skills", columnDefinition = "TEXT")
    private String missingSkills;

    /**
     * AI-generated suggestions for improving the application.
     */
    @Column(name = "suggestions", columnDefinition = "TEXT")
    private String suggestions;

    /**
     * AI-generated interview questions stored as JSON array string.
     * Example: ["Tell me about yourself...", "..."]
     */
    @Column(name = "interview_questions", columnDefinition = "TEXT")
    private String interviewQuestions;

    /**
     * When this match was computed/refreshed.
     */
    @Column(name = "computed_at", nullable = false)
    private LocalDateTime computedAt;

    @PrePersist
    @PreUpdate
    public void setComputedAt() {
        this.computedAt = LocalDateTime.now();
    }
}
