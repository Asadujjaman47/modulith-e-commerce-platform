package com.company.ecommerce.user.application;

import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.user.api.dto.CustomerResponse;
import com.company.ecommerce.user.api.dto.UpdateProfileRequest;
import com.company.ecommerce.user.domain.Customer;
import com.company.ecommerce.user.domain.event.CustomerUpdatedEvent;
import com.company.ecommerce.user.infrastructure.mapper.CustomerMapper;
import com.company.ecommerce.user.infrastructure.persistence.CustomerRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Updates the authenticated user's customer profile. */
@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateCustomerUseCase {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CustomerResponse update(UUID userId, UpdateProfileRequest request) {
        Customer customer =
                customerRepository
                        .findByUserId(userId)
                        .orElseThrow(() -> new EntityNotFoundException("Customer", userId));

        customer.updateProfile(request.firstName(), request.lastName(), request.phone());
        eventPublisher.publishEvent(new CustomerUpdatedEvent(customer.getId(), userId));
        log.info("Customer profile updated. customerId={}", customer.getId());
        return customerMapper.toResponse(customer);
    }
}