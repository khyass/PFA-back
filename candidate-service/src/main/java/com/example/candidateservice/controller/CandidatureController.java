package com.example.candidateservice.controller;

import com.example.candidateservice.dto.*;
import com.example.candidateservice.service.CandidatureService;
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
 * REST controller for Candidature (job application) operations.
 * All endpoints require ROLE_CANDIDATE.
 */
@RestController
@RequestMapping("/api/candidatures")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('CANDIDATE')")
public class CandidatureController {

    private final CandidatureService candidatureService;

    /**
     * Get all candidatures for the authenticated candidate (paginated).
     *
     * @param jwt      The authenticated user's JWT
     * @param pageable Pagination parameters (default: page=0, size=10, sort=appliedDate,desc)
     * @return Page of candidatures
     */
    @GetMapping
    public ResponseEntity<Page<CandidatureResponseDTO>> getAllCandidatures(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 10, sort = "appliedDate", direction = Sort.Direction.DESC) Pageable pageable) {

        String candidateId = jwt.getSubject();
        log.debug("GET /api/candidatures - candidateId={}, page={}", candidateId, pageable);

        Page<CandidatureResponseDTO> candidatures = candidatureService.getAllCandidatures(candidateId, pageable);
        return ResponseEntity.ok(candidatures);
    }

    /**
     * Get a specific candidature by ID.
     *
     * @param id  The candidature ID
     * @param jwt The authenticated user's JWT
     * @return The candidature
     */
    @GetMapping("/{id}")
    public ResponseEntity<CandidatureResponseDTO> getCandidatureById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        String candidateId = jwt.getSubject();
        log.debug("GET /api/candidatures/{} - candidateId={}", id, candidateId);

        CandidatureResponseDTO candidature = candidatureService.getCandidatureById(id, candidateId);
        return ResponseEntity.ok(candidature);
    }

    /**
     * Get the full status history timeline for a candidature.
     *
     * @param id  The candidature ID
     * @param jwt The authenticated user's JWT
     * @return List of status history entries ordered by changedAt ascending
     */
    @GetMapping("/{id}/timeline")
    public ResponseEntity<List<StatusHistoryDTO>> getCandidatureTimeline(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        String candidateId = jwt.getSubject();
        log.debug("GET /api/candidatures/{}/timeline - candidateId={}", id, candidateId);

        List<StatusHistoryDTO> timeline = candidatureService.getCandidatureTimeline(id, candidateId);
        return ResponseEntity.ok(timeline);
    }

    /**
     * Apply to a job offer (create a new candidature).
     *
     * @param request The candidature request
     * @param jwt     The authenticated user's JWT
     * @return The created candidature
     */
    @PostMapping
    public ResponseEntity<CandidatureResponseDTO> createCandidature(
            @Valid @RequestBody CandidatureRequestDTO request,
            @AuthenticationPrincipal Jwt jwt) {

        String candidateId = jwt.getSubject();
        log.info("POST /api/candidatures - candidateId={}, jobOfferId={}", candidateId, request.getJobOfferId());

        CandidatureResponseDTO createdCandidature = candidatureService.createCandidature(request, candidateId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCandidature);
    }

    /**
     * Update a candidature (cover letter only - candidate cannot change status).
     *
     * @param id      The candidature ID
     * @param request The update request
     * @param jwt     The authenticated user's JWT
     * @return The updated candidature
     */
    @PutMapping("/{id}")
    public ResponseEntity<CandidatureResponseDTO> updateCandidature(
            @PathVariable UUID id,
            @Valid @RequestBody CandidatureUpdateDTO request,
            @AuthenticationPrincipal Jwt jwt) {

        String candidateId = jwt.getSubject();
        log.info("PUT /api/candidatures/{} - candidateId={}", id, candidateId);

        CandidatureResponseDTO updatedCandidature = candidatureService.updateCandidature(id, request, candidateId);
        return ResponseEntity.ok(updatedCandidature);
    }

    /**
     * Withdraw (delete) a candidature - only if status is PENDING.
     *
     * @param id  The candidature ID
     * @param jwt The authenticated user's JWT
     * @return No content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> withdrawCandidature(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        String candidateId = jwt.getSubject();
        log.info("DELETE /api/candidatures/{} - candidateId={}", id, candidateId);

        candidatureService.withdrawCandidature(id, candidateId);
        return ResponseEntity.noContent().build();
    }
}
