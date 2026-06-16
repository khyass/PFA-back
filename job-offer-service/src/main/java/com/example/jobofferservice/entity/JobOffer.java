package com.example.jobofferservice.entity;

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
 * Entity representing a job offer posted by an enterprise.
 */
@Entity
@Table(name = "job_offers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "title", nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private JobOfferStatus status;

    @Column(name = "published_date")
    private LocalDate publishedDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "candidature_count", nullable = false)
    @Builder.Default
    private int candidatureCount = 0;

    /**
     * The user ID (from Keycloak) of the enterprise owner.
     */
    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "jobOffer", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Candidature> candidatures = new ArrayList<>();

    /**
     * Adds a candidature to this job offer and updates the count.
     */
    public void addCandidature(Candidature candidature) {
        candidatures.add(candidature);
        candidature.setJobOffer(this);
        this.candidatureCount = candidatures.size();
    }

    /**
     * Removes a candidature from this job offer and updates the count.
     */
    public void removeCandidature(Candidature candidature) {
        candidatures.remove(candidature);
        candidature.setJobOffer(null);
        this.candidatureCount = candidatures.size();
    }
}
