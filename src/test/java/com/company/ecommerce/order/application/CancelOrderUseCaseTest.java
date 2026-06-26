package com.company.ecommerce.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ecommerce.common.exception.BusinessException;
import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.order.domain.Order;
import com.company.ecommerce.order.domain.OrderStatus;
import com.company.ecommerce.order.domain.event.OrderCancelledEvent;
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
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CancelOrderUseCaseTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderMapper orderMapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private CancelOrderUseCase useCase;

    private final UUID customerId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    private Order order() {
        Order order = Order.place("ORD-1", customerId, "USD", null, BigDecimal.ZERO, null);
        order.addItem(UUID.randomUUID(), "Widget", new BigDecimal("10.00"), 2);
        order.recalculateTotals();
        return order;
    }

    @Test
    void cancelsOwnPendingOrder() {
        Order order = order();
        when(orderRepository.findByIdAndCustomerId(orderId, customerId))
                .thenReturn(Optional.of(order));

        useCase.cancel(customerId, orderId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(eventPublisher).publishEvent(any(OrderCancelledEvent.class));
    }

    @Test
    void throwsWhenOrderNotFound() {
        when(orderRepository.findByIdAndCustomerId(orderId, customerId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.cancel(customerId, orderId))
                .isInstanceOf(EntityNotFoundException.class);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void throwsWhenNotCancellable() {
        Order order = order();
        order.transitionTo(OrderStatus.PROCESSING);
        order.transitionTo(OrderStatus.SHIPPED);
        when(orderRepository.findByIdAndCustomerId(orderId, customerId))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() -> useCase.cancel(customerId, orderId))
                .isInstanceOf(BusinessException.class);
        verify(eventPublisher, never()).publishEvent(any());
    }
}
