package com.company.ecommerce.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.order.domain.Order;
import com.company.ecommerce.order.domain.OrderStatus;
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
class OrderLifecycleServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderStatusNotifier statusNotifier;
    @InjectMocks private OrderLifecycleService service;

    private final UUID orderId = UUID.randomUUID();

    private Order order() {
        Order order = Order.place("ORD-1", UUID.randomUUID(), "USD", null, BigDecimal.ZERO, null);
        order.addItem(UUID.randomUUID(), "Widget", new BigDecimal("10.00"), 1);
        order.recalculateTotals();
        return order;
    }

    @Test
    void markPaidMovesPendingToPaid() {
        Order order = order();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        service.markPaid(orderId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(statusNotifier).publishFor(order, OrderStatus.PAID);
    }

    @Test
    void markProcessingWalksThroughPaidWhenStillPending() {
        Order order = order();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        service.markProcessing(orderId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        verify(statusNotifier).publishFor(order, OrderStatus.PAID);
        verify(statusNotifier).publishFor(order, OrderStatus.PROCESSING);
    }

    @Test
    void markDeliveredWalksThroughShipped() {
        Order order = order();
        order.transitionTo(OrderStatus.PROCESSING);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        service.markDelivered(orderId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        verify(statusNotifier).publishFor(order, OrderStatus.SHIPPED);
        verify(statusNotifier).publishFor(order, OrderStatus.DELIVERED);
    }

    @Test
    void isNoOpWhenAlreadyPastTarget() {
        Order order = order();
        order.transitionTo(OrderStatus.PROCESSING);
        order.transitionTo(OrderStatus.SHIPPED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        service.markPaid(orderId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        verify(statusNotifier, never()).publishFor(any(), any());
    }

    @Test
    void isNoOpWhenOffHappyPath() {
        Order order = order();
        order.cancel();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        service.markProcessing(orderId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(statusNotifier, never()).publishFor(any(), any());
    }

    @Test
    void throwsWhenOrderNotFound() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markPaid(orderId))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
