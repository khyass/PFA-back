package com.example.candidateservice.repository;

import com.example.candidateservice.entity.CandidateProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CandidateProfile entity.
 */
@Repository
public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, UUID> {

    /**
     * Finds a profile by user ID.
     */
    Optional<CandidateProfile> findByUserId(String userId);

    /**
     * Checks if a profile exists for a user.
     */
    boolean existsByUserId(String userId);
}
