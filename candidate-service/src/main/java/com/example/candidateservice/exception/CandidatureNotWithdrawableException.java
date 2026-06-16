package com.example.candidateservice.exception;

import java.util.UUID;

/**
 * Exception thrown when trying to withdraw a candidature that is not in PENDING status.
 */
public class CandidatureNotWithdrawableException extends RuntimeException {

    public CandidatureNotWithdrawableException(UUID candidatureId) {
        super("You can only withdraw a pending application");
    }

    public CandidatureNotWithdrawableException(String message) {
        super(message);
    }
}
