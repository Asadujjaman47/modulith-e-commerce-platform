package com.company.ecommerce.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ecommerce.auth.api.dto.RegisterRequest;
import com.company.ecommerce.auth.api.dto.RegisterResponse;
import com.company.ecommerce.auth.domain.Role;
import com.company.ecommerce.auth.domain.UserCredential;
import com.company.ecommerce.auth.domain.event.UserRegisteredEvent;
import com.company.ecommerce.auth.infrastructure.persistence.UserCredentialRepository;
import com.company.ecommerce.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseTest {

    @Mock private UserCredentialRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private RegisterUserUseCase useCase;

    @Test
    void registersNewUserAndPublishesEvent() {
        RegisterRequest request =
                new RegisterRequest("John@Example.com", "Password123!", "John", "Doe");
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed");
        when(userRepository.save(any(UserCredential.class))).thenAnswer(inv -> inv.getArgument(0));

        RegisterResponse response = useCase.register(request);

        assertThat(response.email()).isEqualTo("john@example.com");
        assertThat(response.userId()).isNotNull();

        ArgumentCaptor<UserCredential> savedCaptor = ArgumentCaptor.forClass(UserCredential.class);
        verify(userRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getEmail()).isEqualTo("john@example.com");
        assertThat(savedCaptor.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(savedCaptor.getValue().getRole()).isEqualTo(Role.CUSTOMER);

        ArgumentCaptor<UserRegisteredEvent> eventCaptor =
                ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().email()).isEqualTo("john@example.com");
        assertThat(eventCaptor.getValue().firstName()).isEqualTo("John");
        assertThat(eventCaptor.getValue().lastName()).isEqualTo("Doe");
    }

    @Test
    void rejectsDuplicateEmail() {
        RegisterRequest request =
                new RegisterRequest("john@example.com", "Password123!", "John", "Doe");
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> useCase.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already registered");

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }
}