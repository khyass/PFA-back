package com.example.candidateservice.controller;

import com.example.candidateservice.dto.CandidatureForEnterpriseDTO;
import com.example.candidateservice.dto.CandidatureStatusUpdateDTO;
import com.example.candidateservice.service.CandidatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Entreprise - Candidatures", description = "Gestion des candidatures côté entreprise : consultation et mise à jour des statuts")
public class InternalCandidatureController {

    private final CandidatureService candidatureService;

    @Operation(summary = "Candidatures par offre (interne)", description = "Retourne toutes les candidatures pour une offre d'emploi donnée. Endpoint interne inter-services.")
    @GetMapping("/internal/candidatures/by-job-offer/{jobOfferId}")
    public ResponseEntity<List<CandidatureForEnterpriseDTO>> getCandidaturesForJobOffer(
            @Parameter(description = "ID de l'offre d'emploi") @PathVariable UUID jobOfferId) {

        log.debug("GET /internal/candidatures/by-job-offer/{}", jobOfferId);
        List<CandidatureForEnterpriseDTO> candidatures = candidatureService.getCandidaturesForJobOffer(jobOfferId);
        return ResponseEntity.ok(candidatures);
    }

    @Operation(summary = "Mettre à jour le statut", description = "Permet à une entreprise de changer le statut d'une candidature (accepter, refuser, planifier un entretien).")
    @PatchMapping("/api/candidatures/{id}/status")
    @PreAuthorize("hasRole('ENTERPRISE')")
    public ResponseEntity<Void> updateCandidatureStatus(
            @Parameter(description = "ID de la candidature") @PathVariable UUID id,
            @Valid @RequestBody CandidatureStatusUpdateDTO request) {

        log.info("PATCH /api/candidatures/{}/status - newStatus={}", id, request.getStatus());
        candidatureService.updateCandidatureStatus(id, request.getStatus(), request.getNote(),
                request.getInterviewDate(), request.getInterviewNotes());
        return ResponseEntity.ok().build();
    }
}
