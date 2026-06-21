package com.example.aiservice.service;

import com.example.aiservice.dto.*;
import com.example.aiservice.entity.JobOfferMatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for generating ranked job suggestions for candidates.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobSuggestionService {

    private final AiMatchingService aiMatchingService;
    private final JobOfferClient jobOfferClient;
    private final ResumeExtractionService resumeExtractionService;

    @Value("${app.ai.min-match-score}")
    private int minMatchScore;

    @Value("${app.ai.max-suggestions}")
    private int maxSuggestions;

    // Track which candidates are currently being processed
    private final Set<String> processingCandidates = ConcurrentHashMap.newKeySet();

    /**
     * Gets ranked job suggestions for a candidate.
     * Returns cached results if available, or triggers async computation if first call.
     */
    @Transactional(readOnly = true)
    public SuggestionsResponseDTO getSuggestionsForCandidate(String candidateId) {
        log.info("Getting suggestions for candidate: {}", candidateId);

        // Check if resume exists
        if (!resumeExtractionService.hasResumeAnalysis(candidateId)) {
            return SuggestionsResponseDTO.builder()
                    .computing(false)
                    .message("Please upload your resume before requesting suggestions.")
                    .suggestions(List.of())
                    .build();
        }

        // Get all open job offers
        List<JobOfferDTO> openJobOffers = jobOfferClient.getAllOpenJobOffers();

        if (openJobOffers.isEmpty()) {
            return SuggestionsResponseDTO.builder()
                    .computing(false)
                    .message("No open job offers available at this time.")
                    .suggestions(List.of())
                    .build();
        }

        // Check how many cached matches we have
        long cachedCount = aiMatchingService.countCachedMatches(candidateId);

        // If no cached matches and not currently processing, trigger async computation
        if (cachedCount == 0) {
            if (processingCandidates.contains(candidateId)) {
                return SuggestionsResponseDTO.builder()
                        .computing(true)
                        .message("Your suggestions are being computed. Please check back in a moment.")
                        .suggestions(List.of())
                        .build();
            }

            // Trigger async computation
            computeSuggestionsAsync(candidateId, openJobOffers);

            return SuggestionsResponseDTO.builder()
                    .computing(true)
                    .message("Your suggestions are being computed. Please check back in a moment.")
                    .suggestions(List.of())
                    .build();
        }

        // Return cached results
        List<JobOfferMatch> cachedMatches = aiMatchingService.getCachedMatchesForCandidate(candidateId);

        List<JobSuggestionDTO> suggestions = cachedMatches.stream()
                .filter(match -> match.getMatchScore() >= minMatchScore)
                .limit(maxSuggestions)
                .map(this::toSuggestionDTO)
                .toList();

        return SuggestionsResponseDTO.builder()
                .computing(false)
                .suggestions(suggestions)
                .build();
    }

    /**
     * Asynchronously computes matches for all open job offers.
     */
    @Async("aiTaskExecutor")
    public CompletableFuture<Void> computeSuggestionsAsync(String candidateId, List<JobOfferDTO> jobOffers) {
        if (!processingCandidates.add(candidateId)) {
            log.debug("Candidate {} is already being processed", candidateId);
            return CompletableFuture.completedFuture(null);
        }

        try {
            log.info("Starting async computation of {} job matches for candidate: {}", jobOffers.size(), candidateId);

            for (JobOfferDTO jobOffer : jobOffers) {
                try {
                    aiMatchingService.computeMatch(candidateId, jobOffer.getId(), false);
                } catch (Exception e) {
                    log.warn("Failed to compute match for candidate: {} and job: {}: {}",
                            candidateId, jobOffer.getId(), e.getMessage());
                }
            }

            log.info("Completed async computation for candidate: {}", candidateId);
        } finally {
            processingCandidates.remove(candidateId);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Checks if a candidate's suggestions are currently being computed.
     */
    public boolean isComputingForCandidate(String candidateId) {
        return processingCandidates.contains(candidateId);
    }

    private JobSuggestionDTO toSuggestionDTO(JobOfferMatch match) {
        return JobSuggestionDTO.builder()
                .jobOfferId(match.getJobOfferId())
                .jobTitle(match.getJobTitle())
                .companyName(match.getCompanyName())
                .matchScore(match.getMatchScore())
                .cached(true)
                .build();
    }
}
