package com.company.ecommerce.auth.infrastructure.security;

import com.company.ecommerce.auth.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Issues and validates signed JWT access tokens, and generates/hashes opaque refresh token values.
 *
 * <p>Access tokens are stateless HMAC-SHA256 JWTs carrying the user id (subject), email and role.
 * Refresh tokens are opaque random strings; only their SHA-256 hash is persisted (see
 * {@code RefreshToken}).
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey signingKey;
    private final String issuer;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;
    private final SecureRandom secureRandom = new SecureRandom();

    public JwtTokenProvider(JwtProperties properties) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
        this.issuer = properties.issuer();
        this.accessTokenTtl = properties.accessTokenTtl();
        this.refreshTokenTtl = properties.refreshTokenTtl();
    }

    /** Generates a signed access token for the given user. */
    public String generateAccessToken(UUID userId, String email, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_ROLE, role.name())
                .issuedAt(java.util.Date.from(now))
                .expiration(java.util.Date.from(now.plus(accessTokenTtl)))
                .signWith(signingKey)
                .compact();
    }

    /** Lifetime of access tokens in seconds (the {@code expiresIn} field of the token response). */
    public long accessTokenTtlSeconds() {
        return accessTokenTtl.toSeconds();
    }

    /** Parses and validates an access token, returning the authenticated user when valid. */
    public Optional<AuthenticatedUser> parseAccessToken(String token) {
        try {
            Claims claims =
                    Jwts.parser()
                            .verifyWith(signingKey)
                            .requireIssuer(issuer)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();
            return Optional.of(
                    new AuthenticatedUser(
                            UUID.fromString(claims.getSubject()),
                            claims.get(CLAIM_EMAIL, String.class),
                            Role.valueOf(claims.get(CLAIM_ROLE, String.class))));
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Rejected access token: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    /** Generates a new opaque refresh token value (returned to the client, never persisted raw). */
    public String generateRefreshTokenValue() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Deterministic SHA-256 hash used to store/look up refresh tokens. */
    public String hashRefreshToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    public Instant refreshTokenExpiry() {
        return Instant.now().plus(refreshTokenTtl);
    }
}