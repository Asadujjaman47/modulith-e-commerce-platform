package com.company.ecommerce.auth.application;

import com.company.ecommerce.auth.api.dto.LoginRequest;
import com.company.ecommerce.auth.api.dto.TokenResponse;
import com.company.ecommerce.auth.domain.RefreshToken;
import com.company.ecommerce.auth.domain.UserCredential;
import com.company.ecommerce.auth.domain.event.UserLoggedInEvent;
import com.company.ecommerce.auth.infrastructure.persistence.RefreshTokenRepository;
import com.company.ecommerce.auth.infrastructure.persistence.UserCredentialRepository;
import com.company.ecommerce.auth.infrastructure.security.JwtTokenProvider;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Authenticates a user and issues a fresh access + refresh token pair. */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginUseCase {

    private static final String INVALID_CREDENTIALS = "Invalid email or password";

    private final UserCredentialRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public TokenResponse login(LoginRequest request) {
        UserCredential user =
                userRepository
                        .findByEmail(request.email().toLowerCase())
                        .orElseThrow(() -> new BadCredentialsException(INVALID_CREDENTIALS));

        if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        TokenResponse tokens = issueTokens(user);
        eventPublisher.publishEvent(
                new UserLoggedInEvent(user.getId(), user.getEmail(), Instant.now()));
        log.info("User logged in. userId={}", user.getId());
        return tokens;
    }

    private TokenResponse issueTokens(UserCredential user) {
        String accessToken =
                tokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String rawRefresh = tokenProvider.generateRefreshTokenValue();
        refreshTokenRepository.save(
                RefreshToken.issue(
                        user.getId(),
                        tokenProvider.hashRefreshToken(rawRefresh),
                        tokenProvider.refreshTokenExpiry()));
        return TokenResponse.bearer(accessToken, rawRefresh, tokenProvider.accessTokenTtlSeconds());
    }
}