package com.company.ecommerce.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ecommerce.auth.api.dto.LoginRequest;
import com.company.ecommerce.auth.api.dto.TokenResponse;
import com.company.ecommerce.auth.domain.RefreshToken;
import com.company.ecommerce.auth.domain.Role;
import com.company.ecommerce.auth.domain.UserCredential;
import com.company.ecommerce.auth.domain.event.UserLoggedInEvent;
import com.company.ecommerce.auth.infrastructure.persistence.RefreshTokenRepository;
import com.company.ecommerce.auth.infrastructure.persistence.UserCredentialRepository;
import com.company.ecommerce.auth.infrastructure.security.JwtTokenProvider;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    @Mock private UserCredentialRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider tokenProvider;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private LoginUseCase useCase;

    private UserCredential enabledUser() {
        return UserCredential.register("john@example.com", "hashed", Role.CUSTOMER);
    }

    @Test
    void issuesTokensOnValidCredentials() {
        UserCredential user = enabledUser();
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "hashed")).thenReturn(true);
        when(tokenProvider.generateAccessToken(user.getId(), "john@example.com", Role.CUSTOMER))
                .thenReturn("access-token");
        when(tokenProvider.generateRefreshTokenValue()).thenReturn("raw-refresh");
        when(tokenProvider.hashRefreshToken("raw-refresh")).thenReturn("hashed-refresh");
        when(tokenProvider.refreshTokenExpiry()).thenReturn(Instant.now().plusSeconds(3600));
        when(tokenProvider.accessTokenTtlSeconds()).thenReturn(900L);

        TokenResponse response = useCase.login(new LoginRequest("john@example.com", "Password123!"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("raw-refresh");
        assertThat(response.expiresIn()).isEqualTo(900);
        assertThat(response.tokenType()).isEqualTo("Bearer");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(eventPublisher).publishEvent(any(UserLoggedInEvent.class));
    }

    @Test
    void rejectsUnknownEmail() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.login(new LoginRequest("john@example.com", "Password123!")))
                .isInstanceOf(BadCredentialsException.class);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void rejectsWrongPassword() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(enabledUser()));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> useCase.login(new LoginRequest("john@example.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void rejectsDisabledUser() {
        UserCredential user = enabledUser();
        user.disable();
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> useCase.login(new LoginRequest("john@example.com", "Password123!")))
                .isInstanceOf(BadCredentialsException.class);
    }
}