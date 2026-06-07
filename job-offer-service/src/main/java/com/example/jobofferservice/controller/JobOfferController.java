package com.example.jobofferservice.controller;

import com.example.jobofferservice.dto.CandidatureDTO;
import com.example.jobofferservice.dto.JobOfferRequestDTO;
import com.example.jobofferservice.dto.JobOfferResponseDTO;
import com.example.jobofferservice.dto.JobOfferStatusUpdateDTO;
import com.example.jobofferservice.entity.JobOfferStatus;
import com.example.jobofferservice.service.JobOfferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for JobOffer CRUD operations.
 * All endpoints require ROLE_ENTREPRISE.
 */
@RestController
@RequestMapping("/api/job-offers")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ENTREPRISE')")
public class JobOfferController {

    private final JobOfferService jobOfferService;

    /**
     * Get all job offers with pagination and optional filtering.
     * Supports filtering by status and company name (case-insensitive partial match).
     *
     * @param status   Optional status filter
     * @param company  Optional company name filter
     * @param pageable Pagination parameters (default: page=0, size=10, sort=publishedDate,desc)
     * @return Page of job offers
     */
    @GetMapping
    public ResponseEntity<Page<JobOfferResponseDTO>> getAllJobOffers(
            @RequestParam(required = false) JobOfferStatus status,
            @RequestParam(required = false) String company,
            @PageableDefault(size = 10, sort = "publishedDate", direction = Sort.Direction.DESC) Pageable pageable) {

        log.debug("GET /api/job-offers - status={}, company={}, page={}", status, company, pageable);
        Page<JobOfferResponseDTO> jobOffers = jobOfferService.getAllJobOffers(status, company, pageable);
        return ResponseEntity.ok(jobOffers);
    }

    /**
     * Get a specific job offer by ID.
     *
     * @param id The job offer ID
     * @return The job offer
     */
    @GetMapping("/{id}")
    public ResponseEntity<JobOfferResponseDTO> getJobOfferById(@PathVariable UUID id) {
        log.debug("GET /api/job-offers/{}", id);
        JobOfferResponseDTO jobOffer = jobOfferService.getJobOfferById(id);
        return ResponseEntity.ok(jobOffer);
    }

    /**
     * Get all candidatures for a specific job offer.
     * Only the owner of the job offer can access this endpoint.
     *
     * @param id  The job offer ID
     * @param jwt The authenticated user's JWT
     * @return List of candidatures
     */
    @GetMapping("/{id}/candidatures")
    public ResponseEntity<List<CandidatureDTO>> getCandidaturesForJobOffer(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        log.debug("GET /api/job-offers/{}/candidatures - userId={}", id, userId);

        List<CandidatureDTO> candidatures = jobOfferService.getCandidaturesForJobOffer(id, userId);
        return ResponseEntity.ok(candidatures);
    }

    /**
     * Create a new job offer.
     *
     * @param request The job offer request
     * @param jwt     The authenticated user's JWT
     * @return The created job offer
     */
    @PostMapping
    public ResponseEntity<JobOfferResponseDTO> createJobOffer(
            @Valid @RequestBody JobOfferRequestDTO request,
            @AuthenticationPrincipal Jwt jwt) {

        String ownerId = jwt.getSubject();
        log.info("POST /api/job-offers - ownerId={}", ownerId);

        JobOfferResponseDTO createdJobOffer = jobOfferService.createJobOffer(request, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdJobOffer);
    }

    /**
     * Full update of a job offer.
     * Only the owner of the job offer can update it.
     *
     * @param id      The job offer ID
     * @param request The job offer request
     * @param jwt     The authenticated user's JWT
     * @return The updated job offer
     */
    @PutMapping("/{id}")
    public ResponseEntity<JobOfferResponseDTO> updateJobOffer(
            @PathVariable UUID id,
            @Valid @RequestBody JobOfferRequestDTO request,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        log.info("PUT /api/job-offers/{} - userId={}", id, userId);

        JobOfferResponseDTO updatedJobOffer = jobOfferService.updateJobOffer(id, request, userId);
        return ResponseEntity.ok(updatedJobOffer);
    }

    /**
     * Update only the status of a job offer.
     * Only the owner of the job offer can update its status.
     *
     * @param id      The job offer ID
     * @param request The status update request
     * @param jwt     The authenticated user's JWT
     * @return The updated job offer
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<JobOfferResponseDTO> updateJobOfferStatus(
            @PathVariable UUID id,
            @Valid @RequestBody JobOfferStatusUpdateDTO request,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        log.info("PATCH /api/job-offers/{}/status - userId={}, newStatus={}", id, userId, request.getStatus());

        JobOfferResponseDTO updatedJobOffer = jobOfferService.updateJobOfferStatus(id, request, userId);
        return ResponseEntity.ok(updatedJobOffer);
    }

    /**
     * Delete a job offer.
     * Only the owner can delete it, and only if it has no candidatures.
     *
     * @param id  The job offer ID
     * @param jwt The authenticated user's JWT
     * @return No content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJobOffer(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        log.info("DELETE /api/job-offers/{} - userId={}", id, userId);

        jobOfferService.deleteJobOffer(id, userId);
        return ResponseEntity.noContent().build();
    }
}
