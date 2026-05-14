package com.example.authservice.integration;

import com.example.authservice.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthIntegrationTest {

    @Container
    static KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:24.0.5")
            .withRealmImportFile("realm-export.json")
            .withAdminUsername("admin")
            .withAdminPassword("admin");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String keycloakUrl = keycloak.getAuthServerUrl();
        registry.add("keycloak.auth-server-url", () -> keycloakUrl);
        registry.add("keycloak.realm", () -> "job-platform");
        registry.add("keycloak.client-id", () -> "job-platform-app");
        registry.add("keycloak.api-client-id", () -> "job-platform-api");
        registry.add("keycloak.api-client-secret", () -> "job-platform-api-secret");
        registry.add("keycloak.admin-username", () -> "admin");
        registry.add("keycloak.admin-password", () -> "admin");
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> keycloakUrl + "/realms/job-platform");
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> keycloakUrl + "/realms/job-platform/protocol/openid-connect/certs");
    }

    @Test
    @Order(1)
    @DisplayName("Should register a candidate and receive tokens")
    void shouldRegisterCandidate() throws Exception {
        RegisterCandidateRequest request = RegisterCandidateRequest.builder()
                .firstName("Integration")
                .lastName("Test")
                .email("integration-candidate@test.com")
                .password("TestPass123!")
                .skills(List.of("Java", "Spring Boot"))
                .phone("+33612345678")
                .build();

        mockMvc.perform(post("/auth/register/candidate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.refresh_token").isNotEmpty())
                .andExpect(jsonPath("$.user_profile.email").value("integration-candidate@test.com"))
                .andExpect(jsonPath("$.user_profile.roles").isArray());
    }

    @Test
    @Order(2)
    @DisplayName("Should register an enterprise and receive tokens")
    void shouldRegisterEnterprise() throws Exception {
        RegisterEnterpriseRequest request = RegisterEnterpriseRequest.builder()
                .companyName("Integration Corp")
                .email("integration-enterprise@test.com")
                .password("TestPass123!")
                .industry("Technology")
                .website("https://integration-corp.test")
                .build();

        mockMvc.perform(post("/auth/register/enterprise")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.user_profile.roles").isArray());
    }

    @Test
    @Order(3)
    @DisplayName("Should login with registered candidate credentials")
    void shouldLoginCandidate() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("integration-candidate@test.com")
                .password("TestPass123!")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.refresh_token").isNotEmpty())
                .andExpect(jsonPath("$.token_type").value("Bearer"));
    }

    @Test
    @Order(4)
    @DisplayName("Should fail login with wrong credentials")
    void shouldFailLoginWithWrongCredentials() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("integration-candidate@test.com")
                .password("WrongPassword!")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(5)
    @DisplayName("Should reject duplicate registration")
    void shouldRejectDuplicateRegistration() throws Exception {
        RegisterCandidateRequest request = RegisterCandidateRequest.builder()
                .firstName("Duplicate")
                .lastName("User")
                .email("integration-candidate@test.com")
                .password("TestPass123!")
                .build();

        mockMvc.perform(post("/auth/register/candidate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(6)
    @DisplayName("Should refresh token successfully")
    void shouldRefreshToken() throws Exception {
        // First login to get a refresh token
        LoginRequest loginRequest = LoginRequest.builder()
                .email("integration-candidate@test.com")
                .password("TestPass123!")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(), AuthResponse.class);

        // Now refresh the token
        RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
                .refreshToken(authResponse.getRefreshToken())
                .build();

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.refresh_token").isNotEmpty());
    }

    @Test
    @Order(7)
    @DisplayName("Should get user profile with valid access token")
    void shouldGetUserProfile() throws Exception {
        // First login to get an access token
        LoginRequest loginRequest = LoginRequest.builder()
                .email("integration-candidate@test.com")
                .password("TestPass123!")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(), AuthResponse.class);

        // Get profile
        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + authResponse.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("integration-candidate@test.com"))
                .andExpect(jsonPath("$.firstName").value("Integration"));
    }

    @Test
    @Order(8)
    @DisplayName("GET /auth/me should return 401 without token")
    void shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(9)
    @DisplayName("Health endpoint should be accessible without authentication")
    void shouldAccessHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
