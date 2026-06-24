package com.company.ecommerce.user.application;

import com.company.ecommerce.auth.domain.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Reacts to {@link UserRegisteredEvent} from the {@code auth} module by creating the customer
 * profile. Runs in its own transaction after the registration commits; the Spring Modulith event
 * publication registry guarantees the event is retried if processing fails.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredListener {

    private final CreateCustomerUseCase createCustomerUseCase;

    @ApplicationModuleListener
    public void on(UserRegisteredEvent event) {
        log.debug("Handling UserRegisteredEvent for userId={}", event.userId());
        createCustomerUseCase.create(
                event.userId(), event.email(), event.firstName(), event.lastName());
    }
}