package com.example.aiservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for cached AI interview preparation results.
 */
@Entity
@Table(name = "ai_interview_prep")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewPrepEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "candidate_id", nullable = false)
    private String candidateId;

    @Column(name = "offer_id", nullable = false)
    private UUID offerId;

    @Column(name = "payload", nullable = false, columnDefinition = "CLOB")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
