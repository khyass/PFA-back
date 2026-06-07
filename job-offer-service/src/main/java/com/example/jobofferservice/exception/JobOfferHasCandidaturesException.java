package com.example.jobofferservice.exception;

import java.util.UUID;

/**
 * Exception thrown when trying to delete a job offer that has existing candidatures.
 */
public class JobOfferHasCandidaturesException extends RuntimeException {

    public JobOfferHasCandidaturesException(UUID jobOfferId) {
        super("Cannot delete job offer with id: " + jobOfferId + " because it has existing candidatures");
    }

    public JobOfferHasCandidaturesException(String message) {
        super(message);
    }
}
