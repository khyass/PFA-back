package com.example.candidateservice.integration;

import com.example.candidateservice.dto.CandidatureRequestDTO;
import com.example.candidateservice.dto.JobOfferDTO;
import com.example.candidateservice.entity.JobOfferStatus;
import com.example.candidateservice.service.JobOfferClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Candidature operations using Testcontainers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CandidatureIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("candidates_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JobOfferClient jobOfferClient;

    private static String createdCandidatureId;
    private static final UUID JOB_OFFER_ID = UUID.randomUUID();
    private static final String CANDIDATE_ID = "candidate-123";

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void setupMocks() {
        // Mock job offer service response
        JobOfferDTO mockJobOffer = JobOfferDTO.builder()
                .id(JOB_OFFER_ID)
                .title("Senior Developer")
                .status(JobOfferStatus.OPEN)
                .companyName("Tech Corp")
                .build();

        when(jobOfferClient.getJobOffer(any(UUID.class))).thenReturn(mockJobOffer);
        when(jobOfferClient.isAcceptingApplications(any(JobOfferDTO.class))).thenReturn(true);
    }

    /**
     * Helper method to create a JWT mock for candidate users.
     */
    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor candidateJwt(String userId) {
        return jwt()
                .jwt(builder -> builder
                        .subject(userId)
                        .claim("realm_access", java.util.Map.of("roles", java.util.List.of("CANDIDATE")))
                );
    }

    @Test
    @Order(1)
    @DisplayName("Should apply to a job offer")
    void shouldApplyToJobOffer() throws Exception {
        CandidatureRequestDTO request = CandidatureRequestDTO.builder()
                .jobOfferId(JOB_OFFER_ID)
                .coverLetter("I am excited to apply for this position...")
                .build();

        MvcResult result = mockMvc.perform(post("/api/candidatures")
                        .with(candidateJwt(CANDIDATE_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.jobOfferTitle").value("Senior Developer"))
                .andExpect(jsonPath("$.companyName").value("Tech Corp"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.appliedDate").isNotEmpty())
                .andExpect(jsonPath("$.coverLetter").value("I am excited to apply for this position..."))
                .andReturn();

        createdCandidatureId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }

    @Test
    @Order(2)
    @DisplayName("Should verify timeline has initial entry after application")
    void shouldHaveInitialTimelineEntry() throws Exception {
        mockMvc.perform(get("/api/candidatures/{id}/timeline", createdCandidatureId)
                        .with(candidateJwt(CANDIDATE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].oldStatus").isEmpty())
                .andExpect(jsonPath("$[0].newStatus").value("PENDING"))
                .andExpect(jsonPath("$[0].note").value("Application submitted"))
                .andExpect(jsonPath("$[0].changedAt").isNotEmpty());
    }

    @Test
    @Order(3)
    @DisplayName("Should return 409 for duplicate application")
    void shouldReturn409ForDuplicateApplication() throws Exception {
        CandidatureRequestDTO request = CandidatureRequestDTO.builder()
                .jobOfferId(JOB_OFFER_ID)
                .coverLetter("Trying to apply again...")
                .build();

        mockMvc.perform(post("/api/candidatures")
                        .with(candidateJwt(CANDIDATE_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("You have already applied to this job offer"));
    }

    @Test
    @Order(4)
    @DisplayName("Should get candidature by ID")
    void shouldGetCandidatureById() throws Exception {
        mockMvc.perform(get("/api/candidatures/{id}", createdCandidatureId)
                        .with(candidateJwt(CANDIDATE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdCandidatureId))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @Order(5)
    @DisplayName("Should get all candidatures with pagination")
    void shouldGetAllCandidatures() throws Exception {
        mockMvc.perform(get("/api/candidatures")
                        .with(candidateJwt(CANDIDATE_ID))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @Order(6)
    @DisplayName("Should update candidature cover letter")
    void shouldUpdateCoverLetter() throws Exception {
        String updateJson = "{\"coverLetter\": \"Updated cover letter content\"}";

        mockMvc.perform(put("/api/candidatures/{id}", createdCandidatureId)
                        .with(candidateJwt(CANDIDATE_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverLetter").value("Updated cover letter content"));
    }

    @Test
    @Order(7)
    @DisplayName("Should return 403 when different candidate tries to access")
    void shouldReturn403ForDifferentCandidate() throws Exception {
        mockMvc.perform(get("/api/candidatures/{id}", createdCandidatureId)
                        .with(candidateJwt("different-candidate")))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(8)
    @DisplayName("Should return 401 for unauthenticated request")
    void shouldReturn401ForUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/candidatures"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(9)
    @DisplayName("Should return 403 for non-candidate user")
    void shouldReturn403ForNonCandidateUser() throws Exception {
        mockMvc.perform(get("/api/candidatures")
                        .with(jwt().jwt(builder -> builder
                                .subject("enterprise-user")
                                .claim("realm_access", java.util.Map.of("roles", java.util.List.of("ENTREPRISE"))))))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(10)
    @DisplayName("Should withdraw a pending candidature")
    void shouldWithdrawPendingCandidature() throws Exception {
        // First create a new candidature to withdraw
        UUID newJobOfferId = UUID.randomUUID();
        JobOfferDTO mockJobOffer = JobOfferDTO.builder()
                .id(newJobOfferId)
                .title("Another Position")
                .status(JobOfferStatus.OPEN)
                .companyName("Another Corp")
                .build();
        when(jobOfferClient.getJobOffer(newJobOfferId)).thenReturn(mockJobOffer);

        CandidatureRequestDTO request = CandidatureRequestDTO.builder()
                .jobOfferId(newJobOfferId)
                .coverLetter("Application to withdraw")
                .build();

        MvcResult result = mockMvc.perform(post("/api/candidatures")
                        .with(candidateJwt(CANDIDATE_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String newCandidatureId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();

        // Withdraw it
        mockMvc.perform(delete("/api/candidatures/{id}", newCandidatureId)
                        .with(candidateJwt(CANDIDATE_ID)))
                .andExpect(status().isNoContent());

        // Verify it's deleted
        mockMvc.perform(get("/api/candidatures/{id}", newCandidatureId)
                        .with(candidateJwt(CANDIDATE_ID)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(11)
    @DisplayName("Should return 400 when applying to closed job offer")
    void shouldReturn400ForClosedJobOffer() throws Exception {
        UUID closedJobOfferId = UUID.randomUUID();
        JobOfferDTO closedJobOffer = JobOfferDTO.builder()
                .id(closedJobOfferId)
                .title("Closed Position")
                .status(JobOfferStatus.CLOSED)
                .companyName("Closed Corp")
                .build();
        when(jobOfferClient.getJobOffer(closedJobOfferId)).thenReturn(closedJobOffer);
        when(jobOfferClient.isAcceptingApplications(closedJobOffer)).thenReturn(false);

        CandidatureRequestDTO request = CandidatureRequestDTO.builder()
                .jobOfferId(closedJobOfferId)
                .coverLetter("Trying to apply to closed job")
                .build();

        mockMvc.perform(post("/api/candidatures")
                        .with(candidateJwt(CANDIDATE_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("This job offer is not accepting applications"));
    }

    @Test
    @Order(12)
    @DisplayName("Should return 400 for invalid request body")
    void shouldReturn400ForInvalidRequest() throws Exception {
        String invalidRequest = "{\"coverLetter\": \"Missing jobOfferId\"}";

        mockMvc.perform(post("/api/candidatures")
                        .with(candidateJwt(CANDIDATE_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.jobOfferId").exists());
    }
}
