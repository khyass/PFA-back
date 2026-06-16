package com.example.candidateservice.exception;

import java.util.UUID;

/**
 * Exception thrown when a candidate tries to apply to the same job offer twice.
 */
public class DuplicateCandidatureException extends RuntimeException {

    public DuplicateCandidatureException(UUID jobOfferId) {
        super("You have already applied to this job offer");
    }

    public DuplicateCandidatureException(String message) {
        super(message);
    }
}
