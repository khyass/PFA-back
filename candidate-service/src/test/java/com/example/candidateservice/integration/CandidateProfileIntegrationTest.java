package com.example.candidateservice.integration;

import com.example.candidateservice.dto.ProfileUpdateRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Candidate Profile operations using Testcontainers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CandidateProfileIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("candidates_profile_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String CANDIDATE_ID = "profile-test-user";

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor candidateJwt(String userId) {
        return jwt()
                .jwt(builder -> builder
                        .subject(userId)
                        .claim("realm_access", java.util.Map.of("roles", java.util.List.of("CANDIDATE")))
                );
    }

    @Test
    @Order(1)
    @DisplayName("Should auto-create empty profile on first access")
    void shouldAutoCreateProfileOnFirstAccess() throws Exception {
        mockMvc.perform(get("/api/candidate/profile")
                        .with(candidateJwt(CANDIDATE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.userId").value(CANDIDATE_ID))
                .andExpect(jsonPath("$.fullName").isEmpty())
                .andExpect(jsonPath("$.resumeFileName").isEmpty());
    }

    @Test
    @Order(2)
    @DisplayName("Should update profile")
    void shouldUpdateProfile() throws Exception {
        ProfileUpdateRequestDTO request = ProfileUpdateRequestDTO.builder()
                .fullName("John Doe")
                .phone("+1234567890")
                .bio("Experienced developer")
                .linkedinUrl("https://linkedin.com/in/johndoe")
                .build();

        mockMvc.perform(put("/api/candidate/profile")
                        .with(candidateJwt(CANDIDATE_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("John Doe"))
                .andExpect(jsonPath("$.phone").value("+1234567890"))
                .andExpect(jsonPath("$.bio").value("Experienced developer"))
                .andExpect(jsonPath("$.linkedinUrl").value("https://linkedin.com/in/johndoe"));
    }

    @Test
    @Order(3)
    @DisplayName("Should upload PDF resume")
    void shouldUploadPdfResume() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                "PDF content here".getBytes()
        );

        mockMvc.perform(multipart("/api/candidate/profile/resume")
                        .file(file)
                        .with(candidateJwt(CANDIDATE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumeFileName").value("resume.pdf"));
    }

    @Test
    @Order(4)
    @DisplayName("Should download resume")
    void shouldDownloadResume() throws Exception {
        mockMvc.perform(get("/api/candidate/profile/resume")
                        .with(candidateJwt(CANDIDATE_ID)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"resume.pdf\""))
                .andExpect(content().contentType("application/pdf"));
    }

    @Test
    @Order(5)
    @DisplayName("Should reject non-PDF/DOCX files")
    void shouldRejectInvalidFileType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malware.exe",
                "application/octet-stream",
                "malicious content".getBytes()
        );

        mockMvc.perform(multipart("/api/candidate/profile/resume")
                        .file(file)
                        .with(candidateJwt(CANDIDATE_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Only PDF and DOCX files are allowed"));
    }

    @Test
    @Order(6)
    @DisplayName("Should return 400 for invalid profile update")
    void shouldReturn400ForInvalidProfileUpdate() throws Exception {
        String invalidRequest = "{\"phone\": \"123\"}"; // Missing required fullName

        mockMvc.perform(put("/api/candidate/profile")
                        .with(candidateJwt(CANDIDATE_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fullName").exists());
    }

    @Test
    @Order(7)
    @DisplayName("Should return 403 for non-candidate user")
    void shouldReturn403ForNonCandidateUser() throws Exception {
        mockMvc.perform(get("/api/candidate/profile")
                        .with(jwt().jwt(builder -> builder
                                .subject("enterprise-user")
                                .claim("realm_access", java.util.Map.of("roles", java.util.List.of("ENTREPRISE"))))))
                .andExpect(status().isForbidden());
    }
}
