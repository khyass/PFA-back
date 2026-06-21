package com.example.aiservice.exception;

/**
 * Exception thrown when resume text extraction fails.
 */
public class ResumeExtractionException extends RuntimeException {

    public ResumeExtractionException() {
        super("Could not extract text from the uploaded file. Please upload a readable PDF or DOCX.");
    }

    public ResumeExtractionException(String message) {
        super(message);
    }

    public ResumeExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
