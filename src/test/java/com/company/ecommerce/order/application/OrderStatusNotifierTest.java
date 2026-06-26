package com.company.ecommerce.order.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.company.ecommerce.order.domain.Order;
import com.company.ecommerce.order.domain.OrderStatus;
import com.company.ecommerce.order.domain.event.OrderCancelledEvent;
import com.company.ecommerce.order.domain.event.OrderCompletedEvent;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class OrderStatusNotifierTest {

    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private OrderStatusNotifier notifier;

    private Order order() {
        Order order = Order.place("ORD-1", UUID.randomUUID(), "USD", null, BigDecimal.ZERO, null);
        order.addItem(UUID.randomUUID(), "Widget", new BigDecimal("10.00"), 1);
        order.recalculateTotals();
        return order;
    }

    @Test
    void publishesCompletedOnDelivered() {
        notifier.publishFor(order(), OrderStatus.DELIVERED);
        verify(eventPublisher).publishEvent(any(OrderCompletedEvent.class));
    }

    @Test
    void publishesCancelledOnCancelled() {
        notifier.publishFor(order(), OrderStatus.CANCELLED);
        verify(eventPublisher).publishEvent(any(OrderCancelledEvent.class));
    }

    @Test
    void publishesNothingForIntermediateStatuses() {
        notifier.publishFor(order(), OrderStatus.PROCESSING);
        verify(eventPublisher, never()).publishEvent(any());
    }
}
