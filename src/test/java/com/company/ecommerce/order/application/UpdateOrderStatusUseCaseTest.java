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
import com.company.ecommerce.order.domain.event.OrderCompletedEvent;
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
class UpdateOrderStatusUseCaseTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderMapper orderMapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private UpdateOrderStatusUseCase useCase;

    private final UUID orderId = UUID.randomUUID();

    private Order order() {
        Order order =
                Order.place("ORD-1", UUID.randomUUID(), "USD", null, BigDecimal.ZERO, null);
        order.addItem(UUID.randomUUID(), "Widget", new BigDecimal("10.00"), 1);
        order.recalculateTotals();
        return order;
    }

    @Test
    void appliesLegalTransitionWithoutEvent() {
        Order order = order();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        useCase.updateStatus(orderId, OrderStatus.PROCESSING);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void publishesCompletedOnDelivered() {
        Order order = order();
        order.transitionTo(OrderStatus.PROCESSING);
        order.transitionTo(OrderStatus.SHIPPED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        useCase.updateStatus(orderId, OrderStatus.DELIVERED);

        verify(eventPublisher).publishEvent(any(OrderCompletedEvent.class));
    }

    @Test
    void publishesCancelledOnAdminCancel() {
        Order order = order();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        useCase.updateStatus(orderId, OrderStatus.CANCELLED);

        verify(eventPublisher).publishEvent(any(OrderCancelledEvent.class));
    }

    @Test
    void rejectsIllegalTransition() {
        Order order = order();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> useCase.updateStatus(orderId, OrderStatus.DELIVERED))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void throwsWhenOrderNotFound() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.updateStatus(orderId, OrderStatus.PROCESSING))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
