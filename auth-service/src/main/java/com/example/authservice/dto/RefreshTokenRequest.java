package com.example.authservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for POST /auth/refresh and POST /auth/logout.
 *
 * <p>The refresh_token is a long-lived Keycloak token (default 7 days).
 * For refresh: it is exchanged for a new access/refresh token pair.
 * For logout: it is sent to Keycloak's logout endpoint to invalidate the session.
 *
 * <p>The {@link JsonProperty} annotation ensures the incoming JSON key
 * {@code refresh_token} (snake_case) is correctly deserialized, matching
 * the OAuth2 standard naming used by Keycloak.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {

    /** The refresh token issued at login or after a previous token refresh */
    @NotBlank(message = "Refresh token is required")
    @JsonProperty("refresh_token")
    private String refreshToken;
}
