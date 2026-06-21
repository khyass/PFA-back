package com.example.aiservice.controller;

import com.example.aiservice.dto.*;
import com.example.aiservice.service.AiMatchingService;
import com.example.aiservice.service.JobSuggestionService;
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
public class AiController {

    private final AiMatchingService aiMatchingService;
    private final JobSuggestionService jobSuggestionService;

    /**
     * Get ranked job offer suggestions for the authenticated candidate.
     * Returns 202 Accepted if suggestions are being computed asynchronously.
     *
     * @param jwt The authenticated user's JWT
     * @return List of job suggestions ordered by match score
     */
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
    @GetMapping("/match/{jobOfferId}")
    public ResponseEntity<JobMatchResponseDTO> getMatch(
            @PathVariable UUID jobOfferId,
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
    @PostMapping("/match/{jobOfferId}/refresh")
    public ResponseEntity<JobMatchResponseDTO> refreshMatch(
            @PathVariable UUID jobOfferId,
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
    @GetMapping("/interview-prep/{jobOfferId}")
    public ResponseEntity<InterviewPrepDTO> getInterviewPrep(
            @PathVariable UUID jobOfferId,
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
}
