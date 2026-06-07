package com.example.aiservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralized exception handling for the ai-service.
 * All exceptions are converted to RFC 7807 {@link ProblemDetail} responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_BASE_URI = "https://api.job-platform.com/errors/";

    @ExceptionHandler(ResumeExtractionException.class)
    public ProblemDetail handleResumeExtraction(ResumeExtractionException ex) {
        log.warn("Resume extraction failed: {}", ex.getMessage());
        return buildProblem(HttpStatus.BAD_REQUEST, ex.getMessage(), "Resume Extraction Failed", "resume-extraction-failed");
    }

    @ExceptionHandler(ResumeRequiredException.class)
    public ProblemDetail handleResumeRequired(ResumeRequiredException ex) {
        log.warn("Resume required: {}", ex.getMessage());
        return buildProblem(HttpStatus.BAD_REQUEST, ex.getMessage(), "Resume Required", "resume-required");
    }

    @ExceptionHandler(AiServiceException.class)
    public ProblemDetail handleAiService(AiServiceException ex) {
        log.error("AI service error: {}", ex.getMessage(), ex);
        return buildProblem(HttpStatus.BAD_GATEWAY, ex.getMessage(), "AI Service Error", "ai-service-error");
    }

    @ExceptionHandler(JobOfferNotFoundException.class)
    public ProblemDetail handleJobOfferNotFound(JobOfferNotFoundException ex) {
        log.warn("Job offer not found: {}", ex.getMessage());
        return buildProblem(HttpStatus.NOT_FOUND, ex.getMessage(), "Job Offer Not Found", "job-offer-not-found");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return buildProblem(HttpStatus.FORBIDDEN, "Access denied", "Access Denied", "access-denied");
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
