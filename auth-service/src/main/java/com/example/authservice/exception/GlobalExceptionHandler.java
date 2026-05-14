package com.example.authservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralized exception handling for the auth-service.
 * All exceptions are converted to RFC 7807 {@link ProblemDetail} responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_BASE_URI = "https://api.job-platform.com/errors/";

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ProblemDetail handleUserAlreadyExists(UserAlreadyExistsException ex) {
        log.warn("User already exists: {}", ex.getMessage());
        return buildProblem(HttpStatus.CONFLICT, ex.getMessage(), "User Already Exists", "user-already-exists");
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        log.warn("Invalid credentials attempt");
        return buildProblem(HttpStatus.UNAUTHORIZED, ex.getMessage(), "Invalid Credentials", "invalid-credentials");
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ProblemDetail handleTokenExpired(TokenExpiredException ex) {
        log.warn("Token expired: {}", ex.getMessage());
        return buildProblem(HttpStatus.UNAUTHORIZED, ex.getMessage(), "Token Expired", "token-expired");
    }

    @ExceptionHandler(KeycloakCommunicationException.class)
    public ProblemDetail handleKeycloakCommunication(KeycloakCommunicationException ex) {
        log.error("Keycloak communication error: {}", ex.getMessage(), ex);
        return buildProblem(HttpStatus.SERVICE_UNAVAILABLE,
                "Authentication service is temporarily unavailable", "Service Unavailable", "keycloak-unavailable");
    }

    @ExceptionHandler(JwtException.class)
    public ProblemDetail handleJwtException(JwtException ex) {
        log.warn("JWT error: {}", ex.getMessage());
        return buildProblem(HttpStatus.UNAUTHORIZED, "Invalid or expired token", "Authentication Error", "jwt-error");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return buildProblem(HttpStatus.FORBIDDEN, "Access denied", "Forbidden", "access-denied");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                        (a, b) -> a
                ));

        ProblemDetail problem = buildProblem(HttpStatus.BAD_REQUEST, "Validation failed", "Validation Error", "validation");
        problem.setProperty("errors", fieldErrors);
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildProblem(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred", "Internal Server Error", "internal");
    }

    // ── Helper ─────────────────────────────────────────────────

    /** Builds a standard RFC 7807 ProblemDetail with timestamp. */
    private ProblemDetail buildProblem(HttpStatus status, String detail, String title, String errorSlug) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create(ERROR_BASE_URI + errorSlug));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
