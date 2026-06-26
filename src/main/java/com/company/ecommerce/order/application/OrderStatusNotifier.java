package com.company.ecommerce.order.application;

import com.company.ecommerce.order.domain.Order;
import com.company.ecommerce.order.domain.OrderStatus;
import com.company.ecommerce.order.domain.event.OrderCancelledEvent;
import com.company.ecommerce.order.domain.event.OrderCompletedEvent;
import com.company.ecommerce.order.domain.event.OrderLine;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publishes the domain event (if any) that corresponds to an order reaching a new status. Shared by
 * the admin status-update use case and the {@code OrderLifecycle} {@code spi} so both emit the same
 * events regardless of which path drove the transition.
 */
@Component
@RequiredArgsConstructor
class OrderStatusNotifier {

    private final ApplicationEventPublisher eventPublisher;

    /** Publishes the event for {@code target}, if that status has cross-module consumers. */
    void publishFor(Order order, OrderStatus target) {
        switch (target) {
            case CANCELLED ->
                    eventPublisher.publishEvent(
                            new OrderCancelledEvent(
                                    order.getId(),
                                    order.getCustomerId(),
                                    order.getItems().stream()
                                            .map(item -> new OrderLine(item.getProductId(), item.getQuantity()))
                                            .toList()));
            case DELIVERED ->
                    eventPublisher.publishEvent(
                            new OrderCompletedEvent(order.getId(), order.getCustomerId()));
            default -> {
                // No cross-module side effects for the other transitions.
            }
        }
    }
}
