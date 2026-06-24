package com.company.ecommerce.user.application;

import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.user.api.dto.AddressResponse;
import com.company.ecommerce.user.api.dto.CreateAddressRequest;
import com.company.ecommerce.user.api.dto.UpdateAddressRequest;
import com.company.ecommerce.user.domain.Address;
import com.company.ecommerce.user.domain.Customer;
import com.company.ecommerce.user.domain.event.AddressAddedEvent;
import com.company.ecommerce.user.infrastructure.mapper.AddressMapper;
import com.company.ecommerce.user.infrastructure.persistence.AddressRepository;
import com.company.ecommerce.user.infrastructure.persistence.CustomerRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages the authenticated customer's addresses. Resolves the customer from the auth user id and
 * enforces the single-default-address invariant.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ManageAddressesUseCase {

    private final CustomerRepository customerRepository;
    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<AddressResponse> list(UUID userId) {
        UUID customerId = resolveCustomerId(userId);
        return addressMapper.toResponseList(
                addressRepository.findByCustomerIdOrderByCreatedAtAsc(customerId));
    }

    @Transactional
    public AddressResponse add(UUID userId, CreateAddressRequest request) {
        UUID customerId = resolveCustomerId(userId);
        boolean firstAddress =
                addressRepository.findByCustomerIdOrderByCreatedAtAsc(customerId).isEmpty();
        boolean makeDefault = request.defaultAddress() || firstAddress;
        if (makeDefault) {
            clearExistingDefaults(customerId);
        }

        Address address =
                addressRepository.save(
                        Address.create(
                                customerId,
                                request.label(),
                                request.line1(),
                                request.line2(),
                                request.city(),
                                request.state(),
                                request.postalCode(),
                                request.country(),
                                makeDefault));
        eventPublisher.publishEvent(new AddressAddedEvent(address.getId(), customerId));
        log.info("Address added. addressId={} customerId={}", address.getId(), customerId);
        return addressMapper.toResponse(address);
    }

    @Transactional
    public AddressResponse update(UUID userId, UUID addressId, UpdateAddressRequest request) {
        UUID customerId = resolveCustomerId(userId);
        Address address = requireAddress(addressId, customerId);

        if (request.defaultAddress() && !address.isDefaultAddress()) {
            clearExistingDefaults(customerId);
        }
        address.update(
                request.label(),
                request.line1(),
                request.line2(),
                request.city(),
                request.state(),
                request.postalCode(),
                request.country(),
                request.defaultAddress());
        log.info("Address updated. addressId={} customerId={}", addressId, customerId);
        return addressMapper.toResponse(address);
    }

    @Transactional
    public void delete(UUID userId, UUID addressId) {
        UUID customerId = resolveCustomerId(userId);
        Address address = requireAddress(addressId, customerId);
        addressRepository.delete(address);
        log.info("Address deleted. addressId={} customerId={}", addressId, customerId);
    }

    private void clearExistingDefaults(UUID customerId) {
        addressRepository
                .findByCustomerIdAndDefaultAddressTrue(customerId)
                .forEach(Address::clearDefault);
    }

    private Address requireAddress(UUID addressId, UUID customerId) {
        return addressRepository
                .findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new EntityNotFoundException("Address", addressId));
    }

    private UUID resolveCustomerId(UUID userId) {
        return customerRepository
                .findByUserId(userId)
                .map(Customer::getId)
                .orElseThrow(() -> new EntityNotFoundException("Customer", userId));
    }
}