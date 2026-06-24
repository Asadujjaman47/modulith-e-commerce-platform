package com.company.ecommerce.auth.application;

import com.company.ecommerce.auth.api.dto.LogoutRequest;
import com.company.ecommerce.auth.infrastructure.persistence.RefreshTokenRepository;
import com.company.ecommerce.auth.infrastructure.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logs a user out by revoking the presented refresh token. Idempotent: unknown or already-revoked
 * tokens are silently ignored. The short-lived access token expires naturally.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogoutUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public void logout(LogoutRequest request) {
        String hash = tokenProvider.hashRefreshToken(request.refreshToken());
        refreshTokenRepository
                .findByTokenHash(hash)
                .ifPresent(
                        token -> {
                            token.revoke();
                            log.info("Refresh token revoked on logout. userId={}", token.getUserId());
                        });
    }
}