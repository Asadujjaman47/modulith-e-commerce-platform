package com.company.ecommerce.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ecommerce.common.api.PageResponse;
import com.company.ecommerce.order.api.dto.OrderSummaryResponse;
import com.company.ecommerce.order.domain.Order;
import com.company.ecommerce.order.domain.OrderStatus;
import com.company.ecommerce.order.infrastructure.mapper.OrderMapper;
import com.company.ecommerce.order.infrastructure.persistence.OrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ListOrdersUseCaseTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderMapper orderMapper;
    @InjectMocks private ListOrdersUseCase useCase;

    private final UUID customerId = UUID.randomUUID();
    private final Pageable pageable = PageRequest.of(0, 20);

    private OrderSummaryResponse summary() {
        return new OrderSummaryResponse(
                UUID.randomUUID(), "ORD-1", OrderStatus.PENDING, 1, new BigDecimal("10.00"),
                Instant.now());
    }

    @Test
    void listsCustomerOrdersWithoutStatusFilter() {
        Order order = Order.place("ORD-1", customerId, "USD", null, BigDecimal.ZERO, null);
        when(orderRepository.findByCustomerId(customerId, pageable))
                .thenReturn(new PageImpl<>(List.of(order)));
        when(orderMapper.toSummaryResponse(order)).thenReturn(summary());

        PageResponse<OrderSummaryResponse> result =
                useCase.listForCustomer(customerId, null, pageable);

        assertThat(result.content()).hasSize(1);
        verify(orderRepository).findByCustomerId(customerId, pageable);
    }

    @Test
    void listsCustomerOrdersFilteredByStatus() {
        when(orderRepository.findByCustomerIdAndStatus(customerId, OrderStatus.CANCELLED, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        useCase.listForCustomer(customerId, OrderStatus.CANCELLED, pageable);

        verify(orderRepository).findByCustomerIdAndStatus(customerId, OrderStatus.CANCELLED, pageable);
    }

    @Test
    void adminListsAllOrders() {
        when(orderRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of()));

        useCase.listAll(null, pageable);

        verify(orderRepository).findAll(pageable);
    }

    @Test
    void adminListsByStatus() {
        when(orderRepository.findByStatus(OrderStatus.PENDING, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        useCase.listAll(OrderStatus.PENDING, pageable);

        verify(orderRepository).findByStatus(OrderStatus.PENDING, pageable);
    }
}
