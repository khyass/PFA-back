package com.example.aiservice.repository;

import com.example.aiservice.entity.JobOfferMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for JobOfferMatch entity.
 */
@Repository
public interface JobOfferMatchRepository extends JpaRepository<JobOfferMatch, UUID> {

    /**
     * Finds a cached match for a specific candidate and job offer.
     */
    Optional<JobOfferMatch> findByCandidateIdAndJobOfferId(String candidateId, UUID jobOfferId);

    /**
     * Checks if a match exists for a specific candidate and job offer.
     */
    boolean existsByCandidateIdAndJobOfferId(String candidateId, UUID jobOfferId);

    /**
     * Finds all matches for a candidate ordered by match score descending.
     */
    List<JobOfferMatch> findByCandidateIdOrderByMatchScoreDesc(String candidateId);

    /**
     * Counts how many matches a candidate has.
     */
    long countByCandidateId(String candidateId);
}
