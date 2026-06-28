package com.example.aiservice.repository;

import com.example.aiservice.entity.InterviewPrepEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterviewPrepRepository extends JpaRepository<InterviewPrepEntity, UUID> {

    Optional<InterviewPrepEntity> findByCandidateIdAndOfferId(String candidateId, UUID offerId);

    void deleteByCandidateIdAndOfferId(String candidateId, UUID offerId);
}
