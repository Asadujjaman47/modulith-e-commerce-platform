package com.company.ecommerce.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.user.api.dto.UpdateProfileRequest;
import com.company.ecommerce.user.domain.Customer;
import com.company.ecommerce.user.domain.event.CustomerUpdatedEvent;
import com.company.ecommerce.user.infrastructure.mapper.CustomerMapper;
import com.company.ecommerce.user.infrastructure.persistence.CustomerRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class UpdateCustomerUseCaseTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private CustomerMapper customerMapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private UpdateCustomerUseCase useCase;

    @Test
    void updatesProfileAndPublishesEvent() {
        UUID userId = UUID.randomUUID();
        Customer customer = Customer.create(userId, "john@example.com", "John", "Doe");
        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(customer));

        useCase.update(userId, new UpdateProfileRequest("Jane", "Smith", "+14155552671"));

        assertThat(customer.getFirstName()).isEqualTo("Jane");
        assertThat(customer.getLastName()).isEqualTo("Smith");
        assertThat(customer.getPhone()).isEqualTo("+14155552671");
        verify(eventPublisher).publishEvent(any(CustomerUpdatedEvent.class));
        verify(customerMapper).toResponse(customer);
    }

    @Test
    void throwsWhenCustomerMissing() {
        UUID userId = UUID.randomUUID();
        when(customerRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                useCase.update(
                                        userId, new UpdateProfileRequest("Jane", "Smith", null)))
                .isInstanceOf(EntityNotFoundException.class);
    }
}