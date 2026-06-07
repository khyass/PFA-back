package com.example.aiservice.service;

import com.example.aiservice.dto.*;
import com.example.aiservice.entity.JobOfferMatch;
import com.example.aiservice.exception.ResumeRequiredException;
import com.example.aiservice.repository.JobOfferMatchRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for computing and caching AI matches between resumes and job offers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AiMatchingService {

    private final JobOfferMatchRepository matchRepository;
    private final ResumeExtractionService resumeExtractionService;
    private final OllamaClient ollamaClient;
    private final JobOfferClient jobOfferClient;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.resume-max-chars}")
    private int resumeMaxChars;

    /**
     * Computes or retrieves a cached match for a candidate and job offer.
     *
     * @param candidateId  The candidate's user ID
     * @param jobOfferId   The job offer ID
     * @param forceRefresh If true, bypass cache and recompute
     * @return The match result DTO
     */
    public JobMatchResponseDTO computeMatch(String candidateId, UUID jobOfferId, boolean forceRefresh) {
        log.info("Computing match for candidate: {} and job: {}, forceRefresh: {}", candidateId, jobOfferId, forceRefresh);

        // Check for cached result
        if (!forceRefresh) {
            Optional<JobOfferMatch> cached = matchRepository.findByCandidateIdAndJobOfferId(candidateId, jobOfferId);
            if (cached.isPresent()) {
                log.debug("Returning cached match for candidate: {} and job: {}", candidateId, jobOfferId);
                return toResponseDTO(cached.get(), true);
            }
        }

        // Get resume text
        String resumeText = resumeExtractionService.getExtractedText(candidateId);
        if (resumeText == null || resumeText.isBlank()) {
            throw new ResumeRequiredException();
        }

        // Truncate resume text to control token usage
        if (resumeText.length() > resumeMaxChars) {
            resumeText = resumeText.substring(0, resumeMaxChars);
        }

        // Get job offer details
        JobOfferDTO jobOffer = jobOfferClient.getJobOffer(jobOfferId);

        // Call AI
        AiMatchResult aiResult = ollamaClient.computeMatch(
                resumeText,
                jobOffer.getTitle(),
                jobOffer.getCompanyName(),
                jobOffer.getNotes()
        );

        // Save or update match
        JobOfferMatch match = matchRepository.findByCandidateIdAndJobOfferId(candidateId, jobOfferId)
                .orElse(JobOfferMatch.builder()
                        .candidateId(candidateId)
                        .jobOfferId(jobOfferId)
                        .build());

        match.setJobTitle(jobOffer.getTitle());
        match.setCompanyName(jobOffer.getCompanyName());
        match.setMatchScore(aiResult.getMatchScore());
        match.setMissingSkills(toJsonString(aiResult.getMissingSkills()));
        match.setSuggestions(aiResult.getSuggestions());
        match.setInterviewQuestions(toJsonString(aiResult.getInterviewQuestions()));
        match.setComputedAt(LocalDateTime.now());

        JobOfferMatch saved = matchRepository.save(match);
        log.info("Saved match for candidate: {} and job: {}, score: {}", candidateId, jobOfferId, aiResult.getMatchScore());

        return toResponseDTO(saved, false);
    }

    /**
     * Gets a cached match if it exists.
     */
    @Transactional(readOnly = true)
    public Optional<JobMatchResponseDTO> getCachedMatch(String candidateId, UUID jobOfferId) {
        return matchRepository.findByCandidateIdAndJobOfferId(candidateId, jobOfferId)
                .map(match -> toResponseDTO(match, true));
    }

    /**
     * Gets all cached matches for a candidate, ordered by match score descending.
     */
    @Transactional(readOnly = true)
    public List<JobOfferMatch> getCachedMatchesForCandidate(String candidateId) {
        return matchRepository.findByCandidateIdOrderByMatchScoreDesc(candidateId);
    }

    /**
     * Counts how many matches a candidate has cached.
     */
    @Transactional(readOnly = true)
    public long countCachedMatches(String candidateId) {
        return matchRepository.countByCandidateId(candidateId);
    }

    private JobMatchResponseDTO toResponseDTO(JobOfferMatch match, boolean cached) {
        return JobMatchResponseDTO.builder()
                .jobOfferId(match.getJobOfferId())
                .jobTitle(match.getJobTitle())
                .companyName(match.getCompanyName())
                .matchScore(match.getMatchScore())
                .missingSkills(fromJsonStringList(match.getMissingSkills()))
                .suggestions(match.getSuggestions())
                .interviewQuestions(fromJsonStringList(match.getInterviewQuestions()))
                .computedAt(match.getComputedAt())
                .cached(cached)
                .build();
    }

    private String toJsonString(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize list to JSON: {}", e.getMessage());
            return "[]";
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> fromJsonStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, List.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize JSON list: {}", e.getMessage());
            return List.of();
        }
    }
}
