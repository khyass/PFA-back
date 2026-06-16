package com.example.candidateservice.repository;

import com.example.candidateservice.entity.CandidatureStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for CandidatureStatusHistory entity.
 */
@Repository
public interface CandidatureStatusHistoryRepository extends JpaRepository<CandidatureStatusHistory, UUID> {

    /**
     * Finds all status history entries for a candidature, ordered by changedAt ascending.
     */
    List<CandidatureStatusHistory> findByCandidatureIdOrderByChangedAtAsc(UUID candidatureId);
}
