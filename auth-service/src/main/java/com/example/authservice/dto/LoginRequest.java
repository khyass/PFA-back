package com.example.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for the POST /auth/login endpoint.
 *
 * <p>Credentials are forwarded to the Keycloak token endpoint using the
 * Resource Owner Password Credentials (ROPC) grant. The email is used as the
 * Keycloak username because registration always sets username == email.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    /** User's email address - also serves as the Keycloak username */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    /** Plain-text password - transmitted over HTTPS and validated by Keycloak */
    @NotBlank(message = "Password is required")
    private String password;
}
