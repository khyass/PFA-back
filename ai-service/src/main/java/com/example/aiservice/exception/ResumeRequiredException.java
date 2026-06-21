package com.example.aiservice.exception;

/**
 * Exception thrown when resume is required but not uploaded.
 */
public class ResumeRequiredException extends RuntimeException {

    public ResumeRequiredException() {
        super("Please upload your resume before requesting a match.");
    }

    public ResumeRequiredException(String message) {
        super(message);
    }
}
