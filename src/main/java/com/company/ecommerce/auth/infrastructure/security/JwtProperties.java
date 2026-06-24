package com.company.ecommerce.auth.infrastructure.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalised JWT configuration bound from {@code app.jwt.*}.
 *
 * @param secret base64-encoded HMAC-SHA256 signing secret (>= 256 bits)
 * @param issuer token issuer claim
 * @param accessTokenTtl lifetime of access tokens
 * @param refreshTokenTtl lifetime of refresh tokens
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret, String issuer, Duration accessTokenTtl, Duration refreshTokenTtl) {}