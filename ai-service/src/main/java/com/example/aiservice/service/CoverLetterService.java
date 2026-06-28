package com.example.aiservice.service;

import com.example.aiservice.dto.*;
import com.example.aiservice.entity.ResumeAnalysis;
import com.example.aiservice.exception.AiServiceException;
import com.example.aiservice.repository.ResumeAnalysisRepository;
import com.example.aiservice.util.JsonExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for AI-powered cover letter generation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CoverLetterService {

    private final WebClient ollamaWebClient;
    private final JobOfferClient jobOfferClient;
    private final ResumeAnalysisRepository resumeAnalysisRepository;

    @Value("${app.ollama.model}")
    private String model;

    @Value("${app.ai.resume-max-chars}")
    private int resumeMaxChars;

    private static final String COVER_LETTER_PROMPT = """
            You are an expert career coach helping a candidate write a compelling cover letter.

            JOB TITLE: %s
            COMPANY: %s
            JOB DESCRIPTION: %s

            CANDIDATE CONTEXT:
            %s

            TONE: %s

            Write a professional cover letter (150-250 words) that:
            - Opens with a strong hook mentioning the specific role and company
            - Highlights 2-3 relevant skills/experiences that match the job requirements
            - Shows enthusiasm for the company and role
            - Ends with a confident call to action
            - Uses the specified tone throughout

            Write the cover letter directly in French. Do NOT include any JSON, markdown, or formatting.
            Do NOT include subject lines, headers, or "Objet:" lines.
            Start directly with "Madame, Monsieur," or a similar greeting.
            Return ONLY the cover letter text, nothing else.
            """;

    /**
     * Generates a personalized cover letter for a candidate applying to a specific job offer.
     */
    public CoverLetterResponse generateCoverLetter(String candidateId, UUID offerId, String candidateSkills, String tone) {
        log.info("Generating cover letter for candidate {} applying to offer {}", candidateId, offerId);

        // Fetch job offer details
        JobOfferDTO jobOffer = jobOfferClient.getJobOffer(offerId);

        // Build candidate context
        String candidateContext = buildCandidateContext(candidateId, candidateSkills);

        // Determine tone
        String toneInstruction = switch (tone != null ? tone.toLowerCase() : "formal") {
            case "casual" -> "Conversational and friendly, while remaining professional";
            case "enthusiastic" -> "Highly energetic and passionate, showing strong excitement";
            default -> "Professional and formal, showing confidence and competence";
        };

        // Build prompt
        String prompt = String.format(COVER_LETTER_PROMPT,
                jobOffer.getTitle(),
                jobOffer.getCompanyName(),
                jobOffer.getNotes() != null ? jobOffer.getNotes() : "N/A",
                candidateContext,
                toneInstruction);

        log.debug("Cover letter prompt: {}", prompt);

        // Call Ollama
        String rawResponse = callOllama(prompt);
        log.debug("Cover letter raw response: {}", rawResponse);

        // Clean the response (strip any accidental markdown/JSON)
        String cleanedLetter = JsonExtractor.stripMarkdownFences(rawResponse).trim();

        return CoverLetterResponse.builder()
                .coverLetter(cleanedLetter)
                .jobTitle(jobOffer.getTitle())
                .companyName(jobOffer.getCompanyName())
                .tone(tone != null ? tone : "formal")
                .build();
    }

    private String buildCandidateContext(String candidateId, String candidateSkills) {
        StringBuilder context = new StringBuilder();

        // Try to get resume text for richer context
        Optional<ResumeAnalysis> resume = resumeAnalysisRepository.findByCandidateId(candidateId);
        if (resume.isPresent()) {
            String text = resume.get().getExtractedText();
            String truncated = text.length() > resumeMaxChars ? text.substring(0, resumeMaxChars) : text;
            context.append("Resume summary: ").append(truncated).append("\n");
        }

        // Add explicit skills if provided
        if (candidateSkills != null && !candidateSkills.isBlank()) {
            context.append("Key skills to highlight: ").append(candidateSkills);
        }

        if (context.isEmpty()) {
            context.append("No specific candidate information available. Write a generic but compelling letter.");
        }

        return context.toString();
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
            log.error("Error calling Ollama for cover letter: {}", e.getMessage());
            throw new AiServiceException("AI service is temporarily unavailable. Please try again.", e);
        }
    }
}
