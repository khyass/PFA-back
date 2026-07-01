package com.example.candidateservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a candidate's application to a job offer.
 */
@Entity
@Table(name = "candidatures", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"job_offer_id", "candidate_id"}, name = "uk_candidature_job_candidate")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candidature {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * The job offer ID this candidature is for.
     * We store the ID rather than a foreign key since job offers are in a different service.
     */
    @Column(name = "job_offer_id", nullable = false)
    private UUID jobOfferId;

    /**
     * Cached job offer title for display purposes.
     */
    @Column(name = "job_offer_title", nullable = false)
    private String jobOfferTitle;

    /**
     * Cached company name for display purposes.
     */
    @Column(name = "company_name", nullable = false)
    private String companyName;

    /**
     * The user ID (from Keycloak) of the candidate who applied.
     */
    @Column(name = "candidate_id", nullable = false)
    private String candidateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CandidatureStatus status;

    @Column(name = "applied_date", nullable = false)
    private LocalDate appliedDate;

    @Column(name = "cover_letter", columnDefinition = "TEXT")
    private String coverLetter;

    @Column(name = "interview_date")
    private LocalDateTime interviewDate;

    @Column(name = "interview_notes", columnDefinition = "TEXT")
    private String interviewNotes;

    @OneToMany(mappedBy = "candidature", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("changedAt ASC")
    @Builder.Default
    private List<CandidatureStatusHistory> statusHistory = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Transient field to track the previous status for history tracking.
     */
    @Transient
    private CandidatureStatus previousStatus;

    @PostLoad
    public void trackPreviousStatus() {
        this.previousStatus = this.status;
    }

    /**
     * Adds a status history entry.
     */
    public void addStatusHistory(CandidatureStatusHistory history) {
        statusHistory.add(history);
        history.setCandidature(this);
    }
}
