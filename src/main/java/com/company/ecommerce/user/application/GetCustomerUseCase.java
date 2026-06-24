package com.company.ecommerce.user.application;

import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.user.api.dto.CustomerResponse;
import com.company.ecommerce.user.infrastructure.mapper.CustomerMapper;
import com.company.ecommerce.user.infrastructure.persistence.CustomerRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads the authenticated user's customer profile. */
@Service
@RequiredArgsConstructor
public class GetCustomerUseCase {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Transactional(readOnly = true)
    public CustomerResponse getByUserId(UUID userId) {
        return customerRepository
                .findByUserId(userId)
                .map(customerMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Customer", userId));
    }
}