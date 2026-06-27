package com.company.ecommerce.config.ratelimit;

import java.io.IOException;

import com.company.ecommerce.common.api.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Token-bucket rate limiter for {@code /api/**}. Auth endpoints are limited per client IP (strict);
 * the remaining API is limited per authenticated principal, or per IP when anonymous. Exceeding a
 * limit yields HTTP 429 with the standard error envelope and a {@code Retry-After} header.
 *
 * <p>Runs after Spring Security (so the principal is available) but before the controllers. The
 * limiter fails open: if the backing store errors, the request is allowed through rather than
 * blocked.
 */
@Slf4j
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String AUTH_PREFIX = "/api/v1/auth/";

    private final RateLimitService rateLimitService;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only guard the API; static/actuator/swagger traffic is out of scope.
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        boolean authEndpoint = request.getRequestURI().startsWith(AUTH_PREFIX);
        RateLimitProperties.Limit limit = authEndpoint ? properties.auth() : properties.api();
        String key = resolveKey(request, authEndpoint);

        ConsumptionProbe probe;
        try {
            Bucket bucket = rateLimitService.resolveBucket(key, limit);
            probe = bucket.tryConsumeAndReturnRemaining(1);
        } catch (RuntimeException ex) {
            // Fail open: a rate-limiter backend outage must not take down the API.
            log.warn("Rate limit check failed for {}; allowing request", key, ex);
            filterChain.doFilter(request, response);
            return;
        }

        if (probe.isConsumed()) {
            response.setHeader("X-RateLimit-Remaining", Long.toString(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000L);
        log.warn("Rate limit exceeded for {} on {}", key, request.getRequestURI());
        writeTooManyRequests(response, retryAfterSeconds);
    }

    private String resolveKey(HttpServletRequest request, boolean authEndpoint) {
        if (authEndpoint) {
            return "auth:" + clientIp(request);
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            return "api:user:" + auth.getName();
        }
        return "api:ip:" + clientIp(request);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // First hop is the originating client when behind a trusted proxy.
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response, long retryAfterSeconds)
            throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
        objectMapper.writeValue(
                response.getWriter(),
                ErrorResponse.of("Too many requests. Please retry later."));
    }
}
