package com.example.jobofferservice.exception;

/**
 * Exception thrown when a user tries to access a resource they don't own.
 */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException() {
        super("You do not have permission to access this resource");
    }

    public AccessDeniedException(String message) {
        super(message);
    }
}
