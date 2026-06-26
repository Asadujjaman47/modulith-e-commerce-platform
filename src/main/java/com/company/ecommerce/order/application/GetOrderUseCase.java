package com.company.ecommerce.order.application;

import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.order.api.dto.OrderResponse;
import com.company.ecommerce.order.domain.Order;
import com.company.ecommerce.order.infrastructure.mapper.OrderMapper;
import com.company.ecommerce.order.infrastructure.persistence.OrderRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Returns the details of a single order. */
@Service
@RequiredArgsConstructor
public class GetOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    /** Returns an order owned by the given customer, or 404 if it does not exist / is not theirs. */
    @Transactional(readOnly = true)
    public OrderResponse getForCustomer(UUID customerId, UUID orderId) {
        return orderMapper.toResponse(
                orderRepository
                        .findByIdAndCustomerId(orderId, customerId)
                        .orElseThrow(() -> new EntityNotFoundException("Order", orderId)));
    }

    /** Returns any order by id (admin use), or 404 if it does not exist. */
    @Transactional(readOnly = true)
    public OrderResponse getById(UUID orderId) {
        Order order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(() -> new EntityNotFoundException("Order", orderId));
        return orderMapper.toResponse(order);
    }
}
