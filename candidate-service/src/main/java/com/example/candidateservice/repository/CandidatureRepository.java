package com.example.candidateservice.repository;

import com.example.candidateservice.entity.Candidature;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Candidature entity.
 */
@Repository
public interface CandidatureRepository extends JpaRepository<Candidature, UUID> {

    /**
     * Finds all candidatures for a specific candidate with pagination.
     */
    Page<Candidature> findByCandidateId(String candidateId, Pageable pageable);

    /**
     * Finds a candidature by ID and candidate ID (for ownership check).
     */
    Optional<Candidature> findByIdAndCandidateId(UUID id, String candidateId);

    /**
     * Checks if a candidature already exists for a specific job offer and candidate.
     */
    boolean existsByJobOfferIdAndCandidateId(UUID jobOfferId, String candidateId);

    /**
     * Counts the number of candidatures for a specific job offer.
     */
    long countByJobOfferId(UUID jobOfferId);
}
