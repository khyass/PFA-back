package com.example.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Decoded representation of a Keycloak user returned in API responses.
 *
 * <p>Populated either from the Keycloak Admin API (after registration) or by
 * decoding JWT claims (GET /auth/me). Gives the frontend everything it needs
 * to display user details and perform role-based UI rendering without extra
 * API calls.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

    /** Keycloak user UUID (the "sub" claim in the JWT) */
    private String id;

    /** User's email address (also their Keycloak username) */
    private String email;

    /** User's first name, or company name for ENTERPRISE users */
    private String firstName;

    /** User's last name (empty string for ENTERPRISE users) */
    private String lastName;

    /**
     * Application-level realm roles assigned to the user.
     * Contains only CANDIDATE or ENTERPRISE - Keycloak system roles
     * (e.g. default-roles-job-platform) are filtered out.
     */
    private List<String> roles;

    /**
     * Custom Keycloak user attributes (multi-value map).
     * Examples: skills, phone, companyName, industry, website.
     */
    private Map<String, List<String>> attributes;
}
