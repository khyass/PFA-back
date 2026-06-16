package com.example.jobofferservice.exception;

import java.util.UUID;

/**
 * Exception thrown when a job offer is not found.
 */
public class JobOfferNotFoundException extends RuntimeException {

    public JobOfferNotFoundException(UUID id) {
        super("Job offer not found with id: " + id);
    }

    public JobOfferNotFoundException(String message) {
        super(message);
    }
}
