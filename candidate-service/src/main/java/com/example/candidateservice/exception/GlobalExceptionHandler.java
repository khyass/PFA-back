package com.example.candidateservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralized exception handling for the candidate-service.
 * All exceptions are converted to RFC 7807 {@link ProblemDetail} responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_BASE_URI = "https://api.job-platform.com/errors/";

    @ExceptionHandler(CandidatureNotFoundException.class)
    public ProblemDetail handleCandidatureNotFound(CandidatureNotFoundException ex) {
        log.warn("Candidature not found: {}", ex.getMessage());
        return buildProblem(HttpStatus.NOT_FOUND, ex.getMessage(), "Candidature Not Found", "candidature-not-found");
    }

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

    @ExceptionHandler(DuplicateCandidatureException.class)
    public ProblemDetail handleDuplicateCandidature(DuplicateCandidatureException ex) {
        log.warn("Duplicate candidature: {}", ex.getMessage());
        return buildProblem(HttpStatus.CONFLICT, ex.getMessage(), "Duplicate Application", "duplicate-candidature");
    }

    @ExceptionHandler(JobOfferNotAcceptingApplicationsException.class)
    public ProblemDetail handleJobOfferNotAcceptingApplications(JobOfferNotAcceptingApplicationsException ex) {
        log.warn("Job offer not accepting applications: {}", ex.getMessage());
        return buildProblem(HttpStatus.BAD_REQUEST, ex.getMessage(), "Not Accepting Applications", "job-offer-closed");
    }

    @ExceptionHandler(CandidatureNotWithdrawableException.class)
    public ProblemDetail handleCandidatureNotWithdrawable(CandidatureNotWithdrawableException ex) {
        log.warn("Candidature not withdrawable: {}", ex.getMessage());
        return buildProblem(HttpStatus.BAD_REQUEST, ex.getMessage(), "Cannot Withdraw", "candidature-not-withdrawable");
    }

    @ExceptionHandler(InvalidFileException.class)
    public ProblemDetail handleInvalidFile(InvalidFileException ex) {
        log.warn("Invalid file: {}", ex.getMessage());
        return buildProblem(HttpStatus.BAD_REQUEST, ex.getMessage(), "Invalid File", "invalid-file");
    }

    @ExceptionHandler(FileStorageException.class)
    public ProblemDetail handleFileStorage(FileStorageException ex) {
        log.error("File storage error: {}", ex.getMessage(), ex);
        return buildProblem(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to process file", "File Storage Error", "file-storage-error");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("File too large: {}", ex.getMessage());
        return buildProblem(HttpStatus.BAD_REQUEST, "File size exceeds the maximum allowed (5MB)", "File Too Large", "file-too-large");
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
