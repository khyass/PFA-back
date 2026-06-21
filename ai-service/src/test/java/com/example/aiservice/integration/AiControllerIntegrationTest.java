package com.example.aiservice.integration;

import com.example.aiservice.entity.ResumeAnalysis;
import com.example.aiservice.repository.ResumeAnalysisRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AiControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("aiservice")
            .withUsername("test")
            .withPassword("test");

    static WireMockServer wireMock;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ResumeAnalysisRepository resumeAnalysisRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String CANDIDATE_ID = "test-candidate-123";
    private static final UUID JOB_OFFER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeAll
    static void setupWireMock() {
        wireMock = new WireMockServer(0);
        wireMock.start();
        WireMock.configureFor(wireMock.port());
    }

    @AfterAll
    static void teardownWireMock() {
        if (wireMock != null) {
            wireMock.stop();
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("wiremock.server.port", () -> wireMock.port());
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> "http://localhost:" + wireMock.port() + "/realms/job-tracker/protocol/openid-connect/certs");
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> "http://localhost:" + wireMock.port() + "/realms/job-tracker");
        registry.add("app.ollama.base-url", () -> "http://localhost:" + wireMock.port());
        registry.add("app.job-offer-service.url", () -> "http://localhost:" + wireMock.port());
    }

    @BeforeEach
    void setupMocks() {
        wireMock.resetAll();
        setupJwkMock();
    }

    private void setupJwkMock() {
        // Mock JWK endpoint for JWT validation
        wireMock.stubFor(get(urlPathEqualTo("/realms/job-tracker/protocol/openid-connect/certs"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "keys": [
                                    {
                                      "kty": "RSA",
                                      "e": "AQAB",
                                      "use": "sig",
                                      "kid": "test-key-id",
                                      "alg": "RS256",
                                      "n": "sZ5zZi8eFzI_IbWDBXZDCefDgj0WFz3xZZ9pC2f5j_R_d8SQkxxWVUvI8rxg9LKqJYdLJl8lgPKZSYwjzQWj3Py8n_0T5mcIJPJGQtGYOSBWQN3OGdqhzTn4B5z1T5nIKVEXZBz9pEZXz0Fs6rPmWCx8D3gECqmDZQy8vJOaVDwH9V14Nt_vCFhNPKe0mcENaeSLG9h0QHRWgv3zZG8Z3FKn-Xux9PQXJ2n3dJb0_xKm4_VvGZ5xClZM9pZPYBPzNqkE3o_sEwTx3bEf1TJD_PdQKNR7-HCXPUdBKBx1k1zB5m5GqHqWJK3R7pVQCZEIf7OC9L2dsnX0OPhPHHnHCUdQ"
                                    }
                                  ]
                                }
                                """)));
    }

    private String generateTestJwt(String userId, String role) {
        // Create a test JWT token (in real tests, use a proper JWT library)
        // This is a simplified mock for testing purposes
        long now = Instant.now().getEpochSecond();
        return "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6InRlc3Qta2V5LWlkIn0." +
                java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                        String.format("""
                                {"iss":"http://localhost:%d/realms/job-tracker","sub":"%s","iat":%d,"exp":%d,
                                "realm_access":{"roles":["%s"]},"scope":"openid profile email"}
                                """, wireMock.port(), userId, now, now + 3600, role).getBytes()
                ).replace("+", "-").replace("/", "_") +
                ".test-signature";
    }

    @Test
    @Order(1)
    void shouldReturnMessageWhenNoResume() throws Exception {
        // Given: no resume uploaded
        
        // When/Then: should return message about uploading resume
        mockMvc.perform(get("/api/ai/suggestions")
                        .header("Authorization", "Bearer " + generateTestJwt(CANDIDATE_ID, "CANDIDATE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.computing").value(false))
                .andExpect(jsonPath("$.message").value(containsString("upload your resume")));
    }

    @Test
    @Order(2)
    void internalEndpointShouldAcceptResumeAnalysis() throws Exception {
        // Test the internal resume analysis endpoint
        mockMvc.perform(multipart("/internal/resume/analyze")
                        .file("file", "Sample resume content for testing AI matching".getBytes())
                        .param("candidateId", CANDIDATE_ID)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.textLength").isNumber());
    }

    @Test
    @Order(3)
    void shouldReturnComputingWhenFirstCallWithResume() throws Exception {
        // Given: resume exists, no cached matches
        ResumeAnalysis analysis = ResumeAnalysis.builder()
                .candidateId("new-candidate-456")
                .extractedText("Sample resume with Java, Spring Boot, PostgreSQL experience.")
                .analyzedAt(LocalDateTime.now())
                .build();
        resumeAnalysisRepository.save(analysis);

        // Mock job offer service
        wireMock.stubFor(get(urlPathMatching("/api/job-offers.*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "content": [
                                    {
                                      "id": "11111111-1111-1111-1111-111111111111",
                                      "title": "Java Developer",
                                      "status": "OPEN",
                                      "companyName": "Tech Corp",
                                      "notes": "Looking for Spring Boot developer"
                                    }
                                  ],
                                  "totalElements": 1
                                }
                                """)));

        // When: first call to suggestions
        mockMvc.perform(get("/api/ai/suggestions")
                        .header("Authorization", "Bearer " + generateTestJwt("new-candidate-456", "CANDIDATE")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.computing").value(true))
                .andExpect(jsonPath("$.message").value(containsString("being computed")));
    }

    @Test
    @Order(4)
    void shouldRequireCandidateRoleForAiEndpoints() throws Exception {
        // When: accessing with ENTREPRISE role
        mockMvc.perform(get("/api/ai/suggestions")
                        .header("Authorization", "Bearer " + generateTestJwt(CANDIDATE_ID, "ENTREPRISE")))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(5)
    void shouldRequireAuthenticationForAiEndpoints() throws Exception {
        // When: no authentication
        mockMvc.perform(get("/api/ai/suggestions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(6)
    void shouldAllowInternalEndpointsWithoutAuth() throws Exception {
        // Internal endpoints should be accessible without auth
        mockMvc.perform(get("/internal/resume/exists/" + CANDIDATE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").isBoolean());
    }

    @Test
    @Order(7)
    void matchEndpointShouldRequireResume() throws Exception {
        // Given: candidate with no resume
        String candidateWithNoResume = "no-resume-candidate";

        // When/Then: should return 400 with message
        mockMvc.perform(get("/api/ai/match/" + JOB_OFFER_ID)
                        .header("Authorization", "Bearer " + generateTestJwt(candidateWithNoResume, "CANDIDATE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("resume")));
    }

    @Test
    @Order(8)
    void interviewPrepShouldRequireResume() throws Exception {
        // Given: candidate with no resume
        String candidateWithNoResume = "interview-no-resume";

        // When/Then: should return 400
        mockMvc.perform(get("/api/ai/interview-prep/" + JOB_OFFER_ID)
                        .header("Authorization", "Bearer " + generateTestJwt(candidateWithNoResume, "CANDIDATE")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(9)
    void healthEndpointShouldBeAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
