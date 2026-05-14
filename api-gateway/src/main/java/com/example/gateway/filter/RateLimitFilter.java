package com.example.gateway.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Global gateway filter that enforces per-IP rate limiting using the
 * <a href="https://github.com/bucket4j/bucket4j">Bucket4j</a> token-bucket algorithm.
 *
 * <h3>How it works</h3>
 * <p>Each unique client IP gets its own {@link Bucket}. A bucket starts with
 * {@code burstCapacity} tokens and is refilled at a rate of {@code requestsPerSecond}
 * tokens per second. Each incoming request consumes one token. When the bucket is empty
 * (i.e. the client has exceeded the limit), the request is rejected immediately with
 * HTTP 429 Too Many Requests without forwarding it to any downstream service.
 *
 * <h3>IP resolution</h3>
 * <p>The filter checks the {@code X-Forwarded-For} header first (populated by reverse
 * proxies such as nginx or AWS ALB) before falling back to the direct remote address.
 * This ensures correct tracking when the gateway sits behind a load balancer.
 *
 * <h3>Storage</h3>
 * <p>Buckets are stored in a {@link ConcurrentHashMap} in memory. This is suitable for
 * a single-instance deployment. For multi-instance deployments, replace with a distributed
 * cache (e.g. Redis + bucket4j-redis).
 *
 * <h3>Configuration</h3>
 * <p>Rates are configurable via {@code application.yml}:
 * <pre>{@code
 * gateway:
 *   rate-limit:
 *     requests-per-second: 20
 *     burst-capacity: 40
 * }</pre>
 */
@Slf4j
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    /**
     * In-memory store of per-IP token buckets.
     * ConcurrentHashMap ensures thread-safe lazy creation of buckets
     * without the overhead of synchronised blocks for reads.
     */
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /** Steady-state refill rate: tokens added per second */
    @Value("${gateway.rate-limit.requests-per-second:20}")
    private int requestsPerSecond;

    /** Maximum tokens a bucket can hold (allows short bursts above the steady rate) */
    @Value("${gateway.rate-limit.burst-capacity:40}")
    private int burstCapacity;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientIp = resolveClientIp(exchange);

        // computeIfAbsent is atomic – only one bucket is created per IP even under concurrency
        Bucket bucket = buckets.computeIfAbsent(clientIp, this::createBucket);

        if (bucket.tryConsume(1)) {
            // Token available – allow the request to proceed to the next filter / downstream service
            return chain.filter(exchange);
        }

        // Bucket exhausted – reject the request immediately
        log.warn("Rate limit exceeded for IP: {}", clientIp);
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        // Tell the client how long to wait before retrying (simplified: 1 second)
        exchange.getResponse().getHeaders().add("X-Rate-Limit-Retry-After-Seconds", "1");
        return exchange.getResponse().setComplete();
    }

    /**
     * This filter must run before all other gateway filters (including routing)
     * so that rate-limited requests never reach downstream services.
     */
    @Override
    public int getOrder() {
        return -1; // Execute before other filters
    }

    /**
     * Creates a new Bucket4j token bucket for the given IP.
     *
     * <p>The bucket uses a "greedy" refill, meaning tokens are added continuously
     * (not in batches at the end of each second). This provides a smoother rate
     * limit experience compared to fixed-window approaches.
     *
     * @param key the client IP (used only as the map key; bucket itself is stateless)
     * @return a freshly created token bucket
     */
    private Bucket createBucket(String key) {
        Bandwidth limit = Bandwidth.classic(
                burstCapacity,                                              // max tokens in the bucket
                Refill.greedy(requestsPerSecond, Duration.ofSeconds(1))    // steady refill rate
        );
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Resolves the real client IP from the incoming request.
     *
     * <p>When the gateway is behind a reverse proxy, the original client IP is
     * passed in the {@code X-Forwarded-For} header as a comma-separated list.
     * The leftmost value is the original client; subsequent values are added by
     * each proxy in the chain.
     *
     * @param exchange the current server web exchange
     * @return the resolved client IP, or "unknown" if it cannot be determined
     */
    private String resolveClientIp(ServerWebExchange exchange) {
        // Prefer X-Forwarded-For (set by load balancers / reverse proxies)
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim(); // take the leftmost (original client) IP
        }

        // Fall back to the direct TCP connection's remote address
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress != null) {
            InetAddress address = remoteAddress.getAddress();
            if (address != null) {
                return address.getHostAddress();
            }
        }

        return "unknown";
    }
}
