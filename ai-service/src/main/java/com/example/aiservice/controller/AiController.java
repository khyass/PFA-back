package com.example.aiservice.controller;

import com.example.aiservice.dto.*;
import com.example.aiservice.service.AiMatchingService;
import com.example.aiservice.service.CoverLetterService;
import com.example.aiservice.service.InterviewChatService;
import com.example.aiservice.service.InterviewPrepService;
import com.example.aiservice.service.JobSuggestionService;
import com.example.aiservice.service.KeywordSuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for AI matching and suggestions.
 * All endpoints require ROLE_CANDIDATE.
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('CANDIDATE')")
@Tag(name = "Intelligence Artificielle", description = "Endpoints IA : matching, suggestions, préparation d'entretien, génération de lettre de motivation")
public class AiController {

    private final AiMatchingService aiMatchingService;
    private final JobSuggestionService jobSuggestionService;
    private final KeywordSuggestionService keywordSuggestionService;
    private final InterviewPrepService interviewPrepService;
    private final InterviewChatService interviewChatService;
    private final CoverLetterService coverLetterService;

    /**
     * Get ranked job offer suggestions for the authenticated candidate.
     * Returns 202 Accepted if suggestions are being computed asynchronously.
     *
     * @param jwt The authenticated user's JWT
     * @return List of job suggestions ordered by match score
     */
    @Operation(summary = "Obtenir les suggestions d'offres", description = "Retourne les offres recommandées pour le candidat connecté. Retourne 202 si le calcul est en cours.")
    @ApiResponse(responseCode = "200", description = "Suggestions disponibles")
    @ApiResponse(responseCode = "202", description = "Calcul en cours")
    @GetMapping("/suggestions")
    public ResponseEntity<SuggestionsResponseDTO> getSuggestions(@AuthenticationPrincipal Jwt jwt) {
        String candidateId = jwt.getSubject();
        log.info("GET /api/ai/suggestions - candidateId={}", candidateId);

        SuggestionsResponseDTO response = jobSuggestionService.getSuggestionsForCandidate(candidateId);

        if (response.isComputing()) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get detailed match result for a specific job offer.
     * Uses cached result if available.
     *
     * @param jobOfferId The job offer ID
     * @param jwt        The authenticated user's JWT
     * @return The match result
     */
    @Operation(summary = "Obtenir le score de matching", description = "Calcule ou récupère le score de compatibilité entre le candidat et une offre.")
    @GetMapping("/match/{jobOfferId}")
    public ResponseEntity<JobMatchResponseDTO> getMatch(
            @Parameter(description = "ID de l'offre d'emploi") @PathVariable UUID jobOfferId,
            @AuthenticationPrincipal Jwt jwt) {

        String candidateId = jwt.getSubject();
        log.info("GET /api/ai/match/{} - candidateId={}", jobOfferId, candidateId);

        // Try to get cached result first, if not exists compute it
        JobMatchResponseDTO match = aiMatchingService.getCachedMatch(candidateId, jobOfferId)
                .orElseGet(() -> aiMatchingService.computeMatch(candidateId, jobOfferId, false));

        return ResponseEntity.ok(match);
    }

    /**
     * Force recompute the match for a job offer (bypass cache).
     *
     * @param jobOfferId The job offer ID
     * @param jwt        The authenticated user's JWT
     * @return The freshly computed match result
     */
    @Operation(summary = "Recalculer le matching", description = "Force le recalcul du score de compatibilité (ignore le cache).")
    @PostMapping("/match/{jobOfferId}/refresh")
    public ResponseEntity<JobMatchResponseDTO> refreshMatch(
            @Parameter(description = "ID de l'offre d'emploi") @PathVariable UUID jobOfferId,
            @AuthenticationPrincipal Jwt jwt) {

        String candidateId = jwt.getSubject();
        log.info("POST /api/ai/match/{}/refresh - candidateId={}", jobOfferId, candidateId);

        JobMatchResponseDTO match = aiMatchingService.computeMatch(candidateId, jobOfferId, true);
        return ResponseEntity.ok(match);
    }

    /**
     * Get AI-generated interview questions for a job offer.
     * If no match exists yet, computes it first (synchronously).
     *
     * @param jobOfferId The job offer ID
     * @param jwt        The authenticated user's JWT
     * @return Interview preparation questions
     */
    @Operation(summary = "Préparation d'entretien (par offre)", description = "Génère des questions d'entretien personnalisées pour une offre spécifique.")
    @GetMapping("/interview-prep/{jobOfferId}")
    public ResponseEntity<InterviewPrepDTO> getInterviewPrep(
            @Parameter(description = "ID de l'offre d'emploi") @PathVariable UUID jobOfferId,
            @AuthenticationPrincipal Jwt jwt) {

        String candidateId = jwt.getSubject();
        log.info("GET /api/ai/interview-prep/{} - candidateId={}", jobOfferId, candidateId);

        // Get or compute match
        JobMatchResponseDTO match = aiMatchingService.getCachedMatch(candidateId, jobOfferId)
                .orElseGet(() -> aiMatchingService.computeMatch(candidateId, jobOfferId, false));

        InterviewPrepDTO interviewPrep = InterviewPrepDTO.builder()
                .jobOfferId(match.getJobOfferId())
                .jobTitle(match.getJobTitle())
                .companyName(match.getCompanyName())
                .interviewQuestions(match.getInterviewQuestions())
                .matchScore(match.getMatchScore())
                .build();

        return ResponseEntity.ok(interviewPrep);
    }

    /**
     * Keyword-based offer suggestion.
     * Takes a list of competence keywords and returns ranked matching offers.
     *
     * @param request The suggest offers request with keywords
     * @param jwt     The authenticated user's JWT
     * @return List of offer suggestions with scores and justifications
     */
    @Operation(summary = "Suggestions par mots-clés", description = "Retourne les offres correspondant à une liste de compétences/mots-clés.")
    @PostMapping("/suggest-offers")
    public ResponseEntity<List<OfferSuggestionResponse>> suggestOffers(
            @Valid @RequestBody SuggestOffersRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String candidateId = jwt.getSubject();
        log.info("POST /api/ai/suggest-offers - candidateId={}, keywords={}", candidateId, request.getKeywords().size());

        List<OfferSuggestionResponse> suggestions = keywordSuggestionService.suggestOffers(candidateId, request.getKeywords());
        return ResponseEntity.ok(suggestions);
    }

    /**
     * AI-powered interview preparation with structured Q&A.
     * Generates technical and behavioral questions with answer outlines.
     *
     * @param request The interview prep request with offerId and optional forceRefresh
     * @param jwt     The authenticated user's JWT
     * @return Structured interview preparation response
     */
    @Operation(summary = "Préparation d'entretien structurée", description = "Génère des questions techniques et comportementales avec des pistes de réponses.")
    @PostMapping("/interview-prep")
    public ResponseEntity<InterviewPrepResponse> generateInterviewPrep(
            @Valid @RequestBody InterviewPrepRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String candidateId = jwt.getSubject();
        log.info("POST /api/ai/interview-prep - candidateId={}, offerId={}, forceRefresh={}",
                candidateId, request.getOfferId(), request.isForceRefresh());

        InterviewPrepResponse response = interviewPrepService.getInterviewPrep(
                candidateId, request.getOfferId(), request.isForceRefresh());
        return ResponseEntity.ok(response);
    }

    /**
     * Generate an AI-powered cover letter for a specific job offer.
     *
     * @param request The cover letter request with offerId, optional skills and tone
     * @param jwt     The authenticated user's JWT
     * @return Generated cover letter text
     */
    @Operation(summary = "Générer une lettre de motivation", description = "Génère une lettre de motivation personnalisée par IA pour une offre donnée.")
    @PostMapping("/generate-cover-letter")
    public ResponseEntity<CoverLetterResponse> generateCoverLetter(
            @Valid @RequestBody CoverLetterRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String candidateId = jwt.getSubject();
        log.info("POST /api/ai/generate-cover-letter - candidateId={}, offerId={}, tone={}",
                candidateId, request.getOfferId(), request.getTone());

        CoverLetterResponse response = coverLetterService.generateCoverLetter(
                candidateId, request.getOfferId(), request.getCandidateSkills(), request.getTone());
        return ResponseEntity.ok(response);
    }

    /**
     * Chat-based interview preparation.
     * Allows conversational interaction with an AI interview coach.
     *
     * @param request The chat request with offerId, message, and conversation history
     * @param jwt     The authenticated user's JWT
     * @return AI coach reply
     */
    @Operation(summary = "Chat d'entretien IA", description = "Interaction conversationnelle avec un coach IA pour préparer un entretien.")
    @PostMapping("/interview-chat")
    public ResponseEntity<InterviewChatResponse> interviewChat(
            @Valid @RequestBody InterviewChatRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String candidateId = jwt.getSubject();
        log.info("POST /api/ai/interview-chat - candidateId={}, offerId={}", candidateId, request.getOfferId());

        InterviewChatResponse response = interviewChatService.chat(
                candidateId, request.getOfferId(), request.getMessage(), request.getHistory());
        return ResponseEntity.ok(response);
    }
}
