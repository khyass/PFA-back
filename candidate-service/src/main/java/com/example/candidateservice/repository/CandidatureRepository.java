package com.example.candidateservice.repository;

import com.example.candidateservice.entity.Candidature;
import com.example.candidateservice.entity.CandidatureStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
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
     * Finds all candidatures for a specific candidate filtered by status.
     */
    Page<Candidature> findByCandidateIdAndStatus(String candidateId, CandidatureStatus status, Pageable pageable);

    /**
     * Finds a candidature by ID and candidate ID (for ownership check).
     */
    Optional<Candidature> findByIdAndCandidateId(UUID id, String candidateId);

    /**
     * Finds all candidatures for a specific job offer.
     */
    List<Candidature> findByJobOfferId(UUID jobOfferId);

    /**
     * Checks if a candidature already exists for a specific job offer and candidate.
     */
    boolean existsByJobOfferIdAndCandidateId(UUID jobOfferId, String candidateId);

    /**
     * Counts the number of candidatures for a specific job offer.
     */
    long countByJobOfferId(UUID jobOfferId);
}
