package com.company.ecommerce.config.ratelimit;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalised rate-limiting configuration bound from {@code app.rate-limit.*}.
 *
 * <p>Two policies: a strict per-IP limit on the unauthenticated auth endpoints (login/register/
 * refresh) to blunt credential-stuffing, and a looser per-principal (or per-IP) limit on the rest of
 * the API.
 *
 * @param enabled master switch for the rate-limit filter
 * @param auth limit applied to {@code /api/v1/auth/**}
 * @param api limit applied to the remaining {@code /api/**}
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(Boolean enabled, Limit auth, Limit api) {

    public RateLimitProperties {
        if (enabled == null) {
            enabled = Boolean.TRUE;
        }
        if (auth == null) {
            auth = new Limit(10, Duration.ofMinutes(1));
        }
        if (api == null) {
            api = new Limit(100, Duration.ofMinutes(1));
        }
    }

    /**
     * A token-bucket limit: {@code capacity} tokens that refill fully over {@code refillPeriod}.
     */
    public record Limit(long capacity, Duration refillPeriod) {

        public Limit {
            if (capacity <= 0) {
                capacity = 100;
            }
            if (refillPeriod == null || refillPeriod.isZero() || refillPeriod.isNegative()) {
                refillPeriod = Duration.ofMinutes(1);
            }
        }
    }
}
