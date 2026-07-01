package com.example.authservice.controller;

import com.example.authservice.dto.*;
import com.example.authservice.service.KeycloakService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller that exposes all authentication-related endpoints under {@code /auth}.
 *
 * <p>This controller is intentionally thin – all business logic and Keycloak
 * interactions are delegated to {@link KeycloakService}. The controller is
 * responsible only for:
 * <ul>
 *   <li>Receiving and validating request bodies (via {@code @Valid}).</li>
 *   <li>Calling the appropriate service method.</li>
 *   <li>Wrapping the result in the correct HTTP status code.</li>
 * </ul>
 *
 * <p>Endpoint visibility:
 * <ul>
 *   <li><b>Public</b> (no JWT required): /auth/login, /auth/register/**, /auth/refresh, /auth/introspect</li>
 *   <li><b>Authenticated</b> (valid JWT required): /auth/me, /auth/logout</li>
 * </ul>
 *
 * <p>All error cases are handled globally by {@code GlobalExceptionHandler} and
 * returned as RFC 7807 ProblemDetail responses.
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Inscription, connexion, déconnexion et gestion des tokens")
public class AuthController {

    private final KeycloakService keycloakService;

    @Operation(summary = "Inscrire un candidat", description = "Crée un compte candidat et retourne les tokens d'authentification.")
    @ApiResponse(responseCode = "201", description = "Inscription réussie")
    @PostMapping("/register/candidate")
    public ResponseEntity<AuthResponse> registerCandidate(@Valid @RequestBody RegisterCandidateRequest request) {
        log.info("Registering candidate: {}", request.getEmail());
        UserProfile profile = keycloakService.registerCandidate(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registerAndLogin(request.getEmail(), request.getPassword(), profile));
    }

    @Operation(summary = "Inscrire une entreprise", description = "Crée un compte entreprise et retourne les tokens d'authentification.")
    @ApiResponse(responseCode = "201", description = "Inscription réussie")
    @PostMapping("/register/enterprise")
    public ResponseEntity<AuthResponse> registerEnterprise(@Valid @RequestBody RegisterEnterpriseRequest request) {
        log.info("Registering enterprise: {}", request.getEmail());
        UserProfile profile = keycloakService.registerEnterprise(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registerAndLogin(request.getEmail(), request.getPassword(), profile));
    }

    @Operation(summary = "Connexion", description = "Authentifie un utilisateur et retourne un access token + refresh token.")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt: {}", request.getEmail());
        return ResponseEntity.ok(keycloakService.login(request));
    }

    @Operation(summary = "Rafraîchir le token", description = "Échange un refresh token contre une nouvelle paire de tokens.")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.debug("Refreshing token");
        return ResponseEntity.ok(keycloakService.refreshToken(request.getRefreshToken()));
    }

    @Operation(summary = "Déconnexion", description = "Révoque le refresh token de l'utilisateur.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshTokenRequest request) {
        log.info("Logout request");
        keycloakService.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Profil utilisateur", description = "Retourne le profil de l'utilisateur authentifié à partir du JWT.")
    @GetMapping("/me")
    public ResponseEntity<UserProfile> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        log.debug("Fetching current user profile from token");
        return ResponseEntity.ok(keycloakService.getUserProfileFromToken(jwt));
    }

    @Operation(summary = "Introspection de token", description = "Vérifie la validité d'un token via Keycloak (RFC 7662).")
    @PostMapping("/introspect")
    public ResponseEntity<Map<String, Object>> introspectToken(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(keycloakService.introspectToken(request.get("token")));
    }

    // ──────────────────────────────────────────────
    // Helper
    // ──────────────────────────────────────────────

    /** Auto-login after registration so the client gets tokens immediately. */
    private AuthResponse registerAndLogin(String email, String password, UserProfile profile) {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();
        AuthResponse authResponse = keycloakService.login(loginRequest);
        authResponse.setUserProfile(profile);
        return authResponse;
    }
}
