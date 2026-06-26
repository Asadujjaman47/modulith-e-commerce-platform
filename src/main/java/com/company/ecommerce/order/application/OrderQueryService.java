package com.company.ecommerce.order.application;

import com.company.ecommerce.order.domain.Order;
import com.company.ecommerce.order.domain.OrderAddress;
import com.company.ecommerce.order.infrastructure.persistence.OrderRepository;
import com.company.ecommerce.order.spi.OrderQuery;
import com.company.ecommerce.order.spi.OrderView;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link OrderQuery} implementation. Maps the order aggregate to a flat {@link OrderView} so
 * other modules ({@code payment}, {@code shipment}) read what they need by value without touching the
 * order aggregate.
 */
@Service
@RequiredArgsConstructor
public class OrderQueryService implements OrderQuery {

    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderView> findById(UUID orderId) {
        return orderRepository.findById(orderId).map(OrderQueryService::toView);
    }

    private static OrderView toView(Order order) {
        OrderAddress address = order.getShippingAddress();
        return new OrderView(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerId(),
                order.getStatus().name(),
                order.getCurrency(),
                order.getTotalAmount(),
                address == null ? null : address.getLabel(),
                address == null ? null : address.getLine1(),
                address == null ? null : address.getLine2(),
                address == null ? null : address.getCity(),
                address == null ? null : address.getState(),
                address == null ? null : address.getPostalCode(),
                address == null ? null : address.getCountry());
    }
}
