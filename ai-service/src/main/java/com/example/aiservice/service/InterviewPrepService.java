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
            Tu es un coach d'entretien professionnel expert. Génère des questions d'entretien pour un candidat postulant à ce poste.

            POSTE: %s
            ENTREPRISE: %s
            DESCRIPTION DU POSTE ET PROFIL RECHERCHÉ: %s

            %s

            INSTRUCTIONS:
            1. Les questions TECHNIQUES (5) doivent être directement liées au profil recherché et aux compétences requises dans la description du poste. Par exemple, si le poste demande Java/Spring Boot, pose des questions sur Java/Spring Boot. Si le poste demande React, pose des questions sur React.
            2. Les questions COMPORTEMENTALES (3) doivent évaluer les soft skills du candidat : travail d'équipe, gestion du stress, communication, résolution de conflits, adaptabilité.
            3. TOUTES les questions et réponses doivent être rédigées en FRANÇAIS.

            Génère exactement 5 questions techniques et 3 questions comportementales (8 au total).
            CRITIQUE: Réponds UNIQUEMENT avec un tableau JSON. Pas de markdown. Pas de préambule. Pas de texte supplémentaire.
            Chaque élément doit avoir: type ("technical" ou "behavioral"), question, answerOutline.
            Exemple de format:
            [{"type":"technical","question":"Expliquez le fonctionnement de l'injection de dépendances dans Spring Boot.","answerOutline":"L'injection de dépendances permet de..."},{"type":"behavioral","question":"Décrivez une situation où vous avez dû gérer un conflit au sein de votre équipe.","answerOutline":"Points clés: identifier le problème, écouter les parties..."}]
            Garde chaque answerOutline sous 80 mots. N'utilise pas d'objets imbriqués.
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
            // Try to extract a JSON array (flat format)
            String jsonStr = JsonExtractor.extractJsonArray(rawResponse);
            if (jsonStr == null) {
                // Fallback: try extracting JSON object (legacy format)
                jsonStr = JsonExtractor.extractJsonObject(rawResponse);
            }

            if (jsonStr == null) {
                log.error("Could not extract JSON from interview prep response. Raw (first 500 chars): {}",
                        rawResponse != null ? rawResponse.substring(0, Math.min(500, rawResponse.length())) : "null");
                // Last resort: regex-based extraction
                return buildResponseFromRegex(rawResponse, jobOffer);
            }

            // Sanitize
            jsonStr = jsonStr.replace("\r\n", " ").replace("\n", " ").replace("\r", " ");
            jsonStr = jsonStr.replaceAll("[\\x00-\\x1F&&[^\\x09]]", "");

            log.debug("Sanitized interview prep JSON (first 300 chars): {}", jsonStr.substring(0, Math.min(300, jsonStr.length())));

            // Try parsing as flat array first
            if (jsonStr.trim().startsWith("[")) {
                return parseAsArray(jsonStr, jobOffer);
            }

            // Try parsing as nested object
            jsonStr = repairJsonBrackets(jsonStr);
            InterviewPrepResponse result = objectMapper.readValue(jsonStr, InterviewPrepResponse.class);
            result.setJobTitle(jobOffer.getTitle());
            result.setCompanyName(jobOffer.getCompanyName());
            if (result.getTechnicalQuestions() == null) result.setTechnicalQuestions(java.util.List.of());
            if (result.getBehavioralQuestions() == null) result.setBehavioralQuestions(java.util.List.of());
            return result;

        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.warn("JSON parsing failed ({}), falling back to regex extraction", e.getMessage());
            return buildResponseFromRegex(rawResponse, jobOffer);
        }
    }

    @SuppressWarnings("unchecked")
    private InterviewPrepResponse parseAsArray(String jsonArrayStr, JobOfferDTO jobOffer) throws Exception {
        java.util.List<java.util.Map<String, Object>> items = objectMapper.readValue(jsonArrayStr,
                objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, java.util.Map.class));

        java.util.List<InterviewPrepResponse.QuestionAnswer> technical = new java.util.ArrayList<>();
        java.util.List<InterviewPrepResponse.QuestionAnswer> behavioral = new java.util.ArrayList<>();

        for (java.util.Map<String, Object> map : items) {
            String type = String.valueOf(map.getOrDefault("type", "technical")).toLowerCase();
            String question = String.valueOf(map.getOrDefault("question", ""));
            Object outlineVal = map.containsKey("answerOutline") ? map.get("answerOutline") : map.getOrDefault("answer_outline", "");
            String outline = String.valueOf(outlineVal);
            if (question.isBlank()) continue;
            var qa = InterviewPrepResponse.QuestionAnswer.builder()
                    .question(question)
                    .answerOutline(outline)
                    .build();
            if (type.contains("behavioral")) {
                behavioral.add(qa);
            } else {
                technical.add(qa);
            }
        }

        return InterviewPrepResponse.builder()
                .jobTitle(jobOffer.getTitle())
                .companyName(jobOffer.getCompanyName())
                .technicalQuestions(technical)
                .behavioralQuestions(behavioral)
                .build();
    }

    /**
     * Fallback: extracts question/answerOutline pairs using regex when JSON is too broken.
     */
    private InterviewPrepResponse buildResponseFromRegex(String rawResponse, JobOfferDTO jobOffer) {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new AiServiceException("AI returned an unparseable response. Please try again.");
        }

        java.util.List<InterviewPrepResponse.QuestionAnswer> technical = new java.util.ArrayList<>();
        java.util.List<InterviewPrepResponse.QuestionAnswer> behavioral = new java.util.ArrayList<>();

        // Extract all "question":"..." and "answerOutline":"..." pairs
        java.util.regex.Pattern questionPattern = java.util.regex.Pattern.compile(
                "\"question\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
        java.util.regex.Pattern outlinePattern = java.util.regex.Pattern.compile(
                "\"answerOutline\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
        java.util.regex.Pattern typePattern = java.util.regex.Pattern.compile(
                "\"type\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

        java.util.regex.Matcher qMatcher = questionPattern.matcher(rawResponse);
        java.util.regex.Matcher oMatcher = outlinePattern.matcher(rawResponse);

        java.util.List<String> questions = new java.util.ArrayList<>();
        java.util.List<String> outlines = new java.util.ArrayList<>();
        while (qMatcher.find()) questions.add(qMatcher.group(1));
        while (oMatcher.find()) outlines.add(oMatcher.group(1));

        // Determine if "behavioral" appears before each question to classify
        boolean seenBehavioral = false;
        int behavioralStart = rawResponse.toLowerCase().indexOf("behavioral");

        for (int i = 0; i < questions.size(); i++) {
            String q = questions.get(i);
            String a = i < outlines.size() ? outlines.get(i) : "";
            var qa = InterviewPrepResponse.QuestionAnswer.builder()
                    .question(q).answerOutline(a).build();

            // Check if this question's position is after "behavioral" keyword in raw text
            int qPos = rawResponse.indexOf(q);
            if (behavioralStart > 0 && qPos > behavioralStart) {
                behavioral.add(qa);
            } else {
                technical.add(qa);
            }
        }

        log.info("Regex fallback extracted {} technical and {} behavioral questions", technical.size(), behavioral.size());

        if (technical.isEmpty() && behavioral.isEmpty()) {
            throw new AiServiceException("AI returned an unparseable response. Please try again.");
        }

        return InterviewPrepResponse.builder()
                .jobTitle(jobOffer.getTitle())
                .companyName(jobOffer.getCompanyName())
                .technicalQuestions(technical)
                .behavioralQuestions(behavioral)
                .build();
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

    /**
     * Repairs common JSON bracket issues from LLM output.
     * Appends missing ] and } at the end to balance the structure.
     */
    private String repairJsonBrackets(String json) {
        int openBraces = 0, closeBraces = 0, openBrackets = 0, closeBrackets = 0;
        boolean inString = false;
        char prev = 0;
        for (char c : json.toCharArray()) {
            if (c == '"' && prev != '\\') {
                inString = !inString;
            } else if (!inString) {
                switch (c) {
                    case '{' -> openBraces++;
                    case '}' -> closeBraces++;
                    case '[' -> openBrackets++;
                    case ']' -> closeBrackets++;
                }
            }
            prev = c;
        }

        StringBuilder result = new StringBuilder(json);
        int missingBrackets = openBrackets - closeBrackets;
        int missingBraces = openBraces - closeBraces;

        if (missingBrackets > 0 || missingBraces > 0) {
            // Remove trailing comma if present
            int len = result.length();
            while (len > 0 && Character.isWhitespace(result.charAt(len - 1))) len--;
            if (len > 0 && result.charAt(len - 1) == ',') {
                result.setLength(len - 1);
            }
            // Append missing closers in correct order: ] then }
            for (int i = 0; i < missingBrackets; i++) result.append(']');
            for (int i = 0; i < missingBraces; i++) result.append('}');
            log.debug("Repaired JSON: appended {} missing ']' and {} missing '}}'", missingBrackets, missingBraces);
        }

        return result.toString();
    }
}
