package com.example.aiservice.service;

import com.example.aiservice.dto.AiMatchResult;
import com.example.aiservice.dto.OllamaRequest;
import com.example.aiservice.dto.OllamaResponse;
import com.example.aiservice.exception.AiServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Client service for calling Ollama LLM API (free, local).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OllamaClient {

    private final WebClient ollamaWebClient;
    private final ObjectMapper objectMapper;

    @Value("${app.ollama.model}")
    private String model;

    private static final String PROMPT_TEMPLATE = """
            You are a professional recruiter assistant. Analyze the match between this resume and job description.
            
            RESUME:
            %s
            
            JOB DESCRIPTION:
            %s at %s
            %s
            
            Respond ONLY with a valid JSON object, no explanation, no markdown:
            {
              "matchScore": <integer 0 to 100>,
              "missingSkills": ["skill1", "skill2"],
              "suggestions": "<2-3 sentences on how the candidate can improve their application>",
              "interviewQuestions": ["question1", "question2", "question3", "question4", "question5"]
            }
            """;

    /**
     * Calls Ollama to compute the match between resume and job offer.
     *
     * @param resumeText  The candidate's resume text (truncated)
     * @param jobTitle    The job offer title
     * @param companyName The company name
     * @param jobNotes    Additional job description/notes
     * @return The parsed AI match result
     */
    public AiMatchResult computeMatch(String resumeText, String jobTitle, String companyName, String jobNotes) {
        String prompt = String.format(PROMPT_TEMPLATE,
                resumeText,
                jobTitle,
                companyName,
                jobNotes != null ? jobNotes : "");

        log.debug("Calling Ollama with model: {}", model);

        // Try up to 2 times
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                String response = callOllama(prompt);
                return parseResponse(response);
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse Ollama response on attempt {}: {}", attempt, e.getMessage());
                if (attempt == 2) {
                    throw new AiServiceException();
                }
            }
        }

        throw new AiServiceException();
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
                log.error("Ollama returned null response");
                throw new AiServiceException();
            }

            log.debug("Ollama response received, length: {}", response.getResponse().length());
            return response.getResponse();

        } catch (WebClientResponseException e) {
            log.error("Ollama API error: {} - {}", e.getStatusCode(), e.getMessage());
            throw new AiServiceException("AI service is temporarily unavailable. Please try again.", e);
        } catch (Exception e) {
            if (e instanceof AiServiceException) {
                throw e;
            }
            log.error("Error calling Ollama: {}", e.getMessage(), e);
            throw new AiServiceException("AI service is temporarily unavailable. Please try again.", e);
        }
    }

    private AiMatchResult parseResponse(String response) throws JsonProcessingException {
        // Strip markdown fences if present
        String cleanedResponse = response
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();

        // Find the JSON object in the response
        int startIndex = cleanedResponse.indexOf('{');
        int endIndex = cleanedResponse.lastIndexOf('}');

        if (startIndex == -1 || endIndex == -1 || startIndex >= endIndex) {
            log.error("Could not find JSON object in response: {}", cleanedResponse.substring(0, Math.min(100, cleanedResponse.length())));
            throw new JsonProcessingException("No JSON object found") {};
        }

        String jsonStr = cleanedResponse.substring(startIndex, endIndex + 1);
        log.debug("Parsing JSON: {}", jsonStr.substring(0, Math.min(200, jsonStr.length())));

        AiMatchResult result = objectMapper.readValue(jsonStr, AiMatchResult.class);

        // Validate result
        if (result.getMatchScore() == null || result.getMatchScore() < 0 || result.getMatchScore() > 100) {
            log.warn("Invalid match score: {}, defaulting to 50", result.getMatchScore());
            result.setMatchScore(50);
        }

        if (result.getMissingSkills() == null) {
            result.setMissingSkills(java.util.List.of());
        }

        if (result.getInterviewQuestions() == null) {
            result.setInterviewQuestions(java.util.List.of());
        }

        return result;
    }
}
