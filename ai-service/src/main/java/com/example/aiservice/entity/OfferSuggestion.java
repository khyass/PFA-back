package com.example.aiservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for cached offer suggestions based on keyword matching.
 */
@Entity
@Table(name = "ai_offer_suggestions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfferSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "candidate_id", nullable = false)
    private String candidateId;

    @Column(name = "keywords_hash", nullable = false, length = 64)
    private String keywordsHash;

    @Column(name = "offer_id", nullable = false)
    private UUID offerId;

    @Column(nullable = false)
    private Integer score;

    @Column(columnDefinition = "TEXT")
    private String justification;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
