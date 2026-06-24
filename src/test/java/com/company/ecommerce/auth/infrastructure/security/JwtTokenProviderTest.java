package com.company.ecommerce.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.ecommerce.auth.domain.Role;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String SECRET =
            "ZGV2LXNlY3JldC1jaGFuZ2UtbWUtYXQtbGVhc3QtMzItYnl0ZXMtbG9uZyE=";

    private final JwtTokenProvider provider =
            new JwtTokenProvider(
                    new JwtProperties(SECRET, "ecommerce", Duration.ofMinutes(15), Duration.ofDays(7)));

    @Test
    void generatesAndParsesAccessToken() {
        UUID userId = UUID.randomUUID();
        String token = provider.generateAccessToken(userId, "john@example.com", Role.CUSTOMER);

        var parsed = provider.parseAccessToken(token);

        assertThat(parsed).isPresent();
        assertThat(parsed.get().userId()).isEqualTo(userId);
        assertThat(parsed.get().email()).isEqualTo("john@example.com");
        assertThat(parsed.get().role()).isEqualTo(Role.CUSTOMER);
        assertThat(parsed.get().getName()).isEqualTo(userId.toString());
    }

    @Test
    void rejectsTamperedToken() {
        String token =
                provider.generateAccessToken(UUID.randomUUID(), "john@example.com", Role.CUSTOMER);

        assertThat(provider.parseAccessToken(token + "tampered")).isEmpty();
    }

    @Test
    void rejectsGarbageToken() {
        assertThat(provider.parseAccessToken("not-a-jwt")).isEmpty();
    }

    @Test
    void rejectsTokenSignedWithDifferentSecret() {
        JwtTokenProvider other =
                new JwtTokenProvider(
                        new JwtProperties(
                                "b3RoZXItc2VjcmV0LXRoYXQtaXMtYWxzby0zMi1ieXRlcy1sb25nIQ==",
                                "ecommerce",
                                Duration.ofMinutes(15),
                                Duration.ofDays(7)));
        String token =
                other.generateAccessToken(UUID.randomUUID(), "john@example.com", Role.CUSTOMER);

        assertThat(provider.parseAccessToken(token)).isEmpty();
    }

    @Test
    void hashesRefreshTokenDeterministically() {
        String raw = provider.generateRefreshTokenValue();

        assertThat(provider.hashRefreshToken(raw)).isEqualTo(provider.hashRefreshToken(raw));
        assertThat(provider.hashRefreshToken(raw)).isNotEqualTo(raw);
        assertThat(provider.hashRefreshToken("a")).isNotEqualTo(provider.hashRefreshToken("b"));
    }

    @Test
    void exposesAccessTokenTtlInSeconds() {
        assertThat(provider.accessTokenTtlSeconds()).isEqualTo(900);
    }
}