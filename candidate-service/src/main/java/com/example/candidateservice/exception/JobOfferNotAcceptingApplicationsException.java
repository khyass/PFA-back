package com.example.candidateservice.exception;

/**
 * Exception thrown when a job offer is not accepting applications.
 */
public class JobOfferNotAcceptingApplicationsException extends RuntimeException {

    public JobOfferNotAcceptingApplicationsException() {
        super("This job offer is not accepting applications");
    }

    public JobOfferNotAcceptingApplicationsException(String message) {
        super(message);
    }
}
