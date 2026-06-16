package com.example.candidateservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a candidate's profile.
 */
@Entity
@Table(name = "candidate_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * The user ID (from Keycloak) of the candidate.
     * This creates a one-to-one relationship with the user.
     */
    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "linkedin_url")
    private String linkedinUrl;

    @Column(name = "resume_file_name")
    private String resumeFileName;

    @Column(name = "resume_storage_path")
    private String resumeStoragePath;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
