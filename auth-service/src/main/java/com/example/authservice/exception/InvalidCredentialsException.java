package com.example.authservice.exception;

/**
 * Thrown when a login attempt fails due to incorrect email or password.
 *
 * <p>The error message is intentionally vague ("Invalid email or password")
 * to prevent user enumeration attacks - the client cannot determine whether
 * the email or the password was wrong.
 *
 * <p>The {@code GlobalExceptionHandler} maps this to HTTP 401 Unauthorized.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
