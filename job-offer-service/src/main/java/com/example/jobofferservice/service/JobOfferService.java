package com.example.jobofferservice.service;

import com.example.jobofferservice.dto.CandidatureDTO;
import com.example.jobofferservice.dto.JobOfferRequestDTO;
import com.example.jobofferservice.dto.JobOfferResponseDTO;
import com.example.jobofferservice.dto.JobOfferStatusUpdateDTO;
import com.example.jobofferservice.entity.JobOfferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for JobOffer operations.
 */
public interface JobOfferService {

    /**
     * Gets all job offers with pagination and optional filtering.
     *
     * @param status   Optional status filter
     * @param company  Optional company name filter (case-insensitive partial match)
     * @param pageable Pagination parameters
     * @return Page of job offer DTOs
     */
    Page<JobOfferResponseDTO> getAllJobOffers(JobOfferStatus status, String company, Pageable pageable);

    /**
     * Gets a job offer by its ID.
     *
     * @param id The job offer ID
     * @return The job offer DTO
     */
    JobOfferResponseDTO getJobOfferById(UUID id);

    /**
     * Gets all candidatures for a specific job offer.
     *
     * @param jobOfferId The job offer ID
     * @param userId     The authenticated user's ID (for ownership check)
     * @return List of candidature DTOs
     */
    List<CandidatureDTO> getCandidaturesForJobOffer(UUID jobOfferId, String userId);

    /**
     * Creates a new job offer.
     *
     * @param request The job offer request DTO
     * @param ownerId The owner's user ID (from Keycloak)
     * @return The created job offer DTO
     */
    JobOfferResponseDTO createJobOffer(JobOfferRequestDTO request, String ownerId);

    /**
     * Updates an existing job offer.
     *
     * @param id      The job offer ID
     * @param request The job offer request DTO
     * @param userId  The authenticated user's ID (for ownership check)
     * @return The updated job offer DTO
     */
    JobOfferResponseDTO updateJobOffer(UUID id, JobOfferRequestDTO request, String userId);

    /**
     * Updates only the status of a job offer.
     *
     * @param id      The job offer ID
     * @param request The status update DTO
     * @param userId  The authenticated user's ID (for ownership check)
     * @return The updated job offer DTO
     */
    JobOfferResponseDTO updateJobOfferStatus(UUID id, JobOfferStatusUpdateDTO request, String userId);

    /**
     * Deletes a job offer.
     *
     * @param id     The job offer ID
     * @param userId The authenticated user's ID (for ownership check)
     */
    void deleteJobOffer(UUID id, String userId);
}
