package com.company.ecommerce.user.application;

import com.company.ecommerce.user.domain.Customer;
import com.company.ecommerce.user.domain.event.CustomerCreatedEvent;
import com.company.ecommerce.user.infrastructure.persistence.CustomerRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates a customer profile. Invoked when the {@code auth} module reports a new registration.
 * Idempotent: a profile is created only if one does not already exist for the user.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateCustomerUseCase {

    private final CustomerRepository customerRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void create(UUID userId, String email, String firstName, String lastName) {
        if (customerRepository.existsByUserId(userId)) {
            log.debug("Customer already exists for userId={}, skipping creation", userId);
            return;
        }

        Customer customer = customerRepository.save(Customer.create(userId, email, firstName, lastName));
        eventPublisher.publishEvent(
                new CustomerCreatedEvent(customer.getId(), userId, email));
        log.info("Customer profile created. customerId={} userId={}", customer.getId(), userId);
    }
}