package com.company.ecommerce.user.application;

import com.company.ecommerce.user.domain.Address;
import com.company.ecommerce.user.domain.Customer;
import com.company.ecommerce.user.infrastructure.persistence.AddressRepository;
import com.company.ecommerce.user.infrastructure.persistence.CustomerRepository;
import com.company.ecommerce.user.spi.AddressView;
import com.company.ecommerce.user.spi.CustomerView;
import com.company.ecommerce.user.spi.UserQuery;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Default {@link UserQuery} implementation backed by the customer/address repositories. */
@Service
@RequiredArgsConstructor
public class UserQueryService implements UserQuery {

    private final CustomerRepository customerRepository;
    private final AddressRepository addressRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<AddressView> findAddress(UUID userId, UUID addressId) {
        return customerRepository
                .findByUserId(userId)
                .map(Customer::getId)
                .flatMap(customerId -> addressRepository.findByIdAndCustomerId(addressId, customerId))
                .map(UserQueryService::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerView> findCustomer(UUID userId) {
        return customerRepository.findByUserId(userId).map(UserQueryService::toView);
    }

    private static CustomerView toView(Customer customer) {
        String displayName =
                "%s %s".formatted(customer.getFirstName(), customer.getLastName()).trim();
        return new CustomerView(customer.getUserId(), displayName, customer.getEmail());
    }

    private static AddressView toView(Address address) {
        return new AddressView(
                address.getId(),
                address.getLabel(),
                address.getLine1(),
                address.getLine2(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry());
    }
}
