package com.example.jobofferservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralized exception handling for the job-offer-service.
 * All exceptions are converted to RFC 7807 {@link ProblemDetail} responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_BASE_URI = "https://api.job-platform.com/errors/";

    @ExceptionHandler(JobOfferNotFoundException.class)
    public ProblemDetail handleJobOfferNotFound(JobOfferNotFoundException ex) {
        log.warn("Job offer not found: {}", ex.getMessage());
        return buildProblem(HttpStatus.NOT_FOUND, ex.getMessage(), "Job Offer Not Found", "job-offer-not-found");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return buildProblem(HttpStatus.FORBIDDEN, ex.getMessage(), "Access Denied", "access-denied");
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ProblemDetail handleSpringAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        log.warn("Spring Security access denied: {}", ex.getMessage());
        return buildProblem(HttpStatus.FORBIDDEN, "Access denied", "Access Denied", "access-denied");
    }

    @ExceptionHandler(JobOfferHasCandidaturesException.class)
    public ProblemDetail handleJobOfferHasCandidatures(JobOfferHasCandidaturesException ex) {
        log.warn("Cannot delete job offer with candidatures: {}", ex.getMessage());
        return buildProblem(HttpStatus.CONFLICT, "Cannot delete a job offer with existing candidatures",
                "Job Offer Has Candidatures", "job-offer-has-candidatures");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                        (a, b) -> a
                ));

        ProblemDetail problem = buildProblem(HttpStatus.BAD_REQUEST, "Validation failed", "Validation Error", "validation");
        problem.setProperty("errors", fieldErrors);
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildProblem(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred", "Internal Server Error", "internal-error");
    }

    private ProblemDetail buildProblem(HttpStatus status, String detail, String title, String errorType) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create(ERROR_BASE_URI + errorType));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
