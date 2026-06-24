package com.company.ecommerce.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ecommerce.user.domain.Customer;
import com.company.ecommerce.user.domain.event.CustomerCreatedEvent;
import com.company.ecommerce.user.infrastructure.persistence.CustomerRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CreateCustomerUseCaseTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private CreateCustomerUseCase useCase;

    @Test
    void createsCustomerAndPublishesEvent() {
        UUID userId = UUID.randomUUID();
        when(customerRepository.existsByUserId(userId)).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.create(userId, "john@example.com", "John", "Doe");

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getFirstName()).isEqualTo("John");
        verify(eventPublisher).publishEvent(any(CustomerCreatedEvent.class));
    }

    @Test
    void isIdempotentWhenCustomerExists() {
        UUID userId = UUID.randomUUID();
        when(customerRepository.existsByUserId(userId)).thenReturn(true);

        useCase.create(userId, "john@example.com", "John", "Doe");

        verify(customerRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}