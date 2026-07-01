package com.example.aiservice.service;

import com.example.aiservice.dto.JobOfferDTO;
import com.example.aiservice.dto.OllamaResponse;
import com.example.aiservice.dto.OfferSuggestionResponse;
import com.example.aiservice.entity.OfferSuggestion;
import com.example.aiservice.repository.OfferSuggestionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeywordSuggestionServiceTest {

    @Mock
    private WebClient ollamaWebClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private JobOfferClient jobOfferClient;

    @Mock
    private OfferSuggestionRepository suggestionRepository;

    @InjectMocks
    private KeywordSuggestionService keywordSuggestionService;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(keywordSuggestionService, "model", "llama3.2");
    }

    @Test
    void computeKeywordsHash_sameKeywordsDifferentOrder_sameHash() {
        String hash1 = keywordSuggestionService.computeKeywordsHash(List.of("Java", "Spring", "Docker"));
        String hash2 = keywordSuggestionService.computeKeywordsHash(List.of("Docker", "java", "SPRING"));
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void computeKeywordsHash_differentKeywords_differentHash() {
        String hash1 = keywordSuggestionService.computeKeywordsHash(List.of("Java", "Spring"));
        String hash2 = keywordSuggestionService.computeKeywordsHash(List.of("Python", "Django"));
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void suggestOffers_returnsCachedResults_whenCacheExists() {
        String candidateId = "user-123";
        List<String> keywords = List.of("Java", "Spring");

        OfferSuggestion cached = OfferSuggestion.builder()
                .candidateId(candidateId)
                .offerId(UUID.randomUUID())
                .score(85)
                .justification("Great match")
                .build();

        when(suggestionRepository.findByCandidateIdAndKeywordsHashOrderByScoreDesc(eq(candidateId), anyString()))
                .thenReturn(List.of(cached));

        List<OfferSuggestionResponse> results = keywordSuggestionService.suggestOffers(candidateId, keywords);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getScore()).isEqualTo(85);
        verify(jobOfferClient, never()).getAllOpenJobOffers();
    }

    @Test
    void suggestOffers_returnsEmptyList_whenNoOpenOffers() {
        String candidateId = "user-456";
        List<String> keywords = List.of("Python");

        when(suggestionRepository.findByCandidateIdAndKeywordsHashOrderByScoreDesc(eq(candidateId), anyString()))
                .thenReturn(List.of());
        when(jobOfferClient.getAllOpenJobOffers()).thenReturn(List.of());

        List<OfferSuggestionResponse> results = keywordSuggestionService.suggestOffers(candidateId, keywords);

        assertThat(results).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void suggestOffers_callsOllama_whenNoCacheAndOffersExist() {
        String candidateId = "user-789";
        List<String> keywords = List.of("Java");
        UUID offerId = UUID.randomUUID();

        when(suggestionRepository.findByCandidateIdAndKeywordsHashOrderByScoreDesc(eq(candidateId), anyString()))
                .thenReturn(List.of());

        JobOfferDTO offer = JobOfferDTO.builder()
                .id(offerId)
                .title("Java Developer")
                .companyName("TechCo")
                .notes("Spring Boot experience")
                .build();
        when(jobOfferClient.getAllOpenJobOffers()).thenReturn(List.of(offer));

        String ollamaResponse = String.format(
                "[{\"offerId\": \"%s\", \"score\": 92, \"justification\": \"Perfect match for Java skills\"}]",
                offerId);

        OllamaResponse response = OllamaResponse.builder().response(ollamaResponse).done(true).build();

        when(ollamaWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestBodySpec);
        when(((WebClient.RequestHeadersSpec<?>) requestBodySpec).retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(OllamaResponse.class)).thenReturn(Mono.just(response));

        when(suggestionRepository.save(any(OfferSuggestion.class))).thenAnswer(i -> i.getArgument(0));

        List<OfferSuggestionResponse> results = keywordSuggestionService.suggestOffers(candidateId, keywords);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getScore()).isEqualTo(92);
        assertThat(results.get(0).getOfferId()).isEqualTo(offerId);
    }
}
