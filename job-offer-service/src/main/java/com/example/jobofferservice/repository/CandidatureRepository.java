package com.example.jobofferservice.repository;

import com.example.jobofferservice.entity.Candidature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Candidature entity.
 */
@Repository
public interface CandidatureRepository extends JpaRepository<Candidature, UUID> {

    /**
     * Finds all candidatures for a specific job offer.
     */
    List<Candidature> findByJobOfferId(UUID jobOfferId);

    /**
     * Checks if any candidatures exist for a specific job offer.
     */
    boolean existsByJobOfferId(UUID jobOfferId);

    /**
     * Counts the number of candidatures for a specific job offer.
     */
    long countByJobOfferId(UUID jobOfferId);
}
