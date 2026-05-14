package com.example.authservice.exception;

/**
 * Thrown when a registration request arrives for an email address that is
 * already associated with an existing Keycloak account in the realm.
 *
 * <p>The {@code GlobalExceptionHandler} maps this exception to HTTP 409 Conflict
 * with an RFC 7807 ProblemDetail response body.
 */
public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String email) {
        super("User with email '" + email + "' already exists");
    }
}
