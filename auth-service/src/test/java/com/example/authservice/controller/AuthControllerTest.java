package com.example.authservice.controller;

import com.example.authservice.dto.*;
import com.example.authservice.exception.InvalidCredentialsException;
import com.example.authservice.exception.UserAlreadyExistsException;
import com.example.authservice.service.KeycloakService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KeycloakService keycloakService;

    @Test
    @DisplayName("POST /auth/register/candidate - should register candidate successfully")
    void shouldRegisterCandidate() throws Exception {
        RegisterCandidateRequest request = RegisterCandidateRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("securePass123")
                .skills(List.of("Java", "Spring"))
                .phone("+1234567890")
                .build();

        UserProfile profile = UserProfile.builder()
                .id("user-123")
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .roles(List.of("CANDIDATE"))
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .expiresIn(900)
                .userProfile(profile)
                .build();

        when(keycloakService.registerCandidate(any())).thenReturn(profile);
        when(keycloakService.login(any())).thenReturn(authResponse);

        mockMvc.perform(post("/auth/register/candidate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.access_token").value("access-token"))
                .andExpect(jsonPath("$.user_profile.email").value("john@example.com"))
                .andExpect(jsonPath("$.user_profile.roles[0]").value("CANDIDATE"));
    }

    @Test
    @DisplayName("POST /auth/register/enterprise - should register enterprise successfully")
    void shouldRegisterEnterprise() throws Exception {
        RegisterEnterpriseRequest request = RegisterEnterpriseRequest.builder()
                .companyName("Tech Corp")
                .email("contact@techcorp.com")
                .password("securePass123")
                .industry("Technology")
                .website("https://techcorp.com")
                .build();

        UserProfile profile = UserProfile.builder()
                .id("enterprise-456")
                .email("contact@techcorp.com")
                .firstName("Tech Corp")
                .roles(List.of("ENTERPRISE"))
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .expiresIn(900)
                .userProfile(profile)
                .build();

        when(keycloakService.registerEnterprise(any())).thenReturn(profile);
        when(keycloakService.login(any())).thenReturn(authResponse);

        mockMvc.perform(post("/auth/register/enterprise")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user_profile.roles[0]").value("ENTERPRISE"));
    }

    @Test
    @DisplayName("POST /auth/login - should login successfully")
    void shouldLoginSuccessfully() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("john@example.com")
                .password("securePass123")
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .expiresIn(900)
                .build();

        when(keycloakService.login(any())).thenReturn(authResponse);

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("access-token"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(900));
    }

    @Test
    @DisplayName("POST /auth/login - should return 401 for invalid credentials")
    void shouldReturn401ForInvalidCredentials() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("john@example.com")
                .password("wrongPassword")
                .build();

        when(keycloakService.login(any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Invalid Credentials"));
    }

    @Test
    @DisplayName("POST /auth/register/candidate - should return 409 when user exists")
    void shouldReturn409WhenUserExists() throws Exception {
        RegisterCandidateRequest request = RegisterCandidateRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("existing@example.com")
                .password("securePass123")
                .build();

        when(keycloakService.registerCandidate(any()))
                .thenThrow(new UserAlreadyExistsException("existing@example.com"));

        mockMvc.perform(post("/auth/register/candidate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("User Already Exists"));
    }

    @Test
    @DisplayName("POST /auth/login - should return 400 for missing email")
    void shouldReturn400ForMissingEmail() throws Exception {
        String invalidRequest = "{\"password\": \"securePass123\"}";

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /auth/me - should return user profile with valid JWT")
    void shouldReturnUserProfileWithValidJwt() throws Exception {
        UserProfile profile = UserProfile.builder()
                .id("user-123")
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .roles(List.of("CANDIDATE"))
                .build();

        when(keycloakService.getUserProfileFromToken(any())).thenReturn(profile);

        mockMvc.perform(get("/auth/me")
                        .with(jwt().jwt(builder -> builder
                                .subject("user-123")
                                .claim("email", "john@example.com")
                                .claim("given_name", "John")
                                .claim("family_name", "Doe")
                                .claim("realm_access", Map.of("roles", List.of("CANDIDATE"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-123"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    @DisplayName("GET /auth/me - should return 401 without JWT")
    void shouldReturn401WithoutJwt() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/refresh - should refresh token successfully")
    void shouldRefreshToken() throws Exception {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("old-refresh-token")
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .tokenType("Bearer")
                .expiresIn(900)
                .build();

        when(keycloakService.refreshToken(any())).thenReturn(authResponse);

        mockMvc.perform(post("/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("new-access-token"));
    }
}
