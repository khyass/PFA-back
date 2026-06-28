package com.example.aiservice.service;

import com.example.aiservice.dto.*;
import com.example.aiservice.entity.InterviewPrepEntity;
import com.example.aiservice.entity.ResumeAnalysis;
import com.example.aiservice.exception.AiServiceException;
import com.example.aiservice.repository.InterviewPrepRepository;
import com.example.aiservice.repository.ResumeAnalysisRepository;
import com.example.aiservice.util.JsonExtractor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for AI-powered interview preparation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewPrepService {

    private final WebClient ollamaWebClient;
    private final ObjectMapper objectMapper;
    private final JobOfferClient jobOfferClient;
    private final InterviewPrepRepository interviewPrepRepository;
    private final ResumeAnalysisRepository resumeAnalysisRepository;

    @Value("${app.ollama.model}")
    private String model;

    @Value("${app.ai.resume-max-chars}")
    private int resumeMaxChars;

    private static final String INTERVIEW_PREP_PROMPT = """
            You are an expert interview coach. Generate interview preparation questions and answer outlines for a candidate.

            JOB TITLE: %s
            COMPANY: %s
            JOB DESCRIPTION: %s

            %s

            Generate:
            - 5 technical interview questions specific to the job's required skills
            - 3 behavioral/soft-skill questions
            - For each question, provide a suggested answer outline (key points to cover, not a full scripted answer)

            Respond ONLY with a valid JSON object, no preamble, no markdown, no explanation:
            {"technicalQuestions": [{"question": "...", "answerOutline": "..."}], "behavioralQuestions": [{"question": "...", "answerOutline": "..."}]}
            """;

    /**
     * Gets or generates interview preparation for a candidate and job offer.
     */
    @Transactional
    public InterviewPrepResponse getInterviewPrep(String candidateId, UUID offerId, boolean forceRefresh) {
        log.info("Getting interview prep for candidate {} and offer {}, forceRefresh={}", candidateId, offerId, forceRefresh);

        // Check cache unless forceRefresh
        if (!forceRefresh) {
            Optional<InterviewPrepEntity> cached = interviewPrepRepository.findByCandidateIdAndOfferId(candidateId, offerId);
            if (cached.isPresent()) {
                log.info("Returning cached interview prep for candidate {} and offer {}", candidateId, offerId);
                return deserializePayload(cached.get());
            }
        }

        // Fetch job offer details
        JobOfferDTO jobOffer = jobOfferClient.getJobOffer(offerId);

        // Try to get candidate's resume skills for personalization
        String candidateContext = buildCandidateContext(candidateId);

        // Build prompt
        String prompt = String.format(INTERVIEW_PREP_PROMPT,
                jobOffer.getTitle(),
                jobOffer.getCompanyName(),
                jobOffer.getNotes() != null ? jobOffer.getNotes() : "N/A",
                candidateContext);

        log.debug("Interview prep prompt: {}", prompt);

        // Call Ollama
        String rawResponse = callOllama(prompt);
        log.debug("Interview prep raw response: {}", rawResponse);

        // Parse response
        InterviewPrepResponse result = parseInterviewPrepResponse(rawResponse, jobOffer);

        // Save to cache (upsert)
        saveToCache(candidateId, offerId, result);

        return result;
    }

    private String buildCandidateContext(String candidateId) {
        Optional<ResumeAnalysis> resume = resumeAnalysisRepository.findByCandidateId(candidateId);
        if (resume.isPresent()) {
            String text = resume.get().getExtractedText();
            String truncated = text.length() > resumeMaxChars ? text.substring(0, resumeMaxChars) : text;
            return "CANDIDATE'S RESUME SKILLS CONTEXT:\n" + truncated;
        }
        return "No candidate resume available - generate generic questions for the role.";
    }

    private InterviewPrepResponse parseInterviewPrepResponse(String rawResponse, JobOfferDTO jobOffer) {
        try {
            String jsonStr = JsonExtractor.extractJsonObject(rawResponse);
            if (jsonStr == null) {
                log.error("Could not extract JSON object from interview prep response");
                throw new AiServiceException("AI returned an unparseable response. Please try again.");
            }

            InterviewPrepResponse result = objectMapper.readValue(jsonStr, InterviewPrepResponse.class);
            result.setJobTitle(jobOffer.getTitle());
            result.setCompanyName(jobOffer.getCompanyName());

            if (result.getTechnicalQuestions() == null) {
                result.setTechnicalQuestions(java.util.List.of());
            }
            if (result.getBehavioralQuestions() == null) {
                result.setBehavioralQuestions(java.util.List.of());
            }

            return result;

        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse interview prep response: {}", e.getMessage());
            throw new AiServiceException("AI returned an unparseable response. Please try again.");
        }
    }

    private void saveToCache(String candidateId, UUID offerId, InterviewPrepResponse result) {
        try {
            // Delete existing if any
            interviewPrepRepository.deleteByCandidateIdAndOfferId(candidateId, offerId);
            interviewPrepRepository.flush();

            String payload = objectMapper.writeValueAsString(result);
            InterviewPrepEntity entity = InterviewPrepEntity.builder()
                    .candidateId(candidateId)
                    .offerId(offerId)
                    .payload(payload)
                    .build();
            interviewPrepRepository.save(entity);
        } catch (Exception e) {
            log.error("Failed to cache interview prep result: {}", e.getMessage());
        }
    }

    private InterviewPrepResponse deserializePayload(InterviewPrepEntity entity) {
        try {
            return objectMapper.readValue(entity.getPayload(), InterviewPrepResponse.class);
        } catch (Exception e) {
            log.error("Failed to deserialize cached interview prep: {}", e.getMessage());
            throw new AiServiceException("Failed to read cached result. Please refresh.");
        }
    }

    private String callOllama(String prompt) {
        try {
            OllamaRequest request = OllamaRequest.builder()
                    .model(model)
                    .prompt(prompt)
                    .stream(false)
                    .build();

            OllamaResponse response = ollamaWebClient.post()
                    .uri("/api/generate")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OllamaResponse.class)
                    .block();

            if (response == null || response.getResponse() == null) {
                throw new AiServiceException("AI service returned empty response");
            }

            return response.getResponse();

        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error calling Ollama for interview prep: {}", e.getMessage());
            throw new AiServiceException("AI service is temporarily unavailable. Please try again.", e);
        }
    }
}
