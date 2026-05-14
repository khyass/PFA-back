package com.example.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;

/**
 * Provides fallback responses for circuit-broken gateway routes.
 *
 * <p>When a downstream service (auth-service, candidate-service, etc.) becomes
 * unresponsive or fails repeatedly, the Resilience4j circuit breaker configured
 * in {@code application.yml} opens and redirects traffic to one of these fallback
 * endpoints instead of forwarding to the (broken) upstream service.
 *
 * <p>This prevents cascade failures: rather than letting requests queue up and
 * time out, the gateway immediately returns a structured 503 response so the
 * Angular frontend can display a meaningful error message.
 *
 * <p>All responses follow the RFC 7807 ProblemDetail format to be consistent
 * with errors returned by the downstream services themselves.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    /**
     * Fallback for the auth-service route ({@code /auth/*}).
     *
     * <p>Invoked when the circuit breaker for {@code auth-service} is open, meaning
     * Keycloak or the auth-service has been repeatedly failing. The response body
     * includes the service name so the client knows which service is affected.
     *
     * @return HTTP 503 Service Unavailable with auth-service details
     */
    @GetMapping("/auth")
    public ProblemDetail authFallback() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Authentication service is temporarily unavailable. Please try again later."
        );
        problem.setTitle("Service Unavailable");
        problem.setType(URI.create("https://api.job-platform.com/errors/service-unavailable"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("service", "auth-service"); // identifies which service is down
        return problem;
    }

    /**
     * Generic fallback for any other service route (candidate, enterprise, jobs).
     *
     * <p>Invoked when the circuit breaker for any downstream service (other than auth)
     * is open. Returns a generic message because the gateway route configuration
     * maps all non-auth fallbacks to this single endpoint.
     *
     * @return HTTP 503 Service Unavailable
     */
    @GetMapping("/service")
    public ProblemDetail serviceFallback() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "The requested service is temporarily unavailable. Please try again later."
        );
        problem.setTitle("Service Unavailable");
        problem.setType(URI.create("https://api.job-platform.com/errors/service-unavailable"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
