package com.company.ecommerce.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.user.api.dto.CreateAddressRequest;
import com.company.ecommerce.user.api.dto.UpdateAddressRequest;
import com.company.ecommerce.user.domain.Address;
import com.company.ecommerce.user.domain.Customer;
import com.company.ecommerce.user.infrastructure.mapper.AddressMapper;
import com.company.ecommerce.user.infrastructure.persistence.AddressRepository;
import com.company.ecommerce.user.infrastructure.persistence.CustomerRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ManageAddressesUseCaseTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private AddressMapper addressMapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private ManageAddressesUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = Customer.create(userId, "john@example.com", "John", "Doe");
        lenient().when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(customer));
        lenient().when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private CreateAddressRequest createRequest(boolean asDefault) {
        return new CreateAddressRequest(
                "Home", "221B Baker St", null, "London", null, "NW1", "GB", asDefault);
    }

    @Test
    void firstAddressBecomesDefaultEvenIfNotRequested() {
        when(addressRepository.findByCustomerIdOrderByCreatedAtAsc(customer.getId()))
                .thenReturn(List.of());

        useCase.add(userId, createRequest(false));

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(captor.capture());
        assertThat(captor.getValue().isDefaultAddress()).isTrue();
    }

    @Test
    void newDefaultAddressClearsPreviousDefault() {
        Address existing =
                Address.create(
                        customer.getId(), "Old", "1 Old St", null, "London", null, "E1", "GB", true);
        when(addressRepository.findByCustomerIdOrderByCreatedAtAsc(customer.getId()))
                .thenReturn(List.of(existing));
        when(addressRepository.findByCustomerIdAndDefaultAddressTrue(customer.getId()))
                .thenReturn(List.of(existing));

        useCase.add(userId, createRequest(true));

        assertThat(existing.isDefaultAddress()).isFalse();
    }

    @Test
    void updateThrowsWhenAddressMissing() {
        UUID addressId = UUID.randomUUID();
        when(addressRepository.findByIdAndCustomerId(addressId, customer.getId()))
                .thenReturn(Optional.empty());
        UpdateAddressRequest request =
                new UpdateAddressRequest(
                        "Home", "221B Baker St", null, "London", null, "NW1", "GB", false);

        assertThatThrownBy(() -> useCase.update(userId, addressId, request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deleteThrowsWhenAddressMissing() {
        UUID addressId = UUID.randomUUID();
        when(addressRepository.findByIdAndCustomerId(addressId, customer.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.delete(userId, addressId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void operationsThrowWhenCustomerMissing() {
        UUID unknownUser = UUID.randomUUID();
        when(customerRepository.findByUserId(unknownUser)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.list(unknownUser))
                .isInstanceOf(EntityNotFoundException.class);
    }
}