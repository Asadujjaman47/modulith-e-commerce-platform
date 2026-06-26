package com.company.ecommerce.order.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.order.domain.Order;
import com.company.ecommerce.order.infrastructure.mapper.OrderMapper;
import com.company.ecommerce.order.infrastructure.persistence.OrderRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetOrderUseCaseTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderMapper orderMapper;
    @InjectMocks private GetOrderUseCase useCase;

    private final UUID customerId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    private Order order() {
        Order order = Order.place("ORD-1", customerId, "USD", null, BigDecimal.ZERO, null);
        order.addItem(UUID.randomUUID(), "Widget", new BigDecimal("10.00"), 1);
        order.recalculateTotals();
        return order;
    }

    @Test
    void returnsOwnOrder() {
        Order order = order();
        when(orderRepository.findByIdAndCustomerId(orderId, customerId))
                .thenReturn(Optional.of(order));

        useCase.getForCustomer(customerId, orderId);

        verify(orderMapper).toResponse(order);
    }

    @Test
    void throwsWhenNotOwnedOrMissing() {
        when(orderRepository.findByIdAndCustomerId(orderId, customerId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getForCustomer(customerId, orderId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void adminGetByIdReturnsOrder() {
        Order order = order();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        useCase.getById(orderId);

        verify(orderMapper).toResponse(order);
    }

    @Test
    void adminGetByIdThrowsWhenMissing() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getById(orderId))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
