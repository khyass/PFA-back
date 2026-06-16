package com.example.jobofferservice.integration;

import com.example.jobofferservice.dto.JobOfferRequestDTO;
import com.example.jobofferservice.entity.JobOfferStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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

import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for JobOffer CRUD operations using Testcontainers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JobOfferIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("joboffers_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String createdJobOfferId;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    /**
     * Helper method to create a JWT mock for enterprise users.
     */
    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor enterpriseJwt(String userId) {
        return jwt()
                .jwt(builder -> builder
                        .subject(userId)
                        .claim("realm_access", java.util.Map.of("roles", java.util.List.of("ENTREPRISE")))
                );
    }

    @Test
    @Order(1)
    @DisplayName("Should create a new job offer")
    void shouldCreateJobOffer() throws Exception {
        JobOfferRequestDTO request = JobOfferRequestDTO.builder()
                .title("Senior Java Developer")
                .status(JobOfferStatus.OPEN)
                .publishedDate(LocalDate.now())
                .notes("Looking for experienced Java developers")
                .companyName("Tech Corp")
                .build();

        MvcResult result = mockMvc.perform(post("/api/job-offers")
                        .with(enterpriseJwt("user-123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Senior Java Developer"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.companyName").value("Tech Corp"))
                .andExpect(jsonPath("$.candidatureCount").value(0))
                .andExpect(jsonPath("$.ownerId").value("user-123"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andReturn();

        // Extract the created job offer ID for use in subsequent tests
        String responseBody = result.getResponse().getContentAsString();
        createdJobOfferId = objectMapper.readTree(responseBody).get("id").asText();
    }

    @Test
    @Order(2)
    @DisplayName("Should get job offer by ID")
    void shouldGetJobOfferById() throws Exception {
        mockMvc.perform(get("/api/job-offers/{id}", createdJobOfferId)
                        .with(enterpriseJwt("user-123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdJobOfferId))
                .andExpect(jsonPath("$.title").value("Senior Java Developer"))
                .andExpect(jsonPath("$.companyName").value("Tech Corp"));
    }

    @Test
    @Order(3)
    @DisplayName("Should get all job offers with pagination")
    void shouldGetAllJobOffers() throws Exception {
        mockMvc.perform(get("/api/job-offers")
                        .with(enterpriseJwt("user-123"))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.pageable.pageSize").value(10));
    }

    @Test
    @Order(4)
    @DisplayName("Should filter job offers by status")
    void shouldFilterByStatus() throws Exception {
        mockMvc.perform(get("/api/job-offers")
                        .with(enterpriseJwt("user-123"))
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"));

        mockMvc.perform(get("/api/job-offers")
                        .with(enterpriseJwt("user-123"))
                        .param("status", "CLOSED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    @Order(5)
    @DisplayName("Should filter job offers by company name (case-insensitive)")
    void shouldFilterByCompanyName() throws Exception {
        mockMvc.perform(get("/api/job-offers")
                        .with(enterpriseJwt("user-123"))
                        .param("company", "tech"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].companyName").value("Tech Corp"));

        mockMvc.perform(get("/api/job-offers")
                        .with(enterpriseJwt("user-123"))
                        .param("company", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    @Order(6)
    @DisplayName("Should update job offer")
    void shouldUpdateJobOffer() throws Exception {
        JobOfferRequestDTO updateRequest = JobOfferRequestDTO.builder()
                .title("Lead Java Developer")
                .status(JobOfferStatus.OPEN)
                .publishedDate(LocalDate.now())
                .notes("Updated: Looking for lead developers")
                .companyName("Tech Corp International")
                .build();

        mockMvc.perform(put("/api/job-offers/{id}", createdJobOfferId)
                        .with(enterpriseJwt("user-123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Lead Java Developer"))
                .andExpect(jsonPath("$.companyName").value("Tech Corp International"))
                .andExpect(jsonPath("$.notes").value("Updated: Looking for lead developers"));
    }

    @Test
    @Order(7)
    @DisplayName("Should update job offer status only")
    void shouldUpdateJobOfferStatus() throws Exception {
        String statusUpdateJson = "{\"status\": \"CLOSED\"}";

        mockMvc.perform(patch("/api/job-offers/{id}/status", createdJobOfferId)
                        .with(enterpriseJwt("user-123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusUpdateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    @Order(8)
    @DisplayName("Should return 403 when non-owner tries to update")
    void shouldReturn403ForNonOwner() throws Exception {
        JobOfferRequestDTO updateRequest = JobOfferRequestDTO.builder()
                .title("Unauthorized Update")
                .status(JobOfferStatus.OPEN)
                .publishedDate(LocalDate.now())
                .companyName("Hacker Corp")
                .build();

        mockMvc.perform(put("/api/job-offers/{id}", createdJobOfferId)
                        .with(enterpriseJwt("different-user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(9)
    @DisplayName("Should return 404 for non-existent job offer")
    void shouldReturn404ForNonExistent() throws Exception {
        mockMvc.perform(get("/api/job-offers/{id}", "00000000-0000-0000-0000-000000000000")
                        .with(enterpriseJwt("user-123")))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(10)
    @DisplayName("Should return 400 for invalid request body")
    void shouldReturn400ForInvalidRequest() throws Exception {
        String invalidRequest = "{\"status\": \"OPEN\"}"; // Missing required fields

        mockMvc.perform(post("/api/job-offers")
                        .with(enterpriseJwt("user-123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://api.job-platform.com/errors/validation"))
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.companyName").exists());
    }

    @Test
    @Order(11)
    @DisplayName("Should return 401 for unauthenticated request")
    void shouldReturn401ForUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/job-offers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(12)
    @DisplayName("Should return 403 for non-enterprise user")
    void shouldReturn403ForNonEnterpriseUser() throws Exception {
        mockMvc.perform(get("/api/job-offers")
                        .with(jwt().jwt(builder -> builder
                                .subject("candidate-user")
                                .claim("realm_access", java.util.Map.of("roles", java.util.List.of("CANDIDATE"))))))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(13)
    @DisplayName("Should delete job offer without candidatures")
    void shouldDeleteJobOffer() throws Exception {
        // Create a new job offer to delete
        JobOfferRequestDTO request = JobOfferRequestDTO.builder()
                .title("Job to Delete")
                .status(JobOfferStatus.DRAFT)
                .companyName("Delete Corp")
                .build();

        MvcResult result = mockMvc.perform(post("/api/job-offers")
                        .with(enterpriseJwt("user-123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String jobOfferId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        // Delete it
        mockMvc.perform(delete("/api/job-offers/{id}", jobOfferId)
                        .with(enterpriseJwt("user-123")))
                .andExpect(status().isNoContent());

        // Verify it's deleted
        mockMvc.perform(get("/api/job-offers/{id}", jobOfferId)
                        .with(enterpriseJwt("user-123")))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(14)
    @DisplayName("Should get empty candidatures list for job offer")
    void shouldGetEmptyCandidaturesList() throws Exception {
        mockMvc.perform(get("/api/job-offers/{id}/candidatures", createdJobOfferId)
                        .with(enterpriseJwt("user-123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
