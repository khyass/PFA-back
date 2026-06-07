package com.example.aiservice.exception;

/**
 * Exception thrown when AI service returns an unexpected response.
 */
public class AiServiceException extends RuntimeException {

    public AiServiceException() {
        super("AI service returned an unexpected response. Please try again.");
    }

    public AiServiceException(String message) {
        super(message);
    }

    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
