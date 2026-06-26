package com.company.ecommerce.order.application;

import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.order.domain.Order;
import com.company.ecommerce.order.domain.OrderStatus;
import com.company.ecommerce.order.infrastructure.persistence.OrderRepository;
import com.company.ecommerce.order.spi.OrderLifecycle;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link OrderLifecycle} implementation used by {@code payment} and {@code shipment} to drive
 * the order forward through its status lifecycle.
 *
 * <p>Transitions are "ensure at least": the order is walked one step at a time along the canonical
 * happy path up to the requested status, and left untouched if it is already at/past that status or
 * has left the path (e.g. {@code CANCELLED}). Walking — rather than a single hard-coded hop — makes
 * the order tolerant of the async, unordered events that drive it (e.g. shipment's
 * {@code markProcessing} arriving before payment's {@code markPaid}). Each applied step publishes its
 * event via the shared {@link OrderStatusNotifier}, so reaching {@code DELIVERED} still emits
 * {@code OrderCompletedEvent} exactly like the admin status endpoint.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderLifecycleService implements OrderLifecycle {

    /** Canonical forward path an order follows through fulfilment. */
    private static final List<OrderStatus> HAPPY_PATH =
            List.of(
                    OrderStatus.PENDING,
                    OrderStatus.PAID,
                    OrderStatus.PROCESSING,
                    OrderStatus.SHIPPED,
                    OrderStatus.DELIVERED);

    private final OrderRepository orderRepository;
    private final OrderStatusNotifier statusNotifier;

    @Override
    @Transactional
    public void markPaid(UUID orderId) {
        advanceTo(orderId, OrderStatus.PAID);
    }

    @Override
    @Transactional
    public void markProcessing(UUID orderId) {
        advanceTo(orderId, OrderStatus.PROCESSING);
    }

    @Override
    @Transactional
    public void markDelivered(UUID orderId) {
        advanceTo(orderId, OrderStatus.DELIVERED);
    }

    private void advanceTo(UUID orderId, OrderStatus target) {
        Order order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(() -> new EntityNotFoundException("Order", orderId));

        int currentIndex = HAPPY_PATH.indexOf(order.getStatus());
        int targetIndex = HAPPY_PATH.indexOf(target);
        if (currentIndex < 0) {
            log.debug(
                    "Skipping order {} advance to {}: status {} is off the happy path.",
                    orderId,
                    target,
                    order.getStatus());
            return;
        }
        if (currentIndex >= targetIndex) {
            return; // Already at or past the requested status.
        }

        for (int i = currentIndex; i < targetIndex; i++) {
            OrderStatus next = HAPPY_PATH.get(i + 1);
            order.transitionTo(next);
            statusNotifier.publishFor(order, next);
        }
        log.info("Order {} advanced to {} via lifecycle spi.", orderId, target);
    }
}
