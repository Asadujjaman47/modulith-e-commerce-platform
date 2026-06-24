package com.company.ecommerce.auth.application;

import com.company.ecommerce.auth.api.dto.RegisterRequest;
import com.company.ecommerce.auth.api.dto.RegisterResponse;
import com.company.ecommerce.auth.domain.Role;
import com.company.ecommerce.auth.domain.UserCredential;
import com.company.ecommerce.auth.domain.event.UserRegisteredEvent;
import com.company.ecommerce.auth.infrastructure.persistence.UserCredentialRepository;
import com.company.ecommerce.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registers a new customer account: persists hashed credentials and publishes
 * {@link UserRegisteredEvent} so the {@code user} module can create the matching profile.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterUserUseCase {

    private final UserCredentialRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = request.email().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Email already registered: " + email);
        }

        UserCredential user =
                UserCredential.register(
                        email, passwordEncoder.encode(request.password()), Role.CUSTOMER);
        userRepository.save(user);

        eventPublisher.publishEvent(
                new UserRegisteredEvent(
                        user.getId(), email, request.firstName(), request.lastName()));

        log.info("User registered. userId={}", user.getId());
        return new RegisterResponse(user.getId(), email);
    }
}