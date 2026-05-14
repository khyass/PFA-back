package com.example.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request body for the POST /auth/register/candidate endpoint.
 *
 * <p>Bean Validation annotations ensure that required fields are present and
 * within acceptable bounds before the request reaches the service layer.
 * Validation errors are caught by {@code GlobalExceptionHandler} and returned
 * as RFC 7807 ProblemDetail responses with field-level error details.
 *
 * <p>The {@code skills} and {@code phone} fields are optional and are stored
 * as custom attributes on the Keycloak user representation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterCandidateRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email; // Used as the Keycloak username (must be unique in the realm)

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    private String password; // Hashed by Keycloak; never stored in plain text

    /** Optional list of professional skills stored as a multi-value Keycloak attribute */
    private List<String> skills;

    @Size(max = 20, message = "Phone number must be at most 20 characters")
    private String phone; // Optional – stored as a Keycloak user attribute
}
