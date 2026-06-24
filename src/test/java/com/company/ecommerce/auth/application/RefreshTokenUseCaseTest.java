package com.company.ecommerce.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ecommerce.auth.api.dto.RefreshTokenRequest;
import com.company.ecommerce.auth.api.dto.TokenResponse;
import com.company.ecommerce.auth.domain.RefreshToken;
import com.company.ecommerce.auth.domain.Role;
import com.company.ecommerce.auth.domain.UserCredential;
import com.company.ecommerce.auth.infrastructure.persistence.RefreshTokenRepository;
import com.company.ecommerce.auth.infrastructure.persistence.UserCredentialRepository;
import com.company.ecommerce.auth.infrastructure.security.JwtTokenProvider;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

@ExtendWith(MockitoExtension.class)
class RefreshTokenUseCaseTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserCredentialRepository userRepository;
    @Mock private JwtTokenProvider tokenProvider;
    @InjectMocks private RefreshTokenUseCase useCase;

    @Test
    void rotatesTokenOnValidRefresh() {
        UUID userId = UUID.randomUUID();
        RefreshToken stored =
                RefreshToken.issue(userId, "stored-hash", Instant.now().plusSeconds(3600));
        UserCredential user = UserCredential.register("john@example.com", "hashed", Role.CUSTOMER);

        when(tokenProvider.hashRefreshToken("raw-refresh")).thenReturn("stored-hash");
        when(refreshTokenRepository.findByTokenHash("stored-hash")).thenReturn(Optional.of(stored));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenProvider.generateAccessToken(user.getId(), "john@example.com", Role.CUSTOMER))
                .thenReturn("new-access");
        when(tokenProvider.generateRefreshTokenValue()).thenReturn("new-raw-refresh");
        when(tokenProvider.hashRefreshToken("new-raw-refresh")).thenReturn("new-hash");
        when(tokenProvider.refreshTokenExpiry()).thenReturn(Instant.now().plusSeconds(3600));
        when(tokenProvider.accessTokenTtlSeconds()).thenReturn(900L);

        TokenResponse response = useCase.refresh(new RefreshTokenRequest("raw-refresh"));

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-raw-refresh");
        assertThat(stored.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void rejectsUnknownToken() {
        when(tokenProvider.hashRefreshToken("raw")).thenReturn("hash");
        when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.refresh(new RefreshTokenRequest("raw")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void rejectsRevokedToken() {
        RefreshToken stored =
                RefreshToken.issue(
                        UUID.randomUUID(), "hash", Instant.now().plusSeconds(3600));
        stored.revoke();
        when(tokenProvider.hashRefreshToken("raw")).thenReturn("hash");
        when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> useCase.refresh(new RefreshTokenRequest("raw")))
                .isInstanceOf(BadCredentialsException.class);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void rejectsExpiredToken() {
        RefreshToken stored =
                RefreshToken.issue(
                        UUID.randomUUID(), "hash", Instant.now().minusSeconds(10));
        when(tokenProvider.hashRefreshToken("raw")).thenReturn("hash");
        when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> useCase.refresh(new RefreshTokenRequest("raw")))
                .isInstanceOf(BadCredentialsException.class);
    }
}