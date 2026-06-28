package com.example.candidateservice.service;

import com.example.candidateservice.dto.*;
import com.example.candidateservice.entity.Candidature;
import com.example.candidateservice.entity.CandidatureStatus;
import com.example.candidateservice.entity.CandidatureStatusHistory;
import com.example.candidateservice.exception.*;
import com.example.candidateservice.mapper.CandidateMapper;
import com.example.candidateservice.repository.CandidateProfileRepository;
import com.example.candidateservice.repository.CandidatureRepository;
import com.example.candidateservice.repository.CandidatureStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing candidatures (job applications).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CandidatureService {

    private final CandidatureRepository candidatureRepository;
    private final CandidatureStatusHistoryRepository statusHistoryRepository;
    private final CandidateProfileRepository profileRepository;
    private final JobOfferClient jobOfferClient;
    private final CandidateMapper candidateMapper;

    /**
     * Gets all candidatures for the authenticated candidate, optionally filtered by status.
     */
    @Transactional(readOnly = true)
    public Page<CandidatureResponseDTO> getAllCandidatures(String candidateId, CandidatureStatus status, Pageable pageable) {
        log.debug("Getting all candidatures for candidate: {}, status={}", candidateId, status);
        Page<Candidature> candidatures;
        if (status != null) {
            candidatures = candidatureRepository.findByCandidateIdAndStatus(candidateId, status, pageable);
        } else {
            candidatures = candidatureRepository.findByCandidateId(candidateId, pageable);
        }
        return candidatures.map(candidateMapper::toCandidatureResponseDTO);
    }

    /**
     * Gets a candidature by ID.
     */
    @Transactional(readOnly = true)
    public CandidatureResponseDTO getCandidatureById(UUID id, String candidateId) {
        log.debug("Getting candidature {} for candidate {}", id, candidateId);
        Candidature candidature = findCandidatureOrThrow(id, candidateId);
        return candidateMapper.toCandidatureResponseDTO(candidature);
    }

    /**
     * Gets the status history timeline for a candidature.
     */
    @Transactional(readOnly = true)
    public List<StatusHistoryDTO> getCandidatureTimeline(UUID candidatureId, String candidateId) {
        log.debug("Getting timeline for candidature {} for candidate {}", candidatureId, candidateId);

        // Verify ownership
        findCandidatureOrThrow(candidatureId, candidateId);

        List<CandidatureStatusHistory> history =
                statusHistoryRepository.findByCandidatureIdOrderByChangedAtAsc(candidatureId);

        return candidateMapper.toStatusHistoryDTOList(history);
    }

    /**
     * Creates a new candidature (applies to a job).
     */
    public CandidatureResponseDTO createCandidature(CandidatureRequestDTO request, String candidateId) {
        log.info("Candidate {} applying to job offer {}", candidateId, request.getJobOfferId());

        UUID jobOfferId = request.getJobOfferId();

        // Check for duplicate application
        if (candidatureRepository.existsByJobOfferIdAndCandidateId(jobOfferId, candidateId)) {
            log.warn("Duplicate application attempt by {} for job {}", candidateId, jobOfferId);
            throw new DuplicateCandidatureException(jobOfferId);
        }

        // Fetch and validate job offer
        JobOfferDTO jobOffer = jobOfferClient.getJobOffer(jobOfferId);

        if (!jobOfferClient.isAcceptingApplications(jobOffer)) {
            log.warn("Job offer {} is not accepting applications", jobOfferId);
            throw new JobOfferNotAcceptingApplicationsException();
        }

        // Create the candidature
        Candidature candidature = Candidature.builder()
                .jobOfferId(jobOfferId)
                .jobOfferTitle(jobOffer.getTitle())
                .companyName(jobOffer.getCompanyName())
                .candidateId(candidateId)
                .status(CandidatureStatus.PENDING)
                .appliedDate(LocalDate.now())
                .coverLetter(request.getCoverLetter())
                .build();

        // Save candidature
        Candidature savedCandidature = candidatureRepository.save(candidature);

        // Create initial status history entry
        createStatusHistoryEntry(savedCandidature, null, CandidatureStatus.PENDING, "Application submitted");

        // Notify job-offer-service to increment candidature count
        jobOfferClient.incrementCandidatureCount(jobOfferId);

        log.info("Created candidature {} for candidate {} to job {}", savedCandidature.getId(), candidateId, jobOfferId);

        return candidateMapper.toCandidatureResponseDTO(savedCandidature);
    }

    /**
     * Updates a candidature (cover letter only - candidates cannot change status).
     */
    public CandidatureResponseDTO updateCandidature(UUID id, CandidatureUpdateDTO request, String candidateId) {
        log.info("Updating candidature {} for candidate {}", id, candidateId);

        Candidature candidature = findCandidatureOrThrow(id, candidateId);

        // Only allow updating cover letter
        candidature.setCoverLetter(request.getCoverLetter());

        Candidature updatedCandidature = candidatureRepository.save(candidature);
        log.info("Updated candidature {}", id);

        return candidateMapper.toCandidatureResponseDTO(updatedCandidature);
    }

    /**
     * Withdraws (deletes) a candidature - only if status is PENDING.
     */
    public void withdrawCandidature(UUID id, String candidateId) {
        log.info("Withdrawing candidature {} for candidate {}", id, candidateId);

        Candidature candidature = findCandidatureOrThrow(id, candidateId);

        if (candidature.getStatus() != CandidatureStatus.PENDING) {
            log.warn("Cannot withdraw candidature {} - status is {}", id, candidature.getStatus());
            throw new CandidatureNotWithdrawableException(id);
        }

        UUID jobOfferId = candidature.getJobOfferId();
        candidatureRepository.delete(candidature);
        log.info("Withdrawn (deleted) candidature {}", id);

        // Notify job-offer-service to decrement candidature count
        jobOfferClient.decrementCandidatureCount(jobOfferId);
    }

    /**
     * Updates the status of a candidature (internal use by enterprise service).
     * This would typically be called via an event or internal API.
     */
    public void updateCandidatureStatus(UUID id, CandidatureStatus newStatus, String note) {
        log.info("Updating status of candidature {} to {}", id, newStatus);

        Candidature candidature = candidatureRepository.findById(id)
                .orElseThrow(() -> new CandidatureNotFoundException(id));

        CandidatureStatus oldStatus = candidature.getStatus();
        candidature.setStatus(newStatus);

        candidatureRepository.save(candidature);
        createStatusHistoryEntry(candidature, oldStatus, newStatus, note);

        log.info("Updated candidature {} status from {} to {}", id, oldStatus, newStatus);
    }

    /**
     * Gets all candidatures for a specific job offer (for enterprise/internal use).
     * Enriches with candidate profile name.
     */
    @Transactional(readOnly = true)
    public List<CandidatureForEnterpriseDTO> getCandidaturesForJobOffer(UUID jobOfferId) {
        log.debug("Getting candidatures for job offer: {}", jobOfferId);

        List<Candidature> candidatures = candidatureRepository.findByJobOfferId(jobOfferId);

        return candidatures.stream().map(c -> {
            var builder = CandidatureForEnterpriseDTO.builder()
                    .id(c.getId())
                    .candidateId(c.getCandidateId())
                    .status(c.getStatus())
                    .appliedDate(c.getAppliedDate())
                    .coverLetter(c.getCoverLetter());

            profileRepository.findByUserId(c.getCandidateId()).ifPresent(profile -> {
                builder.candidateName(profile.getFullName() != null ? profile.getFullName() : c.getCandidateId());
                builder.candidatePhone(profile.getPhone());
                builder.candidateBio(profile.getBio());
                builder.candidateLinkedinUrl(profile.getLinkedinUrl());
                builder.resumeFileName(profile.getResumeFileName());
            });

            if (builder.build().getCandidateName() == null) {
                builder.candidateName(c.getCandidateId());
            }

            return builder.build();
        }).toList();
    }

    private Candidature findCandidatureOrThrow(UUID id, String candidateId) {
        return candidatureRepository.findByIdAndCandidateId(id, candidateId)
                .orElseThrow(() -> {
                    // Check if candidature exists but belongs to another user
                    if (candidatureRepository.existsById(id)) {
                        return new AccessDeniedException("You do not have permission to access this candidature");
                    }
                    return new CandidatureNotFoundException(id);
                });
    }

    private void createStatusHistoryEntry(Candidature candidature, CandidatureStatus oldStatus,
                                          CandidatureStatus newStatus, String note) {
        CandidatureStatusHistory history = CandidatureStatusHistory.builder()
                .candidature(candidature)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedAt(LocalDateTime.now())
                .note(note)
                .build();

        statusHistoryRepository.save(history);
        log.debug("Created status history entry for candidature {}: {} -> {}", candidature.getId(), oldStatus, newStatus);
    }
}
