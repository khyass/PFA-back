package com.example.aiservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity storing the extracted text from a candidate's resume.
 * Updated every time the candidate re-uploads their resume.
 */
@Entity
@Table(name = "resume_analysis")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * The user ID (from Keycloak) of the candidate.
     * One-to-one relationship with the user.
     */
    @Column(name = "candidate_id", nullable = false, unique = true)
    private String candidateId;

    /**
     * Raw text extracted from the resume using Apache Tika.
     */
    @Column(name = "extracted_text", columnDefinition = "TEXT", nullable = false)
    private String extractedText;

    /**
     * When the resume was last analyzed/extracted.
     */
    @Column(name = "analyzed_at", nullable = false)
    private LocalDateTime analyzedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.analyzedAt == null) {
            this.analyzedAt = LocalDateTime.now();
        }
    }
}
