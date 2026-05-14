package com.example.authservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that enriches the SLF4J Mapped Diagnostic Context (MDC)
 * with user-specific information extracted from the authenticated JWT.
 *
 * <p>By placing {@code userId} and {@code userEmail} in the MDC before each
 * request is processed, every log statement emitted during that request will
 * automatically include these fields. This makes it straightforward to:
 * <ul>
 *   <li>Correlate all log lines produced for a single user request.</li>
 *   <li>Filter logs by user in log aggregation systems (ELK, Grafana Loki).</li>
 *   <li>Combine with Micrometer's {@code traceId}/{@code spanId} for full
 *       distributed tracing context.</li>
 * </ul>
 *
 * <p>The filter runs once per request ({@link OncePerRequestFilter}) and always
 * cleans up the MDC in the {@code finally} block to prevent context leaking
 * between requests in thread-pool environments.
 *
 * <p>The filter is ordered just after the highest precedence to ensure the
 * Security context is populated (by Spring Security filters) before this filter
 * tries to read the authenticated principal.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class MdcLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // Retrieve the current authentication object from the Security context.
            // At this point Spring Security has already validated the JWT (if present).
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();

                // The JWT subject ("sub" claim) is the Keycloak user UUID.
                // Storing it in MDC lets log appenders (e.g. Logstash encoder) include it automatically.
                MDC.put("userId", jwt.getSubject());

                // The email claim is optional – only add it if present to avoid NPE
                String email = jwt.getClaim("email");
                if (email != null) {
                    MDC.put("userEmail", email);
                }
            }

            // Proceed with the rest of the filter chain (controllers, etc.)
            filterChain.doFilter(request, response);

        } finally {
            // Always remove MDC entries after the request completes.
            // Failing to do so would cause data from one request to appear in
            // unrelated subsequent requests on the same thread (thread-pool reuse).
            MDC.remove("userId");
            MDC.remove("userEmail");
        }
    }
}
