package com.company.ecommerce.auth.application;

import com.company.ecommerce.auth.api.dto.RefreshTokenRequest;
import com.company.ecommerce.auth.api.dto.TokenResponse;
import com.company.ecommerce.auth.domain.RefreshToken;
import com.company.ecommerce.auth.domain.UserCredential;
import com.company.ecommerce.auth.infrastructure.persistence.RefreshTokenRepository;
import com.company.ecommerce.auth.infrastructure.persistence.UserCredentialRepository;
import com.company.ecommerce.auth.infrastructure.security.JwtTokenProvider;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exchanges a valid refresh token for a new access + refresh token pair. Implements rotation: the
 * presented refresh token is revoked and a new one issued.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenUseCase {

    private static final String INVALID_TOKEN = "Invalid or expired refresh token";

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserCredentialRepository userRepository;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        String hash = tokenProvider.hashRefreshToken(request.refreshToken());
        RefreshToken stored =
                refreshTokenRepository
                        .findByTokenHash(hash)
                        .orElseThrow(() -> new BadCredentialsException(INVALID_TOKEN));

        if (!stored.isActive(Instant.now())) {
            throw new BadCredentialsException(INVALID_TOKEN);
        }

        UserCredential user =
                userRepository
                        .findById(stored.getUserId())
                        .filter(UserCredential::isEnabled)
                        .orElseThrow(() -> new BadCredentialsException(INVALID_TOKEN));

        // Rotate: revoke the presented token and issue a new pair.
        stored.revoke();

        String accessToken =
                tokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String rawRefresh = tokenProvider.generateRefreshTokenValue();
        refreshTokenRepository.save(
                RefreshToken.issue(
                        user.getId(),
                        tokenProvider.hashRefreshToken(rawRefresh),
                        tokenProvider.refreshTokenExpiry()));

        log.info("Refresh token rotated. userId={}", user.getId());
        return TokenResponse.bearer(accessToken, rawRefresh, tokenProvider.accessTokenTtlSeconds());
    }
}