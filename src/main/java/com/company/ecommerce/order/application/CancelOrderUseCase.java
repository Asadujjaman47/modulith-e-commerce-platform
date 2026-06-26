package com.company.ecommerce.order.application;

import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.order.api.dto.OrderResponse;
import com.company.ecommerce.order.domain.Order;
import com.company.ecommerce.order.domain.event.OrderCancelledEvent;
import com.company.ecommerce.order.domain.event.OrderLine;
import com.company.ecommerce.order.infrastructure.mapper.OrderMapper;
import com.company.ecommerce.order.infrastructure.persistence.OrderRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cancels one of the customer's own orders, if its current status allows it, and publishes
 * {@link OrderCancelledEvent} so inventory releases the stock reserved for the order.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CancelOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public OrderResponse cancel(UUID customerId, UUID orderId) {
        Order order =
                orderRepository
                        .findByIdAndCustomerId(orderId, customerId)
                        .orElseThrow(() -> new EntityNotFoundException("Order", orderId));

        order.cancel();

        eventPublisher.publishEvent(
                new OrderCancelledEvent(
                        order.getId(),
                        customerId,
                        order.getItems().stream()
                                .map(item -> new OrderLine(item.getProductId(), item.getQuantity()))
                                .toList()));
        log.info("Order cancelled. orderId={} customerId={}", orderId, customerId);
        return orderMapper.toResponse(order);
    }
}
