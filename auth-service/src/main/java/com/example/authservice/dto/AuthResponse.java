package com.example.authservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response body returned by login and registration endpoints.
 *
 * <p>Field names follow the OAuth2/OIDC naming convention (snake_case) via
 * {@link JsonProperty} to match what Keycloak itself returns, making it easy
 * to integrate with OAuth2-aware Angular libraries.
 *
 * <p>The {@code userProfile} field provides decoded user information so that
 * the frontend does not need to parse the JWT after login.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /** Short-lived JWT used to authenticate API calls (default TTL: 15 minutes) */
    @JsonProperty("access_token")
    private String accessToken;

    /** Long-lived token used to obtain a new access_token without re-authentication (default TTL: 7 days) */
    @JsonProperty("refresh_token")
    private String refreshToken;

    /** Always "Bearer" - indicates how to use the token in the Authorization header */
    @JsonProperty("token_type")
    @Builder.Default
    private String tokenType = "Bearer";

    /** Number of seconds until the access_token expires */
    @JsonProperty("expires_in")
    private long expiresIn;

    /** Decoded user profile included for convenience so clients avoid parsing the JWT */
    @JsonProperty("user_profile")
    private UserProfile userProfile;
}
