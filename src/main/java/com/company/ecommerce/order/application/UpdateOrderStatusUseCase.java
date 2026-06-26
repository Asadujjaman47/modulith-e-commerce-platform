package com.company.ecommerce.order.application;

import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.order.api.dto.OrderResponse;
import com.company.ecommerce.order.domain.Order;
import com.company.ecommerce.order.domain.OrderStatus;
import com.company.ecommerce.order.domain.event.OrderCancelledEvent;
import com.company.ecommerce.order.domain.event.OrderCompletedEvent;
import com.company.ecommerce.order.infrastructure.mapper.OrderMapper;
import com.company.ecommerce.order.infrastructure.persistence.OrderRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin-driven order status transitions, guarded by the {@link OrderStatus} state machine. Publishes
 * {@link OrderCompletedEvent} on reaching {@code DELIVERED} and {@link OrderCancelledEvent} when an
 * admin cancels (so inventory releases the reserved stock).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateOrderStatusUseCase {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderStatusNotifier statusNotifier;

    @Transactional
    public OrderResponse updateStatus(UUID orderId, OrderStatus targetStatus) {
        Order order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(() -> new EntityNotFoundException("Order", orderId));

        order.transitionTo(targetStatus);
        statusNotifier.publishFor(order, targetStatus);

        log.info("Order status updated. orderId={} status={}", orderId, targetStatus);
        return orderMapper.toResponse(order);
    }
}
