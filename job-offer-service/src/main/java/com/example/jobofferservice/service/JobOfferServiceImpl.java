package com.example.jobofferservice.service;

import com.example.jobofferservice.dto.CandidatureDTO;
import com.example.jobofferservice.dto.JobOfferRequestDTO;
import com.example.jobofferservice.dto.JobOfferResponseDTO;
import com.example.jobofferservice.dto.JobOfferStatusUpdateDTO;
import com.example.jobofferservice.entity.Candidature;
import com.example.jobofferservice.entity.JobOffer;
import com.example.jobofferservice.entity.JobOfferStatus;
import com.example.jobofferservice.exception.AccessDeniedException;
import com.example.jobofferservice.exception.JobOfferHasCandidaturesException;
import com.example.jobofferservice.exception.JobOfferNotFoundException;
import com.example.jobofferservice.mapper.JobOfferMapper;
import com.example.jobofferservice.repository.CandidatureRepository;
import com.example.jobofferservice.repository.JobOfferRepository;
import com.example.jobofferservice.repository.JobOfferSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implementation of JobOfferService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class JobOfferServiceImpl implements JobOfferService {

    private final JobOfferRepository jobOfferRepository;
    private final CandidatureRepository candidatureRepository;
    private final CandidateServiceClient candidateServiceClient;
    private final JobOfferMapper jobOfferMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<JobOfferResponseDTO> getAllJobOffers(JobOfferStatus status, String company, String ownerId, Pageable pageable) {
        log.debug("Getting all job offers with status={}, company={}, ownerId={}, page={}", status, company, ownerId, pageable);

        Specification<JobOffer> spec = JobOfferSpecifications.withFilters(status, company, ownerId);
        Page<JobOffer> jobOffers = jobOfferRepository.findAll(spec, pageable);

        return jobOffers.map(jobOfferMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public JobOfferResponseDTO getJobOfferById(UUID id) {
        log.debug("Getting job offer by id: {}", id);

        JobOffer jobOffer = findJobOfferOrThrow(id);
        return jobOfferMapper.toResponseDTO(jobOffer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CandidatureDTO> getCandidaturesForJobOffer(UUID jobOfferId, String userId) {
        log.debug("Getting candidatures for job offer: {}", jobOfferId);

        JobOffer jobOffer = findJobOfferOrThrow(jobOfferId);
        verifyOwnership(jobOffer, userId);

        return candidateServiceClient.getCandidaturesForJobOffer(jobOfferId);
    }

    @Override
    public JobOfferResponseDTO createJobOffer(JobOfferRequestDTO request, String ownerId) {
        log.info("Creating new job offer for owner: {}", ownerId);

        JobOffer jobOffer = jobOfferMapper.toEntity(request);
        jobOffer.setOwnerId(ownerId);
        jobOffer.setCandidatureCount(0);

        JobOffer savedJobOffer = jobOfferRepository.save(jobOffer);
        log.info("Created job offer with id: {}", savedJobOffer.getId());

        return jobOfferMapper.toResponseDTO(savedJobOffer);
    }

    @Override
    @Transactional
    public void incrementCandidatureCount(UUID jobOfferId) {
        JobOffer jobOffer = findJobOfferOrThrow(jobOfferId);
        jobOffer.setCandidatureCount(jobOffer.getCandidatureCount() + 1);
        jobOfferRepository.save(jobOffer);
        log.info("Incremented candidature count for job offer {} to {}", jobOfferId, jobOffer.getCandidatureCount());
    }

    @Override
    @Transactional
    public void decrementCandidatureCount(UUID jobOfferId) {
        JobOffer jobOffer = findJobOfferOrThrow(jobOfferId);
        int newCount = Math.max(0, jobOffer.getCandidatureCount() - 1);
        jobOffer.setCandidatureCount(newCount);
        jobOfferRepository.save(jobOffer);
        log.info("Decremented candidature count for job offer {} to {}", jobOfferId, newCount);
    }

    @Override
    public JobOfferResponseDTO updateJobOffer(UUID id, JobOfferRequestDTO request, String userId) {
        log.info("Updating job offer: {} by user: {}", id, userId);

        JobOffer jobOffer = findJobOfferOrThrow(id);
        verifyOwnership(jobOffer, userId);

        jobOfferMapper.updateEntityFromDTO(request, jobOffer);
        JobOffer updatedJobOffer = jobOfferRepository.save(jobOffer);

        log.info("Updated job offer: {}", id);
        return jobOfferMapper.toResponseDTO(updatedJobOffer);
    }

    @Override
    public JobOfferResponseDTO updateJobOfferStatus(UUID id, JobOfferStatusUpdateDTO request, String userId) {
        log.info("Updating status for job offer: {} to {} by user: {}", id, request.getStatus(), userId);

        JobOffer jobOffer = findJobOfferOrThrow(id);
        verifyOwnership(jobOffer, userId);

        jobOffer.setStatus(request.getStatus());
        JobOffer updatedJobOffer = jobOfferRepository.save(jobOffer);

        log.info("Updated status for job offer: {} to {}", id, request.getStatus());
        return jobOfferMapper.toResponseDTO(updatedJobOffer);
    }

    @Override
    public void deleteJobOffer(UUID id, String userId) {
        log.info("Deleting job offer: {} by user: {}", id, userId);

        JobOffer jobOffer = findJobOfferOrThrow(id);
        verifyOwnership(jobOffer, userId);

        // Check if job offer has any candidatures
        if (candidatureRepository.existsByJobOfferId(id)) {
            log.warn("Cannot delete job offer {} because it has existing candidatures", id);
            throw new JobOfferHasCandidaturesException(id);
        }

        jobOfferRepository.delete(jobOffer);
        log.info("Deleted job offer: {}", id);
    }

    /**
     * Finds a job offer by ID or throws JobOfferNotFoundException.
     */
    private JobOffer findJobOfferOrThrow(UUID id) {
        return jobOfferRepository.findById(id)
                .orElseThrow(() -> new JobOfferNotFoundException(id));
    }

    /**
     * Verifies that the given user owns the job offer.
     * Throws AccessDeniedException if the user is not the owner.
     */
    private void verifyOwnership(JobOffer jobOffer, String userId) {
        if (!jobOffer.getOwnerId().equals(userId)) {
            log.warn("User {} attempted to access job offer {} owned by {}",
                    userId, jobOffer.getId(), jobOffer.getOwnerId());
            throw new AccessDeniedException("You do not have permission to access this job offer");
        }
    }
}
