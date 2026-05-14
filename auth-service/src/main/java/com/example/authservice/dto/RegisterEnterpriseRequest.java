package com.example.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for the POST /auth/register/enterprise endpoint.
 *
 * <p>Enterprises are registered with the ENTERPRISE realm role in Keycloak.
 * The {@code companyName}, {@code industry}, and {@code website} fields are
 * stored as custom attributes on the Keycloak user representation so they
 * are available without querying an additional database.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterEnterpriseRequest {

    @NotBlank(message = "Company name is required")
    @Size(min = 2, max = 100, message = "Company name must be between 2 and 100 characters")
    private String companyName; // Stored as Keycloak firstName and as a custom attribute

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email; // Used as the Keycloak username (must be unique in the realm)

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    private String password; // Hashed by Keycloak; never stored in plain text

    /** Business sector of the company (e.g. "Technology", "Finance") - optional Keycloak attribute */
    @Size(max = 100, message = "Industry must be at most 100 characters")
    private String industry;

    /** Company website URL - optional Keycloak attribute */
    @Size(max = 255, message = "Website must be at most 255 characters")
    private String website;
}
