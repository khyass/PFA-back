package com.example.authservice.exception;

/**
 * Thrown when a refresh token is invalid, has already been revoked, or has
 * exceeded its configured lifetime in Keycloak (default: 7 days).
 *
 * <p>The client must re-authenticate by calling POST /auth/login.
 *
 * <p>The {@code GlobalExceptionHandler} maps this to HTTP 401 Unauthorized.
 */
public class TokenExpiredException extends RuntimeException {

    public TokenExpiredException() {
        super("Token has expired");
    }

    public TokenExpiredException(String message) {
        super(message);
    }
}
