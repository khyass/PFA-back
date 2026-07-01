package com.example.candidateservice.controller;

import com.example.candidateservice.dto.*;
import com.example.candidateservice.service.CandidatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@RestController
@RequestMapping("/api/candidatures")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('CANDIDATE')")
@Tag(name = "Candidatures", description = "Gestion des candidatures pour les candidats : postuler, consulter, modifier et retirer")
public class CandidatureController {

    private final CandidatureService candidatureService;

    /**
     * Get all candidatures for the authenticated candidate (paginated).
     *
     * @param jwt      The authenticated user's JWT
     * @param pageable Pagination parameters (default: page=0, size=10, sort=appliedDate,desc)
     * @return Page of candidatures
     */
    @Operation(summary = "Lister mes candidatures", description = "Retourne la liste paginée des candidatures du candidat connecté, avec filtre optionnel par statut.")
    @GetMapping
    public ResponseEntity<Page<CandidatureResponseDTO>> getAllCandidatures(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) com.example.candidateservice.entity.CandidatureStatus status,
            @PageableDefault(size = 10, sort = "appliedDate", direction = Sort.Direction.DESC) Pageable pageable) {

        String candidateId = jwt.getSubject();
        log.debug("GET /api/candidatures - candidateId={}, status={}, page={}", candidateId, status, pageable);

        Page<CandidatureResponseDTO> candidatures = candidatureService.getAllCandidatures(candidateId, status, pageable);
        return ResponseEntity.ok(candidatures);
    }

    /**
     * Get a specific candidature by ID.
     *
     * @param id  The candidature ID
     * @param jwt The authenticated user's JWT
     * @return The candidature
     */
    @Operation(summary = "Détail d'une candidature", description = "Retourne les informations complètes d'une candidature spécifique.")
    @GetMapping("/{id}")
    public ResponseEntity<CandidatureResponseDTO> getCandidatureById(
            @Parameter(description = "ID de la candidature") @PathVariable UUID id,
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
    @Operation(summary = "Historique des statuts", description = "Retourne la timeline complète des changements de statut d'une candidature.")
    @GetMapping("/{id}/timeline")
    public ResponseEntity<List<StatusHistoryDTO>> getCandidatureTimeline(
            @Parameter(description = "ID de la candidature") @PathVariable UUID id,
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
    @Operation(summary = "Postuler à une offre", description = "Crée une nouvelle candidature pour une offre d'emploi.")
    @ApiResponse(responseCode = "201", description = "Candidature créée")
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
    @Operation(summary = "Modifier une candidature", description = "Met à jour la lettre de motivation d'une candidature existante.")
    @PutMapping("/{id}")
    public ResponseEntity<CandidatureResponseDTO> updateCandidature(
            @Parameter(description = "ID de la candidature") @PathVariable UUID id,
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
    @Operation(summary = "Retirer une candidature", description = "Supprime une candidature (uniquement si le statut est PENDING).")
    @ApiResponse(responseCode = "204", description = "Candidature retirée")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> withdrawCandidature(
            @Parameter(description = "ID de la candidature") @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        String candidateId = jwt.getSubject();
        log.info("DELETE /api/candidatures/{} - candidateId={}", id, candidateId);

        candidatureService.withdrawCandidature(id, candidateId);
        return ResponseEntity.noContent().build();
    }
}
