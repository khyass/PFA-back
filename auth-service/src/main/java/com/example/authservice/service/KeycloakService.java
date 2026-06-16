package com.example.authservice.service;

import com.example.authservice.config.KeycloakConfig;
import com.example.authservice.dto.*;
import com.example.authservice.exception.InvalidCredentialsException;
import com.example.authservice.exception.KeycloakCommunicationException;
import com.example.authservice.exception.TokenExpiredException;
import com.example.authservice.exception.UserAlreadyExistsException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jakarta.ws.rs.core.Response;
import java.util.*;

/**
 * Core service that integrates with Keycloak for user management and token operations.
 *
 * <p>Communicates with Keycloak through:
 * <ul>
 *   <li><b>Admin REST API</b> (via {@link Keycloak} client) – user/role management</li>
 *   <li><b>OIDC endpoints</b> (via {@link RestTemplate}) – token operations</li>
 * </ul>
 *
 * <p>All Keycloak-facing methods are protected by Resilience4j circuit breaker
 * and retry (configured under the name "keycloak" in application.yml).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakService {

    private final Keycloak keycloakAdmin;
    private final KeycloakConfig keycloakConfig;
    private final RestTemplate restTemplate = new RestTemplate();

    /** Application-specific roles (used to filter out Keycloak system roles). */
    private static final Set<String> APP_ROLES = Set.of("CANDIDATE", "ENTERPRISE");

    // ──────────────────────────────────────────────
    // Registration
    // ──────────────────────────────────────────────

    /**
     * Registers a new candidate user and assigns the CANDIDATE role.
     * Skills and phone are stored as custom Keycloak attributes.
     */
    @CircuitBreaker(name = "keycloak", fallbackMethod = "fallback")
    @Retry(name = "keycloak")
    public UserProfile registerCandidate(RegisterCandidateRequest req) {
        log.info("Registering candidate: {}", req.getEmail());

        Map<String, List<String>> attrs = new HashMap<>();
        if (req.getSkills() != null && !req.getSkills().isEmpty()) {
            attrs.put("skills", req.getSkills());
        }
        if (req.getPhone() != null) {
            attrs.put("phone", List.of(req.getPhone()));
        }

        return registerUser(
                req.getEmail(), req.getFirstName(), req.getLastName(),
                req.getPassword(), "CANDIDATE", attrs
        );
    }

    /**
     * Registers a new enterprise user and assigns the ENTERPRISE role.
     * Company name, industry and website are stored as custom Keycloak attributes.
     */
    @CircuitBreaker(name = "keycloak", fallbackMethod = "fallback")
    @Retry(name = "keycloak")
    public UserProfile registerEnterprise(RegisterEnterpriseRequest req) {
        log.info("Registering enterprise: {}", req.getEmail());

        Map<String, List<String>> attrs = new HashMap<>();
        attrs.put("companyName", List.of(req.getCompanyName()));
        if (req.getIndustry() != null) {
            attrs.put("industry", List.of(req.getIndustry()));
        }
        if (req.getWebsite() != null) {
            attrs.put("website", List.of(req.getWebsite()));
        }

        return registerUser(
                req.getEmail(), req.getCompanyName(), req.getCompanyName(),
                req.getPassword(), "ENTERPRISE", attrs
        );
    }

    // ──────────────────────────────────────────────
    // Token operations
    // ──────────────────────────────────────────────

    /**
     * Authenticates a user via the Keycloak ROPC grant and returns tokens + profile.
     */
    @CircuitBreaker(name = "keycloak", fallbackMethod = "fallback")
    @Retry(name = "keycloak")
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for: {}", request.getEmail());

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", keycloakConfig.getClientId());
        form.add("username", request.getEmail());
        form.add("password", request.getPassword());
        form.add("scope", "openid");

        try {
            Map<String, Object> tokenResponse = postForm(keycloakConfig.getTokenUrl(), form);
            UserProfile profile = getUserProfileByEmail(request.getEmail());
            return toAuthResponse(tokenResponse, profile);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.BadRequest e) {
            throw new InvalidCredentialsException();
        } catch (RestClientException e) {
            throw new KeycloakCommunicationException("Failed to communicate with Keycloak", e);
        }
    }

    /**
     * Exchanges a refresh token for a new access/refresh token pair.
     */
    @CircuitBreaker(name = "keycloak", fallbackMethod = "fallback")
    @Retry(name = "keycloak")
    public AuthResponse refreshToken(String refreshToken) {
        log.debug("Refreshing token");

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", keycloakConfig.getClientId());
        form.add("refresh_token", refreshToken);

        try {
            Map<String, Object> tokenResponse = postForm(keycloakConfig.getTokenUrl(), form);
            return toAuthResponse(tokenResponse, null);
        } catch (HttpClientErrorException.BadRequest e) {
            throw new TokenExpiredException("Refresh token is invalid or expired");
        } catch (RestClientException e) {
            throw new KeycloakCommunicationException("Failed to refresh token", e);
        }
    }

    /**
     * Invalidates the user's Keycloak session by revoking the refresh token.
     */
    public void logout(String refreshToken) {
        log.info("Logging out user");

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", keycloakConfig.getClientId());
        form.add("refresh_token", refreshToken);

        try {
            postForm(keycloakConfig.getLogoutUrl(), form);
        } catch (HttpClientErrorException.BadRequest e) {
            // Token is already invalid/expired/revoked — treat as successful logout
            log.warn("Refresh token already invalid during logout (likely expired or rotated): {}", e.getMessage());
        } catch (RestClientException e) {
            throw new KeycloakCommunicationException("Failed to logout from Keycloak", e);
        }
    }

    // ──────────────────────────────────────────────
    // User profile
    // ──────────────────────────────────────────────

    /**
     * Builds a {@link UserProfile} directly from the validated JWT claims (no API call needed).
     */
    public UserProfile getUserProfileFromToken(org.springframework.security.oauth2.jwt.Jwt jwt) {
        List<String> appRoles = extractRolesFromJwt(jwt);

        return UserProfile.builder()
                .id(jwt.getSubject())
                .email(jwt.getClaim("email"))
                .firstName(jwt.getClaim("given_name"))
                .lastName(jwt.getClaim("family_name"))
                .roles(appRoles)
                .build();
    }

    /**
     * Introspects a token via Keycloak's RFC 7662 introspection endpoint.
     * Uses the confidential API client credentials.
     */
    @CircuitBreaker(name = "keycloak")
    public Map<String, Object> introspectToken(String token) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", keycloakConfig.getApiClientId());
        form.add("client_secret", keycloakConfig.getApiClientSecret());
        form.add("token", token);

        try {
            return postForm(keycloakConfig.getIntrospectUrl(), form);
        } catch (RestClientException e) {
            throw new KeycloakCommunicationException("Failed to introspect token", e);
        }
    }

    // ──────────────────────────────────────────────
    // Shared helpers (eliminate duplication)
    // ──────────────────────────────────────────────

    /**
     * Common registration logic shared by candidate and enterprise flows.
     * Creates a Keycloak user, assigns the role, and returns the profile.
     */
    private UserProfile registerUser(String email, String firstName, String lastName,
                                     String password, String roleName,
                                     Map<String, List<String>> customAttributes) {
        customAttributes.put("userType", List.of(roleName));

        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername(email);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmailVerified(true);
        user.setRequiredActions(List.of()); // no required actions — allows immediate login
        user.setAttributes(customAttributes);

        String userId = createKeycloakUser(user, password);
        assignRealmRole(userId, roleName);

        return UserProfile.builder()
                .id(userId)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .roles(List.of(roleName))
                .attributes(customAttributes)
                .build();
    }

    /**
     * Posts a form-encoded request to a Keycloak endpoint and returns the response body.
     * Eliminates the repeated HttpHeaders + RestTemplate boilerplate.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> postForm(String url, MultiValueMap<String, String> form) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(form, headers), Map.class
        );

        Map<String, Object> body = response.getBody();
        if (body == null) {
            throw new KeycloakCommunicationException("Empty response from Keycloak: " + url);
        }
        return body;
    }

    /** Converts a raw Keycloak token response map into an {@link AuthResponse}. */
    private AuthResponse toAuthResponse(Map<String, Object> tokenResponse, UserProfile profile) {
        return AuthResponse.builder()
                .accessToken((String) tokenResponse.get("access_token"))
                .refreshToken((String) tokenResponse.get("refresh_token"))
                .tokenType((String) tokenResponse.get("token_type"))
                .expiresIn(((Number) tokenResponse.get("expires_in")).longValue())
                .userProfile(profile)
                .build();
    }

    /**
     * Extracts application-specific roles from a JWT.
     * Merges both realm_access.roles and top-level "roles" claim, then filters.
     */
    @SuppressWarnings("unchecked")
    private List<String> extractRolesFromJwt(org.springframework.security.oauth2.jwt.Jwt jwt) {
        Set<String> roles = new HashSet<>();

        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof List<?> realmRoles) {
            realmRoles.forEach(r -> roles.add((String) r));
        }

        List<String> topRoles = jwt.getClaim("roles");
        if (topRoles != null) {
            roles.addAll(topRoles);
        }

        return roles.stream().filter(APP_ROLES::contains).toList();
    }

    /** Creates a Keycloak user with hashed password, returns the new user ID. */
    private String createKeycloakUser(UserRepresentation user, String password) {
        UsersResource users = getRealmResource().users();

        List<UserRepresentation> existing = users.searchByEmail(user.getEmail(), true);
        if (existing != null && !existing.isEmpty()) {
            throw new UserAlreadyExistsException(user.getEmail());
        }

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        user.setCredentials(List.of(credential));

        try (Response response = users.create(user)) {
            if (response.getStatus() == 201) {
                String location = response.getHeaderString("Location");
                String userId = location.substring(location.lastIndexOf('/') + 1);
                log.info("Created Keycloak user: {}", userId);
                return userId;
            } else if (response.getStatus() == 409) {
                throw new UserAlreadyExistsException(user.getEmail());
            } else {
                throw new KeycloakCommunicationException(
                        "Failed to create user. Status: " + response.getStatus());
            }
        }
    }

    /** Assigns a realm role to a user. The role must already exist in Keycloak. */
    private void assignRealmRole(String userId, String roleName) {
        RealmResource realm = getRealmResource();
        RoleRepresentation role = realm.roles().get(roleName).toRepresentation();
        realm.users().get(userId).roles().realmLevel().add(List.of(role));
        log.info("Assigned role '{}' to user '{}'", roleName, userId);
    }

    /** Fetches a user profile from Keycloak Admin API by email. */
    private UserProfile getUserProfileByEmail(String email) {
        RealmResource realm = getRealmResource();
        List<UserRepresentation> found = realm.users().searchByEmail(email, true);
        if (found == null || found.isEmpty()) {
            return null;
        }

        UserRepresentation kcUser = found.get(0);

        List<String> roleNames = realm.users().get(kcUser.getId())
                .roles().realmLevel().listEffective().stream()
                .map(RoleRepresentation::getName)
                .filter(APP_ROLES::contains)
                .toList();

        return UserProfile.builder()
                .id(kcUser.getId())
                .email(kcUser.getEmail())
                .firstName(kcUser.getFirstName())
                .lastName(kcUser.getLastName())
                .roles(roleNames)
                .attributes(kcUser.getAttributes())
                .build();
    }

    private RealmResource getRealmResource() {
        return keycloakAdmin.realm(keycloakConfig.getRealm());
    }

    // ──────────────────────────────────────────────
    // Resilience4j fallbacks (must match each annotated method's signature + Throwable)
    // ──────────────────────────────────────────────

    @SuppressWarnings("unused")
    private UserProfile fallback(RegisterCandidateRequest req, Throwable t) {
        log.error("Circuit breaker triggered for registerCandidate: {}", t.getMessage());
        throw new KeycloakCommunicationException(
                "Authentication service is currently unavailable. Please try again later.", t);
    }

    @SuppressWarnings("unused")
    private UserProfile fallback(RegisterEnterpriseRequest req, Throwable t) {
        log.error("Circuit breaker triggered for registerEnterprise: {}", t.getMessage());
        throw new KeycloakCommunicationException(
                "Authentication service is currently unavailable. Please try again later.", t);
    }

    @SuppressWarnings("unused")
    private AuthResponse fallback(LoginRequest req, Throwable t) {
        log.error("Circuit breaker triggered for login: {}", t.getMessage());
        throw new KeycloakCommunicationException(
                "Authentication service is currently unavailable. Please try again later.", t);
    }

    @SuppressWarnings("unused")
    private AuthResponse fallback(String token, Throwable t) {
        log.error("Circuit breaker triggered for token operation: {}", t.getMessage());
        throw new KeycloakCommunicationException(
                "Authentication service is currently unavailable. Please try again later.", t);
    }
}
