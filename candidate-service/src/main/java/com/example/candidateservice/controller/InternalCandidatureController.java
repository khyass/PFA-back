package com.example.candidateservice.controller;

import com.example.candidateservice.dto.CandidatureForEnterpriseDTO;
import com.example.candidateservice.dto.CandidatureStatusUpdateDTO;
import com.example.candidateservice.service.CandidatureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Internal REST controller for inter-service communication.
 * These endpoints are called by other microservices (e.g., job-offer-service).
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class InternalCandidatureController {

    private final CandidatureService candidatureService;

    /**
     * Get all candidatures for a specific job offer.
     * Called by job-offer-service to display candidatures to the enterprise user.
     */
    @GetMapping("/internal/candidatures/by-job-offer/{jobOfferId}")
    public ResponseEntity<List<CandidatureForEnterpriseDTO>> getCandidaturesForJobOffer(
            @PathVariable UUID jobOfferId) {

        log.debug("GET /internal/candidatures/by-job-offer/{}", jobOfferId);
        List<CandidatureForEnterpriseDTO> candidatures = candidatureService.getCandidaturesForJobOffer(jobOfferId);
        return ResponseEntity.ok(candidatures);
    }

    /**
     * Update the status of a candidature (enterprise only).
     * Called by the frontend when an enterprise user accepts/rejects a candidature.
     */
    @PatchMapping("/api/candidatures/{id}/status")
    @PreAuthorize("hasRole('ENTERPRISE')")
    public ResponseEntity<Void> updateCandidatureStatus(
            @PathVariable UUID id,
            @Valid @RequestBody CandidatureStatusUpdateDTO request) {

        log.info("PATCH /api/candidatures/{}/status - newStatus={}", id, request.getStatus());
        candidatureService.updateCandidatureStatus(id, request.getStatus(), request.getNote());
        return ResponseEntity.ok().build();
    }
}
