package com.example.aiservice.service;

import com.example.aiservice.dto.*;
import com.example.aiservice.entity.ResumeAnalysis;
import com.example.aiservice.exception.AiServiceException;
import com.example.aiservice.repository.ResumeAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for AI-powered interview chatbot.
 * Maintains conversational context for interview preparation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewChatService {

    private final WebClient ollamaWebClient;
    private final JobOfferClient jobOfferClient;
    private final ResumeAnalysisRepository resumeAnalysisRepository;

    @Value("${app.ollama.model}")
    private String model;

    @Value("${app.ai.resume-max-chars}")
    private int resumeMaxChars;

    private static final String SYSTEM_PROMPT = """
            Tu es un coach d'entretien professionnel expert et bienveillant. Tu aides un candidat à se préparer pour un entretien d'embauche.

            POSTE: %s
            ENTREPRISE: %s
            DESCRIPTION DU POSTE: %s

            %s

            RÈGLES:
            - Réponds toujours en FRANÇAIS.
            - Sois encourageant mais honnête dans tes retours.
            - Pose des questions techniques et comportementales pertinentes pour le poste.
            - Quand le candidat répond à une question, donne-lui un feedback constructif avec des points forts et des axes d'amélioration.
            - Propose ensuite une nouvelle question ou demande s'il veut approfondir un sujet.
            - Si le candidat demande de l'aide, donne des pistes de réponse sans donner la réponse complète.
            - Garde un ton conversationnel et professionnel.
            - Ne fais pas de listes trop longues, reste concis (max 150 mots par réponse).
            """;

    /**
     * Process a chat message for interview preparation.
     */
    public InterviewChatResponse chat(String candidateId, UUID offerId, String userMessage, List<InterviewChatRequest.ChatMessage> history) {
        log.info("Interview chat for candidate {} and offer {}", candidateId, offerId);

        // Fetch job offer details
        JobOfferDTO jobOffer = jobOfferClient.getJobOffer(offerId);

        // Build candidate context
        String candidateContext = buildCandidateContext(candidateId);

        // Build the full prompt with conversation history
        String fullPrompt = buildConversationPrompt(jobOffer, candidateContext, history, userMessage);

        // Call Ollama
        String reply = callOllama(fullPrompt);

        return InterviewChatResponse.builder()
                .reply(reply)
                .jobTitle(jobOffer.getTitle())
                .companyName(jobOffer.getCompanyName())
                .build();
    }

    private String buildCandidateContext(String candidateId) {
        Optional<ResumeAnalysis> resume = resumeAnalysisRepository.findByCandidateId(candidateId);
        if (resume.isPresent()) {
            String text = resume.get().getExtractedText();
            String truncated = text.length() > resumeMaxChars ? text.substring(0, resumeMaxChars) : text;
            return "COMPÉTENCES DU CANDIDAT (extraites du CV):\n" + truncated;
        }
        return "Pas de CV disponible - adapte tes questions au poste.";
    }

    private String buildConversationPrompt(JobOfferDTO jobOffer, String candidateContext,
                                           List<InterviewChatRequest.ChatMessage> history, String userMessage) {
        StringBuilder sb = new StringBuilder();

        // System prompt with job context
        sb.append(String.format(SYSTEM_PROMPT,
                jobOffer.getTitle(),
                jobOffer.getCompanyName(),
                jobOffer.getNotes() != null ? jobOffer.getNotes() : "N/A",
                candidateContext));

        sb.append("\n\n--- CONVERSATION ---\n");

        // Append conversation history
        if (history != null && !history.isEmpty()) {
            for (InterviewChatRequest.ChatMessage msg : history) {
                if ("user".equals(msg.getRole())) {
                    sb.append("Candidat: ").append(msg.getContent()).append("\n");
                } else {
                    sb.append("Coach: ").append(msg.getContent()).append("\n");
                }
            }
        }

        // Append current message
        sb.append("Candidat: ").append(userMessage).append("\n");
        sb.append("Coach: ");

        return sb.toString();
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

            return response.getResponse().trim();

        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error calling Ollama for interview chat: {}", e.getMessage());
            throw new AiServiceException("AI service is temporarily unavailable. Please try again.", e);
        }
    }
}
