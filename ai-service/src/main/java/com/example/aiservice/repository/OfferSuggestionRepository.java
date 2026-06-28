package com.example.aiservice.repository;

import com.example.aiservice.entity.OfferSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OfferSuggestionRepository extends JpaRepository<OfferSuggestion, UUID> {

    List<OfferSuggestion> findByCandidateIdAndKeywordsHashOrderByScoreDesc(String candidateId, String keywordsHash);

    void deleteByCandidateIdAndKeywordsHash(String candidateId, String keywordsHash);
}
