package com.example.candidateservice.exception;

import java.util.UUID;

/**
 * Exception thrown when a candidature is not found.
 */
public class CandidatureNotFoundException extends RuntimeException {

    public CandidatureNotFoundException(UUID id) {
        super("Candidature not found with id: " + id);
    }

    public CandidatureNotFoundException(String message) {
        super(message);
    }
}
