package com.example.aiservice.service;

import com.example.aiservice.dto.JobOfferDTO;
import com.example.aiservice.dto.OfferSuggestionResponse;
import com.example.aiservice.dto.OllamaRequest;
import com.example.aiservice.dto.OllamaResponse;
import com.example.aiservice.entity.OfferSuggestion;
import com.example.aiservice.exception.AiServiceException;
import com.example.aiservice.repository.OfferSuggestionRepository;
import com.example.aiservice.util.JsonExtractor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for keyword-based job offer suggestions using Ollama LLM.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KeywordSuggestionService {

    private final WebClient ollamaWebClient;
    private final ObjectMapper objectMapper;
    private final JobOfferClient jobOfferClient;
    private final OfferSuggestionRepository suggestionRepository;

    @Value("${app.ollama.model}")
    private String model;

    private static final String SUGGEST_PROMPT_TEMPLATE = """
            You are a job matching assistant. Given candidate keywords and job offers, determine which offers match.

            IMPORTANT RULES:
            - Perform CASE-INSENSITIVE matching: "python" matches "Python", "javascript" matches "JavaScript"
            - Search the ENTIRE text of each offer (title + company + description/notes)
            - If a keyword appears ANYWHERE in the offer text (even inside parentheses or as part of a list), it is a MATCH
            - Related/synonym terms also count (e.g., "JS" matches "JavaScript", "dev" matches "developer")
            - Score 70-100 if the keyword is explicitly mentioned in the offer
            - Score 40-69 if the keyword is only loosely related
            - ONLY include offers from the list above. Use the EXACT offer ID provided. Do NOT invent IDs.
            - Return ONLY real matching offers. Do NOT add placeholder or template entries.

            CANDIDATE KEYWORDS: %s

            JOB OFFERS:
            %s

            Return a JSON array with ALL matching offers. An offer matches if ANY keyword appears in its text.
            Each entry must use the real offer ID from the list above.
            Format example: [{"offerId": "actual-uuid-from-above", "score": 80, "justification": "keyword found in description"}]
            Respond with ONLY the JSON array on a single line. No markdown, no explanation, no preamble.
            If no offers match, return: []
            """;

    /**
     * Suggests job offers based on candidate keywords.
     * Returns cached results if the same keywords were used before.
     */
    @Transactional
    public List<OfferSuggestionResponse> suggestOffers(String candidateId, List<String> keywords) {
        log.info("Suggesting offers for candidate {} with {} keywords", candidateId, keywords.size());

        String keywordsHash = computeKeywordsHash(keywords);

        // Check cache
        List<OfferSuggestion> cached = suggestionRepository
                .findByCandidateIdAndKeywordsHashOrderByScoreDesc(candidateId, keywordsHash);

        if (!cached.isEmpty()) {
            log.info("Returning {} cached suggestions for candidate {}", cached.size(), candidateId);
            // Fetch offer details to enrich cached results with title/company
            List<JobOfferDTO> openOffers = jobOfferClient.getAllOpenJobOffers();
            Map<String, JobOfferDTO> offerMap = openOffers.stream()
                    .collect(Collectors.toMap(o -> o.getId().toString(), o -> o, (a, b) -> a));
            return cached.stream()
                    .map(entity -> toResponse(entity, offerMap))
                    .toList();
        }

        // Fetch all open job offers
        List<JobOfferDTO> openOffers = jobOfferClient.getAllOpenJobOffers();
        if (openOffers.isEmpty()) {
            log.info("No open job offers available");
            return List.of();
        }

        // Build the offers description for the prompt
        String offersDescription = openOffers.stream()
                .map(o -> String.format("- ID: %s | Title: %s | Company: %s | Description: %s",
                        o.getId(), o.getTitle(), o.getCompanyName(), o.getNotes() != null ? o.getNotes() : "N/A"))
                .collect(Collectors.joining("\n"));

        String keywordsStr = String.join(", ", keywords);
        String prompt = String.format(SUGGEST_PROMPT_TEMPLATE, keywordsStr, offersDescription);

        log.debug("Suggest-offers prompt: {}", prompt);

        // Call Ollama
        String rawResponse = callOllama(prompt);
        log.debug("Suggest-offers raw response: {}", rawResponse);

        // Parse response
        List<OfferSuggestionResponse> results = parseOfferSuggestions(rawResponse, openOffers);

        // Save to cache (clear any stale entries first)
        suggestionRepository.deleteByCandidateIdAndKeywordsHash(candidateId, keywordsHash);
        for (OfferSuggestionResponse result : results) {
            OfferSuggestion entity = OfferSuggestion.builder()
                    .candidateId(candidateId)
                    .keywordsHash(keywordsHash)
                    .offerId(result.getOfferId())
                    .score(result.getScore())
                    .justification(result.getJustification())
                    .build();
            suggestionRepository.save(entity);
        }

        log.info("Generated {} offer suggestions for candidate {}", results.size(), candidateId);
        return results;
    }

    private List<OfferSuggestionResponse> parseOfferSuggestions(String rawResponse, List<JobOfferDTO> openOffers) {
        try {
            String jsonArray = JsonExtractor.extractJsonArray(rawResponse);
            if (jsonArray == null) {
                log.error("Could not extract JSON array from Ollama response");
                return List.of();
            }

            List<Map<String, Object>> parsed = objectMapper.readValue(jsonArray, new TypeReference<>() {});

            // Build lookup map for offer details
            Map<String, JobOfferDTO> offerMap = openOffers.stream()
                    .collect(Collectors.toMap(o -> o.getId().toString(), o -> o, (a, b) -> a));

            return parsed.stream()
                    .filter(item -> item.containsKey("offerId") && item.containsKey("score"))
                    .filter(item -> {
                        String id = String.valueOf(item.get("offerId"));
                        return offerMap.containsKey(id); // Skip invalid/placeholder UUIDs
                    })
                    .map(item -> {
                        String offerId = String.valueOf(item.get("offerId"));
                        int score = item.get("score") instanceof Number n ? n.intValue() : 50;
                        score = Math.max(0, Math.min(100, score));
                        String justification = item.get("justification") != null ? String.valueOf(item.get("justification")) : "";

                        JobOfferDTO offer = offerMap.get(offerId);
                        return OfferSuggestionResponse.builder()
                                .offerId(UUID.fromString(offerId))
                                .offerTitle(offer != null ? offer.getTitle() : "Unknown")
                                .companyName(offer != null ? offer.getCompanyName() : "Unknown")
                                .score(score)
                                .justification(justification)
                                .build();
                    })
                    .sorted(Comparator.comparingInt(OfferSuggestionResponse::getScore).reversed())
                    // Deduplicate by offerId, keeping the highest score
                    .collect(Collectors.toMap(
                            r -> r.getOfferId(),
                            r -> r,
                            (a, b) -> a  // keep first (highest score due to sort)
                    ))
                    .values().stream()
                    .sorted(Comparator.comparingInt(OfferSuggestionResponse::getScore).reversed())
                    .toList();

        } catch (Exception e) {
            log.error("Failed to parse offer suggestions from Ollama response: {}", e.getMessage());
            return List.of();
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
            log.error("Error calling Ollama for suggestions: {}", e.getMessage());
            throw new AiServiceException("AI service is temporarily unavailable. Please try again.", e);
        }
    }

    String computeKeywordsHash(List<String> keywords) {
        List<String> sorted = keywords.stream()
                .map(String::toLowerCase)
                .map(String::trim)
                .sorted()
                .toList();

        String joined = String.join("|", sorted);

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(joined.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private OfferSuggestionResponse toResponse(OfferSuggestion entity, Map<String, JobOfferDTO> offerMap) {
        JobOfferDTO offer = offerMap.get(entity.getOfferId().toString());
        return OfferSuggestionResponse.builder()
                .offerId(entity.getOfferId())
                .offerTitle(offer != null ? offer.getTitle() : "Offre inconnue")
                .companyName(offer != null ? offer.getCompanyName() : "Entreprise inconnue")
                .score(entity.getScore())
                .justification(entity.getJustification())
                .build();
    }
}
