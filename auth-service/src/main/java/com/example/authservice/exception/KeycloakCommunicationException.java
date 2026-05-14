package com.example.authservice.exception;

/**
 * Thrown when the auth-service cannot communicate with the Keycloak server,
 * or when Keycloak returns an unexpected error response.
 *
 * <p>Typical causes: network issues, Keycloak being down, or misconfigured
 * server URL. The Resilience4j circuit breaker catches failures at this level
 * and triggers this exception in its fallback methods.
 *
 * <p>The {@code GlobalExceptionHandler} maps this to HTTP 503 Service Unavailable
 * with a generic message so that internal details are not exposed to clients.
 */
public class KeycloakCommunicationException extends RuntimeException {

    public KeycloakCommunicationException(String message) {
        super(message);
    }

    public KeycloakCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
