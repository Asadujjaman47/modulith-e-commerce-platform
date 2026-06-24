package com.company.ecommerce.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.company.ecommerce.auth.api.dto.LogoutRequest;
import com.company.ecommerce.auth.domain.RefreshToken;
import com.company.ecommerce.auth.infrastructure.persistence.RefreshTokenRepository;
import com.company.ecommerce.auth.infrastructure.security.JwtTokenProvider;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LogoutUseCaseTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtTokenProvider tokenProvider;
    @InjectMocks private LogoutUseCase useCase;

    @Test
    void revokesExistingToken() {
        RefreshToken stored =
                RefreshToken.issue(UUID.randomUUID(), "hash", Instant.now().plusSeconds(3600));
        when(tokenProvider.hashRefreshToken("raw")).thenReturn("hash");
        when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(stored));

        useCase.logout(new LogoutRequest("raw"));

        assertThat(stored.isRevoked()).isTrue();
    }

    @Test
    void isIdempotentWhenTokenUnknown() {
        when(tokenProvider.hashRefreshToken("raw")).thenReturn("hash");
        when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.empty());

        useCase.logout(new LogoutRequest("raw")); // no exception
    }
}