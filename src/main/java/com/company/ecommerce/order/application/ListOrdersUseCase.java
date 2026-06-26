package com.company.ecommerce.order.application;

import com.company.ecommerce.common.api.PageResponse;
import com.company.ecommerce.order.api.dto.OrderSummaryResponse;
import com.company.ecommerce.order.domain.Order;
import com.company.ecommerce.order.domain.OrderStatus;
import com.company.ecommerce.order.infrastructure.mapper.OrderMapper;
import com.company.ecommerce.order.infrastructure.persistence.OrderRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lists orders as summaries, for the customer's own order history and for admin order management. */
@Service
@RequiredArgsConstructor
public class ListOrdersUseCase {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    /** The authenticated customer's orders, newest control left to the supplied {@link Pageable}. */
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> listForCustomer(
            UUID customerId, OrderStatus status, Pageable pageable) {
        Page<Order> page =
                status == null
                        ? orderRepository.findByCustomerId(customerId, pageable)
                        : orderRepository.findByCustomerIdAndStatus(customerId, status, pageable);
        return PageResponse.from(page.map(orderMapper::toSummaryResponse));
    }

    /** All orders (admin), optionally filtered by status. */
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> listAll(OrderStatus status, Pageable pageable) {
        Page<Order> page =
                status == null
                        ? orderRepository.findAll(pageable)
                        : orderRepository.findByStatus(status, pageable);
        return PageResponse.from(page.map(orderMapper::toSummaryResponse));
    }
}
